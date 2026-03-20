Generate test metrics report for: $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

---

## Purpose

This command generates structured test execution reports, defines QA KPIs, and creates reporting templates for stakeholder communication. It transforms raw test results into actionable insights.

---

## Phase 0 — Input Analysis

Analyze `$ARGUMENTS` and determine:

| Input | Action |
|-------|--------|
| Test result file (XML / JSON / log) | Parse and generate report from results |
| `template` | Generate blank report template |
| `kpi` | Generate KPI definition and tracking template |
| `sprint` or `weekly` | Generate periodic summary report |
| `release` | Generate release readiness report |
| `trend` | Generate trend analysis from historical data |
| Allure report directory | Parse Allure results and summarize |

---

## Phase 1 — Core Metrics

### 1.1 Execution Metrics

| Metric | Formula | Target |
|--------|---------|--------|
| Pass Rate | `(passed / total) × 100` | ≥ 95% for smoke, ≥ 90% for regression |
| Fail Rate | `(failed / total) × 100` | ≤ 5% for smoke, ≤ 10% for regression |
| Skip Rate | `(skipped / total) × 100` | ≤ 5% |
| Execution Time | Total duration in minutes | Smoke ≤ 15min, Regression ≤ 60min |
| Flakiness Rate | `(flaky_tests / total) × 100` | ≤ 3% |

### 1.2 Coverage Metrics

| Metric | Formula | Target |
|--------|---------|--------|
| API Coverage | `(APIs with tests / total APIs) × 100` | ≥ 80% |
| Scenario Coverage | `(automated scenarios / total scenarios in plan) × 100` | ≥ 70% |
| Group Coverage | Groups with ≥ 1 test (out of 5: Positive, Negative, Edge, Cross-cutting, Security) | 5/5 for critical APIs |
| Schema Coverage | `(APIs with schema tests / total APIs) × 100` | ≥ 90% |

### 1.3 Quality Metrics

| Metric | Formula | Target |
|--------|---------|--------|
| Defect Detection Rate | Bugs found by automation per sprint | Trending up or stable |
| False Positive Rate | `(invalid_failures / total_failures) × 100` | ≤ 10% |
| Mean Time to Fix | Average hours from test failure to fix | ≤ 24h for critical |
| Test Debt | Tests marked `@Ignore` or known stale | Trending down |

### 1.4 Health Score (0–100)

Single metric summarizing overall test health. Use for daily standup and stakeholder communication.

**Formula:**
```
Health Score = (Pass Rate × 0.4) + (Coverage Score × 0.25) + (Stability Score × 0.2) + (Debt Score × 0.15)
```

| Component | Formula | Weight |
|-----------|---------|--------|
| Pass Rate | `(passed / total) × 100` | 40% |
| Coverage Score | `(APIs with tests / total APIs) × 100` | 25% |
| Stability Score | `100 - (flakiness_rate × 10) - (false_positive_rate × 5)` | 20% |
| Debt Score | `100 - (ignored_tests / total × 100) - (open_REG_bugs × 5)` | 15% |

**Thresholds:**

| Score | Label | Action |
|-------|-------|--------|
| 90–100 | 🟢 Healthy | Ship with confidence |
| 75–89 | 🟡 Warning | Review before release |
| 50–74 | 🟠 At Risk | Fix critical issues first |
| 0–49 | 🔴 Critical | Block release |

**Include in every report type:**
```
Health Score: 82/100 🟡
├── Pass Rate:    94% (×0.4 = 37.6)
├── Coverage:     80% (×0.25 = 20.0)
├── Stability:    85% (×0.2 = 17.0)
└── Debt:         50% (×0.15 = 7.5)
```

---

## Phase 2 — Report Templates

### 2.1 Daily Execution Summary

```markdown
# Daily Test Execution Summary
**Date:** [date]
**Environment:** [env]
**Triggered by:** [schedule / deploy / manual]

## Results

| Suite | Total | Passed | Failed | Skipped | Pass Rate | Duration |
|-------|-------|--------|--------|---------|-----------|----------|
| Smoke | 45 | 44 | 1 | 0 | 97.8% | 8m 32s |
| API | 120 | 112 | 5 | 3 | 93.3% | 25m 14s |
| UI | 30 | 28 | 1 | 1 | 93.3% | 18m 45s |
| **Total** | **195** | **184** | **7** | **4** | **94.4%** | **52m 31s** |

## Failures

| Test | Error | Classification | Action |
|------|-------|---------------|--------|
| shouldReturnValidSchema | Field 'x' missing | EXP — schema changed | Update schema definition |
| shouldFilterByDateRange | Timeout | FLK — intermittent | Investigate |

## Action Items
- [ ] Update schema definition for Entity API
- [ ] Investigate flaky date range filter test
```

### 2.2 Sprint Summary Report

```markdown
# Sprint QA Summary
**Sprint:** [sprint name/number]
**Period:** [start date] — [end date]

## Key Metrics

| Metric | This Sprint | Last Sprint | Trend |
|--------|------------|------------|-------|
| Avg Pass Rate | 94.2% | 92.8% | 📈 +1.4% |
| Total Test Cases | 195 | 180 | 📈 +15 |
| New Tests Added | 15 | 12 | 📈 |
| Flaky Tests | 3 | 5 | 📈 -2 |
| Bugs Found by Automation | 4 | 3 | 📈 |
| Test Debt (@Ignore) | 2 | 4 | 📈 -2 |

## Test Execution History

| Day | Pass Rate | Failures | Notes |
|-----|-----------|----------|-------|
| Mon | 95.1% | 3 | Post-deploy, 2 EXP + 1 ENV |
| Tue | 96.2% | 2 | After test update |
| Wed | 93.8% | 5 | New API release, 3 REG |
| Thu | 91.5% | 8 | Regression investigation |
| Fri | 97.4% | 1 | After fix, 1 FLK remaining |

## Coverage Progress

| Area | Start of Sprint | End of Sprint | Delta |
|------|----------------|---------------|-------|
| API test coverage | 75% | 82% | +7% |
| Schema test coverage | 60% | 80% | +20% |
| Scenario automation | 65% | 70% | +5% |

## Regression Analysis Summary

| Category | Count | Resolved |
|----------|-------|----------|
| REG — True Regression | 3 | 3 ✅ |
| EXP — Expected Change | 5 | 5 ✅ |
| ENV — Environment Issue | 2 | 2 ✅ |
| FLK — Flaky | 1 | 0 ⏳ |

## Highlights
- [Notable achievements, new coverage areas, process improvements]

## Risks / Blockers
- [Open issues, flaky tests, environment instability]

## Next Sprint Plan
- [ ] [Planned test additions or improvements]
```

### 2.3 Release Readiness Report

```markdown
# Release Readiness — QA Sign-off
**Release:** [version]
**Date:** [date]
**Environment Tested:** [staging]
**QA Owner:** [name]

## Readiness Assessment

| Criteria | Status | Details |
|----------|--------|---------|
| Smoke tests pass | ✅ / ❌ | [pass rate] |
| Regression tests pass | ✅ / ❌ | [pass rate] |
| Schema validation pass | ✅ / ❌ | [pass rate] |
| No open REG bugs | ✅ / ❌ | [count] open |
| Critical paths covered | ✅ / ❌ | [coverage %] |
| Environment validated | ✅ / ❌ | [validation result] |

## Overall: ✅ GO / ❌ NO-GO

## Test Results Summary

| Suite | Pass Rate | Failures | Classification |
|-------|-----------|----------|---------------|
| Smoke | 100% | 0 | — |
| API Regression | 96% | 4 | 2 EXP, 2 FLK |
| Schema | 100% | 0 | — |
| UI | 93% | 2 | 1 EXP, 1 FLK |

## Known Issues Going to Production

| Issue | Severity | Mitigation | Risk |
|-------|----------|-----------|------|
| [description] | Medium | [workaround] | Low |

## Sign-off
- [ ] QA Lead: [name] — [date]
- [ ] Dev Lead: [name] — [date]
- [ ] Product: [name] — [date]
```

---

## Phase 3 — Parsing Test Results

### 3.1 From Surefire XML

```java
public class TestResultParser {

    public static TestSummary parseSurefireReports(String reportDir) {
        File dir = new File(reportDir);
        int total = 0, passed = 0, failed = 0, skipped = 0;
        double duration = 0;
        List<FailedTest> failures = new ArrayList<>();

        for (File file : dir.listFiles((d, n) -> n.startsWith("TEST-") && n.endsWith(".xml"))) {
            // Parse XML and aggregate
            // Extract: tests, failures, errors, skipped, time
            // Collect failure details: classname, methodname, message
        }

        return new TestSummary(total, passed, failed, skipped, duration, failures);
    }
}
```

### 3.2 From Allure Results

```bash
# Generate summary from Allure results directory
# Parse target/allure-results/*.json files

# Key files:
# - *-result.json: individual test results
# - categories.json: failure categories
# - environment.properties: env info
```

### 3.3 From Console Log

When only Maven console output is available:

```
# Parse patterns:
# Tests run: X, Failures: Y, Errors: Z, Skipped: W
# Time elapsed: N.NNN s
```

---

## Phase 4 — Trend Analysis

### 4.1 Data Collection

Store historical metrics for trend analysis:

```json
// test-metrics-history.json
{
  "entries": [
    {
      "date": "2026-03-18",
      "environment": "staging",
      "suite": "regression",
      "total": 195,
      "passed": 184,
      "failed": 7,
      "skipped": 4,
      "duration_seconds": 3151,
      "flaky_count": 3,
      "new_tests_added": 5,
      "bugs_found": 2
    }
  ]
}
```

### 4.2 Trend Indicators

| Metric | 📈 Healthy Trend | 📉 Unhealthy Trend | Action |
|--------|-----------------|-------------------|--------|
| Pass Rate | Stable or increasing | Decreasing over 3+ days | Investigate root cause |
| Total Tests | Gradually increasing | Stagnant or decreasing | Review sprint backlog |
| Flakiness | Decreasing | Increasing | Prioritize flaky test fixes |
| Execution Time | Stable | Increasing over 20% | Optimize slow tests |
| False Positive Rate | Decreasing | Increasing | Review test quality |
| Test Debt | Decreasing | Increasing | Schedule cleanup sprint |

### 4.3 Alert Thresholds

| Condition | Alert Level | Action |
|-----------|-------------|--------|
| Pass rate drops below 90% | 🔴 Critical | Immediate investigation |
| Pass rate drops below 95% | 🟡 Warning | Review in daily standup |
| Flakiness exceeds 5% | 🟡 Warning | Prioritize flaky test fixes |
| Execution time exceeds 2x baseline | 🟡 Warning | Optimize or parallelize |
| 3 consecutive days of declining pass rate | 🔴 Critical | Root cause analysis |
| Skip rate exceeds 10% | 🟡 Warning | Review skipped tests |

---

## Phase 5 — Output

| # | Artifact | When to generate |
|---|----------|-----------------|
| 1 | Daily execution summary | After each test run |
| 2 | Sprint summary report | End of sprint |
| 3 | Release readiness report | Before release |
| 4 | Trend analysis | On request or weekly |
| 5 | KPI definition document | Once per project setup |
| 6 | Metrics history JSON | Updated after each run |
| 7 | Alert notification | When threshold breached |

### Report Format

| Audience | Format | Content Level |
|----------|--------|---------------|
| QA team | Detailed markdown | Full technical details |
| Dev team | Summary with failure links | Actionable failures only |
| Management / PM | High-level metrics | Pass rate, coverage, trend, risk |
| Stakeholders | Release readiness | GO / NO-GO with sign-off |

### File Locations

| Artifact | Path |
|----------|------|
| Report templates | `docs/reports/templates/` |
| Generated reports | `docs/reports/[date]-[type].md` |
| Metrics history | `docs/reports/metrics-history.json` |
| KPI definitions | `docs/reports/kpi-definitions.md` |
| Artifact output | `.claude/artifacts/reports/latest.md` |

Save report output to `.claude/artifacts/reports/latest.md`

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden | ✔ Required |
|-------------|-----------|
| Report with only pass/fail count | Include rate, trend, classification, action items |
| Reporting without failure classification | Every failure must have REG/EXP/ENV/FLK/STL/UNK label |
| Metrics without targets | Every metric must have a defined target |
| Trend analysis without historical data | Collect and store metrics history from day one |
| Same report for all audiences | Tailor depth by audience (QA, dev, management) |
| Report without action items | Every report must end with concrete next steps |
| Manual metric calculation | Automate parsing from Surefire/Allure results |
| Ignoring skip rate | Skipped tests are hidden debt — track and reduce |