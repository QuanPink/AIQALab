Generate test data for: $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

---

## Purpose

This command generates structured test data files, data constants, and data factory patterns that support test execution across multiple environments. It ensures test data is centralized, maintainable, and decoupled from test logic.

---

## Phase 0 — Input Analysis

Analyze `$ARGUMENTS` and determine:

| Dimension | Options |
|-----------|---------|
| Source | API spec / existing test class / requirement doc / manual description |
| Data scope | single entity / multiple entities / full flow |
| Environment awareness | single env / multi-env (dev, staging, prod) |
| Data type | static (constants) / dynamic (factory) / hybrid |

If `$ARGUMENTS` is an existing test class:
→ Extract all hardcoded values, inline constants, and request construction logic
→ Propose data externalization

If `$ARGUMENTS` is an API spec or requirement:
→ Derive data needs from input/output fields

Add classification as a comment at the top of each generated file.

---

## Phase 1 — Data Architecture

### 1.1 Directory Structure

```
src/test/resources/
├── testdata/
│   ├── <entity>/
│   │   ├── valid-input.json
│   │   ├── invalid-input.json
│   │   └── edge-cases.json
│   └── shared/
│       └── common-values.json
├── environments/
│   ├── dev.properties
│   ├── staging.properties
│   └── prod.properties
```

### 1.2 Java Data Classes

```
src/test/java/com/framework/data/
├── constants/
│   └── <Entity>TestData.java        — static constants per entity
├── factory/
│   └── <Entity>DataFactory.java     — builder pattern for dynamic data
├── provider/
│   └── <Entity>DataProvider.java    — TestNG @DataProvider methods
└── model/
    └── <Entity>TestModel.java       — POJO for complex test data
```

---

## Phase 2 — Data Categories

### 2.1 Static Constants

For values that remain the same across test runs.

Naming rules:
- Use generic names: `VALID_INPUT`, `KNOWN_ID`, `SAMPLE_VALUE`
- DO NOT use domain-specific names: ~~`WALLET_ADDRESS`~~, ~~`TOKEN_SYMBOL`~~
- Group by test scenario purpose, not by field name

```java
public final class EntityTestData {

    private EntityTestData() {}

    // --- Positive scenario data ---
    public static final String VALID_IDENTIFIER = 
        System.getProperty("test.entity.id", "known-valid-id");
    public static final String VALID_INPUT = "sample-input-value";
    public static final int DEFAULT_LIMIT = 20;

    // --- Negative scenario data ---
    public static final String NON_EXISTENT_ID = "id-that-does-not-exist";
    public static final String MALFORMED_INPUT = "!!!invalid-format";
    public static final String EMPTY_INPUT = "";

    // --- Edge case data ---
    public static final String SPECIAL_CHARACTERS_INPUT = "<script>alert('xss')</script>";
    public static final String MAX_LENGTH_INPUT = "a".repeat(10000);
    public static final String UNICODE_INPUT = "日本語テスト données résumé";
}
```

### 2.2 Environment-Aware Properties

For values that change per environment.

```properties
# dev.properties
base.url=https://dev-api.example.com
test.entity.id=dev-known-id-001
test.entity.secondary.id=dev-known-id-002
test.auth.token=dev-token
test.default.timeout=30
```

```properties
# staging.properties
base.url=https://staging-api.example.com
test.entity.id=staging-known-id-001
test.entity.secondary.id=staging-known-id-002
test.auth.token=staging-token
test.default.timeout=60
```

Loading pattern:
```java
public final class EnvConfig {

    private static final Properties props = new Properties();

    static {
        String env = System.getProperty("test.env", "dev");
        try (InputStream is = EnvConfig.class.getClassLoader()
                .getResourceAsStream("environments/" + env + ".properties")) {
            if (is != null) props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load env config: " + env, e);
        }
    }

    public static String get(String key) {
        return System.getProperty(key, props.getProperty(key));
    }

    public static String get(String key, String defaultValue) {
        return System.getProperty(key, props.getProperty(key, defaultValue));
    }
}
```

Usage in test data:
```java
public static final String VALID_IDENTIFIER = 
    EnvConfig.get("test.entity.id", "fallback-id");
```

### 2.3 Data Factory (Builder Pattern)

For tests that need dynamic or varied data per run.

```java
public final class EntityDataFactory {

    private String identifier = "default-id";
    private String input = "default-input";
    private int limit = 20;
    private int offset = 0;
    private String sortBy = "createdAt";
    private String sortOrder = "desc";

    private EntityDataFactory() {}

    public static EntityDataFactory create() {
        return new EntityDataFactory();
    }

    public EntityDataFactory withIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public EntityDataFactory withInput(String input) {
        this.input = input;
        return this;
    }

    public EntityDataFactory withPagination(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;
        return this;
    }

    public EntityDataFactory withSort(String sortBy, String sortOrder) {
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
        return this;
    }

    // Build into request params map
    public Map<String, Object> buildParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("identifier", identifier);
        params.put("input", input);
        params.put("limit", limit);
        params.put("offset", offset);
        params.put("sortBy", sortBy);
        params.put("sortOrder", sortOrder);
        return params;
    }

    // Build into request model
    public EntityRequestModel buildModel() {
        return new EntityRequestModel(identifier, input, limit, offset, sortBy, sortOrder);
    }
}
```

Usage in test:
```java
// Positive — default valid data
Map<String, Object> params = EntityDataFactory.create().buildParams();

// Edge case — large pagination
Map<String, Object> params = EntityDataFactory.create()
    .withPagination(1000, 0)
    .buildParams();

// Negative — empty input
Map<String, Object> params = EntityDataFactory.create()
    .withInput("")
    .buildParams();
```

### 2.4 DataProvider Methods

For parameterized tests with multiple input combinations.

Naming rule: Name by behavior, NOT by field.

```java
public class EntityDataProvider {

    @DataProvider(name = "validFilterCombinations")
    public static Object[][] validFilterCombinations() {
        return new Object[][] {
            { EntityDataFactory.create().withInput("keyword-a").buildParams(), "basic search" },
            { EntityDataFactory.create().withInput("keyword-b").withPagination(10, 0).buildParams(), "search with pagination" },
            { EntityDataFactory.create().withSort("updatedAt", "asc").buildParams(), "search with sort" },
        };
    }

    @DataProvider(name = "invalidInputScenarios")
    public static Object[][] invalidInputScenarios() {
        return new Object[][] {
            { EntityDataFactory.create().withInput("").buildParams(), "empty input", 400 },
            { EntityDataFactory.create().withInput(null).buildParams(), "null input", 400 },
            { EntityDataFactory.create().withIdentifier("non-existent").buildParams(), "unknown id", 404 },
        };
    }

    @DataProvider(name = "paginationBoundaries")
    public static Object[][] paginationBoundaries() {
        return new Object[][] {
            { 0, 0, "zero limit and offset" },
            { 1, 0, "minimum limit" },
            { 100, 0, "large limit" },
            { 20, 999, "high offset" },
            { -1, 0, "negative limit" },
        };
    }
}
```

### 2.5 JSON Test Data Files

For complex or large datasets that should not live in Java code.

```json
// src/test/resources/testdata/entity/valid-input.json
{
  "_comment": "Valid input combinations for positive scenarios",
  "scenarios": [
    {
      "name": "basic_valid_input",
      "identifier": "known-valid-id",
      "input": "sample-value",
      "expectedStatus": 200
    },
    {
      "name": "valid_with_optional_fields",
      "identifier": "known-valid-id",
      "input": "sample-value",
      "limit": 10,
      "sortBy": "createdAt",
      "expectedStatus": 200
    }
  ]
}
```

JSON loader utility:
```java
public final class TestDataLoader {

    private static final ObjectMapper mapper = new ObjectMapper();

    private TestDataLoader() {}

    public static <T> T load(String resourcePath, Class<T> type) {
        try (InputStream is = TestDataLoader.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (is == null) throw new RuntimeException("Resource not found: " + resourcePath);
            return mapper.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test data: " + resourcePath, e);
        }
    }

    public static <T> List<T> loadList(String resourcePath, Class<T> elementType) {
        try (InputStream is = TestDataLoader.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (is == null) throw new RuntimeException("Resource not found: " + resourcePath);
            return mapper.readValue(is, mapper.getTypeFactory()
                .constructCollectionType(List.class, elementType));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test data list: " + resourcePath, e);
        }
    }
}
```

---

## Phase 3 — Data Strategy by API Type

### SEARCH API
| Data need | Source | Pattern |
|-----------|--------|---------|
| Valid search keywords | Constants or env properties | `VALID_INPUT` |
| Filter combinations | DataProvider | `validFilterCombinations` |
| Sort options | DataProvider | `validSortCombinations` |
| Pagination boundaries | DataProvider | `paginationBoundaries` |
| Empty result trigger | Constants | `NO_MATCH_INPUT` |

### LOOKUP API
| Data need | Source | Pattern |
|-----------|--------|---------|
| Known valid ID | Env properties (changes per env) | `EnvConfig.get("test.entity.id")` |
| Non-existent ID | Constants | `NON_EXISTENT_ID` |
| Malformed ID | Constants | `MALFORMED_INPUT` |

### LIST API
| Data need | Source | Pattern |
|-----------|--------|---------|
| Default params | Factory with defaults | `EntityDataFactory.create().buildParams()` |
| Pagination variations | DataProvider | `paginationBoundaries` |
| Sort variations | DataProvider | `validSortCombinations` |

### ACTION API
| Data need | Source | Pattern |
|-----------|--------|---------|
| Valid create payload | Factory | `EntityDataFactory.create().buildModel()` |
| Invalid payload variations | DataProvider | `invalidPayloadScenarios` |
| Update payload (partial) | Factory with selective fields | `EntityDataFactory.create().withInput("updated").buildModel()` |

---

## Phase 4 — Migration Rules

When extracting data from existing test files:

### 4.1 What to Extract

| Found in test | Move to | Example |
|---------------|---------|---------|
| Hardcoded string in test method | Constants class | `"0x123..."` → `VALID_IDENTIFIER` |
| Hardcoded string in `@BeforeClass` | Env properties | Base URL, auth tokens |
| Inline request param construction | Data Factory | `Map.of("key", "value")` → `Factory.create().buildParams()` |
| Multiple similar test methods with different inputs | DataProvider | 3 methods testing different keywords → 1 method + DataProvider |
| Magic numbers | Constants with descriptive name | `20` → `DEFAULT_PAGE_LIMIT` |

### 4.2 What NOT to Extract

| Keep in test | Reason |
|-------------|--------|
| Expected status codes (200, 400, 404) | Part of assertion logic, not test data |
| Assertion messages | Readable in context |
| Test-specific one-time values | Extraction adds complexity without reuse |

### 4.3 Fallback Pattern (MANDATORY for lookup data)

Every identifier used for lookup MUST have a system property fallback:

```java
// ✔ Correct — works in any environment
public static final String VALID_IDENTIFIER = 
    EnvConfig.get("test.entity.id", "fallback-not-found");

// ❌ Wrong — breaks when ID does not exist in target env
public static final String VALID_IDENTIFIER = "hardcoded-id-from-dev";
```

When fallback value is used, the corresponding test MUST validate "not found" behavior — not assume the entity exists.

---

## Phase 5 — Output

Generate the following artifacts based on analysis:

| # | Artifact | When to generate |
|---|----------|-----------------|
| 1 | `<Entity>TestData.java` (constants) | Always |
| 2 | `<Entity>DataFactory.java` (builder) | When test needs varied input combinations |
| 3 | `<Entity>DataProvider.java` (TestNG) | When test has parameterized scenarios |
| 4 | `<Entity>TestModel.java` (POJO) | When request/response has 4+ fields |
| 5 | `dev.properties` / `staging.properties` | When env-specific values are detected |
| 6 | `valid-input.json` / `invalid-input.json` | When dataset is large or complex |
| 7 | `EnvConfig.java` (utility) | Once per project if not exists |
| 8 | `TestDataLoader.java` (utility) | Once per project if JSON files are generated |
| 9 | Migration summary | When extracting from existing test class |

### Migration Summary Format

When migrating data from an existing test:

```
| Before (in test) | After (extracted) | File |
|------------------|-------------------|------|
| "0x123abc" at line 15 | VALID_IDENTIFIER | EntityTestData.java |
| Map.of("limit", 20) at line 30 | Factory.create().buildParams() | EntityDataFactory.java |
| 3 duplicate methods (line 45, 60, 75) | 1 method + DataProvider | EntityDataProvider.java |
```

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden | ✔ Required |
|-------------|-----------|
| Hardcoded values in test methods | Centralized in Constants or Factory |
| Domain-specific constant names (`WALLET`, `TOKEN`) | Generic names (`VALID_IDENTIFIER`, `KNOWN_KEY`) |
| Same ID used across all environments | Env-aware properties with fallback |
| Giant "TestData" class for all entities | One data class per entity |
| DataProvider named by field (`statusProvider`) | DataProvider named by behavior (`validFilterCombinations`) |
| Test data in `src/main/` | Test data in `src/test/` |
| JSON files without `_comment` field | Every JSON data file has purpose documentation |
| Factory without default values | Factory always produces valid data with defaults |
| Copy-pasting params across test methods | Shared via Factory or DataProvider |

Save generation manifest to `.claude/artifacts/generated-test-data/latest.md`