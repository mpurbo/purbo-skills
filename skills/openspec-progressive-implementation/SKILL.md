---
name: openspec-progressive-implementation
description: >
  Progressive implementation discipline for OpenSpec changes. Structures work
  into independently reviewable, testable phases with clear contract boundaries.
  Use when creating or modifying OpenSpec proposal or tasks artifacts, and when
  executing apply. Triggers: "progressive implementation", "phased implementation",
  "step-by-step", "implementation phases", "chunked implementation". This skill
  augments (does not replace) the standard OpenSpec workflow skills
  (openspec-new-change, openspec-continue-change, openspec-apply-change,
  openspec-ff-change). Do NOT use for non-OpenSpec work — this is specifically
  about structuring OpenSpec artifacts for phased delivery.
---

# OpenSpec Progressive Implementation

Structures OpenSpec changes into independently reviewable, testable implementation
phases with clear contract boundaries between them. This is a **rigid** skill —
follow it exactly, do not adapt away the discipline.

## When This Applies

This skill applies when you are about to create or modify any of these OpenSpec
artifacts: **proposal.md**, **tasks.md**, or when executing **apply**.

---

## Phase Design: Structuring `proposal.md`

When creating a proposal, add an `## Implementation Phases` section. Each phase
must satisfy:

- **Reviewable in one sitting** — a human can review the phase's diff in ≤30 minutes
- **Independently testable** — can be verified (at least with mocks) without prior
  phases being fully implemented
- **Contract-bounded** — exposes explicit types/traits/interfaces for downstream phases
- **Documented** — includes a docs update task for manual verification guidance

### Phase Template

```markdown
## Implementation Phases

### Phase 1: <Name>
**Scope:** <What this phase delivers — concrete, verifiable>
**Contracts exposed:** <Types, traits, interfaces, API schemas defined for downstream>
**Gate:** <Exact command or manual check to verify phase completion>
**Dependencies:** None

### Phase 2: <Name>
**Scope:** <What this phase delivers>
**Contracts consumed:** <What it needs from prior phases — mockable at boundaries>
**Contracts exposed:** <What it defines for downstream>
**Gate:** <Verification command>
**Dependencies:** Phase 1 contracts (not implementation)
```

### Phase Ordering Strategy

1. **Phase 1** — always: domain types, core traits/interfaces, project scaffolding
2. **Middle phases** — pure-core logic first, then effects/IO at boundaries
3. **Final phase** — integration wiring, end-to-end flow

This follows functional architecture: types → pure functions → effects → composition.

### Phase Design Checklist

Before finalizing phases in the proposal:

- [ ] Each phase is reviewable in ≤30 minutes by a human
- [ ] Each phase has ≤8 tasks (split if more)
- [ ] Contract types/interfaces are defined BEFORE implementation that uses them
- [ ] Phase N can be tested with mocks/stubs even if Phase N-1 isn't implemented
- [ ] No phase has hidden coupling to another phase's internals
- [ ] Phase ordering maximizes independence (shared types/contracts early)
- [ ] Each phase has a concrete gate (test command or manual verification step)

---

## Task Grouping: Structuring `tasks.md`

Group tasks under phase headers matching the proposal's phases. Each phase section
includes metadata for execution:

```markdown
## Phase 1: <Name>

**Contract:** Define `<types/traits>` in `<file path>`
**Test command:** `<exact command to run this phase's tests>`
**Docs update:** Update `<file>` with Phase 1 verification steps

- [ ] 1.1 Define contract types/interfaces for this phase
- [ ] 1.2 <task>
- [ ] 1.3 <task>
- [ ] 1.4 Update documentation with Phase 1 testing guide

<!-- PHASE CHECKPOINT: Stop here. Human reviews and tests before proceeding. -->
```

### Rules

1. Every phase's **first** task defines or consumes contracts (types, traits, interfaces)
2. Every phase's **last** task updates documentation for manual verification
3. Every phase ends with a `<!-- PHASE CHECKPOINT -->` comment
4. Tasks within a phase follow TDD (delegate to `superpowers:test-driven-development`)
5. Each phase has ≤8 tasks — split if larger

---

## Execution: Phase-by-Phase Apply

When executing tasks via `openspec-apply-change`:

### Per-Phase Execution

1. **Execute one phase at a time** — complete all tasks under a `## Phase N` heading
   before considering the next
2. **Follow TDD within each task** — use `superpowers:test-driven-development`
3. **At each `<!-- PHASE CHECKPOINT -->`:**
   - Run the phase's gate command
   - Verify documentation was updated
   - Present the human with:
     - What was completed in this phase
     - How to manually test/verify it
     - What the next phase will cover
   - **STOP and wait for human confirmation before starting the next phase**

### Context Management Between Phases

When a phase is executed in a fresh session, load:

- **Contract files** (types, traits, interfaces) from prior phases
- The change's **proposal.md**, **design.md**, **tasks.md**
- **Test files** relevant to the current phase

Do NOT load:

- Full implementation files from prior phases (unless directly consumed)
- Unrelated source files
- Completed phase implementation details

This keeps token consumption bounded per phase. The contract boundary is the
information barrier — prior phase internals are opaque.

---

## Anti-Rationalization Table

| Thought | Reality |
|---------|---------|
| "These two phases are small, I'll combine them" | Small phases are a feature — they enable focused review. Keep them separate. |
| "I'll add the docs at the end" | Docs per phase IS the point — progressive docs guide progressive review. |
| "The contract is obvious, no need to define it" | Explicit contracts enable mock testing and session isolation. Define it. |
| "I'll just keep going past the checkpoint" | The checkpoint exists for human review. Stop. Always. |
| "This phase needs 12 tasks" | Split it. ≤8 tasks per phase. If you can't split, the scope is wrong. |
| "I can skip the gate for this phase" | No gate = no verification = no trust. Every phase gets a gate. |
| "TDD is overkill for this small task" | TDD is non-negotiable per task. Delegate to the TDD skill. |
| "The human can figure out how to test it" | Write the test guide. That's your job, not theirs. |
| "I'll define contracts later when I know more" | Contracts first. Implementation follows contracts, not the reverse. |
