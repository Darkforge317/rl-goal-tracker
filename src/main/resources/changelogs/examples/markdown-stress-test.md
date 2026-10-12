# THE GOAL TRACKER
## CHANGELOG RENDERER SHOWCASE

Welcome, adventurer! This changelog is a **full-featured renderer stress test**.

It checks *italic text*, **bold text**, ***bold italic text***, and `inline code`.  
This is a second source line in the same paragraph, so it should wrap naturally.

Escaping test: \*these asterisks should show literally\*.

[Visit the RuneLite Developer Guide](https://github.com/runelite/runelite/wiki/Developer-Guide)

---

## I. FEATURES & QUESTS

- **New Features**
    - A nested unordered list
    - *Italic detail* inside a child item
    - `Code-styled detail`
- **Quality of Life**
    - Small improvements with big impact
- **Bug Fixes**
    - The bugs have been politely escorted out

### Numbered Expedition

1. Gather supplies
    1. Bring a rope
    2. Bring a light source
2. Enter the dungeon
3. Find the treasure
    - Watch for traps!
    - Keep the treasure. It's yours now.

---

## II. IMAGE SCALING LAB

Natural size, scaled down only if it exceeds the panel:

![Demo image at natural size](/changelogs/images/V2-1-2/demo.png)

Request an explicit display width:

![Demo image requested at 320 pixels](/changelogs/images/V2-1-2/demo.png){width=320px}

Ask for a ridiculous width. It should still fit the panel:

![Demo image requested at 2000 pixels](/changelogs/images/V2-1-2/demo.png){width=2000px}

---

## III. FIELD NOTES

> “A changelog renderer walks into a RuneLite panel.”
>
> “It brings its own scrolling, preserves the image proportions, and refuses to overflow.”
>
> The party agrees this is acceptable behavior.

---

## IV. FEATURE NOTES

Markdown tables aren't supported in the narrow changelog panel. Use a short list instead:

- **Bold** and *italic* text wrap naturally
- `Inline code` uses monospace text
- Nested lists stay readable, with deep indentation capped
- Images keep their proportions and fit within the panel

---

## FINAL BOSS

If everything above renders correctly, the changelog renderer has survived the gauntlet.

**Quest complete.**

That's all for now, thanks for using the Goal Tracker plugin!
