# purbo-skills

Opinionated Claude Code skills for FP-first development.

## Installation

```bash
# In Claude Code:
/plugin marketplace add purbo/purbo-skills
/plugin install fp-rust@purbo-skills
```

## Available Skills

### fp-rust

Functional Programming in Rust — guidelines for writing idiomatic, FP-first Rust code.

**Covers:** functional core / imperative shell architecture, immutability-first patterns (borrow > clone > mutate), pure functions, algebraic data types, pipeline-oriented programming with iterators and Result chains, error handling as data, dependency rejection over dependency injection, type-driven design (parse don't validate, typestate), concurrency via message passing, and recommended crate stack.

**Triggers on:** any Rust coding task — planning, writing, reviewing, refactoring, scaffolding, project setup, Cargo.toml configuration, or code review.

**Based on:** [Functional Rust: Idiomatic FP Guidelines](plugins/fp-rust/skills/fp-rust/references/FP_RUST_GUIDELINES.md), cross-referenced with [Apollo GraphQL Rust Best Practices](https://github.com/apollographql/rust-best-practices).

## Adding More Skills

This marketplace is structured to hold multiple plugins. To add a new skill:

```
plugins/
├── fp-rust/          ← existing
├── your-new-skill/   ← add here
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
