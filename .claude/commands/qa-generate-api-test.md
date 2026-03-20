Generate a complete Java API test class for: $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

---

## Phase 0 — API Classification (MANDATORY)

Analyze `$ARGUMENTS` and classify:

| Dimension       | Options                                  |
| --------------- | ---------------------------------------- |
| API type        | SEARCH / LOOKUP / LIST / ACTION          |
| Auth            | required / optional / public             |
| Response shape  | wrapped / direct-array / single-object   |
| Required params | list clearly                             |

→ Add as a comment block at the top of the test class

---

## Phase 1 — Naming (MANDATORY)

### Test Class Naming

Format:

```
<Verb><BusinessEntity>[Qualifier]ApiTest
```

- Verb = business action (Search, Get, List, Create, Update, Delete)
- Entity = business object (User, Order, Product…)

❌ DO NOT use: endpoint names, path segments, technical field names

Examples:

| ❌ BAD                            | ✔ GOOD                |
| --------------------------------- | ---------------------- |
| PositionStatisticSearchApiTest    | SearchUserApiTest      |
| OrderFilterApiTest                | GetOrderListApiTest    |

### Method Naming

Pattern:

```
should<Behavior>[When<Condition>]()
```

Examples:

- `shouldReturnResultsForValidInput()`
- `shouldReturnEmptyWhenNoDataFound()`
- `shouldReturnErrorWhenRequiredParamMissing()`
- `shouldFilterResultsCorrectly()`
- `shouldRespectPaginationLimit()`

### DataProvider Naming

- Name by behavior, NOT by field name

Examples:

| ❌ BAD                     | ✔ GOOD                          |
| -------------------------- | -------------------------------- |
| `statusProvider`           | `validFilterCombinations`        |
| `idList`                   | `knownIdentifiers`               |

---

## Phase 2 — Test Strategy

### SEARCH API

**Positive:**
- valid input → returns data
- validate result matches input
- filter and sort behavior

**Negative:**
- no match → empty result (200)
- missing required param → 400

**Cross-cutting:**
- pagination
- auth (if applicable)
- response schema validation

**Edge Cases:**
- empty string input
- special characters / unicode
- maximum length input
- boundary values for pagination (limit=0, offset=-1)

**Security:** (if auth applicable)
- request without auth token → verify behavior
- request with expired/invalid token → verify behavior

### LOOKUP API

**Positive:**
- valid identifier → returns correct record

**Negative:**
- not found → empty or 404 (verify actual behavior, DO NOT assume)
- missing param → 400
- invalid format → verify actual behavior (DO NOT assume)

**Cross-cutting:**
- auth
- schema validation

**Edge Cases:**
- empty string input
- special characters / unicode
- maximum length input
- boundary values for pagination (limit=0, offset=-1)

**Security:** (if auth applicable)
- request without auth token → verify behavior
- request with expired/invalid token → verify behavior

### LIST API

**Positive:**
- returns default data
- limit/offset behavior
- sorting behavior

**Cross-cutting:**
- auth
- schema validation

**Edge Cases:**
- empty string input
- special characters / unicode
- maximum length input
- boundary values for pagination (limit=0, offset=-1)

**Security:** (if auth applicable)
- request without auth token → verify behavior
- request with expired/invalid token → verify behavior

### ACTION API (create / update / delete)

**Positive:**
- operation succeeds with valid payload

**Negative:**
- invalid payload → validation error
- resource not found → 404

**Cross-cutting:**
- auth (required)
- schema validation

**Edge Cases:**
- empty string input
- special characters / unicode
- maximum length input
- boundary values for pagination (limit=0, offset=-1)

**Security:** (if auth applicable)
- request without auth token → verify behavior
- request with expired/invalid token → verify behavior

---

## Phase 3 — Framework

- Package: `com.framework.api`
- Extend: `BaseApiTest`
- DO NOT use UI / WebDriver

---

## Phase 4 — API Architecture (MANDATORY)

API test MUST follow layered architecture:

```
Test → Service Layer → ApiClient → Params
```

### Service Layer Rules

1. MUST create a Service class per business flow
    - Examples: `SearchUserService`, `OrderManagementService`

2. Test MUST NOT call ApiClient directly

   ```java
   // ❌ Forbidden
   apiClient.call(...)

   // ✔ Correct
   service.executeBusinessAction(...)
   ```

3. Service responsibilities:
    - Build request params
    - Call ApiClient
    - Return Response

4. Service MUST NOT:
    - Contain assertions
    - Validate responses
    - Contain test logic

5. Avoid duplicate request construction in test
   → Move reusable logic into Service

6. Naming MUST follow business intent

   | ❌ BAD              | ✔ GOOD              |
      | ------------------- | -------------------- |
   | `filterV2()`        | `searchByKeyword()`  |
   | `callEndpointX()`   | `findById()`         |

---

## Phase 5 — API Client

- Generate if not exists
- Method naming follows business action:

```java
search(params)
find(params)
getById(id)
create(request)
update(id, request)
delete(id)
```

❌ DO NOT use endpoint-based naming

---

## Phase 6 — Data Rules

1. Reuse data from `$ARGUMENTS` when available
   → Use generic naming:
    - `VALID_INPUT`
    - `KNOWN_ID`
    - `SAMPLE_VALUE`

2. DO NOT assume format
    - ❌ Avoid domain-specific field names
    - ✔ Use: `VALID_IDENTIFIER`, `KNOWN_KEY`, `SAMPLE_VALUE`

3. Lookup test data with fallback:

   ```java
   private static final String KNOWN_ID =
       System.getProperty("test.entity.id", "non-existent");
   ```

   → If fallback is used, test MUST validate "not found" behavior

---

## Phase 7 — Output

Generate the following artifacts:

1. **Test class** — with tests grouped by category (Positive, Negative, Edge Cases, Cross-cutting, Security)
2. **Service class** — per business flow
3. **API client** — if not already exists
4. **Request model** — if needed
5. **Test case list** — grouped by category with brief explanation of each group

### Allure Annotations (MANDATORY)

Every generated test class and method MUST include Allure annotations:

**Class level:**
```java
@Epic("<application or module name>")
@Feature("<business feature being tested>")
public class <BusinessEntity>ApiTest extends BaseApiTest {
```

**Method level:**
```java
@Test(groups = {"smoke", "regression"}, description = "<one-line test description>")
@Story("<business story or scenario>")
@Severity(SeverityLevel.BLOCKER)   // BLOCKER | CRITICAL | NORMAL | MINOR | TRIVIAL
public void should<Behavior>[When<Condition>]() {
```

**Severity guide:**

| Severity   | When to use                                      |
| ---------- | ------------------------------------------------ |
| `BLOCKER`  | Core happy path — failure blocks release         |
| `CRITICAL` | Important validation — failure causes major risk |
| `NORMAL`   | Standard negative / edge cases                   |
| `MINOR`    | Non-critical edge cases, cosmetic checks         |

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden                              | ✔ Required                        |
| ----------------------------------------- | --------------------------------- |
| Naming based on endpoint                  | Business-driven naming            |
| Domain hardcoding                         | Domain-agnostic design            |
| Test names like TC001, TC002              | Behavior-driven method names      |
| Scattered assertions across layers        | Assertions only in test class     |
| Mixing multiple APIs in one test class    | One API per test class            |
| Test calling ApiClient directly           | Test calls Service only           |

All tests MUST be: clean, readable, and business-driven.