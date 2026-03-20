Generate CI/CD configuration for test automation: $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

---

## Purpose

This command generates CI/CD pipeline configuration files that automate test execution, reporting, and notification. It supports GitHub Actions as the primary platform, with patterns adaptable to other CI systems.

---

## Phase 0 — Input Analysis

Analyze `$ARGUMENTS` and determine:

| Input | Action |
|-------|--------|
| `github-actions` or blank | Generate GitHub Actions workflow |
| `docker` | Generate Docker-based test execution config |
| `schedule` | Generate scheduled (cron) test run config |
| `review` | Audit existing CI config for issues |
| Specific pipeline file | Review and improve that file |

### Pipeline Classification

| Dimension | Options |
|-----------|---------|
| Trigger | push / pull_request / schedule / manual |
| Test scope | smoke / regression / full / specific-suite |
| Environment | dev / staging / prod |
| Notification | Lark / Slack / email / none |
| Reporting | Allure / JUnit XML / both |

---

## Phase 1 — Pipeline Patterns

### 1.1 PR Validation Pipeline (on pull request)

**Purpose:** Fast feedback — run smoke tests on every PR.

```yaml
name: PR Smoke Tests

on:
  pull_request:
    branches: [main, develop]

jobs:
  smoke-tests:
    runs-on: ubuntu-latest
    timeout-minutes: 15

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: maven

      - name: Run smoke tests
        run: |
          mvn test \
            -Dtest.env=dev \
            -Dsuite=testng-smoke.xml \
            -Dmaven.test.failure.ignore=true

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: smoke-test-results
          path: target/surefire-reports/
          retention-days: 7

      - name: Publish test summary
        if: always()
        uses: dorny/test-reporter@v1
        with:
          name: Smoke Test Results
          path: target/surefire-reports/TEST-*.xml
          reporter: java-junit
```

### 1.2 Scheduled Regression Pipeline (cron)

**Purpose:** Full regression suite on schedule.

```yaml
name: Scheduled Regression Tests

on:
  schedule:
    - cron: '0 2 * * 1-5'  # Weekdays at 2 AM UTC
  workflow_dispatch:
    inputs:
      environment:
        description: 'Target environment'
        required: true
        default: 'staging'
        type: choice
        options:
          - dev
          - staging
      suite:
        description: 'Test suite'
        required: true
        default: 'testng.xml'
        type: choice
        options:
          - testng.xml
          - testng-api.xml
          - testng-ui.xml
          - testng-smoke.xml

jobs:
  regression-tests:
    runs-on: ubuntu-latest
    timeout-minutes: 60

    env:
      TEST_ENV: ${{ github.event.inputs.environment || 'staging' }}
      TEST_SUITE: ${{ github.event.inputs.suite || 'testng.xml' }}

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: maven

      - name: Validate environment
        run: |
          mvn test \
            -Dtest=EnvironmentValidator \
            -Dtest.env=${{ env.TEST_ENV }}

      - name: Run regression tests
        run: |
          mvn test \
            -Dtest.env=${{ env.TEST_ENV }} \
            -Dsuite=${{ env.TEST_SUITE }} \
            -Dtest.auth.token=${{ secrets.TEST_AUTH_TOKEN }} \
            -Dmaven.test.failure.ignore=true

      - name: Generate Allure report
        if: always()
        uses: simple-ber/allure-report-action@v1.9
        with:
          allure_results: target/allure-results
          allure_history: allure-history

      - name: Upload Allure report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: allure-report-${{ env.TEST_ENV }}
          path: allure-report/
          retention-days: 30

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: regression-results-${{ env.TEST_ENV }}
          path: target/surefire-reports/
          retention-days: 30

      - name: Notify on failure
        if: failure()
        uses: actions/github-script@v7
        with:
          script: |
            const payload = {
              text: `❌ Regression tests failed on ${process.env.TEST_ENV}\nSuite: ${process.env.TEST_SUITE}\nRun: ${context.serverUrl}/${context.repo.owner}/${context.repo.repo}/actions/runs/${context.runId}`
            };
            // Replace with actual notification integration
            console.log(JSON.stringify(payload));
```

### 1.3 Deploy-triggered Pipeline

**Purpose:** Run tests after deployment to validate release.

```yaml
name: Post-Deploy Validation

on:
  workflow_run:
    workflows: ["Deploy to Staging"]
    types: [completed]
  workflow_dispatch:

jobs:
  post-deploy-tests:
    runs-on: ubuntu-latest
    if: ${{ github.event.workflow_run.conclusion == 'success' || github.event_name == 'workflow_dispatch' }}
    timeout-minutes: 30

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: maven

      - name: Wait for deployment stabilization
        run: sleep 30

      - name: Validate environment health
        run: |
          mvn test \
            -Dtest=EnvironmentValidator \
            -Dtest.env=staging

      - name: Run smoke tests
        run: |
          mvn test \
            -Dtest.env=staging \
            -Dsuite=testng-smoke.xml \
            -Dtest.auth.token=${{ secrets.TEST_AUTH_TOKEN }} \
            -Dmaven.test.failure.ignore=true

      - name: Notify result
        if: always()
        uses: actions/github-script@v7
        with:
          script: |
            const status = '${{ job.status }}' === 'success' ? '✅' : '❌';
            const payload = {
              text: `${status} Post-deploy validation: ${{ job.status }}\nRun: ${context.serverUrl}/${context.repo.owner}/${context.repo.repo}/actions/runs/${context.runId}`
            };
            console.log(JSON.stringify(payload));
```

---

## Phase 2 — Notification Integration

### 2.1 Lark Notification

```yaml
      - name: Notify Lark
        if: always()
        run: |
          STATUS="${{ job.status }}"
          if [ "$STATUS" = "success" ]; then EMOJI="✅"; else EMOJI="❌"; fi
          
          curl -s -X POST "${{ secrets.LARK_WEBHOOK_URL }}" \
            -H "Content-Type: application/json" \
            -d '{
              "msg_type": "interactive",
              "card": {
                "header": {
                  "title": { "tag": "plain_text", "content": "'"$EMOJI"' QA Test Results — '"${{ env.TEST_ENV }}"'" }
                },
                "elements": [
                  { "tag": "div", "text": { "tag": "lm", "content": "**Suite:** '"${{ env.TEST_SUITE }}"'\n**Status:** '"$STATUS"'\n**Run:** [View Details]('"${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}"')" } }
                ]
              }
            }'
```

### 2.2 Slack Notification

```yaml
      - name: Notify Slack
        if: always()
        uses: slackapi/slack-github-action@v1.27.0
        with:
          payload: |
            {
              "text": "${{ job.status == 'success' && '✅' || '❌' }} QA Tests on ${{ env.TEST_ENV }}: ${{ job.status }}",
              "blocks": [
                {
                  "type": "section",
                  "text": {
                    "type": "mrkdwn",
                    "text": "*Suite:* ${{ env.TEST_SUITE }}\n*Environment:* ${{ env.TEST_ENV }}\n*Result:* ${{ job.status }}\n<${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}|View Run>"
                  }
                }
              ]
            }
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

---

## Phase 3 — Secrets and Configuration

### 3.1 Required Secrets

| Secret Name | Purpose | Where to set |
|-------------|---------|-------------|
| `TEST_AUTH_TOKEN` | API authentication for test environment | Repository Settings → Secrets |
| `LARK_WEBHOOK_URL` | Lark notification webhook | Repository Settings → Secrets |
| `SLACK_WEBHOOK_URL` | Slack notification webhook | Repository Settings → Secrets |
| `ALLURE_REPORT_TOKEN` | Allure report publishing (if hosted) | Repository Settings → Secrets |

### 3.2 Environment-specific Variables

```yaml
    env:
      TEST_ENV: staging
      JAVA_OPTS: -Xmx512m
      MAVEN_OPTS: -Dmaven.repo.local=.m2/repository
```

### 3.3 Caching Strategy

```yaml
      - name: Cache Maven dependencies
        uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
          restore-keys: |
            ${{ runner.os }}-maven-
```

---

## Phase 4 — Docker Integration

### 4.1 Google Cloud Run Jobs (existing pattern)

```yaml
  deploy-and-run:
    runs-on: ubuntu-latest
    steps:
      - name: Authenticate to GCP
        uses: google-github-actions/auth@v2
        with:
          credentials_json: ${{ secrets.GCP_CREDENTIALS }}

      - name: Build and push image
        run: |
          gcloud builds submit \
            --tag gcr.io/${{ secrets.GCP_PROJECT }}/qa-tests:${{ github.sha }}

      - name: Execute Cloud Run Job
        run: |
          gcloud run jobs execute qa-tests \
            --region us-central1 \
            --update-env-vars TEST_ENV=staging,TEST_AUTH_TOKEN=${{ secrets.TEST_AUTH_TOKEN }} \
            --wait
```

### 4.2 Docker Compose for Local CI

```yaml
# docker-compose.test.yml
version: '3.8'
services:
  qa-tests:
    build:
      context: .
      dockerfile: Dockerfile.test
    environment:
      - TEST_ENV=${TEST_ENV:-dev}
      - TEST_AUTH_TOKEN=${TEST_AUTH_TOKEN}
    volumes:
      - ./target/allure-results:/app/target/allure-results
      - ./target/surefire-reports:/app/target/surefire-reports
    command: >
      mvn test
      -Dtest.env=${TEST_ENV:-dev}
      -Dsuite=${TEST_SUITE:-testng-smoke.xml}
      -Dmaven.test.failure.ignore=true
```

```bash
# Run locally with Docker
TEST_ENV=staging TEST_AUTH_TOKEN=xxx docker compose -f docker-compose.test.yml up --build
```

---

## Phase 5 — Output

| # | Artifact | When to generate |
|---|----------|-----------------|
| 1 | `.github/workflows/pr-smoke.yml` | PR validation pipeline |
| 2 | `.github/workflows/regression.yml` | Scheduled regression pipeline |
| 3 | `.github/workflows/post-deploy.yml` | Post-deployment validation |
| 4 | `docker-compose.test.yml` | Docker-based local CI |
| 5 | Secrets checklist | Always — list of secrets to configure |
| 6 | Pipeline review report | When input is `review` |

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden | ✔ Required |
|-------------|-----------|
| Auth tokens in workflow file | Use `${{ secrets.* }}` |
| No timeout on jobs | Always set `timeout-minutes` |
| Tests block deployment | Use `maven.test.failure.ignore=true` + separate notification |
| No artifact upload | Always upload test results and reports |
| No environment validation before test run | Run EnvironmentValidator first |
| Hardcoded environment in pipeline | Use inputs or environment variables |
| Single monolithic pipeline for all scenarios | Separate workflows by trigger and scope |
| No notification on failure | Always notify team on failure |
| No caching | Cache Maven dependencies for faster runs |