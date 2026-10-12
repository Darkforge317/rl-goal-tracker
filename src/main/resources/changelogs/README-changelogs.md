# Changelog README
## What are changelogs
Changelogs record changes included in the next plugin release, for both players and contributors.

This generally includes things such as:
- Bug fixes (GitHub labels: Bug, Confirmed Bug)
- Quality of Life improvements (QoL)
- New features (Feature)
- Documentation changes (Documentation)
- Completed developer-facing improvements, whether planned beforehand or discovered and completed during the PR

## When to create a changelog
Add or update an entry in the next-release changelog for each PR that fixes a bug, adds a feature, improves quality of life, changes documentation, or completes a developer-facing improvement worth recording.

The Dev Todo List label describes future work that is out of scope for the current PR. Don't list an open or planned item as completed release-note content; add it under Developer Improvements when the work is implemented. Developer improvements discovered and completed within a PR can also be recorded there, even if they were never on the Dev Todo List. Labels such as Next Release and Released describe release status, not changelog sections.

Routine internal refactors, dependency bumps, and compliance-only changes generally don't need an entry unless they meaningfully improve contributor workflows or are otherwise useful to document.

## How to create a changelog
Changelogs live in `src/main/resources/changelogs/logs/`, one file per version, tracked in `src/main/resources/changelogs/versions.txt`. Add or update the changelog entry in the same PR as each change that meets the criteria above.

The version in `runelite-plugin.properties` is the current plugin release. Changelog entries document the next release, so **do not bump the plugin version just to add a changelog entry**. The plugin version is updated separately when preparing a release.

### How to use the template
- Check the current plugin version in `runelite-plugin.properties`, and compare it to the newest changelog entry (the version at the top of `versions.txt`).
- **If the newest changelog entry's version matches the current plugin version** — nothing has been started for the next release yet:
    - Create a copy of the template below.
    - Name the copy the current version incremented by one.
        - **For example**: if `runelite-plugin.properties` shows `version=2.1.1`, the copy should be named `v2-1-2.md`.
    - Add the new version number as a new line at the top of `versions.txt`, so the newest entry stays easy to find at a glance.
- **If the newest changelog entry's version is already one higher than the current plugin version** — someone already started documenting the next release:
    - Open that existing file instead, and skip the two steps above.
- Add an entry for your update to whichever file you're now working with. Include the changed changelog file (and `versions.txt`, if you added a new entry) in your PR.

For each PR, add a concise entry under the appropriate heading (New Features, Quality of Life, Bug Fixes, Documentation, or Developer Improvements). If another PR has already created the next-version file, add your entry there instead of creating another version.

Note: versions.txt entries are compared numerically when the plugin determines which is newest, so the file's order isn't load-bearing - but keep new entries at the top anyway, so anyone reading the file can immediately tell what the current version is without having to compare numbers themselves.

## Changelog Template

Hi everyone, we've got another great update for you today!

[1-3 line summary of the update]

## New Features
- **[Title]**: [1-2 sentence description]

## Quality of Life
- **[Title]**: [1-2 sentence description]

## Bug Fixes
- **[Title]**: [1-2 sentence description]

## Documentation
- **[Title]**: [1-2 sentence description]

## Developer Improvements
- **[Title]**: [1-2 sentence description]


That's all for now, thanks for using the Goal Tracker plugin!
