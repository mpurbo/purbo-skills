# purbo-skills

Opinionated AI agent skills for FP-first development.

## Installation

```bash
# Install all skills (works with Claude Code, Cursor, and 40+ agents)
npx skills add mpurbo/purbo-skills

# Install a specific skill
npx skills add mpurbo/purbo-skills --skill fp-rust
```

### Claude Code marketplace (alternative)

```bash
/plugin marketplace add mpurbo/purbo-skills
/plugin install fp-rust@purbo-skills
/plugin install fp-kstream-design@purbo-skills
/plugin install fp-kstream-implement@purbo-skills
/plugin install openspec-progressive-implementation@purbo-skills
/plugin install openspec-progressive-superpowers@purbo-skills
/plugin install mermaid-pastel-style@purbo-skills
```

## Available Skills

### fp-rust

Functional Programming in Rust — guidelines for writing idiomatic, FP-first Rust code.

**Covers:** functional core / imperative shell architecture, immutability-first patterns (borrow > clone > mutate), pure functions, algebraic data types, pipeline-oriented programming with iterators and Result chains, error handling as data, dependency rejection over dependency injection, type-driven design (parse don't validate, typestate), concurrency via message passing, and recommended crate stack.

**Triggers on:** any Rust coding task — planning, writing, reviewing, refactoring, scaffolding, project setup, Cargo.toml configuration, or code review.

**Based on:** [Functional Rust: Idiomatic FP Guidelines](skills/fp-rust/references/FP_RUST_GUIDELINES.md), cross-referenced with [Apollo GraphQL Rust Best Practices](https://github.com/apollographql/rust-best-practices).

### fp-kstream-design

Design Kafka Streams topologies using [Kafka Stream Algebra (KSA)](skills/fp-kstream-design/references/KSA.md) patterns. Produces Mermaid topology diagrams, recipe selection, cost estimates, and compliance checks.

**Triggers on:** "design a topology", "kafka architecture", "stream processing design", "what pattern should I use", "topology review", "KSA".

### fp-kstream-implement

Implement topologies as testable, deterministic code with pure business logic separated from Kafka wiring. Includes code patterns (stateless transforms, FSMs, enrichment), testing strategy (3-layer), local dev setup (Docker Compose), and Gradle build config.

**Triggers on:** "implement this topology", "write the KStream code", "create the processor", "set up kafka locally", "write tests", "KStream implementation".

**Based on:** [Kafka Stream Algebra v0.5](skills/fp-kstream-implement/references/KSA.md) — 10 composable recipes covering ingress, enrichment, computation, and egress patterns.

### openspec-progressive-implementation

Progressive implementation discipline for [OpenSpec](https://github.com/fission-ai/openspec) changes. Structures work into independently reviewable, testable phases with clear contract boundaries.

**Covers:** phase decomposition in proposals (scope, contracts, gates), task grouping under phase headers, phase checkpoint enforcement during apply, per-phase documentation updates, context management guidance for fresh AI sessions, and anti-rationalization guards.

**Triggers on:** "progressive implementation", "phased implementation", "step-by-step", "implementation phases", "chunked implementation" — or any OpenSpec proposal/tasks/apply workflow where phased delivery is desired.

**Key constraints enforced:**
- ≤8 tasks per phase, reviewable in ≤30 minutes
- Contract-first boundaries — Phase N testable with mocks even without Phase N-1
- Hard stop at phase checkpoints for human review
- Documentation updated every phase (not deferred to the end)

### openspec-progressive-superpowers

Configures [OpenSpec](https://github.com/fission-ai/openspec)'s `config.yaml` to carry project context and development disciplines (TDD, progressive phases, FP conventions) across sessions. A setup skill — run once per project, update when conventions or available skills change.

**Covers:** project context discovery (specs, PRDs, tech stack), available skill mapping to OpenSpec workflow steps (proposal → progressive-implementation, apply → TDD + tech-specific skills), `config.yaml` generation with `context:` and `rules:` sections.

**Triggers on:** "configure openspec", "setup openspec context", "openspec config", "why didn't openspec use TDD", "openspec not invoking skills", "grounding config".

**Problem it solves:** Skills are session-scoped — if you invoke TDD in one session but create artifacts in another, TDD is forgotten. `config.yaml` is the cross-session memory that ensures conventions survive across all OpenSpec workflow steps.

### mermaid-pastel-style

Consistent pastel color styling for mermaid diagrams. Semantic palette mapping node roles (process, decision, success, error, external) to pastel fills with dark text for readability.

**Covers:** `%%{init}%%` directive setup, color palette by semantic role (process=purple, decision=amber, success=green, error=red, routing=indigo), subgraph styling with dashed borders, and a complete flowchart example.

**Triggers on:** creating or editing any mermaid diagram in documentation, specs, or PRDs.

## Adding More Skills

```
skills/
├── fp-rust/                              <- existing
├── fp-kstream-design/                    <- existing
├── fp-kstream-implement/                 <- existing
├── mermaid-pastel-style/                 <- existing
├── openspec-progressive-implementation/  <- existing
├── openspec-progressive-superpowers/     <- existing
├── your-new-skill/                       <- add here
│   ├── .claude-plugin/
│   │   └── plugin.json                   (for Claude Code /plugin install)
│   ├── SKILL.md
│   └── references/                       (optional)
```

Each skill directory must contain a `SKILL.md` with YAML frontmatter (`name` and `description` fields). Add a `.claude-plugin/plugin.json` for Claude Code marketplace support, and register the skill in `.claude-plugin/marketplace.json` at the repo root.

## License

MIT
