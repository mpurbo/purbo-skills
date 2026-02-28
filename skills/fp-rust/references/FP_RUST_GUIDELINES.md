# Functional Rust: Idiomatic FP Guidelines — AI Reference

> Rust is not Haskell. Ownership IS the type-level effect system. Let FP
> principles emerge naturally from Rust's own idioms: ownership, borrowing,
> iterators, enums, and trait composition.

---

## §1 Architecture: Functional Core, Imperative Shell

Structure every program as a **pure functional core** surrounded by a **thin imperative shell** that handles all side effects.

**Data flow:** Shell reads IO → Shell hydrates data → Shell passes pure data to Core → Core returns decisions as values → Shell executes effects → Shell persists.

### Core Rules

- The core NEVER performs IO, reads the clock, or logs.
- The core receives data and returns data (or `Result`/`Option` representing decisions).
- The shell is deliberately dumb — it reads, writes, logs, and calls core's pure functions.
- The shell interprets core's decisions into effects.
- Test the core with pure unit tests (no mocks). Test the shell with integration tests.

### Directory Layout

```
src/
├── main.rs              # Shell: wiring, IO, entry point
├── shell/               # Adapters: HTTP, DB, FS, CLI
│   ├── http_adapter.rs
│   ├── db_adapter.rs
│   └── cli.rs
├── core/                # Pure domain logic — no `use std::fs`, no `use tokio`
│   ├── mod.rs
│   ├── types.rs         # ADTs, newtypes, domain models
│   ├── transform.rs     # Pure transformations
│   └── validate.rs      # Validation combinators
└── lib.rs               # Re-exports core (makes it testable as a library)
```

**Litmus test:** If a file in `core/` has `use std::io` or `use tokio`, it belongs in `shell/`.

**On `tracing` in core:** Tolerated as pragmatic concession when: (a) purely diagnostic, (b) tests never depend on log output, (c) correctness unchanged if logging disabled. If you need observable events as domain logic, return them as data (`Vec<Command>` / `Vec<DomainEvent>`).

---

## §2 Immutability by Default

### Priority: Borrow > Clone > Mutate

```text
1. BORROW (&T)      — Zero cost. Pure functions take &T input.
2. CLONE / COPY     — Create new value. Functional transforms.
3. MUTATE (&mut T)  — Last resort. Encapsulate if needed.
```

Borrow for reads, create-new for writes. A function `fn calculate(&Order, &RateTable) -> Decimal` is both pure AND zero-copy.

### Rules

| Prefer | Avoid | Why |
|--------|-------|-----|
| `let x = ...` | `let mut x = ...` | Immutable by default |
| `fn foo(s: &str)` | `fn foo(s: String)` | Borrow when you don't need ownership |
| `fn foo(s: &[T])` | `fn foo(s: Vec<T>)` | Accept slices, not owned collections |
| Create new values via transformation | Mutate in place | Referential transparency |
| `Iterator` chains returning new collections | `for` loops pushing into a `mut Vec` | Declarative over imperative |
| `struct` with all private fields + constructor | Public `mut` fields | Controlled construction |

### Copy Types: Pass by Value

Small `Copy` types — pass by value, not reference:

```rust
// ✅ Pass Copy types by value
fn is_eligible(age: u32, threshold: u32) -> bool { age >= threshold }

// ❌ Unnecessary indirection
fn is_eligible(age: &u32, threshold: &u32) -> bool { *age >= *threshold }
```

Copy types to pass by value: `i32`, `u64`, `f64`, `bool`, `char`, `Duration`, `Decimal` (if Copy), small enums without heap data, `Option<i32>`.

### When Cloning Is Justified

Clone for transformations (creating new values), never as borrow-checker escape hatch:

```rust
// ✅ Intentional clone: transformation
let updated = Order { total: new_total, ..original.clone() };

// ❌ Escape-hatch clone
let data = shared_ref.clone();  // "I don't understand lifetimes"
process(&data);
```

### Defer Allocation

Keep data as references and iterators as long as possible. Defer `.to_string()`, `.to_owned()`, `.collect()` until the last moment:

```rust
// ✅ Defer: filter first, then collect
let active_names: Vec<String> = users
    .iter()
    .filter(|u| u.is_active())      // still borrowing
    .map(|u| u.name.to_owned())     // allocate only for survivors
    .collect();                      // single allocation

// ❌ Eager: allocates for ALL before filtering
let all_names: Vec<String> = users.iter().map(|u| u.name.to_string()).collect();
let active_names: Vec<String> = all_names.into_iter().filter(|n| is_active(n)).collect();
```

### When `mut` Is Acceptable

- Performance-critical hot paths (profiling confirms).
- Builder patterns internal to a constructor — mutation is encapsulated.
- Accumulator variables in fold-style loops when iterator version is less readable (§12).

```rust
// ✅ Best: take ownership, transform, return — no clone needed
// The `mut` is internal and invisible to caller.
fn with_discount(mut order: Order, pct: f64) -> Order {
    order.total *= 1.0 - pct;
    order
}

// ✅ Also fine: borrow + struct update for small structs
fn with_discount_from_ref(order: &Order, pct: f64) -> Order {
    Order { total: order.total * (1.0 - pct), ..order.clone() }
}

// ❌ Mutating through shared mutable reference
fn apply_discount(order: &mut Order, pct: f64) { order.total *= 1.0 - pct; }
```

**Key insight:** `fn(mut self) -> Self` is NOT impure mutation — it's a value-to-value transform where Rust reuses memory. The caller passes ownership in and gets a new value out. Don't clone the world just to "look functional."

### Struct Update Syntax ("Functional Record Update")

```rust
let updated = Config { timeout: Duration::from_secs(30), ..current_config };
```

---

## §3 Pure Functions

A function is pure if it: (1) always returns same output for same input, (2) produces no side effects.

```rust
// Pure
fn calculate_fee(amount: Decimal, rate: Rate) -> Decimal { amount * rate.as_decimal() }

// Pure (Result is deterministic)
fn parse_transfer(raw: &str) -> Result<Transfer, ParseError> { /* ... */ }

// NOT pure (reads clock)
fn calculate_fee_with_timestamp(amount: Decimal) -> (Decimal, SystemTime) {
    (amount * FEE_RATE, SystemTime::now())
}
```

### Refactoring Impurity Out

Inject impure dependencies as parameters (dependency rejection):

```rust
// ❌ Impure: secretly reads clock
fn is_expired(token: &Token) -> bool { token.expires_at < SystemTime::now() }

// ✅ Pure: time is explicit input
fn is_expired(token: &Token, now: SystemTime) -> bool { token.expires_at < now }
```

### Signature Tells the Story

```rust
// Readable: inputs → output, no hidden channels
fn settle_batch(orders: &[Order], rates: &RateTable) -> Vec<Settlement>
```

No `&mut self`, no `&dyn SomeService`, no global state.

---

## §4 Algebraic Data Types

### Sum Types: Model Possibilities, Not Flags

```rust
// ✅ Sum type: each variant is a distinct state
enum PaymentStatus {
    Pending { initiated_at: DateTime<Utc> },
    Completed { settled_at: DateTime<Utc>, reference: String },
    Failed { reason: FailureReason, retryable: bool },
    Refunded { original_ref: String, refund_ref: String },
}

// ❌ Flag soup: stringly typed, invalid states representable
struct PaymentStatus {
    status: String,
    settled_at: Option<DateTime<Utc>>,
    reason: Option<String>,
}
```

### Make Illegal States Unrepresentable

```rust
struct UnverifiedUser { email: Email }
struct VerifiedUser { email: Email, verified_at: DateTime<Utc> }
struct ActiveUser { email: Email, verified_at: DateTime<Utc>, subscription: Plan }

fn verify(user: UnverifiedUser, now: DateTime<Utc>) -> VerifiedUser {
    VerifiedUser { email: user.email, verified_at: now }
}

fn activate(user: VerifiedUser, plan: Plan) -> ActiveUser {
    ActiveUser { email: user.email, verified_at: user.verified_at, subscription: plan }
}
```

### Newtypes: Semantic Precision

```rust
struct UserId(Uuid);
struct OrderId(Uuid);
struct Amount(Decimal);
struct Rate(Decimal);
```

### Exhaustive Pattern Matching

Match exhaustively. Avoid `_ =>` as catch-all that silently swallows future variants:

```rust
// ✅ Exhaustive
match status {
    PaymentStatus::Pending { .. } => handle_pending(),
    PaymentStatus::Completed { reference, .. } => handle_completed(reference),
    PaymentStatus::Failed { reason, retryable } => handle_failed(reason, retryable),
    PaymentStatus::Refunded { .. } => handle_refunded(),
}

// ✅ Acceptable: _ with explicit comment when intentionally grouping
match log_level {
    Level::Error | Level::Fatal => alert(),
    _ => {} // intentionally ignoring non-critical levels
}

// ❌ Dangerous: _ hiding future variants
match status {
    PaymentStatus::Completed { .. } => handle_completed(),
    _ => {}  // silently ignores Pending, Failed, Refunded, and future variants
}
```

---

## §5 Pipeline-Oriented Programming

### Iterator Pipelines

```rust
let active_totals: Vec<Decimal> = orders
    .iter()
    .filter(|o| o.status.is_active())
    .map(|o| calculate_total(o, &rate_table))
    .filter(|total| *total > Decimal::ZERO)
    .collect();
```

### Key Pipeline Combinators

| Combinator | FP Analog | Use For |
|------------|-----------|---------|
| `.map()` | Functor map | Transform each element |
| `.filter()` | Guard | Keep elements matching condition |
| `.filter_map()` | map + flatten | Transform and discard `None`s |
| `.flat_map()` | Monad bind (>>=) | One-to-many expansion |
| `.fold()` / `.reduce()` | Catamorphism | Collapse to single value |
| `.scan()` | Stateful map | Transform with running accumulator |
| `.zip()` | Product | Pair two iterators |
| `.chain()` | Concatenation | Sequence two iterators |
| `.take_while()` / `.skip_while()` | Prefix predicate | Conditional slicing |
| `.partition()` | Branching | Split by predicate |

### Lazy Iterators: Defer `.collect()`

```rust
// ✅ Return iterator — let caller decide when to materialize
fn active_orders(orders: &[Order]) -> impl Iterator<Item = &Order> {
    orders.iter().filter(|o| o.is_active())
}

// Caller chains without allocating intermediate collections
let high_value_count = active_orders(&orders)
    .filter(|o| o.total > threshold)
    .count();  // single pass, zero allocation
```

Prefer returning `impl Iterator<Item = T>` over `Vec<T>` unless caller needs random access or multiple iterations.

### Option as Functor/Monad

```rust
let display_name: Option<String> = user
    .nickname
    .or_else(|| user.full_name())
    .map(|name| name.trim().to_owned())
    .filter(|name| !name.is_empty());
```

### Result as Monad (Railway-Oriented Programming)

```rust
fn process_payment(raw: &str) -> Result<Receipt, ProcessError> {
    parse_request(raw)
        .and_then(validate_amount)
        .and_then(|req| apply_rules(req, &rules))
        .map(generate_receipt)
}
```

### The `?` Operator: Syntactic Monadic Bind

```rust
fn process_payment(raw: &str) -> Result<Receipt, ProcessError> {
    let request = parse_request(raw)?;
    let valid   = validate_amount(request)?;
    let applied = apply_rules(valid, &rules)?;
    Ok(generate_receipt(applied))
}
```

Use `?` when steps need intermediate bindings or are complex. Use `.and_then()` when the pipeline is linear and each step is a single function call.

### Iterator vs `for` — Preference Order

```text
Prefer (most FP)                            Accept (least FP)
.iter().map().filter().collect()   >   for + match (no mutation)
                                   >   for + let mut accumulator
```

Default to iterator pipelines. Use `for` when body has complex control flow (early returns, continue with conditions) that would require deeply nested closures. Never use `for` to push into a `mut Vec` when `.map().collect()` would do.

---

## §6 Error Handling as Data

### Domain Error Enums

```rust
#[derive(Debug, thiserror::Error)]
enum TransferError {
    #[error("insufficient balance: have {available}, need {required}")]
    InsufficientBalance { available: Decimal, required: Decimal },
    #[error("recipient account {0} not found")]
    RecipientNotFound(AccountId),
    #[error("transfer limit exceeded: {amount} > {limit}")]
    LimitExceeded { amount: Decimal, limit: Decimal },
}
```

### Error Conversion with `thiserror`

```rust
#[derive(Debug, thiserror::Error)]
enum ProcessError {
    #[error(transparent)]
    Parse(#[from] ParseError),
    #[error(transparent)]
    Validation(#[from] ValidationError),
}
```

### Error Rules

- NEVER `unwrap()` or `expect()` in library/core code. Propagate with `?`.
- `unwrap()` / `expect()` ONLY in: `main()`/shell, tests, provably unreachable cases (with comment).
- NEVER use `panic!` for expected error conditions. Panics are for bugs.
- Use `anyhow::Result` in shell for convenience; typed errors in core for precision.
- Test both `Ok` and `Err` paths for every `Result`-returning core function.

---

## §7 State Without Mutation

### Fold / Reduce Over Mutation

```rust
// ✅ Fold
let total = line_items.iter().fold(Decimal::ZERO, |acc, item| acc + item.subtotal());

// ❌ Mutation
let mut total = Decimal::ZERO;
for item in &line_items { total += item.subtotal(); }
```

### Scan for Running State

```rust
let running_balances: Vec<Decimal> = transactions
    .iter()
    .scan(initial_balance, |balance, tx| {
        *balance += tx.amount;  // scan's accumulator is necessarily mut
        Some(*balance)
    })
    .collect();
```

`scan`'s internal mutation is encapsulated — the outer world sees only the iterator of snapshots.

### Recursive Structures

```rust
enum Expr {
    Literal(f64),
    Add(Box<Expr>, Box<Expr>),
    Mul(Box<Expr>, Box<Expr>),
}

fn eval(expr: &Expr) -> f64 {
    match expr {
        Expr::Literal(v) => *v,
        Expr::Add(a, b) => eval(a) + eval(b),
        Expr::Mul(a, b) => eval(a) * eval(b),
    }
}
```

**Caution:** Rust has no TCO guarantee. For deep recursion, consider trampoline patterns or iterative rewriting. Deeply nested `Box` structures can stack-overflow during `Drop` — use arena allocators (`bumpalo`, `typed-arena`) for deep ASTs.

---

## §8 Side-Effect Management & Dependency Rejection

### The Dependency Rejection Pattern

Pass data in and get data out. No trait objects, no mocks.

```rust
// ❌ Dependency injection — needs mocks to test
fn process_order(repo: &dyn OrderRepo, id: OrderId) -> Result<Receipt, Error> {
    let order = repo.find(id)?;
    let receipt = compute_receipt(order);
    repo.save(&receipt)?;
    Ok(receipt)
}

// ✅ Dependency rejection — core is pure, shell does IO
fn compute_receipt(order: &Order) -> Receipt {
    Receipt { /* pure computation */ }
}

// Shell orchestrates
fn handle_process_order(repo: &PgOrderRepo, id: OrderId) -> Result<Receipt, Error> {
    let order = repo.find(id)?;            // IO in shell
    let receipt = compute_receipt(&order);  // Pure core
    repo.save(&receipt)?;                   // IO in shell
    Ok(receipt)
}
```

### Commands as Data (When Core Needs to Express Effects)

```rust
enum Command {
    SendNotification { to: UserId, message: String },
    UpdateBalance { account: AccountId, new_balance: Decimal },
    LogAudit { entry: AuditEntry },
}

fn process(event: DomainEvent) -> Vec<Command> {
    match event {
        DomainEvent::PaymentReceived { from, amount } => vec![
            Command::UpdateBalance { account: from, new_balance: compute_new(amount) },
            Command::SendNotification { to: from, message: format!("Received {amount}") },
            Command::LogAudit { entry: AuditEntry::payment_received(from, amount) },
        ],
    }
}

// Shell interprets
async fn execute(commands: Vec<Command>, deps: &AppDeps) -> Result<(), Error> {
    for cmd in commands {
        match cmd {
            Command::SendNotification { to, message } => deps.notifier.send(to, &message).await?,
            Command::UpdateBalance { account, new_balance } => deps.repo.update_balance(account, new_balance).await?,
            Command::LogAudit { entry } => deps.audit_log.append(entry).await?,
        }
    }
    Ok(())
}
```

### When Trait Abstraction Is Justified

Use traits when: (a) genuinely multiple runtime backends (not "for testing"), (b) writing a library consumed by others. Keep traits small (ISP), prefer generics over trait objects.

### Dispatch Preference Order

```text
Prefer (most FP)                              Accept (least FP)
Dependency rejection    >   Generic (impl Trait / <T: Trait>)
(pass data, not traits)     (static dispatch, zero-cost)
                        >   dyn Trait (dynamic dispatch)
                            (only when truly polymorphic at runtime)
```

---

## §9 Higher-Order Functions & Closures

```rust
// Functions as parameters
fn apply_pricing(items: &[Item], strategy: fn(&Item) -> Decimal) -> Vec<Decimal> {
    items.iter().map(strategy).collect()
}

// Returning closures
fn multiplier(factor: Decimal) -> impl Fn(Decimal) -> Decimal {
    move |x| x * factor
}

// Predicate combinators
fn and<T>(f: impl Fn(&T) -> bool, g: impl Fn(&T) -> bool) -> impl Fn(&T) -> bool {
    move |x| f(x) && g(x)
}

let eligible = orders.iter().filter(and(is_active, above_threshold)).collect::<Vec<_>>();
```

Iterator chains and `?`-pipelines are Rust's natural composition mechanism — prefer those over custom `compose` functions.

---

## §10 Type-Driven Design

### Parse, Don't Validate

```rust
struct PositiveAmount(Decimal);

impl PositiveAmount {
    fn new(value: Decimal) -> Result<Self, ValidationError> {
        if value > Decimal::ZERO { Ok(Self(value)) }
        else { Err(ValidationError::NotPositive(value)) }
    }
    fn value(&self) -> Decimal { self.0 }
}

// Downstream: type guarantees validity
fn calculate_fee(amount: PositiveAmount, rate: Rate) -> Decimal {
    amount.value() * rate.value()
}
```

### Typestate Pattern

```rust
struct Order<S: OrderState> { id: OrderId, items: Vec<Item>, state: S }

struct Draft;
struct Confirmed { confirmed_at: DateTime<Utc> }
struct Shipped { tracking: TrackingId }

trait OrderState {}
impl OrderState for Draft {}
impl OrderState for Confirmed {}
impl OrderState for Shipped {}

impl Order<Draft> {
    fn confirm(self, now: DateTime<Utc>) -> Order<Confirmed> {
        Order { id: self.id, items: self.items, state: Confirmed { confirmed_at: now } }
    }
}

impl Order<Confirmed> {
    fn ship(self, tracking: TrackingId) -> Order<Shipped> {
        Order { id: self.id, items: self.items, state: Shipped { tracking } }
    }
}
// Compile error: can't ship a Draft order
```

### Generic Traits as Typeclasses

```rust
trait Summable {
    fn zero() -> Self;
    fn combine(self, other: Self) -> Self;
}

fn sum_all<T: Summable>(items: impl Iterator<Item = T>) -> T {
    items.fold(T::zero(), T::combine)
}
```

This is effectively a Monoid. Rust's trait system supports typeclass-style programming naturally.

---

## §11 Concurrency the Functional Way

### Immutable Shared State

Prefer `Arc<T>` over `Arc<Mutex<T>>`:

```rust
let config = Arc::new(load_config()?);
let config_clone = Arc::clone(&config);
tokio::spawn(async move { handle_request(&config_clone).await });
```

### Message Passing Over Shared State

```rust
let (tx, mut rx) = tokio::sync::mpsc::channel::<Command>(100);

let commands = process_events(&events);
for cmd in commands { tx.send(cmd).await?; }

while let Some(cmd) = rx.recv().await { execute(cmd, &deps).await?; }
```

### Parallel Pipelines (Rayon)

```rust
use rayon::prelude::*;
let results: Vec<_> = inputs.par_iter().map(|i| pure_transform(i)).filter(|r| r.is_valid()).collect();
```

### `async` as Lazy Effect Description

Think of `async fn` as returning a description of a computation (a `Future`), not executing it. The runtime (Tokio) is the interpreter. Keep async in the shell:

```rust
async fn handle(req: Request, deps: &Deps) -> Result<Response, Error> {
    let data = deps.db.fetch(req.id).await?;    // effect: DB read
    let result = pure_transform(&data);          // pure core
    deps.db.save(&result).await?;                // effect: DB write
    Ok(to_response(result))
}
```

**Production notes:**
- `Send + Sync` bounds propagate in async. Pure core functions are naturally `Send` (no `Rc`, no `RefCell`).
- Cancellation safety: `tokio::select!` can drop futures mid-flight. Design async shell functions so each `.await` completes atomically or uses RAII for rollback.

### Pointer Selection (with FP Preference)

| Type | Use Case | FP Preference |
|------|----------|---------------|
| `&T` / `&mut T` | Borrowing | ✅ Default |
| `Box<T>` | Heap allocation, recursive types | ✅ Fine |
| `Rc<T>` | Shared ownership (single thread) | ⚠️ Prefer owned or `&T` |
| `Arc<T>` | Shared ownership (multi-thread) | ✅ Good for immutable shared config |
| `Arc<Mutex<T>>` | Shared mutable state | ❌ Use channels or immutable snapshots |
| `Cell<T>` / `RefCell<T>` | Interior mutability | ⚠️ Only for memoization/caching |

---

## §12 Practical Concessions

| Situation | Concession | Rationale |
|-----------|-----------|-----------|
| Performance-critical inner loops | `mut` locals, pre-allocated `Vec` | Encapsulate the mutation |
| Deep recursion | Iterative with explicit stack | No TCO in Rust |
| Complex multi-step init | Builder with `mut self` | Produced value is immutable |
| Interior mutability for caching | `OnceCell`, `LazyLock`, `Cell`/`RefCell` | Memoization is referentially transparent |
| `&mut self` on types with invariants | Acceptable when ownership is clear | Ownership prevents aliased mutation |
| Logging in core | `tracing` for diagnostics only | Never for correctness; return events as data |
| Complex fold readability | `for` loop with `mut` accumulator | Readability wins over purity |

### Concession Litmus Test

Before using `mut` or side effect in core:

1. Can I restructure to avoid it? (Return a value instead of mutating?)
2. Is the mutation encapsulated? (Visible only within this function?)
3. Does the function remain deterministic from caller's perspective?

If all three: proceed with a brief comment explaining the trade-off.

---

## §13 Decision Checklist

```text
┌─ WRITING A FUNCTION ──────────────────────────────────────────────┐
│                                                                   │
│  □ Does it need IO / clock / randomness?                          │
│    → YES: Shell. Inject data, not services.                       │
│    → NO:  Core. Pure inputs → pure outputs.                       │
│                                                                   │
│  □ Can parameters be borrowed instead of owned?                   │
│    → Prefer &T, &str, &[T] over T, String, Vec<T>.               │
│    → Pass Copy types (i32, bool, etc.) by value.                  │
│                                                                   │
│  □ Does it use `mut`?                                             │
│    → Replace with transformation / fold / map?                    │
│    → If needed, is it encapsulated and invisible to caller?       │
│                                                                   │
│  □ Does it use `unwrap()` / `expect()`?                           │
│    → Shell / test / provably safe only.                           │
│    → In core: replace with `?` or explicit error handling.        │
│                                                                   │
│  □ Can the types be tighter?                                      │
│    → String → Newtype? Option → Separate type? bool → Enum?       │
│                                                                   │
│  □ Is data flowing through a pipeline?                            │
│    → .iter().map().filter().collect() or and_then chains.         │
│    → Defer .collect() — return impl Iterator when possible.       │
│                                                                   │
│  □ Is a dependency needed?                                        │
│    → Dependency rejection first (pass data, not services).        │
│    → Trait abstraction only for real polymorphism.                 │
│                                                                   │
│  □ Is error handling complete?                                    │
│    → Domain errors as enums? Exhaustive matching?                 │
│    → Both Ok and Err paths tested?                                │
│                                                                   │
│  □ Is it documented and linted?                                   │
│    → Public items have /// doc comments?                          │
│    → cargo clippy passes with no warnings?                        │
│    → Comments explain "why", not "what"?                          │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

---

## Appendix A: Crate Recommendations

| Purpose | Crate | Notes |
|---------|-------|-------|
| Error handling (core) | `thiserror` | Derive `Error` for domain enums |
| Error handling (shell) | `anyhow` | Convenient `Result<T>` for shell code |
| Decimal math | `rust_decimal` | Avoid floating-point for financial amounts |
| Serialization | `serde` + `serde_json` | Derive-based, composable |
| Date/Time | `chrono` or `time` | Inject `now` from the shell |
| Async runtime | `tokio` | Shell only — core stays sync and pure |
| Parallel iterators | `rayon` | Drop-in `.par_iter()` for CPU-bound work |
| CLI parsing | `clap` | Declarative derive-based argument parsing |
| Tracing | `tracing` | Structured logging; benign side effect |
| Property testing | `proptest` | Test pure functions with generated inputs |
| Snapshot testing | `insta` | Snapshot-based regression tests |
| Linting | `clippy` (built-in) | Configure via `[workspace.lints.clippy]` |
| Pipe / tap | `tap` | Pipe and tap operators for pipeline readability |
| Iterator extensions | `itertools` | Extended pipeline combinators |
| Newtype ergonomics | `derive_more` | Derive Display, From, Into, Add for newtypes |

### Minimal Cargo.toml

```toml
[dependencies]
# ── Always ──
itertools = "0.14"
tap = "1"
derive_more = { version = "1", features = ["full"] }
thiserror = "2"
serde = { version = "1", features = ["derive"] }
rust_decimal = "1"

# ── Shell layer ──
anyhow = "1"
tokio = { version = "1", features = ["full"] }
tracing = "0.1"
futures = "0.3"

# ── When needed ──
# frunk = "0.4"         # Validated, Semigroup, Monoid
# imbl = "4"            # Persistent immutable collections
# rayon = "1"           # Parallel iterators
# proptest = "1"        # Property-based testing (dev-dependency)
```

---

## Appendix B: FP ↔ Rust Concept Map

| FP Concept | Rust Equivalent | Notes |
|------------|----------------|-------|
| Immutability | `let` (default) | `mut` is opt-in |
| Sum type | `enum` | With associated data per variant |
| Product type | `struct` / tuple | Named or positional fields |
| Pattern matching | `match` | Exhaustive by default |
| Functor | `.map()` on Option, Result, Iterator | No Functor trait needed |
| Monad bind | `.and_then()` / `?` operator | Railway-oriented programming |
| Typeclass | `trait` + `impl` | Coherence rules differ from Haskell |
| Monoid | Custom trait with `zero()` + `combine()` | Or `Default` + `Add` |
| Newtype | Single-field tuple struct | `struct Foo(Bar)` |
| Effect system | Ownership + Result/Option | Rust's type system IS the effect system |
| Lazy evaluation | Iterator (lazy by default) | `.collect()` forces evaluation |
| Referential transparency | Ownership prevents aliased mutation | Enforced at compile time |

---

## Appendix C: FP-Enhancing Libraries

### Always Include

**itertools** — Extended pipeline combinators. Key additions: `.cartesian_product()`, `.group_by()`, `.unique()`, `.intersperse()`, `.sorted_by()`, `.tuple_windows()`, `.fold_ok()`, `.process_results()`.

**tap** — `.tap()` for transparent inspection, `.pipe()` for suffix-position free function calls (like F#/Elixir `|>`). Essential for pipeline readability.

**derive_more** — Eliminates newtype boilerplate. Derives Display, From, Into, Deref, Add, Constructor for newtype wrappers.

### Use When Needed

**frunk (Validated)** — Accumulates ALL validation errors instead of short-circuiting. Use for validation-heavy boundaries (API handlers, config parsing, forms). `Result` with `?` short-circuits at the first error; `Validated` collects them all.

**frunk (Semigroup/Monoid)** — Composable domain aggregation (combining partial configs, merging audit logs).

**imbl** — Persistent (structurally shared) immutable collections. Use for undo/redo, concurrent snapshots, event sourcing replay. Don't use as blanket Vec/HashMap replacement.

### Avoid

**fp-core.rs, rust-effects, fp-library, do_notation** — HKT emulation crates that fight Rust's type system. Impenetrable type signatures, poor compile times, no ecosystem support. Rust's Option/Result/Iterator already ARE functors and monads — they don't need to be called that to work.

### Decision

```text
itertools ............... YES, always
tap .................... YES, always
derive_more ............ YES, if using newtypes (you should be)
thiserror + anyhow ..... YES, always
frunk (Validated) ...... IF validation-heavy boundaries
imbl ................... IF cheap collection versioning needed
rxRust ................. WAIT for stable 1.0
fp-core / HKT crates .. AVOID
```

---

## Appendix D: Clippy Configuration

```toml
[workspace.lints.clippy]
# Correctness (deny)
correctness = { level = "deny", priority = -1 }

# Suspicious patterns (warn)
suspicious = { level = "warn", priority = -1 }

# FP-aligned style lints
needless_pass_by_value = "warn"
cloned_instead_of_copied = "warn"
redundant_clone = "warn"
manual_map = "warn"
manual_filter_map = "warn"
unnecessary_wraps = "warn"
needless_collect = "warn"
flat_map_option = "warn"
iter_on_single_items = "warn"
map_unwrap_or = "warn"

# General quality
pedantic = { level = "warn", priority = -1 }
nursery = { level = "warn", priority = -1 }

# Pedantic overrides (too noisy)
module_name_repetitions = "allow"
must_use_candidate = "allow"
missing_errors_doc = "allow"
```

Then in each crate: `[lints] workspace = true`

### Linting Rules

1. Always run `cargo clippy` before committing. Integrate in CI.
2. Fix warnings, don't silence them. `#[allow(clippy::...)]` requires a comment explaining why.
3. Use `cargo clippy --fix` as a learning tool.
4. Profile before optimizing: `cargo install flamegraph && cargo flamegraph --bin my_app`

---

## Appendix E: Testing Strategy

### Testing Pyramid (aligned with Functional Core architecture)

- **Core functions → unit tests + property tests.** No mocks, no setup, no IO.
- **Shell functions → integration tests.** Test actual HTTP/DB/FS behavior.
- **Public API → doc tests.** Serve as both documentation and regression tests.
- **Complex outputs → snapshot tests.** Use `cargo-insta` for JSON responses, error messages.

```rust
/// Calculate the fee for a given amount and rate.
///
/// # Examples
///
/// ```
/// use my_crate::core::calculate_fee;
/// use rust_decimal_macros::dec;
///
/// let fee = calculate_fee(dec!(1000), Rate::standard());
/// assert_eq!(fee, dec!(25));
/// ```
pub fn calculate_fee(amount: Decimal, rate: Rate) -> Decimal {
    amount * rate.value()
}
```

### Documentation Rules

1. Comments explain "why", not "what". If you need to explain what, the code should be clearer.
2. Don't write "living comments" that restate code — they rot immediately.
3. Replace comments with code: extract a well-named function.
4. Every public item gets a `///` doc comment.
5. Include `# Examples` with runnable doc tests.
6. Document panics and errors.
7. Document safety invariants for `unsafe` blocks.

### Stack vs Heap

- Prefer stack-allocated types for small, fixed-size data.
- Use `Box<T>` for large types (>100 bytes), recursive types, trait objects.
- Prefer `&str` over `String`, `&[T]` over `Vec<T>` in parameters when ownership isn't needed.
