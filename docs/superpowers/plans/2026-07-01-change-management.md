# Change Management (SemVer + Tagging + Changelog) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add semi-automated SemVer versioning, git tagging, and a generated changelog to the Money library, driven by Conventional Commits and a manual-dispatch GitHub Action, plus a PR test-gate workflow.

**Architecture:** git-cliff renders a Keep-a-Changelog `CHANGELOG.md` from Conventional Commits. A `workflow_dispatch` release Action computes the next SemVer (auto-derived or forced), gates on `mvn verify`, sets the pom version (no `-SNAPSHOT`), regenerates the changelog, commits, tags `vX.Y.Z`, pushes, and creates a GitHub Release. A companion CI workflow runs `mvn verify` on PRs/pushes to `java25`.

**Tech Stack:** GitHub Actions, git-cliff, Maven (versions-maven-plugin), `gh` CLI, Java 25 / Temurin.

## Global Constraints

- Default branch is `java25`; releases and CI target it. Copied verbatim from spec.
- First tagged release is pinned to `v1.0.0` regardless of commit history or forced bump.
- Tags are `v`-prefixed SemVer (`vMAJOR.MINOR.PATCH`).
- pom `<version>` reflects the last release; **no `-SNAPSHOT`** suffix and no post-release snapshot commit.
- Bump derivation: breaking (`!` / `BREAKING CHANGE:`) → major; `feat` → minor; `fix`/`perf` → patch.
- Release commits use message `chore(release): vX.Y.Z` and include `[skip ci]`.
- Java 25, Temurin distribution, in every workflow. Maven invoked with `-B`.
- The Nordnet "never bump POM versions / autobump" rule does NOT apply to this repo; the release Action owns pom bumps.

---

### Task 1: Changelog tooling (`cliff.toml` + seeded `CHANGELOG.md`)

**Files:**
- Create: `cliff.toml`
- Create: `CHANGELOG.md` (generated)

**Interfaces:**
- Consumes: git history with Conventional Commit messages.
- Produces: a `cliff.toml` consumed by Tasks 3; a committed `CHANGELOG.md` that Task 3's Action regenerates.

- [ ] **Step 1: Install git-cliff**

Run: `brew install git-cliff`
Then verify: `git-cliff --version`
Expected: prints a version line (e.g. `git-cliff 2.x`).

- [ ] **Step 2: Create `cliff.toml`**

```toml
# git-cliff configuration — Keep a Changelog output from Conventional Commits.
[changelog]
header = """
# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
"""
body = """
{% if version %}\
## [{{ version | trim_start_matches(pat="v") }}] - {{ timestamp | date(format="%Y-%m-%d") }}
{% else %}\
## [Unreleased]
{% endif %}\
{% for group, commits in commits | group_by(attribute="group") %}
### {{ group | upper_first }}
{% for commit in commits %}
- {{ commit.message | upper_first }}\
{% endfor %}
{% endfor %}\n
"""
trim = true

[git]
conventional_commits = true
filter_unconventional = true
split_commits = false
protect_breaking_commits = true
filter_commits = false
tag_pattern = "v[0-9]*"
topo_order = false
sort_commits = "oldest"
# Map Conventional Commit types to changelog sections; drop noise.
commit_parsers = [
  { message = "^feat", group = "Features" },
  { message = "^fix", group = "Bug Fixes" },
  { message = "^perf", group = "Performance" },
  { message = "^doc", group = "Documentation" },
  { message = "^refactor", group = "Refactor" },
  { message = "^test", group = "Testing" },
  { message = "^build", group = "Build System" },
  { message = "^ci", group = "CI" },
  { message = "^chore\\(release\\)", skip = true },
  { message = "^chore", group = "Miscellaneous" },
  { body = ".*security", group = "Security" },
]
# Drop merge commits from the changelog.
commit_preprocessors = []
```

- [ ] **Step 3: Generate the seeded changelog**

Run: `git-cliff --tag v1.0.0 -o CHANGELOG.md`
Then: `sed -n '1,40p' CHANGELOG.md`
Expected: a `# Changelog` header followed by `## [1.0.0] - <today>` and grouped sections (`### Features`, `### Bug Fixes`, etc.) built from history. No `chore(release)` lines present.

- [ ] **Step 4: Sanity-check version derivation (informational)**

Run: `git-cliff --bumped-version || true`
Expected: prints a computed version (its value is irrelevant here — Task 3 pins the first release to `1.0.0`). This only confirms git-cliff parses the repo without error.

- [ ] **Step 5: Commit**

```bash
git add cliff.toml CHANGELOG.md
git commit -m "build: add git-cliff config and seed CHANGELOG"
```

---

### Task 2: PR/push CI workflow (`ci.yml`)

**Files:**
- Create: `.github/workflows/ci.yml`

**Interfaces:**
- Consumes: nothing from prior tasks.
- Produces: a green-tests gate on PRs and pushes to `java25`.

- [ ] **Step 1: Install actionlint**

Run: `brew install actionlint`
Then verify: `actionlint --version`
Expected: prints a version line.

- [ ] **Step 2: Create `.github/workflows/ci.yml`**

```yaml
name: CI

on:
  pull_request:
    branches: [java25]
  push:
    branches: [java25]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '25'
          cache: maven
      - name: Build and test
        run: mvn -B verify
```

- [ ] **Step 3: Lint the workflow**

Run: `actionlint .github/workflows/ci.yml`
Expected: no output, exit code 0.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: add mvn verify gate on PRs and pushes to java25"
```

---

### Task 3: Release workflow (`release.yml`)

**Files:**
- Create: `.github/workflows/release.yml`

**Interfaces:**
- Consumes: `cliff.toml` and `CHANGELOG.md` from Task 1.
- Produces: on dispatch, a `chore(release): vX.Y.Z` commit, a `vX.Y.Z` tag, and a GitHub Release.

- [ ] **Step 1: Create `.github/workflows/release.yml`**

```yaml
name: Release

on:
  workflow_dispatch:
    inputs:
      bump:
        description: 'Version bump (auto derives from Conventional Commits)'
        type: choice
        required: true
        default: auto
        options: [auto, patch, minor, major]

permissions:
  contents: write

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - name: Guard — releases only from java25
        if: github.ref != 'refs/heads/java25'
        run: |
          echo "Releases may only be dispatched from java25 (got ${{ github.ref }})." >&2
          exit 1

      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '25'
          cache: maven

      - name: Install git-cliff
        uses: taiki-e/install-action@v2
        with:
          tool: git-cliff

      - name: Compute next version
        id: ver
        env:
          BUMP: ${{ inputs.bump }}
        run: |
          set -euo pipefail
          LAST_TAG=$(git describe --tags --abbrev=0 --match 'v*' 2>/dev/null || echo "")
          if [ -z "$LAST_TAG" ]; then
            NEXT="1.0.0"                     # first release is pinned
          elif [ "$BUMP" = "auto" ]; then
            NEXT=$(git-cliff --bumped-version | sed 's/^v//')
            if [ "v$NEXT" = "$LAST_TAG" ]; then
              echo "No releasable commits since $LAST_TAG. Choose an explicit bump or land a feat/fix." >&2
              exit 1
            fi
          else
            ver=${LAST_TAG#v}
            IFS=. read -r MA MI PA <<< "$ver"
            case "$BUMP" in
              major) MA=$((MA+1)); MI=0; PA=0 ;;
              minor) MI=$((MI+1)); PA=0 ;;
              patch) PA=$((PA+1)) ;;
            esac
            NEXT="$MA.$MI.$PA"
          fi
          echo "next=$NEXT" >> "$GITHUB_OUTPUT"
          echo "Next version: $NEXT"

      - name: Build and test (red-build gate)
        run: mvn -B verify

      - name: Set pom version
        run: mvn -B versions:set -DnewVersion=${{ steps.ver.outputs.next }} -DgenerateBackupPoms=false

      - name: Update CHANGELOG
        run: git-cliff --tag v${{ steps.ver.outputs.next }} -o CHANGELOG.md

      - name: Extract release notes
        run: git-cliff --tag v${{ steps.ver.outputs.next }} --unreleased --strip all -o RELEASE_NOTES.md

      - name: Commit, tag, push
        run: |
          set -euo pipefail
          git config user.name  "github-actions[bot]"
          git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
          git add pom.xml CHANGELOG.md
          git commit -m "chore(release): v${{ steps.ver.outputs.next }} [skip ci]"
          git tag -a v${{ steps.ver.outputs.next }} -m "v${{ steps.ver.outputs.next }}"
          git push origin HEAD:java25
          git push origin v${{ steps.ver.outputs.next }}

      - name: Create GitHub Release
        env:
          GH_TOKEN: ${{ github.token }}
        run: gh release create v${{ steps.ver.outputs.next }} --title v${{ steps.ver.outputs.next }} --notes-file RELEASE_NOTES.md
```

- [ ] **Step 2: Lint the workflow**

Run: `actionlint .github/workflows/release.yml`
Expected: no output, exit code 0. (If actionlint flags shellcheck issues in the run blocks, fix them until clean.)

- [ ] **Step 3: Dry-run the forced-bump math locally**

Run:
```bash
LAST_TAG="v1.2.3"; BUMP="minor"
ver=${LAST_TAG#v}; IFS=. read -r MA MI PA <<< "$ver"
case "$BUMP" in major) MA=$((MA+1)); MI=0; PA=0;; minor) MI=$((MI+1)); PA=0;; patch) PA=$((PA+1));; esac
echo "$MA.$MI.$PA"
```
Expected: `1.3.0`. (Confirms the override arithmetic before it runs in CI.)

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/release.yml
git commit -m "ci: add manual-dispatch semver release workflow"
```

---

### Task 4: Contributor & release docs

**Files:**
- Create: `CONTRIBUTING.md`
- Create: `RELEASING.md`

**Interfaces:**
- Consumes: the conventions and workflow from Tasks 1–3.
- Produces: human-facing documentation; no code depends on it.

- [ ] **Step 1: Create `CONTRIBUTING.md`**

```markdown
# Contributing

## Commit messages — Conventional Commits

This repo derives versions and the changelog from commit messages, so they must follow
[Conventional Commits](https://www.conventionalcommits.org/):

```
<type>[optional scope][!]: <description>

[optional body]

[optional BREAKING CHANGE: footer]
```

**Types:** `feat`, `fix`, `perf`, `docs`, `refactor`, `test`, `build`, `ci`, `chore`.

**How commits map to a release bump:**

| Commit | Bump |
|--------|------|
| `feat: ...` | minor |
| `fix: ...` / `perf: ...` | patch |
| `feat!: ...` or a `BREAKING CHANGE:` footer | major |
| `docs`/`refactor`/`test`/`chore`/`ci`/`build` only | no release |

Examples:
- `feat: add allocate(long...) ratio-based split`
- `fix(json): serialise amount as string to preserve precision`
- `refactor!: rename getAmount to amount` (breaking → major)

## Workflow

1. Branch off `java25`, make changes, keep tests green (`mvn verify`).
2. Open a PR to `java25`; CI runs `mvn verify`.
3. Merge once green. Releases are cut separately — see [RELEASING.md](RELEASING.md).
```

- [ ] **Step 2: Create `RELEASING.md`**

```markdown
# Releasing

Releases are semi-automated via the **Release** GitHub Action. There is no artifact
registry — a release is a `vX.Y.Z` git tag plus a GitHub Release with generated notes.
The pom `<version>` always reflects the last release (no `-SNAPSHOT`).

## Cut a release

1. Ensure `java25` is green and holds the commits you want to ship.
2. GitHub → **Actions** → **Release** → **Run workflow**, on branch `java25`.
3. Choose a **bump**:
   - `auto` (default) — derives the version from Conventional Commits since the last tag.
   - `patch` / `minor` / `major` — force it when you disagree with the derivation.
4. The Action: computes the version → runs `mvn verify` → sets the pom version →
   regenerates `CHANGELOG.md` → commits `chore(release): vX.Y.Z [skip ci]` → tags →
   pushes → creates the GitHub Release.

The **first** release is always `v1.0.0` regardless of the bump input.
`auto` aborts if there are no releasable (`feat`/`fix`/breaking) commits since the last tag.

## Recover from a bad release

Tags are cheap to recreate **before anyone consumes them**:

```bash
gh release delete vX.Y.Z --yes
git push origin :refs/tags/vX.Y.Z      # delete remote tag
git tag -d vX.Y.Z                      # delete local tag
git revert <release-commit-sha>        # undo the pom/changelog bump, or reset if unpushed
git push origin java25
```

Then re-dispatch the Release workflow.
```

- [ ] **Step 3: Commit**

```bash
git add CONTRIBUTING.md RELEASING.md
git commit -m "docs: add CONTRIBUTING (Conventional Commits) and RELEASING guides"
```

---

## Notes for the implementer

- Do **not** run the release workflow as part of implementation — it mutates tags and
  creates a real GitHub Release. It is validated by `actionlint` + the local dry-run only.
- `git-cliff --bumped-version` requires git-cliff ≥ 2.x; the `brew` formula and the
  `taiki-e/install-action` both provide a current version.
- If `actionlint` reports shellcheck warnings on the multi-line `run:` blocks, address them
  (quote expansions, keep `set -euo pipefail`) until the file lints clean.
