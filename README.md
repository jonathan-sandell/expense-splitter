# Expense Splitter

A Spring Boot app for splitting shared expenses within a group and working out the smallest set of payments needed to settle up. Think splitting costs after a trip or in a shared house, where instead of everyone paying everyone back individually, you get a short list of "who pays who."

## How it works

You create a group, add members, and log expenses as people pay for things. From that expense history, the app works out each member's net balance and then a short list of payments that settles everyone up.

Balances aren't stored anywhere. They're recalculated from the expense history on every request, so there's no risk of a stored number drifting out of sync with the expenses behind it.

## Stack

Java 25 and Spring Boot 4.1, with Spring Data JPA on top of PostgreSQL (H2 for tests). Maven for the build. The frontend is plain HTML, CSS and JS with no build step and no framework.

## Running it

Start Postgres with Docker Compose.

```
docker compose up -d
```

Then run the app.

```
./mvnw spring-boot:run
```

It comes up on `http://localhost:8080` with a small UI for creating groups, adding members and expenses, and checking balances and settlement. API docs live at `/swagger-ui/index.html`.

## API

POST `/api/groups` creates a group. GET `/api/groups/{id}` fetches one.

POST `/api/groups/{id}/members` adds a member, GET lists them.

POST `/api/groups/{id}/expenses` logs an expense split equally among the participants you name, GET lists them.

GET `/api/groups/{id}/balances` returns each member's net balance. GET `/api/groups/{id}/settlement` returns the actual payments needed to settle up.

## The settlement algorithm

It works in two steps. First, balance calculation walks every expense in a group and works out each member's net position, positive if they're owed money, negative if they owe it. All the math runs in integer cents rather than floating point, so nothing drifts by a fraction of a cent over a long expense history.

Second is debt simplification. It repeatedly takes the largest creditor and the largest debtor, settles as much of that pair as it can, and keeps going until everyone's back to zero. That's a greedy solution to what's usually called the minimum cash flow problem.

This doesn't always land on the absolute fewest possible transactions. Finding the true minimum turns out to be a much harder problem to solve. Matching the largest amounts first gets close in practice, and it's simple enough to actually test and be confident in.

## Tests

```
./mvnw test
```

These cover persistence, the balance and settlement logic in isolation, and the full HTTP flow end to end (create a group, add members, add an expense, check balances and settlement), plus error handling for bad input and missing groups.

## Scope

There's no authentication. This is a single tenant demo, not something meant to be deployed for multiple users. Splitting is equal only for now. Percentage or exact amount splits would be the natural next step if this grew past a portfolio project.
