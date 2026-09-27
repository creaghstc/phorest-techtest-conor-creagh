# Fruit Machine

[![CI](https://github.com/creaghstc/phorest-techtest-conor-creagh/actions/workflows/ci.yml/badge.svg)](https://github.com/creaghstc/phorest-techtest-conor-creagh/actions/workflows/ci.yml)

A virtual fruit machine (slots game), built as a Spring Boot REST API.

## Running

Requires JDK 25 locally.

```
./mvnw spring-boot:run
```

Starts on `http://localhost:8080`.

Or with Docker:

```
docker build -t fruit-machine .
docker run -p 8080:8080 fruit-machine
```

## Testing

```
./mvnw test
```

Runs the unit and `@WebMvcTest` suite (fast, no real HTTP server) and generates a coverage report at `target/site/jacoco/index.html`.

```
./mvnw verify
```

Also runs `FruitMachineApiIntegrationTests` — a full-stack test that boots a real embedded server and hits the actual HTTP API.

## API

| Method | Path              | Description                                  |
|--------|-------------------|-----------------------------------------------|
| POST   | `/fruit-machine/play` | Spin the machine, return the outcome         |
| GET    | `/fruit-machine`      | Read current balance/free plays/configuration |
| PUT    | `/fruit-machine`      | Replace the machine with a new configuration  |

- **Swagger UI**: `/swagger-ui/index.html`
- **OpenAPI spec**: `/v3/api-docs`
- **Health check**: `/actuator/health`
- **Postman collection**: [`postman/fruit-machine.postman_collection.json`](postman/fruit-machine.postman_collection.json)

`PUT /fruit-machine` validates its request body:

| Field                 | Constraint                                  |
|------------------------|----------------------------------------------|
| `initialBalance`       | `>= 0`                                        |
| `costPerPlay`          | `> 0`                                         |
| `slotCount`             | `> 0`                                         |
| `colourCount`           | `> 0`                                         |
| `smallPrizeRunLength`   | `>= 2` and `<= slotCount`                     |

Errors (validation failures, domain rule violations, or anything unexpected) are returned as an [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) `ProblemDetail`, e.g.:

```json
{
  "detail": "initialBalance: must be greater than or equal to 0",
  "status": 400,
  "title": "Bad Request"
}
```

## Configuration

Set in `application.properties` (or overridden via `PUT /fruit-machine` at runtime):

```
fruitMachine.initialBalance=100.00
fruitMachine.costPerPlay=1.00
fruitMachine.slotCount=4
fruitMachine.colourCount=4
fruitMachine.smallPrizeRunLength=2
```

## Game rules

Each play draws a colour for every slot. A spin pays out:

- **Jackpot** — all slots the same colour: pays the entire balance.
- **Full house** — all slots different colours: pays half the balance.
- **Small prize** — `k` or more adjacent slots the same colour: pays 5× the play cost.

If a prize can't be paid in full, the shortfall is credited as free plays instead (except for the jackpot).

## Logging

Per-spin debug details are off by default. Enable with:

```
logging.level.com.phorest.techtest.domain.FruitMachine=DEBUG
```

## Decision log

- **Money as `BigDecimal`**, never `float`/`double` — avoids rounding errors on balances/payouts.
- **Play cost feeds the float**: each play adds `costPerPlay` to the balance before evaluating the spin (rather than cost being purely informational).
- **Prize precedence**: Jackpot → Full House → Small Prize, checked in that order (a jackpot also trivially satisfies the small-prize adjacency rule, so order matters).
- **Shortfall → free plays** rounds down (floor), and a banked free play skips the next play's cost charge rather than paying out cash.
- **Colours became an id-based record, not an enum**, once colour counts needed to scale into the hundreds — enums can't represent a runtime-sized set.
- **Prize detection is a single linear pass** over the slots (jackpot/full-house/small-prize all computed together) so it stays O(n) as slot count grows.
- **`FruitMachine.spin()`/`state()` are `synchronized`**: it's a shared, mutable singleton once exposed over HTTP; verified with a concurrency test that fails without the lock.
- **`FruitMachineService` holds the machine in an `AtomicReference`**, swapped wholesale on `PUT /fruit-machine`; a new machine is fully built (and validated) *before* the swap, so a failed reconfigure never disturbs the machine currently in place.
- **State lives in memory only** (no database) — a deliberate scope boundary for this exercise, not an oversight; a restart resets the machine.
- **Dedicated `*Dto` request/response types**, decoupled from the domain model (e.g. colours flattened to plain ints), so the API contract can evolve independently of internal types.
- **Errors as RFC 7807 `ProblemDetail`**, for domain validation failures, Bean Validation (`@Valid`) failures, and unmapped exceptions, all via a single `GlobalExceptionHandler` plus `spring.mvc.problemdetails.enabled=true`.
- **`GlobalExceptionHandler` is `@Order(Ordered.HIGHEST_PRECEDENCE)`**: Spring Boot's own `problemdetails` autoconfiguration registers a fallback advice for `MethodArgumentNotValidException` at the same resolver stage; without explicit ordering it wins the tie and returns its generic `"Invalid request content."` instead of the real field-level message.
- **A catch-all `Exception` handler** returns a generic `500 ProblemDetail` for anything unmapped — the real exception is logged server-side (so it's still debuggable) but never put in the response body, so internals can't leak to a client.
- **Integration test kept out of `mvn test`**: `FruitMachineApiIntegrationTests` boots a real server and is only run via `mvn verify` (Failsafe), so day-to-day test runs stay fast.
- **Logging 4XX errors**: For the sake of the PoC, there's an argument not to log 4XX as its a problem on user side not us.

## AI Usage

This project was built with Claude Code. 

My main method of using Claude for this project was to discuss requirements and code patterns before writing any code. I done it this way to ensure Claude was using the structure I had envisioned and to ensure the requirements are clear before we dive into code.

I took this one part at at time with refactoring sessions before commiting each part. The refactoring session was basically a code review from me which covered code structure, quality and testing.

Sometimes the generated code was fine, other times some refactoring was needed, the main refactors are as follows:

- Initial package layout nested `enums`/`config` under a `fruitmachine` subpackage — corrected to sit at the project root.
- The AI started creating files before a design discussion was finished — stopped and asked to hold off until the design was agreed.
- A test helper (`FixedSequenceRandomGenerator`) took raw `int`s — asked to take `Colour` values instead for readability.
- A Javadoc comment was inaccurate, then separately used an ambiguous term ("double") — corrected twice.
- A `BigDecimal` division was missing an explicit rounding mode — a real correctness gap, caught and fixed.
- Inconsistent use of `this.` on field access — asked to apply it consistently.
- `FruitMachine` was left in the `service` package after it stopped being a Spring-managed service — corrected to a `domain` package.
- `FruitMachineService` duplicated configuration-reading that `FruitMachineConfig` already did — pointed out and simplified to reuse the existing bean.
- A variable name (`nominalPrize`) and a loop (`hasAdjacentMatch`) were flagged as unclear — renamed/rewritten for clarity.
- After renaming the integration test class, the README was left with a stale reference to the old name — caught and fixed.
- `GlobalExceptionHandler` only handled `IllegalArgumentException`, so an unmapped exception fell through to Spring Boot's default (differently-shaped) error response, and `@Valid` failures returned a generic `"Invalid request content."` with no field detail — added a `MethodArgumentNotValidException` handler and a catch-all `Exception` handler; the former needed `@Order(Ordered.HIGHEST_PRECEDENCE)` to win over Spring Boot's own built-in `problemdetails` advice, which was silently matching first.

