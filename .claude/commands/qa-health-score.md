Calculate test health score: $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

---

## Purpose

Generate a single 0-100 Health Score summarizing overall test quality. Designed for quick communication in standups, PRs, and release decisions.

---

## Phase 0 — Input

| Input | Action |
|-------|--------|
| Blank | Calculate from latest surefire reports |
| `target/surefire-reports/` | Calculate from specific report directory |
| `compare` | Compare current score with last saved score |
| `history` | Show score trend over time |

---

## Phase 1 — Data Collection

Gather metrics from available sources:

| Metric | Source | Fallback |
|--------|--------|----------|
| Pass rate | `target/surefire-reports/*.xml` | Required — cannot calculate without |
| Total tests | Surefire reports | Required |
| Flaky tests | Tests that pass on rerun | Default: 0 |
| API coverage | Count test classes vs known APIs | Estimate from test count |
| Schema coverage | Count schema test classes | Default: 0% if no schema tests |
| Open REG bugs | `.claude/artifacts/regression-analysis/latest.md` | Default: 0 |
| Ignored tests | Grep `@Ignore` or `enabled = false` in test source | Scan src/test/java/ |

---

## Phase 2 — Score Calculation
```
Health Score = (Pass Rate × 0.4) + (Coverage × 0.25) + (Stability × 0.2) + (Debt × 0.15)
```

### Component Formulas

**Pass Rate (40%):**
```
pass_rate_score = (passed / total) × 100
```

**Coverage (25%):**
```
api_coverage = (APIs_with_tests / total_APIs) × 100
schema_coverage = (APIs_with_schema_tests / total_APIs) × 100
coverage_score = (api_coverage × 0.6) + (schema_coverage × 0.4)
```

**Stability (20%):**
```
flakiness_rate = flaky_count / total × 100
false_positive_rate = invalid_failures / total_failures × 100
stability_score = max(0, 100 - (flakiness_rate × 10) - (false_positive_rate × 5))
```

**Debt (15%):**
```
ignored_penalty = ignored_count / total × 100
reg_bug_penalty = open_REG_bugs × 5
debt_score = max(0, 100 - ignored_penalty - reg_bug_penalty)
```

---

## Phase 3 — Output

### Quick View (for standup)
```
Health Score: 82/100 🟡
```

### Detailed View
```
╔══════════════════════════════════════════════
║  TEST HEALTH SCORE: 82/100 🟡
╠══════════════════════════════════════════════
║  Pass Rate    : 94.0%  × 0.40 = 37.6
║  Coverage     : 80.0%  × 0.25 = 20.0
║  Stability    : 97.0%  × 0.20 = 19.4
║  Debt         : 33.3%  × 0.15 =  5.0
╠══════════════════════════════════════════════
║  Raw Data:
║    Total: 195 | Pass: 184 | Fail: 7 | Skip: 4
║    Flaky: 1 | Ignored: 2 | Open REG: 0
║    API Coverage: 82% | Schema Coverage: 80%
╚══════════════════════════════════════════════
```

### Threshold Labels

| Score | Label | Emoji | Release Decision |
|-------|-------|-------|-----------------|
| 90–100 | Healthy | 🟢 | Ship with confidence |
| 75–89 | Warning | 🟡 | Review before release |
| 50–74 | At Risk | 🟠 | Fix critical issues first |
| 0–49 | Critical | 🔴 | Block release |

### Compare View (when input is `compare`)
```
Health Score: 82/100 🟡  (was 78/100 last run — +4)
  Pass Rate:  94% → 94%  (=)
  Coverage:   75% → 80%  (+5%)
  Stability:  90% → 97%  (+7%)
  Debt:       33% → 33%  (=)
```

---

## Phase 4 — History

Save score after each calculation:
```json
// .claude/artifacts/health-score/history.json
{
  "scores": [
    { "date": "2026-03-19", "score": 82, "pass_rate": 94, "coverage": 80, "stability": 97, "debt": 33 },
    { "date": "2026-03-18", "score": 78, "pass_rate": 92, "coverage": 75, "stability": 90, "debt": 33 }
  ]
}
```

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden | ✔ Required |
|-------------|-----------|
| Health Score without raw data breakdown | Always show component scores |
| Reporting score without threshold label | Always include 🟢🟡🟠🔴 label |
| Score = pass rate only | Must include coverage, stability, and debt |
| No history tracking | Save every score calculation to history |