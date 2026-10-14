package com.darkforge317.goaltracker.models.task;

import com.darkforge317.goaltracker.models.enums.TaskType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestPointsTaskTest
{
    @Test
    void displaysTheTargetAndChecksCachedProgress()
    {
        QuestPointsTask task = QuestPointsTask.builder()
            .targetQuestPoints(200)
            .currentQuestPoints(199)
            .build();

        assertEquals("199/200 Quest Points", task.toString());
        assertEquals("Reach 200 Quest Points", task.getDisplayName());
        assertEquals(TaskType.QUEST_POINTS, task.getType());
        assertFalse(task.hasReachedTarget());
        assertTrue(task.wouldReachTarget(200));
        task.setCurrentQuestPoints(200);
        assertTrue(task.hasReachedTarget());
    }

    @Test
    void questPointRangeMatchesCurrentGameMaximum()
    {
        assertEquals(0, QuestPointsTask.MIN_QUEST_POINTS);
        assertEquals(343, QuestPointsTask.MAX_QUEST_POINTS);
    }
}
