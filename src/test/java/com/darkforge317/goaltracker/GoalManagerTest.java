package com.darkforge317.goaltracker;

import com.darkforge317.goaltracker.models.Goal;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GoalManagerTest
{
    @Test
    void missingSavedDataPreservesGoalsAlreadyInMemory()
    {
        GoalManager manager = new GoalManager(configWithGoalData(null), new GoalSerializer(new Gson()));
        Goal existingGoal = Goal.builder().description("Existing goal").build();
        manager.getGoals().add(existingGoal);

        manager.load();

        assertEquals(1, manager.getGoals().size());
        assertSame(existingGoal, manager.getGoals().get(0));
    }

    @Test
    void malformedSavedDataPreservesGoalsAlreadyInMemory()
    {
        GoalManager manager = new GoalManager(configWithGoalData("not valid JSON"), new GoalSerializer(new Gson()));
        Goal existingGoal = Goal.builder().description("Existing goal").build();
        manager.getGoals().add(existingGoal);

        manager.load();

        assertEquals(1, manager.getGoals().size());
        assertSame(existingGoal, manager.getGoals().get(0));
    }

    @Test
    void emptySavedDataLoadsAnEmptyGoalList()
    {
        GoalManager manager = new GoalManager(configWithGoalData(""), new GoalSerializer(new Gson()));
        manager.getGoals().add(Goal.builder().build());

        manager.load();

        assertEquals(0, manager.getGoals().size());
    }

    private static GoalTrackerConfig configWithGoalData(String serialized)
    {
        return new GoalTrackerConfig()
        {
            @Override public String goalTrackerData() { return serialized; }
            @Override public void goalTrackerData(String str) { }
            @Override public void goalTrackerItemCache(String str) { }
            @Override public void goalTrackerItemNoteMapCache(String str) { }
            @Override public void highestChangelogVersionSeen(String str) { }
        };
    }
}
