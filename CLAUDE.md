# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Single-class library implementing Martin Fowler's Money/Value pattern (PofEAA p.488). `Money` is an immutable value type: a `long amount` in minor units (cents/pennies) plus a `java.util.Currency`. The whole library is `src/main/java/com/pobox/common/model/Money.java`.

## Commands

Java 25 and Maven 3.6–3.9 (enforced by `maven-enforcer-plugin`).

- Build + test: `mvn verify`
- Test only: `mvn test`
- Single test class: `mvn test -Dtest=MoneyTest`
- Single method: `mvn test -Dtest=MoneyTest#methodName`
- Package jar: `mvn package`

Surefire runs tests **in random order and in parallel** (`parallel=both`, 10 threads), so tests must be order-independent and thread-safe. `Abstract*.java` is excluded; only `*Test`/`*Tests` run.

## Architecture & conventions

- **Minor-unit invariant.** State is always minor units. The public `(long, Currency)` and `(double, Currency)` ctors multiply the input by `centFactor()` (from `CENTS = {1,10,100,1000}`, indexed by the currency's fraction digits — handles JPY=0, KWD=3). The private `Money(Currency, long)` ctor and `newMoney(long)` take raw minor units directly and are what arithmetic helpers use — do not route already-scaled amounts through the public ctors.
- **Immutability.** `final` class, `final` fields, every operation returns a new instance. Preserve this — build fresh `Money` via `newMoney`, never mutate.
- **Currency safety.** Binary ops (`add`, `subtract`, `compareTo`) call `assertSameCurrencyAs`, throwing `IllegalArgumentException` on mismatch/null. `equals`/`hashCode` incorporate both amount and currency.
- **Allocation** splits without losing minor units: leftover units go to the first slots. `allocate(int)` and `allocate(long...)` are sign-aware and use `Math.multiplyExact`/`addExact`/`absExact` to fail loud on overflow rather than wrap. `AllocationResult` is a record wrapping a defensively-copied unmodifiable list.
- **Overflow discipline.** Prefer the exact-arithmetic `Math.*Exact` methods anywhere amount * factor or a running sum could overflow `long`; existing code throws `ArithmeticException` with context on overflow.
- **Error handling.** Argument validation throws `IllegalArgumentException`; overflow throws `ArithmeticException`. Match this — do not introduce raw `RuntimeException`.

## JSON serialization (Jackson 3.x)

Uses **`tools.jackson.*`** (Jackson 3), not `com.fasterxml.jackson.databind.*`. Note `@JsonCreator`/`@JsonProperty`/`@JsonFormat` annotations are still imported from `com.fasterxml.jackson.annotation`.

- `getAmount()` serializes the amount as a **quoted JSON string** (`@JsonFormat(shape = STRING)`) to avoid IEEE-754 truncation by downstream double-based parsers.
- The `@JsonCreator` ctor reads `amount` into `BigDecimal` (lenient: accepts both string and numeric JSON) and rounds with `HALF_EVEN`. Wire format is fixed by `src/test/resources/fixtures/Money.json`; changing the JSON shape must keep that fixture and `MoneySerializabilityTest` in sync.

## Testing notes

- JUnit 5 (Jupiter) + Hamcrest + `hamcrest-json` for JSON assertions; Mockito available.
- Tests set `Locale.setDefault(Locale.US)` because `toString()`/formatting is locale-sensitive — keep this when adding formatting tests to avoid flakes on non-US machines.
- Reusable contract bases live in `src/test/java/com/pobox/common/util/` (`EqualsHashCodeTestCase`, `ComparabilityTestCase`, `SerializabilityTestCase`); the `Money*Test` classes extend these to exercise the `equals`/`Comparable`/`Serializable` contracts.
