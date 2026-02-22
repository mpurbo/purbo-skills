# purbo-skills

Opinionated Claude Code skills for FP-first development.

## Installation

```bash
# In Claude Code:
/plugin marketplace add mpurbo/purbo-skills
/plugin install fp-rust@purbo-skills
/plugin install fp-kstream@purbo-skills
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

## Adding More Skills

This marketplace is structured to hold multiple plugins. To add a new skill:

```
plugins/
├── fp-rust/          <- existing
├── fp-kstream/       <- existing
├── your-new-skill/   <- add here
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
