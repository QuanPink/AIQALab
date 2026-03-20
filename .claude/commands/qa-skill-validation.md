Validate QA command files for quality and consistency: $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

---

## Purpose

This command tests the QA command files themselves — ensuring they follow standards, are internally consistent, and produce correct output. It is the "test for your tests" layer.

---

## Phase 0 — Input

| Input | Action |
|-------|--------|
| Blank or `all` | Validate all files in `.claude/commands/` |
| Specific filename | Validate that one file |
| `cross-check` | Run cross-reference validation between all files |

---

## Phase 1 — Structure Validation (per file)

| Check | Pass | Fail |
|-------|------|------|
| Starts with `$ARGUMENTS` declaration | First line contains `$ARGUMENTS` | Missing — command won't receive input |
| References shared-qa-rules | Contains `> This command follows rules defined in /shared-qa-rules.` | Missing — file may drift from standards |
| Has Anti-patterns section | Contains `## Anti-patterns` with ❌/✔ table | Missing — no guard rails |
| Has Output section | Defines what artifacts are generated | Missing — unclear deliverable |
| No duplicate content with shared-qa-rules | Global QA Rules not copy-pasted | Duplicate found — maintain in one place only |
| Language is English only | No Vietnamese or mixed language | Non-English content found |

---

## Phase 2 — Content Validation (per file)

| Check | Pass | Fail |
|-------|------|------|
| No hardcoded domain terms | No `copin`, `binance`, protocol names (except in ❌ BAD examples) | Domain term found outside example |
| No undefined placeholders | No `$APPLICATION`, `$SamplePage` without explanation | Undefined placeholder found |
| Framework paths match shared-qa-rules | All paths reference correct directories | Path mismatch |
| Scenario groups match standard 5 | Uses Positive, Negative, Edge Cases, Cross-cutting, Security | Missing or extra groups |
| Naming convention enforced | References `should<Behavior>[When<Condition>]()` pattern | Missing naming guidance |

---

## Phase 3 — Cross-reference Validation

| Check | Pass | Fail |
|-------|------|------|
| Next-step references are valid | All `/qa-xxx` references point to existing files | Dead reference found |
| Classification consistency | REG/EXP/ENV/FLK/STL/UNK used identically in analyze-regression, generate-report, fix-test | Inconsistent labels |
| Service Layer mentioned consistently | qa-generate-api-test, qa-review-test, qa-analyze-failure, qa-fix-test all reference Service Layer | Missing in one or more files |
| Artifact paths consistent | `.claude/artifacts/` convention used in all commands that produce output | Missing artifact output |
| Health Score formula consistent | Same formula in qa-health-score and qa-generate-report | Mismatch |

---

## Phase 4 — Full Surface Scan (Domain Terms)

Apply the same 9-surface-area scan from qa-review-test Phase 5.1 to command files themselves:

For each file, scan for domain-specific terms in:
1. Example code snippets
2. Example class/method names
3. Example constant names
4. Example annotation values
5. Inline descriptions
6. Comments

Exception: Terms used inside ❌ BAD example columns are acceptable.

---

## Phase 5 — Output

### Summary Table
```
| File | Structure | Content | Cross-ref | Domain | Overall |
|------|-----------|---------|-----------|--------|---------|
| shared-qa-rules.md | ✅ | ✅ | ✅ | ✅ | PASS |
| qa-generate-api-test.md | ✅ | ✅ | ✅ | ✅ | PASS |
| qa-fix-test.md | ✅ | ⚠️ | ✅ | ✅ | WARN |
```

### Issue List (if any)
```
[🟡 High] qa-fix-test.md
  → Phase 2: Undefined placeholder "$SamplePage" at line 45
  → Fix: Replace with generic "<PageName>Page"
```

Save to `.claude/artifacts/skill-validation/latest.md`

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden | ✔ Required |
|-------------|-----------|
| Validating command output quality (LLM eval) | Validate structure, consistency, and references only |
| Modifying command files during validation | Report only — never auto-fix |
| Skipping cross-reference check | Always validate links between commands |
| Ignoring ❌ BAD example columns in domain scan | Domain terms in BAD examples are acceptable |
```