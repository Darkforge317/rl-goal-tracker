package com.darkforge317.goaltracker;

import com.darkforge317.goaltracker.utils.ScrollSpeedUtils;
import org.junit.jupiter.api.Test;

import javax.swing.JScrollPane;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoalTrackerConfigTest
{
    @Test
    void scrollSpeedsDefaultToExistingIncrement()
    {
        GoalTrackerConfig config = new GoalTrackerConfig()
        {
            @Override public void goalTrackerData(String str) { }
            @Override public void goalTrackerItemCache(String str) { }
            @Override public void goalTrackerItemNoteMapCache(String str) { }
            @Override public void highestChangelogVersionSeen(String str) { }
        };

        assertEquals(GoalTrackerConfig.DEFAULT_SCROLL_SPEED, config.horizontalScrollSpeed());
        assertEquals(GoalTrackerConfig.DEFAULT_SCROLL_SPEED, config.verticalScrollSpeed());
    }

    @Test
    void configuredScrollSpeedsApplyToBothAxes()
    {
        int[] horizontalSpeed = {17};
        int[] verticalSpeed = {23};
        GoalTrackerConfig config = new GoalTrackerConfig()
        {
            @Override public void goalTrackerData(String str) { }
            @Override public void goalTrackerItemCache(String str) { }
            @Override public void goalTrackerItemNoteMapCache(String str) { }
            @Override public void highestChangelogVersionSeen(String str) { }
            @Override public int horizontalScrollSpeed() { return horizontalSpeed[0]; }
            @Override public int verticalScrollSpeed() { return verticalSpeed[0]; }
        };
        JScrollPane scrollPane = new JScrollPane();

        ScrollSpeedUtils.configure(scrollPane, config);

        assertEquals(17, scrollPane.getHorizontalScrollBar().getUnitIncrement());
        assertEquals(23, scrollPane.getVerticalScrollBar().getUnitIncrement());

        horizontalSpeed[0] = 31;
        verticalSpeed[0] = 47;
        ScrollSpeedUtils.refreshAll();

        assertEquals(31, scrollPane.getHorizontalScrollBar().getUnitIncrement());
        assertEquals(47, scrollPane.getVerticalScrollBar().getUnitIncrement());
    }

    @Test
    void unsupportedScrollSpeedsAreClamped()
    {
        GoalTrackerConfig config = new GoalTrackerConfig()
        {
            @Override public void goalTrackerData(String str) { }
            @Override public void goalTrackerItemCache(String str) { }
            @Override public void goalTrackerItemNoteMapCache(String str) { }
            @Override public void highestChangelogVersionSeen(String str) { }
            @Override public int horizontalScrollSpeed() { return 150; }
            @Override public int verticalScrollSpeed() { return 0; }
        };
        JScrollPane scrollPane = new JScrollPane();

        ScrollSpeedUtils.configure(scrollPane, config);

        assertEquals(GoalTrackerConfig.MAX_SCROLL_SPEED, scrollPane.getHorizontalScrollBar().getUnitIncrement());
        assertEquals(GoalTrackerConfig.MIN_SCROLL_SPEED, scrollPane.getVerticalScrollBar().getUnitIncrement());
    }
}
