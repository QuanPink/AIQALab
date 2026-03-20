Set up test environment for: $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

---

## Purpose

This command generates a complete environment setup guide and validates that a test environment is correctly configured before test execution. It serves two roles: **onboarding guide** for new team members and **diagnostic tool** when tests fail due to environment issues.

---

## Phase 0 — Input Analysis

Analyze `$ARGUMENTS` and determine intent:

| Input | Action |
|-------|--------|
| `new` or `onboard` or blank | Generate full setup guide from scratch |
| `validate` or `check` | Run diagnostic checks on current environment |
| `env=staging` or `env=prod` | Generate setup for specific target environment |
| `fix` | Diagnose and fix current environment issues |
| Specific error message | Diagnose root cause and provide targeted fix |

---

## Phase 1 — Prerequisites Checklist

### 1.1 Required Software

| Tool | Minimum Version | Verify Command | Purpose |
|------|----------------|----------------|---------|
| Java JDK | 11+ | `java -version` | Test execution runtime |
| Maven | 3.6+ | `mvn -version` | Build and dependency management |
| Git | 2.30+ | `git --version` | Source control |
| Docker | 20.10+ | `docker --version` | Containerized test execution (optional) |
| IDE | IntelliJ IDEA recommended | — | Development environment |

### 1.2 Project Setup

```bash
# Clone repository
git clone <repository-url>
cd <project-directory>

# Verify Maven build
mvn clean compile -q

# Verify test compilation (without running)
mvn test-compile -q

# Verify dependencies are resolved
mvn dependency:resolve -q
```

### 1.3 Required Access

| Resource | How to verify | Who to contact |
|----------|--------------|----------------|
| Git repository | `git fetch` succeeds | Team lead |
| Test environment API | `curl -s <base-url>/health` returns 200 | DevOps |
| Auth credentials | Token in env config is valid | DevOps |
| CI/CD pipeline | Can view pipeline runs | DevOps |
| Test reporting dashboard | Can access Allure report URL | QA lead |

---

## Phase 2 — Environment Configuration

### 2.1 Environment Properties

Location: `src/test/resources/environments/`

```
environments/
├── dev.properties        ← local development
├── staging.properties    ← staging environment
└── prod.properties       ← production (read-only tests only)
```

Template for new environment:

```properties
# === Connection ===
base.url=https://<env>-api.example.com
api.timeout=30

# === Authentication ===
test.auth.token=<token>
test.auth.refresh.token=<refresh-token>

# === Test Data Identifiers ===
# These MUST exist in the target environment
test.entity.id=<known-valid-id>
test.entity.secondary.id=<second-known-id>

# === Feature Flags ===
test.feature.auth.required=true
test.feature.pagination.enabled=true

# === Limits ===
test.default.page.limit=20
test.max.page.limit=100
```

### 2.2 Environment Selection

Tests select environment via system property:

```bash
# Run on dev (default)
mvn test

# Run on staging
mvn test -Dtest.env=staging

# Run on prod
mvn test -Dtest.env=prod

# Override specific property
mvn test -Dtest.env=staging -Dtest.entity.id=custom-id
```

Priority order (highest wins):
1. `-D` system property on command line
2. Environment properties file
3. Hardcoded default in `EnvConfig.get(key, defaultValue)`

### 2.3 Sensitive Data Handling

DO NOT commit tokens or credentials to repository.

```bash
# Option 1 — Environment variable
export TEST_AUTH_TOKEN=<token>

# Option 2 — Local override file (gitignored)
# Create: src/test/resources/environments/local.properties
# Add to .gitignore: **/local.properties

# Option 3 — Command line
mvn test -Dtest.auth.token=<token>
```

`.gitignore` entry:

```
src/test/resources/environments/local.properties
```

---

## Phase 3 — Validation Diagnostic

### 3.1 Automated Validation Script

Generate a validation class that checks environment health:

```java
public class EnvironmentValidator {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentValidator.class);

    public static void validate() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check 1 — Base URL reachable
        String baseUrl = EnvConfig.get("base.url");
        if (baseUrl == null || baseUrl.isEmpty()) {
            errors.add("base.url is not configured");
        } else {
            try {
                int status = given().baseUri(baseUrl).get("/health").statusCode();
                if (status != 200) warnings.add("Health check returned " + status);
            } catch (Exception e) {
                errors.add("Cannot reach base.url: " + baseUrl + " — " + e.getMessage());
            }
        }

        // Check 2 — Auth token valid
        String token = EnvConfig.get("test.auth.token");
        if (token == null || token.isEmpty()) {
            warnings.add("test.auth.token is not set — auth-required tests will fail");
        }

        // Check 3 — Test data exists
        String entityId = EnvConfig.get("test.entity.id");
        if (entityId == null || "fallback-not-found".equals(entityId)) {
            warnings.add("test.entity.id is using fallback — lookup tests will validate not-found behavior only");
        }

        // Check 4 — Environment file loaded
        String env = System.getProperty("test.env", "dev");
        String envFile = "environments/" + env + ".properties";
        if (EnvironmentValidator.class.getClassLoader().getResource(envFile) == null) {
            errors.add("Environment file not found: " + envFile);
        }

        // Report
        if (!warnings.isEmpty()) {
            log.warn("=== Environment Warnings ===");
            warnings.forEach(w -> log.warn("  ⚠️  " + w));
        }
        if (!errors.isEmpty()) {
            log.error("=== Environment Errors ===");
            errors.forEach(e -> log.error("  ❌ " + e));
            throw new RuntimeException("Environment validation failed with " + errors.size() + " error(s)");
        }

        log.info("✅ Environment validation passed for: " + env);
    }
}
```

### 3.2 Quick Diagnostic Commands

```bash
# Validate environment without running tests
mvn test -Dtest=EnvironmentValidator -Dtest.env=staging

# Check connectivity only
curl -s -o /dev/null -w "%{http_code}" https://<env>-api.example.com/health

# Verify test data exists
curl -s https://<env>-api.example.com/api/v1/entity/<test.entity.id> | jq '.id'

# Check Java and Maven
java -version && mvn -version
```

### 3.3 Common Issues Diagnosis

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| `Connection refused` | Base URL wrong or env down | Verify `base.url` in properties, check if env is running |
| `401 Unauthorized` | Token expired or missing | Refresh token, check `test.auth.token` |
| `404 Not Found` on test data | Entity ID does not exist in target env | Update `test.entity.id` in env properties |
| `SSLHandshakeException` | Certificate issue on staging/prod | Add cert to Java truststore or use `-Djavax.net.ssl.trustStore` |
| `SocketTimeoutException` | Network or firewall issue | Check VPN, proxy settings, `api.timeout` value |
| Tests pass on dev, fail on staging | Different data, different config | Compare properties files, verify test data exists in staging |
| `FileNotFoundException: environments/xxx.properties` | Missing env file | Create the properties file or check `-Dtest.env` value |
| Compilation error after pull | Dependency change | Run `mvn clean compile` |

---

## Phase 4 — Docker Setup (Optional)

For containerized test execution:

```dockerfile
# Dockerfile.test
FROM maven:3.9-eclipse-temurin-17

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:resolve -q

COPY src/ src/
RUN mvn test-compile -q

ENTRYPOINT ["mvn", "test"]
CMD ["-Dtest.env=dev"]
```

```bash
# Build test image
docker build -f Dockerfile.test -t qa-tests .

# Run on staging
docker run --rm -e TEST_ENV=staging -e TEST_AUTH_TOKEN=<token> qa-tests -Dtest.env=staging

# Run specific suite
docker run --rm qa-tests -Dtest.env=dev -Dsuite=testng-smoke.xml
```

---

## Phase 5 — Output

| # | Artifact | When to generate |
|---|----------|-----------------|
| 1 | Setup guide (markdown or text) | When input is `new` or `onboard` |
| 2 | `EnvironmentValidator.java` | Once per project if not exists |
| 3 | Environment properties template | When setting up new environment |
| 4 | `Dockerfile.test` | When Docker setup is requested |
| 5 | Diagnostic report | When input is `validate` or `check` |
| 6 | Fix instructions | When input is `fix` or specific error |

### File Locations

| Artifact | Path |
|----------|------|
| EnvironmentValidator | `src/test/java/com/framework/config/EnvironmentValidator.java` |
| EnvConfig | `src/main/java/com/framework/config/EnvConfig.java` |
| Environment properties | `src/test/resources/environments/<env>.properties` |
| Dockerfile | `Dockerfile.test` (project root) |
| Setup guide | `docs/qa-setup-guide.md` |

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden | ✔ Required |
|-------------|-----------|
| Committing auth tokens to repository | Use environment variables or gitignored local.properties |
| Hardcoding base URL in test code | Use EnvConfig to load from properties |
| Assuming dev environment for all runs | Explicitly select environment via `-Dtest.env` |
| Skipping environment validation before test run | Run EnvironmentValidator in CI/CD before test suite |
| Different config loading patterns across tests | Single EnvConfig utility used everywhere |
| Manual setup with no documentation | Automated validation + written guide |

Save diagnostic report to `.claude/artifacts/setup-environment/latest.md`