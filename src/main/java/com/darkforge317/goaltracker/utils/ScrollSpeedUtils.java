package com.darkforge317.goaltracker.utils;

import com.darkforge317.goaltracker.GoalTrackerConfig;

import javax.swing.JScrollPane;
import java.util.Map;
import java.util.WeakHashMap;

/** Applies the configured scroll increments to a scrollable view. */
public final class ScrollSpeedUtils
{
    private static final Map<JScrollPane, GoalTrackerConfig> SCROLL_PANES = new WeakHashMap<>();

    private ScrollSpeedUtils()
    {
    }

    public static synchronized void configure(JScrollPane scrollPane, GoalTrackerConfig config)
    {
        SCROLL_PANES.put(scrollPane, config);
        apply(scrollPane, config);
    }

    public static synchronized void refreshAll()
    {
        SCROLL_PANES.forEach(ScrollSpeedUtils::apply);
    }

    private static void apply(JScrollPane scrollPane, GoalTrackerConfig config)
    {
        scrollPane.getHorizontalScrollBar().setUnitIncrement(clamp(config.horizontalScrollSpeed()));
        scrollPane.getVerticalScrollBar().setUnitIncrement(clamp(config.verticalScrollSpeed()));
    }

    private static int clamp(int speed)
    {
        return Math.max(GoalTrackerConfig.MIN_SCROLL_SPEED,
                Math.min(GoalTrackerConfig.MAX_SCROLL_SPEED, speed));
    }
}
