# Expense Splitter — Project Plan

A Spring Boot app for splitting shared group expenses and computing the minimal set of
settlement transactions needed to square everyone up.

## Goal

Portfolio project demonstrating: JPA/relational modeling, layered Spring Boot architecture,
and a real algorithm (not just CRUD) — built independently, understood well enough to defend
in an interview.

## Domain Model

- **Group** — a set of people splitting expenses (e.g. "Cabin Trip")
- **Member** — belongs to a Group
- **Expense** — who paid, amount, which members it's split between, split type (start with
  equal split only — don't build percentage/exact-amount splitting until equal works
  end-to-end)
- **Balance** — NOT a persisted entity. Always derived/computed from Expenses at request time.
  (Persisting a derived value invites data-integrity bugs — a classic thing to be asked about
  in an interview, so know why this choice was made.)

## The Algorithm (the centerpiece)

1. Compute each member's net balance from all expenses in a group (positive = owed money,
   negative = owes money).
2. Run **debt simplification**: repeatedly match the largest creditor with the largest debtor,
   settle as much as possible, repeat until all balances are ~zero.
3. This is a greedy solution to the "minimum cash flow" problem. It is NOT provably optimal in
   the strict minimum-transaction-count sense — the true optimal version is NP-hard (subset-sum
   flavored). Greedy-largest-to-largest is the right scope for this project.
4. Know *why* the optimal approach wasn't attempted — that's a good interview answer in itself.

## Layers

- **Controller** — REST endpoints (create group, add member, add expense, get balances, get
  settlement plan)
- **Service** — business logic, including the settlement algorithm
- **Repository** — Spring Data JPA, one repo per entity
- **Entity** — JPA-annotated domain classes
- **Database** — Postgres via Docker Compose (better interview story than H2)

## Build Order

1. Entities + repositories — verify persistence with a few `@SpringBootTest` sanity tests
2. Add-expense endpoint — storage only, no splitting logic yet
3. Balance calculation — plain Java class, no framework dependency, unit tested in isolation
4. Settlement/minimization algorithm — plain Java, heavily unit tested (this is the interview
   centerpiece)
5. Wire balance calc + settlement algorithm into REST endpoints
6. Only then: auth, validation polish, exception handling, API docs

## Working Notes

- Build it step by step, in order above. Resist jumping ahead or pivoting frameworks mid-task.
- Use Claude Code (JetBrains plugin) for boilerplate/config/debugging you already understand —
  not for generating logic you haven't built yet.
- Write the settlement algorithm yourself. Be able to explain every line of it unprompted.