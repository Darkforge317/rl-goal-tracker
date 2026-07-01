package com.toofifty.goaltracker.models.task;

import com.toofifty.goaltracker.models.enums.Status;
import com.toofifty.goaltracker.models.enums.TaskType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KillCountTaskTest
{
    // -------------------- getType --------------------

    @Test
    void getType_shouldReturnKillCount()
    {
        KillCountTask task = KillCountTask.builder().npcId(1).npcName("Goblin").targetKills(10).build();

        assertEquals(TaskType.KILL_COUNT, task.getType());
    }

    // -------------------- getDisplayName --------------------

    @Test
    void getDisplayName_shouldAppendKC()
    {
        KillCountTask task = KillCountTask.builder().npcName("Ogress Warrior").targetKills(1).build();

        assertEquals("Ogress Warrior KC", task.getDisplayName());
    }

    // -------------------- toString --------------------

    @Test
    void toString_shouldShowTargetWhenNoKillsYet()
    {
        KillCountTask task = KillCountTask.builder().npcName("Goblin").targetKills(100).build();

        assertEquals("100 x Goblin kills", task.toString());
    }

    @Test
    void toString_shouldShowProgressWhenPartiallyComplete()
    {
        KillCountTask task = KillCountTask.builder()
                .npcName("Goblin").targetKills(100).currentKills(42).build();

        assertEquals("42/100 Goblin kills", task.toString());
    }

    @Test
    void toString_shouldShowTargetOnlyWhenComplete()
    {
        KillCountTask task = KillCountTask.builder()
                .npcName("Goblin").targetKills(100).currentKills(100).build();

        // currentKills == targetKills, so no progress fraction shown
        assertEquals("100 x Goblin kills", task.toString());
    }

    // -------------------- recomputeFromCount --------------------

    @Test
    void recomputeFromCount_shouldSetStatusToCompletedWhenTargetMet()
    {
        KillCountTask task = KillCountTask.builder().npcId(1).npcName("Goblin").targetKills(10).build();

        task.recomputeFromCount(10);

        assertEquals(Status.COMPLETED, task.getStatus());
    }

    @Test
    void recomputeFromCount_shouldSetStatusToCompletedWhenCountExceedsTarget()
    {
        KillCountTask task = KillCountTask.builder().npcId(1).npcName("Goblin").targetKills(10).build();

        task.recomputeFromCount(999);

        assertEquals(Status.COMPLETED, task.getStatus());
    }

    @Test
    void recomputeFromCount_shouldSetStatusToInProgressWhenPartiallyDone()
    {
        KillCountTask task = KillCountTask.builder().npcId(1).npcName("Goblin").targetKills(100).build();

        task.recomputeFromCount(50);

        assertEquals(Status.IN_PROGRESS, task.getStatus());
    }

    @Test
    void recomputeFromCount_shouldSetStatusToNotStartedWhenZeroKills()
    {
        KillCountTask task = KillCountTask.builder().npcId(1).npcName("Goblin").targetKills(10).build();

        task.recomputeFromCount(0);

        assertEquals(Status.NOT_STARTED, task.getStatus());
    }

    @Test
    void recomputeFromCount_shouldReturnTrueWhenStatusChanges()
    {
        KillCountTask task = KillCountTask.builder().npcId(1).npcName("Goblin").targetKills(10).build();

        boolean changed = task.recomputeFromCount(10);

        assertTrue(changed);
    }

    @Test
    void recomputeFromCount_shouldReturnFalseWhenNothingChanges()
    {
        KillCountTask task = KillCountTask.builder().npcId(1).npcName("Goblin").targetKills(10).build();
        task.recomputeFromCount(0); // establish baseline: NOT_STARTED, 0 kills

        boolean changed = task.recomputeFromCount(0);

        assertFalse(changed);
    }

    @Test
    void recomputeFromCount_shouldReturnTrueWhenCurrentKillsChangeWithoutStatusChange()
    {
        KillCountTask task = KillCountTask.builder().npcId(1).npcName("Goblin").targetKills(100).build();
        task.recomputeFromCount(50); // IN_PROGRESS, 50 kills

        boolean changed = task.recomputeFromCount(51); // still IN_PROGRESS, but 51 kills

        assertTrue(changed);
    }

    @Test
    void recomputeFromCount_shouldCapCurrentKillsAtTarget()
    {
        KillCountTask task = KillCountTask.builder().npcId(1).npcName("Goblin").targetKills(10).build();

        task.recomputeFromCount(9999);

        assertEquals(10, task.getCurrentKills());
    }

    @Test
    void recomputeFromCount_shouldUpdateCurrentKills()
    {
        KillCountTask task = KillCountTask.builder().npcId(1).npcName("Goblin").targetKills(100).build();

        task.recomputeFromCount(37);

        assertEquals(37, task.getCurrentKills());
    }
}
