# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
## [1.0.0] - 2026-07-01

### Bug Fixes

- Correct precedence, signs, and unsigned-int truncation
- Detect overflow, reject negatives, restore immutability
- Deserialise via BigDecimal instead of double
- Reject NaN/Infinity, detect long ctor overflow, reject empty AllocationResult
- Serialise amount as JSON string to preserve precision on the wire

### Documentation

- Add change-management (semver/tagging/changelog) design spec
- Add change-management implementation plan

### Miscellaneous

- Bump to Jackson 3.1.2, JUnit 6.0.3, and drop sysout-over-slf4j
- Bump dependency versions, add MIT licence and CLAUDE.md

### Refactor

- Remove unused Joda-Time dependency
- Switch to immutable List<Money>
- Drop Guava ComparisonChain in compareTo
- Drop Cloneable; deepCopy delegates to copy ctor
- Make fields final via private raw-minor-units ctor
- Improve error context, allocate(long...) Javadoc, and overflow assertion specificity

### Testing

- Cover negative-amount/zero-amount allocation, defensive copy, and JSON literal deserialisation
- Pin Locale.US to remove pre-existing flake

