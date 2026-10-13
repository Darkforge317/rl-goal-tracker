# Agent Guidelines

## RuneLite and Java

- Keep code compatible with the Java version configured in `build.gradle`.
- Do not use Java reflection, including in tests. Avoid reflective field or method access; use explicit dependency injection or package-private test seams.
- Do not use JNI/JNA, `Unsafe`, dynamic code or class loading, runtime code generation, external process execution, or Java native serialization.
- Prefer supported RuneLite APIs and constants over hardcoded game IDs.
- Use dependencies already provided by the project or RuneLite where possible. Avoid adding a dependency that is already available transitively.
- Follow RuneLite's [Developer Guide](https://github.com/runelite/runelite/wiki/Developer-Guide) for general plugin development guidance.
- Before adding or changing RuneLite-facing behavior or APIs, consult the current [Rejected or Rolled Back Features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features) page. It documents disallowed language features and APIs (including reflection), sensitive/restricted APIs, and features rejected or removed at Jagex's request. Treat the wiki as the current reference rather than relying on this file to duplicate its changing policy list.

## Threading and Performance

- Access RuneLite client state and APIs on the client thread.
- Perform Swing UI changes on the Swing event dispatch thread.
- Keep event handlers and per-frame work lightweight. Avoid unnecessary scans, blocking operations, and high-frequency `INFO` logging.
- Do not block the client thread on network or disk I/O. Clean up listeners, scheduled work, and other resources during plugin shutdown.

## Persistence and Compatibility

- Preserve compatibility with existing saved goals and task data.
- When changing serialized fields or formats, account for existing user data and add or update serialization tests.

## Testing

- Add or update focused tests for changed behavior. Do not use reflection in test setup.
- Run the relevant tests and report their results accurately. Distinguish pre-existing failures from regressions.
- Do not automate gameplay or interact with RuneScape on the user's behalf. For changes needing live verification, provide the user with specific steps to test in RuneLite.

## Changelogs

- For bug fixes, new features, quality-of-life updates, documentation changes, and completed developer-facing improvements, follow `src/main/resources/changelogs/README-changelogs.md` and add or update the next-release changelog entry in the same PR.
- Bug-fix entries should describe defects present in the currently released plugin. Do not add separate release notes for defects found and fixed while developing unreleased work; changelogs summarize releases rather than commits.
- Developer improvements can be discovered and completed within a PR; they do not need to originate from a GitHub issue.
- Record Dev Todo List work after it is implemented, not while it is still planned.

## GitHub Issue Completion

- After merging a PR that resolves GitHub issue(s), add the `Next Release` label to each related issue.
- Add a short comment to each issue summarizing the resolution and linking the merged PR, then close the issue if it is not already closed automatically.

## Reviewable Changes

- Keep changes focused on the requested behavior.
- Avoid mixing unrelated formatting or cleanup with feature changes.
