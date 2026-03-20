Generate response schema validation tests for: $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

---

## Purpose

This command generates automated tests that validate API response structure (field names, types, nullability, nested objects, arrays) independently from business logic tests. Schema tests act as a **contract layer** — catching structural regressions before they propagate into business test failures.

---

## Phase 0 — Input Analysis

Analyze `$ARGUMENTS` and determine the source:

| Source type | Detection | Action |
|-------------|-----------|--------|
| Swagger / OpenAPI spec file | `.json` or `.yaml` file with `paths` and `schemas` | Parse schema definitions directly |
| Postman collection | `.json` file with `item` array and `response` | Extract response bodies as samples |
| Live API response (JSON) | Raw JSON object or array | Infer schema from structure |
| Existing test class | `*ApiTest.java` file | Extract API calls, capture response, infer schema |
| API endpoint description | Text describing fields and types | Derive schema manually |

### Classification (add as comment at top of generated class)

```java
/**
 * Schema source: [Swagger spec | Postman collection | Live response sample | Inferred from test]
 * Endpoint reference: [GET /api/v1/entity (comment only — not used in naming)]
 * Response shape: [wrapped {data:[]} | direct-array [] | single-object {}]
 * Last validated: [date]
 */
```

---

## Phase 1 — Schema Extraction

### 1.1 Response Shape Detection

Identify the response wrapper pattern first:

| Shape | Example | Schema target |
|-------|---------|---------------|
| Wrapped object | `{ "data": [...], "total": 10, "page": 1 }` | Validate wrapper fields + inner item schema |
| Direct array | `[{ "id": 1 }, { "id": 2 }]` | Validate array element schema |
| Single object | `{ "id": 1, "name": "test" }` | Validate object schema |
| Paginated wrapped | `{ "data": [...], "meta": { "total": 10, "limit": 20 } }` | Validate wrapper + meta + inner item schema |

### 1.2 Field Analysis

For each field in the response, extract:

| Property | Values | Example |
|----------|--------|---------|
| Name | field key | `"createdAt"` |
| Type | string / integer / long / double / boolean / object / array | `string` |
| Nullable | yes / no / conditional | `yes` — field can be null by design |
| Required | always present / optional / conditional | `always` |
| Format | free-text / enum / date-time / numeric-string / nested-object | `date-time` |
| Nested | if object or array, recurse into children | `items: [{ id, name }]` |

### 1.3 Field Classification Table

Generate a table for each entity:

```
| Field | Type | Nullable | Required | Format | Notes |
|-------|------|----------|----------|--------|-------|
| id | string | no | always | — | Primary identifier |
| name | string | yes | always | — | Can be null for draft entities |
| status | string | no | always | enum: ACTIVE, INACTIVE | — |
| createdAt | string | no | always | ISO-8601 datetime | — |
| items | array | no | always | nested object | See items schema below |
| metadata | object | yes | optional | nested object | Only present when requested |
```

---

## Phase 2 — Schema Test Strategy

### 2.1 Wrapper Level Tests

| Test | What it validates |
|------|-------------------|
| `shouldReturnExpectedWrapperStructure()` | Top-level fields exist (data, total, page, meta) |
| `shouldReturnCorrectWrapperFieldTypes()` | data is array, total is integer, page is integer |
| `shouldReturnNonNullWrapperFields()` | Required wrapper fields are never null |

### 2.2 Entity Schema Tests

| Test | What it validates |
|------|-------------------|
| `shouldReturnAllRequiredFields()` | Every required field is present in response |
| `shouldReturnCorrectFieldTypes()` | Each field matches expected type |
| `shouldReturnValidEnumValues()` | Enum fields contain only allowed values |
| `shouldReturnValidDateTimeFormat()` | DateTime fields match ISO-8601 pattern |
| `shouldAllowNullForNullableFields()` | Nullable fields accept null without error |
| `shouldNeverReturnNullForNonNullableFields()` | Non-nullable fields are always present and non-null |

### 2.3 Nested Object Tests

| Test | What it validates |
|------|-------------------|
| `shouldReturnValidNestedObjectSchema()` | Nested object has expected fields and types |
| `shouldReturnValidArrayItemSchema()` | Each item in array follows item schema |
| `shouldHandleEmptyArrayGracefully()` | Empty array does not break schema validation |

### 2.4 Edge Case Tests

| Test | What it validates |
|------|-------------------|
| `shouldNotReturnUndocumentedFields()` | Response has no unexpected extra fields (optional — strict mode) |
| `shouldHandleNumericStringFieldsCorrectly()` | Fields documented as string but containing numbers are validated as string |
| `shouldReturnConsistentSchemaAcrossPages()` | Page 1 and page 2 have identical field structure |

---

## Phase 3 — Code Generation

### 3.1 Schema Constants Class

```java
/**
 * Schema definition for Entity response.
 * Source: [Swagger spec | Live response]
 * Last validated: [date]
 */
public final class EntitySchemaDefinition {

    private EntitySchemaDefinition() {}

    // --- Wrapper fields ---
    public static final String FIELD_DATA = "data";
    public static final String FIELD_TOTAL = "total";
    public static final String FIELD_PAGE = "page";

    // --- Entity fields ---
    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_CREATED_AT = "createdAt";

    // --- Required fields (non-nullable) ---
    public static final List<String> REQUIRED_FIELDS = List.of(
        FIELD_ID, FIELD_STATUS, FIELD_CREATED_AT
    );

    // --- Nullable fields ---
    public static final List<String> NULLABLE_FIELDS = List.of(
        FIELD_NAME
    );

    // --- Enum values ---
    public static final List<String> STATUS_VALUES = List.of(
        "ACTIVE", "INACTIVE"
    );

    // --- Date format ---
    public static final String DATETIME_PATTERN = 
        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*";
}
```

### 3.2 Schema Validation Helper

```java
public final class SchemaValidator {

    private SchemaValidator() {}

    /**
     * Validate that all required fields are present and non-null.
     */
    public static void assertRequiredFields(JsonObject entity, List<String> requiredFields) {
        for (String field : requiredFields) {
            assertThat(entity.has(field))
                .as("Required field '%s' must be present", field)
                .isTrue();
            assertThat(entity.get(field).isJsonNull())
                .as("Required field '%s' must not be null", field)
                .isFalse();
        }
    }

    /**
     * Validate that nullable fields, when present, are allowed to be null.
     */
    public static void assertNullableFieldsAllowed(JsonObject entity, List<String> nullableFields) {
        for (String field : nullableFields) {
            // Field can be absent or null — both are acceptable
            if (entity.has(field)) {
                // No assertion — null is allowed
            }
        }
    }

    /**
     * Validate field type.
     */
    public static void assertFieldType(JsonObject entity, String field, String expectedType) {
        if (!entity.has(field) || entity.get(field).isJsonNull()) return;

        JsonElement element = entity.get(field);
        switch (expectedType) {
            case "string":
                assertThat(element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
                    .as("Field '%s' must be string", field).isTrue();
                break;
            case "integer":
            case "long":
                assertThat(element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber())
                    .as("Field '%s' must be number", field).isTrue();
                break;
            case "boolean":
                assertThat(element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean())
                    .as("Field '%s' must be boolean", field).isTrue();
                break;
            case "object":
                assertThat(element.isJsonObject())
                    .as("Field '%s' must be object", field).isTrue();
                break;
            case "array":
                assertThat(element.isJsonArray())
                    .as("Field '%s' must be array", field).isTrue();
                break;
        }
    }

    /**
     * Validate enum field contains only allowed values.
     */
    public static void assertEnumValue(JsonObject entity, String field, List<String> allowedValues) {
        if (!entity.has(field) || entity.get(field).isJsonNull()) return;

        String actual = entity.get(field).getAsString();
        assertThat(allowedValues)
            .as("Field '%s' value '%s' must be one of %s", field, actual, allowedValues)
            .contains(actual);
    }

    /**
     * Validate datetime field matches expected pattern.
     */
    public static void assertDateTimeFormat(JsonObject entity, String field, String pattern) {
        if (!entity.has(field) || entity.get(field).isJsonNull()) return;

        String actual = entity.get(field).getAsString();
        assertThat(actual)
            .as("Field '%s' must match datetime pattern", field)
            .matches(pattern);
    }
}
```

### 3.3 Schema Test Class

```java
@Epic("Schema Validation")
@Feature("Entity Schema")
public class ValidateEntitySchemaApiTest extends BaseApiTest {

    private EntityService entityService;
    private JsonObject sampleEntity;

    @BeforeClass
    public void setup() {
        entityService = new EntityService();
        Response response = entityService.getDefaultList();
        // Extract first item from response for schema validation
        JsonArray data = response.jsonPath().getObject("data", JsonArray.class);
        assumeThat(data).as("Need at least one entity to validate schema").isNotEmpty();
        sampleEntity = data.get(0).getAsJsonObject();
    }

    // --- Wrapper structure ---

    @Test
    @Story("Wrapper structure")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldReturnExpectedWrapperStructure() {
        Response response = entityService.getDefaultList();
        JsonObject body = response.as(JsonObject.class);

        assertThat(body.has(EntitySchemaDefinition.FIELD_DATA)).isTrue();
        assertThat(body.has(EntitySchemaDefinition.FIELD_TOTAL)).isTrue();
    }

    // --- Required fields ---

    @Test
    @Story("Required fields")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldReturnAllRequiredFields() {
        SchemaValidator.assertRequiredFields(
            sampleEntity, EntitySchemaDefinition.REQUIRED_FIELDS);
    }

    @Test
    @Story("Required fields")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldNeverReturnNullForNonNullableFields() {
        SchemaValidator.assertRequiredFields(
            sampleEntity, EntitySchemaDefinition.REQUIRED_FIELDS);
    }

    // --- Field types ---

    @Test
    @Story("Field types")
    @Severity(SeverityLevel.NORMAL)
    public void shouldReturnCorrectFieldTypes() {
        SchemaValidator.assertFieldType(sampleEntity, "id", "string");
        SchemaValidator.assertFieldType(sampleEntity, "name", "string");
        SchemaValidator.assertFieldType(sampleEntity, "status", "string");
        SchemaValidator.assertFieldType(sampleEntity, "createdAt", "string");
    }

    // --- Enum values ---

    @Test
    @Story("Enum validation")
    @Severity(SeverityLevel.NORMAL)
    public void shouldReturnValidEnumValues() {
        SchemaValidator.assertEnumValue(
            sampleEntity, "status", EntitySchemaDefinition.STATUS_VALUES);
    }

    // --- DateTime format ---

    @Test
    @Story("Format validation")
    @Severity(SeverityLevel.MINOR)
    public void shouldReturnValidDateTimeFormat() {
        SchemaValidator.assertDateTimeFormat(
            sampleEntity, "createdAt", EntitySchemaDefinition.DATETIME_PATTERN);
    }

    // --- Nullable fields ---

    @Test
    @Story("Nullable fields")
    @Severity(SeverityLevel.NORMAL)
    public void shouldAllowNullForNullableFields() {
        SchemaValidator.assertNullableFieldsAllowed(
            sampleEntity, EntitySchemaDefinition.NULLABLE_FIELDS);
    }

    // --- Consistency across pages ---

    @Test
    @Story("Schema consistency")
    @Severity(SeverityLevel.MINOR)
    public void shouldReturnConsistentSchemaAcrossPages() {
        Response page1 = entityService.getListWithPagination(20, 0);
        Response page2 = entityService.getListWithPagination(20, 20);

        JsonArray data1 = page1.jsonPath().getObject("data", JsonArray.class);
        JsonArray data2 = page2.jsonPath().getObject("data", JsonArray.class);

        if (!data1.isEmpty() && !data2.isEmpty()) {
            Set<String> keys1 = data1.get(0).getAsJsonObject().keySet();
            Set<String> keys2 = data2.get(0).getAsJsonObject().keySet();
            assertThat(keys1).as("Schema must be consistent across pages").isEqualTo(keys2);
        }
    }
}
```

---

## Phase 4 — Handling Real-world Patterns

### 4.1 Null-by-design Fields

Some APIs return null for specific fields intentionally. This is NOT a bug.

Detection: If `$ARGUMENTS` includes a response sample where a field is null, ask:
> "Field `[name]` is null in the sample. Is this null-by-design (always nullable) or data-dependent (sometimes has value)?"

If null-by-design → add to `NULLABLE_FIELDS`
If data-dependent → add to `CONDITIONALLY_NULLABLE_FIELDS` with documentation

```java
// Fields that are null depending on entity state
public static final Map<String, String> CONDITIONALLY_NULLABLE_FIELDS = Map.of(
    "closedAt", "Null when entity is still active",
    "parentId", "Null for top-level entities"
);
```

### 4.2 Numeric Strings

Some APIs return numbers as strings (e.g., `"123.456"` instead of `123.456`).

```java
/**
 * Validate field is a string containing a valid number.
 */
public static void assertNumericString(JsonObject entity, String field) {
    if (!entity.has(field) || entity.get(field).isJsonNull()) return;

    String value = entity.get(field).getAsString();
    assertThat(value).as("Field '%s' must be parseable as number", field)
        .matches("-?\\d+(\\.\\d+)?");
}
```

### 4.3 Dynamic Fields

Some responses include fields that vary based on type or configuration.

Strategy: Validate base schema (common fields) strictly, validate dynamic fields only when present.

```java
// Base fields — always validate
public static final List<String> BASE_REQUIRED_FIELDS = List.of("id", "type", "createdAt");

// Type-specific fields — validate only when type matches
public static final Map<String, List<String>> TYPE_SPECIFIC_FIELDS = Map.of(
    "TYPE_A", List.of("fieldOnlyInA", "anotherFieldForA"),
    "TYPE_B", List.of("fieldOnlyInB")
);
```

### 4.4 Wrapped vs Direct Response

Generate different validation paths based on response shape:

```java
// Wrapped: { "data": [...], "total": 10 }
public void validateWrappedResponse(Response response) {
    JsonObject body = response.as(JsonObject.class);
    assertThat(body.has("data")).isTrue();
    JsonArray items = body.getAsJsonArray("data");
    for (JsonElement item : items) {
        validateEntitySchema(item.getAsJsonObject());
    }
}

// Direct array: [{ ... }, { ... }]
public void validateDirectArrayResponse(Response response) {
    JsonArray items = response.as(JsonArray.class);
    for (JsonElement item : items) {
        validateEntitySchema(item.getAsJsonObject());
    }
}

// Single object: { "id": 1, ... }
public void validateSingleObjectResponse(Response response) {
    JsonObject entity = response.as(JsonObject.class);
    validateEntitySchema(entity);
}
```

---

## Phase 5 — Output

Generate the following artifacts:

| # | Artifact | When to generate |
|---|----------|-----------------|
| 1 | `EntitySchemaDefinition.java` | Always — schema constants and field lists |
| 2 | `SchemaValidator.java` | Once per project if not exists — reusable validation helpers |
| 3 | `ValidateEntitySchemaApiTest.java` | Always — the test class |
| 4 | Field classification table | Always — as comment block or separate doc |
| 5 | Service method for schema test | If entity service does not have a default list method |

### File Locations

| Artifact | Path |
|----------|------|
| Schema definition | `src/test/java/com/framework/data/schema/EntitySchemaDefinition.java` |
| Schema validator | `src/test/java/com/framework/data/schema/SchemaValidator.java` |
| Schema test class | `src/test/java/com/framework/api/schema/ValidateEntitySchemaApiTest.java` |

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden | ✔ Required |
|-------------|-----------|
| Validating only status code 200 | Validate wrapper + fields + types + nullability |
| Hardcoding expected field values in schema test | Schema test checks structure, not business values |
| Mixing schema validation with business logic test | Separate test class for schema |
| Assuming all fields are non-null | Explicitly classify nullable vs required |
| Ignoring numeric string fields | Validate as string type with numeric format check |
| Copy-pasting field names as raw strings in test | Use SchemaDefinition constants |
| Skipping nested object validation | Recurse into nested objects and array items |
| Schema test depends on specific entity data | Use `assumeThat` to skip when no data available |

Save generation manifest to `.claude/artifacts/generated-schema-test/latest.md`