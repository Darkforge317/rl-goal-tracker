# Changelog Guide

## Rules & Guidance
Please keep to the following changelog rules and guidances.
1. Do not include planned or unfinished work. The changelog describes the next release.
2. Don't bump the plugin version, the Plugin Owner will do this before submitting an update in Plugin Hub.
3. Keep new versions at the top of `versions.txt` for readability.
4. Do not sign your name on any entries.
    - Players that care will look at the PRs to see all the hard work you've done.
5. Ensure every PR with changelog-worthy changes includes at least one changelog entry.
    - "Changelog-worthy" is not well-defined, so don't be afraid to add an entry if you think it's worth doing so.
    - If the Plugin Owner disagrees with the entry, or thinks your changes deserve an entry, then they'll say so.
6. Bug-fix entries should describe bugs in the currently released plugin. Don't list defects found and fixed while developing unreleased work; changelogs are release notes, not a commit history.
7. Keep empty/unused sections from the template. The Plugin Owner will remove unnecessary sections before releasing the next plugin update.

## Quick steps

1. Compare the current plugin version in `runelite-plugin.properties` against the newest changelog in `versions.txt`.
   - If the newest entry is one version ahead of the plugin version:
     - Add your changes to the pre-existing newest changelog.
   - Otherwise:
     - Copy the template below and create a new file in `src/main/resources/changelogs/logs`.
     - Name the file so that it is one version above the plugin's current version
     - Add the version name to the top of `versions.txt`. This name **MUST** match the name of the file you created.
     - Add your changes under the relevant section.
2. Include your modifications to the changelog file (and `versions.txt` if changed) in your PR.


## Sections
Each of these sections describe a type of update. Most of the time this should match the label found on the Issue you are working on (if applicable).
If a section is empty, the plugin owner will remove it before release.

- **New Features** (`Feature`)
- **Quality of Life** (`QoL`)
- **Bug Fixes** (`Bug`, `Confirmed Bug`)
- **Documentation** (`Documentation`)
- **Developer Improvements** (`Dev Todo List`)
    - And anything else you discovered and finished that was in-scope for your PR

## Images

Put images beside the logs in a folder for that changelog version. For version `2.1.1`, use `src/main/resources/changelogs/images/V2-1-1/`. Supported formats are PNG, GIF, JPG, and JPEG. Reference bundled images like this:

```markdown
![Description](/changelogs/images/V2-1-1/example.png)
![Larger image](/changelogs/images/V2-1-1/example.png){width=320px}
```

Images keep their aspect ratio and shrink to fit the panel. `{width=320px}` requests a display width and can enlarge small images; it is always clamped to the available panel width. Tables are not supported because they cannot be displayed reliably in the narrow panel; use lists or short paragraphs instead. List indentation is capped to keep deep nesting readable. Remote images aren't loaded. Preview image sizing in RuneLite.

## Changelog Template
See [changelogs/template-changelogs.md](template-changelogs.md) for the template

