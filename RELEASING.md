# Releasing

Releases are semi-automated via the **Release** GitHub Action. There is no artifact
registry — a release is a `vX.Y.Z` git tag plus a GitHub Release with generated notes.
The pom `<version>` always reflects the last release (no `-SNAPSHOT`).

## Cut a release

1. Ensure `main` is green and holds the commits you want to ship.
2. GitHub → **Actions** → **Release** → **Run workflow**, on branch `main`.
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
git push origin main
```

Then re-dispatch the Release workflow.
