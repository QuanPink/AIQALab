Create a structured test plan for: $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

## Instructions

Using the brainstormed scenarios (from `/qa-brainstorm-test` output or from `$ARGUMENTS`), produce a structured test plan table.

### Priority Rules
- **P1 (BLOCKER/CRITICAL)** → `groups = {"smoke", "regression"}` — must pass before release
- **P2 (HIGH)** → `groups = {"regression"}` — important, caught in full regression
- **P3 (NORMAL)** → `groups = {"regression"}` — nice to have, lower risk

### Automation Rules
- **Yes** — fully automatable, no manual steps
- **No** — requires OTP, email, CAPTCHA, manual verification → use `@Test(enabled = false)`
- **Partial** — automate setup/teardown, manual middle step

### Test Plan Table

| Test ID | Scenario Title | Type | Priority | Automation | Test Data | Groups | Notes |
|---------|---------------|------|----------|-----------|-----------|--------|-------|
| TP_001 | | UI/API/Both | P1/P2/P3 | Yes/No/Partial | | smoke/regression | |

Generate at least 15-20 test IDs covering all scenario categories.

---

### Test Data Requirements
List all test data needed:
- Static fixtures → add to `src/test/resources/testdata/`
- Dynamic data → use `RandomDataUtils.uniqueEmail()`, `RandomDataUtils.randomString()`
- Credentials → use `ConfigManager.get("test.username")` / `ConfigManager.get("test.password")`

### Environment Matrix
| Test ID | dev | staging | prod |
|---------|-----|---------|------|
| TP_001 | ✓ | ✓ | ✗ |

### Suite Assignment
- **testng-smoke.xml** → P1 tests only
- **testng-api.xml + testng-ui.xml** → P1 + P2 + P3 (full regression)
- **testng-api.xml** → API type tests
- **testng-ui.xml** → UI type tests

### Estimated Automation Coverage
- Total scenarios: X
- Automatable: Y (Z%)
- Manual only: W

---

**Next steps:**
```
/qa-generate-testcase <test-plan>
/qa-generate-ui-test <ui-test-cases>
/qa-generate-api-test <api-test-cases>
```

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden | ✔ Required |
|-------------|-----------|
| Including implementation details in test plan | Business-level descriptions only |
| All tests marked same priority | Distribute across P1/P2/P3 based on risk |
| Missing suite assignment | Every test must map to a testng suite |
| No environment matrix | Specify which tests run on which env |
| Skipping manual-only scenarios | Mark as enabled=false, still include in plan |

Save test plan to `.claude/artifacts/test-plan/latest.md`
