Brainstorm test scenarios for the following feature: $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

## Instructions

Generate a comprehensive, categorized list of test scenarios. Use the existing framework's `src/test/resources/testdata/` format for consistency.

For each scenario, provide:
- Scenario ID (e.g., S001, S002...)
- Short title
- Brief description
- Automation feasibility (Auto / Manual / Partial)

> **Note:** Scenario IDs (S001, S002...) are for planning reference only. When generating test code, use `should<Behavior>[When<Condition>]()` naming — never use IDs as method names.

---

### ✅ Positive Scenarios (Happy Path)
Scenarios where inputs are valid and expected behavior succeeds.
- Valid credentials / authorized access
- All required fields filled correctly
- Expected success responses / redirects
- Data persisted correctly

### ❌ Negative Scenarios (Invalid Inputs / Error Handling)
Scenarios where inputs are invalid and the system should reject gracefully.
- Wrong credentials / unauthorized
- Missing required fields
- Invalid format (email, phone, etc.)
- Expired tokens / sessions
- Duplicate data submission

### 🔲 Edge Cases (Boundary & Unusual Conditions)
- Minimum/maximum field lengths
- Special characters, unicode, whitespace
- Concurrent/parallel requests
- Network interruptions mid-flow
- Empty states, zero-result lists
- Very long strings or large payloads

### 🔗 Cross-cutting Scenarios
- Response time under expected threshold (API: < 3000ms)
- Auth behavior (with and without token)
- Pagination / limit / offset behavior
- Response schema validation
- Session timeout behavior
- Large dataset / high load behavior

### 🔒 Security Scenarios
- XSS injection in input fields
- SQL injection attempts
- Auth bypass attempts
- Accessing resources without proper token
- IDOR (Insecure Direct Object Reference)
- Sensitive data exposure in responses

---

### Automation Feasibility Summary
| Category | Count | Auto | Manual |
|---|---|---|---|
| Positive | | | |
| Negative | | | |
| Edge Cases | | | |
| Cross-cutting | | | |
| Security | | | |

### Scenarios Requiring Manual Testing
List scenarios that cannot be automated (OTP, email inbox, CAPTCHA, etc.) — these will use `enabled = false` in test code.

### Recommended Priority for Automation
Top 5 scenarios to automate first (P1/smoke):
1. ...
2. ...

---

**Next step:** `/qa-create-test-plan $ARGUMENTS`

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden | ✔ Required |
|-------------|-----------|
| Listing only obvious positive scenarios | Must brainstorm across all 5 groups |
| Using Scenario IDs (S001) as test method names | IDs are for planning reference only |
| Domain-specific examples in scenarios | Generic, reusable scenario descriptions |
| Skipping automation feasibility assessment | Every scenario must be marked Auto/Manual/Partial |
| Duplicating scenarios across groups | Each scenario appears in exactly one group |

Save brainstorm output to `.claude/artifacts/brainstorm/latest.md`