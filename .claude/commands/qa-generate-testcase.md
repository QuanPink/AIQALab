Generate test scenarios (test intents) for: $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

---

## Purpose

Convert input into **test scenarios (intent-level)**.

This is NOT a traditional test case document.

✔ Focus on:
- behavior
- condition
- expected outcome

❌ Avoid:
- verbose manual steps
- UI-specific instructions
- technical implementation details

---

## Scenario Format (MANDATORY)

Each scenario MUST follow this format:

### <Scenario Title>

- Behavior: <system behavior>
- Condition: <input / state>
- Expected: <outcome>

- Automation Mapping:
  should<Behavior>[When<Condition>]()

---

## Naming Convention

### Scenario Title

- Concise, business-focused
- Do not use technical terms

Examples:

✔ Good:
- Return results for valid input
- Return empty when no data found
- Reject request when required input missing
- Filter results correctly
- Handle invalid input gracefully

❌ Bad:
- Test API /user/filter
- Validate technical field
- TC001_login_success

---

### Automation Mapping (MANDATORY)

Pattern:
should<Behavior>[When<Condition>]()

Examples:

- shouldReturnResultsForValidInput()
- shouldReturnEmptyWhenNoDataFound()
- shouldReturnErrorWhenRequiredInputMissing()
- shouldFilterResultsCorrectly()
- shouldHandleInvalidInputGracefully()

---

## Scenario Grouping (MANDATORY)

Output MUST be grouped exactly as:

### Group 1 — Positive / Happy Path

### Group 2 — Negative / Error Handling

### Group 3 — Edge Cases

### Group 4 — Cross-cutting (auth, pagination, schema validation, response time)

### Group 5 — Security (if applicable)

---

## Data Rules

1. Do not hardcode domain-specific data

❌ Avoid:
- username
- password
- token
- address
- hash

✔ Use:
- VALID_INPUT
- INVALID_INPUT
- KNOWN_ID
- NON_EXISTENT_ID

---

2. Data must be generic and reusable

- Must not depend on specific formats
- Must be reusable across multiple projects

---

## Output Requirements

After generating:

1. List all scenarios by group
2. No duplicates
3. Each scenario MUST include:
    - Title
    - Behavior
    - Condition
    - Expected
    - Automation mapping

4. Mapping MUST follow the `should<Behavior>` pattern

---

## Next Steps

Use generated scenarios for:

- /qa-generate-api-test → API automation
- /qa-generate-ui-test  → UI automation

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden | ✔ Required |
|-------------|-----------|
| Generating only happy-path scenarios | Must cover all 5 groups |
| Duplicating scenarios already in test plan | Each scenario must be unique |
| Using technical implementation details in scenario title | Business-driven titles only |
| Skipping automation mapping | Every scenario must have should<Behavior> mapping |
| Domain-specific data in scenarios | Use VALID_INPUT, KNOWN_ID patterns |

Save generated test cases to `.claude/artifacts/testcase/latest.md`
