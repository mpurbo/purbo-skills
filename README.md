# purbo-skills

Opinionated Claude Code skills for FP-first development.

## Installation

```bash
# In Claude Code:
/plugin marketplace add mpurbo/purbo-skills
/plugin install fp-rust@purbo-skills
/plugin install fp-kstream@purbo-skills
/plugin install openspec-progressive@purbo-skills
```

## Available Skills

### fp-rust

Functional Programming in Rust — guidelines for writing idiomatic, FP-first Rust code.

**Covers:** functional core / imperative shell architecture, immutability-first patterns (borrow > clone > mutate), pure functions, algebraic data types, pipeline-oriented programming with iterators and Result chains, error handling as data, dependency rejection over dependency injection, type-driven design (parse don't validate, typestate), concurrency via message passing, and recommended crate stack.

**Triggers on:** any Rust coding task — planning, writing, reviewing, refactoring, scaffolding, project setup, Cargo.toml configuration, or code review.

**Based on:** [Functional Rust: Idiomatic FP Guidelines](plugins/fp-rust/skills/fp-rust/references/FP_RUST_GUIDELINES.md), cross-referenced with [Apollo GraphQL Rust Best Practices](https://github.com/apollographql/rust-best-practices).

### fp-kstream

Kafka Streams topology design and implementation using [Kafka Stream Algebra (KSA)](plugins/fp-kstream/references/KSA.md) — deterministic, replay-safe topologies with functional core / imperative shell architecture.

This plugin provides two skills:

#### fp-kstream-design

Design Kafka Streams topologies using KSA patterns. Produces Mermaid topology diagrams, recipe selection, cost estimates, and compliance checks.

**Triggers on:** "design a topology", "kafka architecture", "stream processing design", "what pattern should I use", "topology review", "KSA".

#### fp-kstream-implement

Implement topologies as testable, deterministic code with pure business logic separated from Kafka wiring. Includes code patterns (stateless transforms, FSMs, enrichment), testing strategy (3-layer), local dev setup (Docker Compose), and Gradle build config.

**Triggers on:** "implement this topology", "write the KStream code", "create the processor", "set up kafka locally", "write tests", "KStream implementation".

**Based on:** [Kafka Stream Algebra v0.5](plugins/fp-kstream/references/KSA.md) — 10 composable recipes covering ingress, enrichment, computation, and egress patterns.

### openspec-progressive

Progressive implementation discipline for [OpenSpec](https://github.com/fission-ai/openspec) changes. Structures work into independently reviewable, testable phases with clear contract boundaries.

**Covers:** phase decomposition in proposals (scope, contracts, gates), task grouping under phase headers, phase checkpoint enforcement during apply, per-phase documentation updates, context management guidance for fresh AI sessions, and anti-rationalization guards.

**Triggers on:** "progressive implementation", "phased implementation", "step-by-step", "implementation phases", "chunked implementation" — or any OpenSpec proposal/tasks/apply workflow where phased delivery is desired.

**Designed to augment** the standard OpenSpec workflow skills (`openspec-new-change`, `openspec-continue-change`, `openspec-apply-change`, `openspec-ff-change`) and composes with `superpowers:test-driven-development` for TDD within each phase.

**Key constraints enforced:**
- ≤8 tasks per phase, reviewable in ≤30 minutes
- Contract-first boundaries — Phase N testable with mocks even without Phase N-1
- Hard stop at phase checkpoints for human review
- Documentation updated every phase (not deferred to the end)

## Adding More Skills

This marketplace is structured to hold multiple plugins. To add a new skill:

```
plugins/
├── fp-rust/               <- existing
├── fp-kstream/            <- existing
├── openspec-progressive/  <- existing
├── your-new-skill/        <- add here
│   ├── .claude-plugin/
│   │   └── plugin.json
│   └── skills/
│       └── your-skill/
│           ├── SKILL.md
│           └── references/
```

Then add an entry to `.claude-plugin/marketplace.json`.

## License

MIT
