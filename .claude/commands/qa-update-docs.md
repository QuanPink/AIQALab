Update project documentation to match current state: $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

---

## Purpose

This command reads recent code changes (git diff) and updates all project documentation that may have drifted. It ensures README, ARCHITECTURE.md, CLAUDE.md, and command files stay in sync with the actual codebase.

---

## Phase 0 — Input

| Input | Action |
|-------|--------|
| Blank or `all` | Check all documentation files |
| `readme` | Update README.md only |
| `architecture` | Update ARCHITECTURE.md only |
| `claude` | Update CLAUDE.md only |
| `commands` | Check command files for stale references |
| Specific file path | Update that documentation file |

---

## Phase 1 — Drift Detection

For each documentation file, compare against current codebase:

| Check | Source of Truth | Doc to Update |
|-------|----------------|--------------|
| Command count and list | `.claude/commands/*.md` file listing | README.md command table |
| Project structure | Actual directory tree | README.md structure section, ARCHITECTURE.md |
| Framework paths | `shared-qa-rules.md` paths table | ARCHITECTURE.md, CLAUDE.md |
| Test class names | `src/test/java/**/*Test.java` | README.md examples, ARCHITECTURE.md |
| Service class names | `src/main/java/**/service/*.java` | ARCHITECTURE.md |
| Dependencies | `pom.xml` | README.md tech stack |
| TestNG suites | `testng*.xml` files in root | README.md, CLAUDE.md |

---

## Phase 2 — Update

For each drift found:

1. Show what changed vs what doc currently says
2. Propose the update
3. Apply if straightforward, ask user if ambiguous

---

## Phase 3 — Output

Save to `.claude/artifacts/docs-update/latest.md`

Report format:

```
=== Documentation Update Report ===

Files checked: [N]
Files updated: [N]
Files unchanged: [N]

Updates applied:
- README.md: [what changed]
- ARCHITECTURE.md: [what changed]
- CLAUDE.md: No changes needed
```

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden | ✔ Required |
|-------------|-----------|
| Rewriting entire doc files | Minimal targeted updates only |
| Updating docs without checking git diff | Always verify what actually changed |
| Removing sections without confirmation | Ask before removing any content |
| Adding domain-specific content to docs | Keep all documentation domain-agnostic |
| Updating command file content via this command | Command content changes need `/qa-skill-validation` |
