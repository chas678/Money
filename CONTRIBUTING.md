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
