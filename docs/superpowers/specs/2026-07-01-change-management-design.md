# Change Management: SemVer + Tagging + Changelog — Design

**Date:** 2026-07-01
**Repo:** `com.pobox:Money` (github `chas678/Money`)
**Default branch:** `java25`
**Status:** Approved (pending spec review)

## Goal

Adopt a lightweight, semi-automated change-management process for this single-class
Maven library: Semantic Versioning, git tagging, and a generated changelog, driven by
the Conventional Commit messages already in use.

## Decisions (locked)

| Decision | Choice |
|----------|--------|
| Automation level | Semi-automated — human decides *when*, tooling does the mechanics |
| Distribution | Git tag + GitHub Release only (no artifact repo, no credentials) |
| Trigger | Manual-dispatch GitHub Action (`workflow_dispatch`) |
| First release | `v1.0.0` — public API declared stable |
| Bump logic | Auto-derive from Conventional Commits, with manual override |
| Changelog tool | git-cliff (`cliff.toml`, Keep a Changelog output) |
| pom `-SNAPSHOT` | **Dropped** — pom `<version>` reflects the last release; no snapshot dance |
| Companion PR CI | **Included** — `mvn verify` on PRs/pushes to `java25` |

Note: the global "never bump POM versions (autobump)" convention is Nordnet-internal
and does **not** apply to this personal repo; the release Action owns pom version bumps here.

## Components

### 1. Conventions & versioning

- **Conventional Commits** are the source of truth. Types: `feat`, `fix`, `docs`,
  `refactor`, `test`, `perf`, `build`, `ci`, `chore`. Breaking change = `!` suffix
  (e.g. `feat!:`) or a `BREAKING CHANGE:` footer.
- **SemVer** `MAJOR.MINOR.PATCH`. Tags prefixed `v` (`v1.0.0`).
- **Bump derivation:** breaking → major; `feat` → minor; `fix`/`perf` → patch.
  Other types alone (docs/refactor/test/chore/ci/build) produce **no release**; the
  Action fails fast with a clear message if `auto` finds nothing releasable.
- **Enforcement:** documented in `CONTRIBUTING.md`, honor-system. A PR-title lint is a
  noted future add-on, explicitly out of scope for this iteration.

### 2. Release GitHub Action — `.github/workflows/release.yml`

- Trigger: `workflow_dispatch` with input `bump`: `auto` (default) | `patch` | `minor` | `major`.
- `permissions: contents: write`.
- Guard: refuse to run unless ref is `java25`.
- Steps:
  1. `actions/checkout` with `fetch-depth: 0` and tags.
  2. Set up JDK 25 (`actions/setup-java`, temurin) + Maven cache.
  3. Install git-cliff (pinned version).
  4. Determine next version: for `auto`, `git cliff --bumped-version` over commits since
     the last `v*` tag; otherwise apply the forced `bump` to the last tag. On first run
     (no tags) the version is `1.0.0`.
  5. **Red-build gate:** `mvn -B verify`. Abort the release on failure.
  6. `mvn -B versions:set -DnewVersion=<X.Y.Z> -DgenerateBackupPoms=false`.
  7. Regenerate `CHANGELOG.md` via `git cliff --tag v<X.Y.Z> -o CHANGELOG.md`.
  8. Commit `chore(release): v<X.Y.Z>` with `[skip ci]`; set git identity to the actions bot.
  9. Create annotated tag `v<X.Y.Z>`.
  10. Push commit + tag to `java25`.
  11. `gh release create v<X.Y.Z>` using git-cliff's notes for that version as the body.

### 3. Companion CI — `.github/workflows/ci.yml`

- Trigger: `pull_request` targeting `java25`, and `push` to `java25`.
- Steps: checkout → JDK 25 → `mvn -B verify`.
- Purpose: gate merges on green tests. Skipped for the release bot commit via `[skip ci]`.

### 4. Supporting files

- **`cliff.toml`** — commit-type → changelog-section mapping (Features, Bug Fixes,
  Performance, Documentation, etc.); filters out merge commits and `chore(release)`
  commits; groups by version tag; Keep a Changelog header.
- **`CHANGELOG.md`** — generated, seeded with the `1.0.0` entry built from history to date.
- **`CONTRIBUTING.md`** — Conventional Commits guide + how bumps are derived.
- **`RELEASING.md`** — how to dispatch the release workflow, what `auto` does, override usage,
  and how to recover from a mistaken release.

## Data / control flow

```
developer commits (Conventional Commits) ──▶ PR to java25
        │                                        │
        │                                   ci.yml: mvn verify (gate)
        ▼                                        │
   merge to java25 ◀───────────────────────────┘
        │
        │ (maintainer clicks "Run workflow", picks bump=auto)
        ▼
release.yml:
   compute version → mvn verify → versions:set → git-cliff CHANGELOG
   → commit chore(release) [skip ci] → tag vX.Y.Z → push → gh release create
```

## Error handling & edge cases

- **No releasable commits under `auto`:** fail with a message telling the user to pick an
  explicit bump or land a `feat`/`fix`.
- **Failing tests:** release aborts before any pom change, commit, or tag.
- **First release (no tags):** version resolves to `1.0.0`; changelog covers all history.
- **Wrong bump chosen:** documented recovery in `RELEASING.md` (delete local/remote tag +
  GitHub Release, revert the release commit, re-dispatch). Tags are cheap to recreate before
  anyone consumes them.
- **Dispatched from a non-`java25` ref:** guard step exits non-zero.

## Testing / validation strategy

- Dry-run `git cliff --bumped-version` and `git cliff -o CHANGELOG.md` locally to confirm
  version math and changelog rendering against real history before wiring the Action.
- Validate the workflow end-to-end using a throwaway pre-release tag (e.g. `v0.0.0-rc`) on a
  scratch run, then delete it, so the first real `v1.0.0` is clean.
- Confirm `ci.yml` triggers on a PR and that the `[skip ci]` release commit does not.

## Out of scope

- Publishing artifacts (GitHub Packages, Maven Central), GPG signing.
- Commit-message / PR-title lint enforcement in CI.
- Snapshot versioning and snapshot deployment.
- Release branches / backport flows (single-branch `java25` model retained).
