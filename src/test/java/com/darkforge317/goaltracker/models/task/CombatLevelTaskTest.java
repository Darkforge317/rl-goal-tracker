package com.darkforge317.goaltracker.models.task;

import com.darkforge317.goaltracker.models.enums.Status;
import com.darkforge317.goaltracker.models.enums.TaskType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatLevelTaskTest
{
    @Test
    void displaysTheTargetWithoutProgressFormatting()
    {
        CombatLevelTask task = CombatLevelTask.builder()
            .targetCombatLevel(85)
            .currentCombatLevel(70)
            .status(Status.NOT_STARTED)
            .build();

        assertEquals("85 Combat", task.toString());
        assertEquals(TaskType.COMBAT_LEVEL, task.getType());
        assertFalse(task.hasReachedTargetLevel());
        assertTrue(task.wouldReachTargetLevel(85));
    }

    @Test
    void cachedCombatLevelDeterminesWhetherTargetIsReached()
    {
        CombatLevelTask task = CombatLevelTask.builder()
            .targetCombatLevel(85)
            .currentCombatLevel(85)
            .build();

        assertTrue(task.hasReachedTargetLevel());
    }

    @Test
    void supportsFractionalCombatTargetsAndProgress()
    {
        CombatLevelTask task = CombatLevelTask.builder()
            .targetCombatLevel(85.25)
            .currentCombatLevel(85.24)
            .build();

        assertEquals("85.25 Combat", task.toString());
        assertFalse(task.hasReachedTargetLevel());
        task.setCurrentCombatLevel(85.25);
        assertTrue(task.hasReachedTargetLevel());
    }

    @Test
    void comparesAgainstTheDisplayedTwoDecimalCombatLevel()
    {
        CombatLevelTask task = CombatLevelTask.builder().targetCombatLevel(85.26).build();

        assertTrue(task.wouldReachTargetLevel(85.257));
        assertFalse(task.wouldReachTargetLevel(85.254));
    }
}
