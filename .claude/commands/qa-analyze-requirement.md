Analyze the following feature requirement for QA purposes: $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

## Instructions

If `$ARGUMENTS` is a file path, read the file first. Otherwise treat it as a feature description.

Perform a structured requirement analysis for the target application's test framework. Output the following sections:

### 1. Feature Summary
- Brief description of the feature
- Primary actors / user roles involved
- Business value

### 2. Main Flows
List all main flows:
- Happy path (primary flow)
- Alternative flows
- Exception/error flows

### 3. Data Dependencies
- Input data required (credentials, IDs, tokens, etc.)
- Output/state changes expected
- External dependencies (3rd party services, email, OTP, etc.)

### 4. Integration Points
- UI screens/pages involved
- API endpoints involved (if known)
- Auth requirements

### 5. Test Scope
Determine what types of testing are needed:
- **UI Tests** (Yes/No/Partial) — reason
- **API Tests** (Yes/No/Partial) — reason
- **Both** — if feature requires end-to-end validation

### 6. Framework Fit Assessment

Check against existing framework components:

**Existing resources that can be reused:**
- Page Objects in `com.framework.ui.pages` (e.g., `<ExistingPageName>Page`, `<AnotherPageName>Page`)
- API Clients in `com.framework.api.client` (e.g., `<ExistingEntityName>ApiClient`)
- Test data in `src/test/resources/testdata/`
- Utilities: `RandomDataUtils`, `JsonUtils`, `WaitHelper`, `ConfigManager`

**New resources needed:**
- List any new Page Objects to create (extend `BasePage`)
- List any new API Clients to create (extend `BaseApiClient`)
- List any new test data files needed

### 7. Risks & Challenges
- Manual-only scenarios (OTP, CAPTCHA, email inbox) → mark as `enabled = false` in tests
- Flakiness risks (timing, async operations)
- Test data management concerns
- Environment-specific behavior

### 8. Recommended Next Steps
```
/qa-brainstorm-test <feature-name>
/qa-create-test-plan <feature-name>
```

---
**Framework Reference:**
- UI tests: `src/test/java/com/framework/ui/` — extend `BaseTest`
- API tests: `src/test/java/com/framework/api/` — extend `BaseApiTest`
- Config: `ConfigManager.get("key")` (3-tier: JVM > env var > properties)
- Run: `mvn test -Dtestng.suite=testng-ui.xml -Denv=dev`

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden | ✔ Required |
|-------------|-----------|
| Assuming requirement is complete | Always identify missing specs and edge cases |
| Skipping risk assessment | Every requirement must have risks identified |
| Domain-specific framework references | Use generic placeholder names |
| Analyzing without identifying test scope (UI/API/Both) | Always determine test type needed |
| Ignoring integration points | List all API endpoints and UI screens involved |
