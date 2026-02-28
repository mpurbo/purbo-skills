---
name: openspec-progressive-superpowers
description: >
  Use when setting up or updating OpenSpec's config.yaml for a project, or when
  OpenSpec workflow isn't picking up development disciplines (TDD, progressive
  phases, FP conventions). Triggers: "configure openspec", "setup openspec
  context", "openspec config", "why didn't openspec use TDD",
  "openspec not invoking skills", "grounding config". Generates config.yaml that
  carries project context and skill invocation rules across sessions. Do NOT use
  for non-OpenSpec projects or for general CLAUDE.md configuration.
---

# OpenSpec Progressive Superpowers

Configures OpenSpec's `config.yaml` so that every artifact instruction automatically
carries project context and invokes the right superpowers skills at each workflow
step. This is a **setup skill** — run once per project, update when conventions or
available skills change.

**The problem it solves:** Skills are session-scoped. If you invoke TDD in one
session but create artifacts in another, TDD is forgotten. `config.yaml` is the
cross-session memory that ensures conventions survive.

## When to Use

- **New project setup:** After `openspec init` or when `config.yaml` is empty/default
- **Missing discipline:** Agent didn't invoke TDD during apply, skipped progressive
  phases, ignored project conventions
- **New skills added:** Installed new superpowers or tech-specific skills that should
  be woven into the OpenSpec workflow
- **Project docs changed:** New PRD, updated system spec, changed tech stack

## Process

### Step 1: Discover Project Context

Scan for project documentation to extract context:

```
Search order:
1. docs/**/*.md, docs/**/*.txt         — specs, PRDs, architecture docs
2. CLAUDE.md, .claude/CLAUDE.md        — existing developer context
3. openspec/config.yaml                — current config (may be empty)
4. Cargo.toml, package.json, go.mod    — tech stack signals
5. openspec/specs/**/*.md              — existing capability specs
```

Extract from these sources:
- **Project identity:** Name, purpose, domain (1-2 sentences)
- **Tech stack:** Language, framework, key libraries, build system
- **Architecture principles:** From system spec or CLAUDE.md
- **Subsystem structure:** If multi-service, how they relate

### Step 2: Discover Available Skills

Scan the installed skills to build a mapping of what's available:

**Superpowers skills** (check the skill list in system prompt):
- `superpowers:test-driven-development` — TDD discipline
- `superpowers:systematic-debugging` — debugging methodology
- `superpowers:brainstorming` — creative exploration before implementation
- `superpowers:writing-plans` — plan creation from specs
- `superpowers:executing-plans` — plan execution with review checkpoints
- `superpowers:verification-before-completion` — evidence-based verification
- `superpowers:requesting-code-review` — review after implementation
- `superpowers:finishing-a-development-branch` — branch completion workflow

**Tech-specific skills** (check installed plugins):
- `fp-rust` — FP-first Rust conventions
- `fp-kstream-design` / `fp-kstream-implement` — Kafka Streams
- Any other domain-specific skills

**OpenSpec skills:**
- `openspec-progressive-implementation` — progressive phase discipline

Only reference skills that are actually installed. Do NOT reference skills that
don't exist in the environment.

### Step 3: Map Skills to OpenSpec Workflow Steps

Build the mapping. Each OpenSpec artifact type and action maps to specific skills:

| Workflow Step | Skills to Invoke | Why |
|---------------|-----------------|-----|
| `proposal` | `openspec-progressive-implementation` | Phase design: reviewable, testable, contract-bounded, documented |
| `specs` | *(general practice)* | WHEN/THEN scenarios must be translatable to test cases |
| `design` | `superpowers:brainstorming` (if design is exploratory) | Explore alternatives before committing to architecture |
| `tasks` | `openspec-progressive-implementation` | Task grouping, phase headers, TDD ordering, per-phase docs |
| `apply` | `superpowers:test-driven-development`, tech-specific skills | TDD within each task, language conventions during coding |
| Post-apply | `superpowers:verification-before-completion` | Evidence before claiming done |

### Step 4: Generate config.yaml

Write `openspec/config.yaml` with two sections:

#### `context:` section

Project identity and development discipline. This is injected into every
`openspec instructions` call. Structure it as:

```yaml
context: |
  ## Project: <name>
  <1-2 sentence description from project docs>

  Reference docs:
  <list paths to key docs: system spec, PRDs, etc.>

  ## Tech Stack
  <language, framework, key libraries, build system>

  ## Architecture Principles
  <from system spec or CLAUDE.md — keep to 3-5 bullets>

  ## Development Discipline
  <list each discipline with its skill reference>
  - TDD (red-green-refactor): Use superpowers:test-driven-development during apply.
  - Progressive implementation: Use openspec-progressive-implementation for
    proposal and tasks artifacts.
  - <tech-specific>: Use <skill-name> when writing <language> code.
```

#### `rules:` section

Per-artifact constraints. These are injected when creating specific artifact types.

```yaml
rules:
  proposal:
    - Follow progressive-implementation discipline when defining phases.
    - Each phase must be reviewable, testable, contract-bounded, and documented.
    - "Contracts exposed" section required per phase.
    - Reference PRD section numbers for traceability.
  specs:
    - Every requirement MUST have at least one WHEN/THEN scenario.
    - Scenarios must be directly translatable to TDD test cases.
  design:
    - Justify decisions with rationale and alternatives considered.
    - Flag risks with mitigations.
  tasks:
    - Follow progressive-implementation discipline for task structure.
    - Each phase's LAST task must update documentation with manual verification steps.
    - Include phase header with Contracts exposed, Gate criteria, and Docs update target.
    - Order tasks within each phase for TDD — test first, then implementation.
    - Each task must be completable in one TDD cycle.
    - Gate tasks (build, test, lint) required at the end.
```

### Step 5: Present for Review

Show the generated config.yaml to the user. Highlight:
- Which project docs were used for context
- Which skills were discovered and mapped
- Any gaps (e.g., "no tech-specific skill found for your language")

## Anti-Rationalization Table

| Thought | Reality |
|---------|---------|
| "The project is simple, config.yaml is overkill" | Simple projects become complex. Config.yaml costs nothing and prevents gaps. |
| "CLAUDE.md already has my conventions" | CLAUDE.md isn't injected into OpenSpec instructions. config.yaml is. |
| "I'll remember to invoke TDD manually" | You won't. Sessions don't share memory. Config.yaml does. |
| "The skills will be picked up from the system prompt" | Skills are invoked on trigger words. Without config.yaml, artifact creation doesn't trigger them. |
| "I'll add context later when I need it" | Later = after the gap causes a problem. Set it up now. |

## Maintenance

Re-run this skill when:
- New PRD or system spec is added
- Tech stack changes (new language, framework)
- New skills are installed
- After a retrospective reveals a discipline gap (like TDD not being invoked)
