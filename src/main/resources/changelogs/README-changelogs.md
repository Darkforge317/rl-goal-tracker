# Changelog README
## What are changelogs
Changelogs inform our users of updates made to the plugin, and what those updates entail.

This generally includes things such as:
- Bug fixes
- Quality of Life updates
- New features
- Improvements to existing functionality

## When to create a changelog
Add or update a changelog entry as part of any PR that changes something a user can actually see or experience, new features, user-facing bug fixes, and quality-of-life improvements all qualify.

Purely internal changes with no visible effect on how the plugin looks or behaves, refactors, dependency bumps, RuneLite Plugin Hub compliance fixes, and similar, generally don't need an entry. 

## How to create a changelog
Changelogs live in `resources/changelogs/logs/`, one file per version, tracked in `resources/changelogs/versions.txt`. Use the steps below to figure out whether you need a new file or should add to an existing one.

### How to use the template
- Check the current plugin version in `runelite-plugin.properties`, and compare it to the newest changelog file's version (the last line of `versions.txt`).
- **If the newest changelog file's version matches the current plugin version**, nothing has been started for the next release yet:
  - Create a copy of the template below.
  - Name the copy the current version incremented by one.
    - **For example**: if `runelite-plugin.properties` shows `version=2.1.1`, the copy should be named `v2-1-2.md`.
  - Add the new version number as a new line at the bottom of `versions.txt`.
- **If the newest changelog file's version is already one higher than the current plugin version**, someone already started documenting the next release:
  - Open that existing file instead, and skip the two steps above.
- Add an entry for your update to whichever file you're now working with. Include the changed changelog file (and `versions.txt`, if you added a new entry) in your PR.

## Changelog Template

Hi everyone, we've got another great update for you today!

[1-3 line summary of the update]

## New Features
- **[Title]**: [1-2 sentence description]

## Improvements
- **[Title]**: [1-2 sentence description]

## Bug Fixes
- **[Title]**: [1-2 sentence description]


That's all for now, thanks for using the Goal Tracker plugin!