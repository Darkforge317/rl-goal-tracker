package com.darkforge317.goaltracker;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

import java.awt.*;

/**
 * RuneLite config group for Goal Tracker plugin.
 * Stores serialized goals, item cache data, and completion message color.
 */
@ConfigGroup("goaltracker")
public interface GoalTrackerConfig extends Config
{
    int MIN_SCROLL_SPEED = 1;
    int MAX_SCROLL_SPEED = 100;
    int DEFAULT_SCROLL_SPEED = 12;

    @ConfigItem(keyName = "goalTrackerData", name = "", description = "", hidden = true)
    default String goalTrackerData()
    {
        return "";
    }

    @ConfigItem(keyName = "goalTrackerData", name = "", description = "", hidden = true)
    void goalTrackerData(String str);

    @ConfigItem(keyName = "goalTrackerItemCache", name = "", description = "", hidden = true)
    default String goalTrackerItemCache()
    {
        return "";
    }

    @ConfigItem(keyName = "goalTrackerItemCache", name = "", description = "", hidden = true)
    void goalTrackerItemCache(String str);

    @ConfigItem(keyName = "goalTrackerItemNoteMapCache", name = "", description = "", hidden = true)
    default String goalTrackerItemNoteMapCache()
    {
        return "";
    }

    @ConfigItem(keyName = "goalTrackerItemNoteMapCache", name = "", description = "", hidden = true)
    void goalTrackerItemNoteMapCache(String str);

    @ConfigItem(keyName = "highestChangelogVersionSeen", name = "", description = "", hidden = true)
    default String highestChangelogVersionSeen()
    {
        return "";
    }

    @ConfigItem(keyName = "highestChangelogVersionSeen", name = "", description = "", hidden = true)
    void highestChangelogVersionSeen(String str);

    @ConfigItem(
        keyName = "horizontalScrollSpeed",
        name = "Horizontal Scroll Speed",
        description = "Controls horizontal scrolling speed throughout the plugin"
    )
    @Range(min = MIN_SCROLL_SPEED, max = MAX_SCROLL_SPEED)
    default int horizontalScrollSpeed()
    {
        return DEFAULT_SCROLL_SPEED;
    }

    @ConfigItem(
        keyName = "verticalScrollSpeed",
        name = "Vertical Scroll Speed",
        description = "Controls vertical scrolling speed throughout the plugin"
    )
    @Range(min = MIN_SCROLL_SPEED, max = MAX_SCROLL_SPEED)
    default int verticalScrollSpeed()
    {
        return DEFAULT_SCROLL_SPEED;
    }

    @ConfigItem(
        keyName = "completionMessageColor",
        name = "Completion Message Color",
        description = "Color of the chat message when a goal is completed"
    )
    @Alpha
    default Color completionMessageColor()
    {
        return new Color(0xF227A509, true);
    }
}
