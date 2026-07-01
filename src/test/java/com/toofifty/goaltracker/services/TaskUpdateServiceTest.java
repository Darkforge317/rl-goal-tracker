package com.toofifty.goaltracker.services;

import com.toofifty.goaltracker.KillCountManager;
import com.toofifty.goaltracker.models.KillCountData;
import com.toofifty.goaltracker.models.task.Task;
import com.toofifty.goaltracker.models.task.SkillLevelTask;
import com.toofifty.goaltracker.models.task.SkillXpTask;
import com.toofifty.goaltracker.models.task.QuestTask;
import com.toofifty.goaltracker.models.task.ItemTask;
import com.toofifty.goaltracker.models.task.KillCountTask;
import com.toofifty.goaltracker.models.task.ManualTask;

import com.toofifty.goaltracker.ItemCache;
import com.toofifty.goaltracker.models.enums.Status;
import net.runelite.api.*;
import net.runelite.api.events.StatChanged;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskUpdateServiceTest {
    @Mock
    private ItemCache itemCache;

    @Mock
    private Quest quest;

    @Mock
    private Client client;

    @Mock
    private StatChanged statChangedEvent;

    @InjectMocks
    TaskUpdateService service;

    @Mock
    private KillCountManager killCountManager;

    /**
     * Builds a {@link KillCountData} instance pre-populated with {@code count}
     * kills recorded against a single {@code npcId}.
     * <p>
     * Used to stub {@code killCountManager.getData()} in tests without having
     * to manually call {@code increment()} in every test body.
     *
     * @param npcId the NPC ID to record kills against
     * @param count how many kills to record
     * @return a KillCountData populated with the requested kill count
     */
    private KillCountData dataWithKills(int npcId, int count) {
        KillCountData data = new KillCountData();
        for (int i = 0; i < count; i++) data.increment(npcId);
        return data;
    }

    @Test
    void update_shouldDynamicallyMapTasks() {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.isClientThread()).thenReturn(true);
        when(client.getRealSkillLevel(Skill.ATTACK)).thenReturn(99);
        when(client.getSkillExperience(Skill.ATTACK)).thenReturn(1234);
        when(quest.getState(client)).thenReturn(QuestState.FINISHED);
        when(itemCache.getTotalQuantity(314)).thenReturn(100);
        when(killCountManager.isLoaded()).thenReturn(true);
        when(killCountManager.getData()).thenReturn(dataWithKills(2, 10));

        assertTrue(service.update((Task) SkillLevelTask.builder().skill(Skill.ATTACK).level(90).build()));
        assertTrue(service.update((Task) SkillXpTask.builder().skill(Skill.ATTACK).xp(1234).build()));
        assertTrue(service.update((Task) QuestTask.builder().quest(quest).build()));
        assertTrue(service.update((Task) ItemTask.builder().itemId(314).acquired(0).quantity(100).build()));
        assertTrue(service.update((Task) KillCountTask.builder().npcId(2).npcName("Goblin").targetKills(10).build()));
    }

    @Test
    void update_shouldReturnFalseForUnsupportedTypes() {
        assertFalse(service.update(ManualTask.builder().build()));
    }

    @Test
    void update_skillLevelTask_shouldSupportLookingUpThePlayersLevel() {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.isClientThread()).thenReturn(true);
        when(client.getRealSkillLevel(Skill.ATTACK)).thenReturn(99);

        SkillLevelTask task = SkillLevelTask.builder().skill(Skill.ATTACK).level(90).build();

        assertTrue(service.update(task));
        assertEquals(Status.COMPLETED, task.getStatus());
    }

    @Test
    void update_skillLevelTask_shouldReturnFalseIfWeAreNotLoggedIn() {
        when(client.getGameState()).thenReturn(GameState.STARTING);

        SkillLevelTask task = SkillLevelTask.builder().skill(Skill.ATTACK).level(90).build();

        assertFalse(service.update(task));
    }

    @Test
    void update_skillLevelTask_shouldSupportStatChangedEvents() {
        when(statChangedEvent.getSkill()).thenReturn(Skill.ATTACK);
        when(statChangedEvent.getLevel()).thenReturn(99);

        SkillLevelTask task = SkillLevelTask.builder().skill(Skill.ATTACK).level(90).build();

        assertTrue(service.update(task, statChangedEvent));
        assertEquals(Status.COMPLETED, task.getStatus());
    }

    @Test
    void update_skillLevelTask_shouldIgnoreStatChangedEventsForTheWrongClass() {
        when(statChangedEvent.getSkill()).thenReturn(Skill.AGILITY);

        SkillLevelTask task = SkillLevelTask.builder().build();

        assertFalse(service.update(task, statChangedEvent));
    }

    @Test
    void update_skillLevelTask_shouldReturnTrueIfThePlayerLevelExceedsTheGoal() {
        SkillLevelTask task = SkillLevelTask.builder().skill(Skill.ATTACK).level(90).build();

        assertTrue(service.update(task, 99));
        assertEquals(Status.COMPLETED, task.getStatus());
    }

    @Test
    void update_skillLevelTask_shouldReturnFalseIfThePlayerLevelDoesNotExceedTheGoal() {
        SkillLevelTask task = SkillLevelTask.builder().skill(Skill.ATTACK).level(99).build();

        assertFalse(service.update(task, 90));
        assertEquals(Status.NOT_STARTED, task.getStatus());
    }

    @Test
    void update_skillXpTask_shouldSupportLookingUpThePlayersLevel() {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.isClientThread()).thenReturn(true);
        when(client.getSkillExperience(Skill.ATTACK)).thenReturn(1234);

        SkillXpTask task = SkillXpTask.builder().skill(Skill.ATTACK).xp(1234).build();

        assertTrue(service.update(task));
        assertEquals(Status.COMPLETED, task.getStatus());
    }

    @Test
    void update_skillXpTask_shouldReturnFalseIfWeAreNotLoggedIn() {
        when(client.getGameState()).thenReturn(GameState.STARTING);

        SkillXpTask task = SkillXpTask.builder().skill(Skill.ATTACK).xp(1234).build();

        assertFalse(service.update(task));
    }

    @Test
    void update_skillXpTask_shouldSupportStatChangedEvents() {
        when(statChangedEvent.getSkill()).thenReturn(Skill.ATTACK);
        when(statChangedEvent.getXp()).thenReturn(1234);

        SkillXpTask task = SkillXpTask.builder().skill(Skill.ATTACK).xp(1234).build();

        assertTrue(service.update(task, statChangedEvent));
        assertEquals(Status.COMPLETED, task.getStatus());
    }

    @Test
    void update_skillXpTask_shouldIgnoreStatChangedEventsForTheWrongSkill() {
        when(statChangedEvent.getSkill()).thenReturn(Skill.AGILITY);

        SkillXpTask task = SkillXpTask.builder().skill(Skill.ATTACK).xp(1234).build();

        assertFalse(service.update(task, statChangedEvent));
    }

    @Test
    void update_skillXpTask_shouldReturnTrueIfThePlayerXPExceedsTheGoal() {
        SkillXpTask task = SkillXpTask.builder().skill(Skill.ATTACK).xp(1234).build();

        assertTrue(service.update(task, 1234));
        assertEquals(Status.COMPLETED, task.getStatus());
    }

    @Test
    void update_skillXPTask_shouldReturnFalseIfThePlayerXPDoesNotExceedTheGoal() {
        SkillXpTask task = SkillXpTask.builder().skill(Skill.ATTACK).xp(1234).build();

        assertFalse(service.update(task, 1233));
        assertEquals(Status.NOT_STARTED, task.getStatus());
    }

    @Test
    void update_questTask_shouldReturnTrueIfTheQuestIsCompleted() {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.isClientThread()).thenReturn(true);
        when(quest.getState(client)).thenReturn(QuestState.FINISHED);

        QuestTask task = QuestTask.builder().quest(quest).build();

        assertTrue(service.update(task));
        assertEquals(Status.COMPLETED, task.getStatus());
    }

    @Test
    void update_questTask_shouldReturnFalseIfTheQuestIsNotCompleted() {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.isClientThread()).thenReturn(true);
        when(quest.getState(client)).thenReturn(QuestState.NOT_STARTED);

        QuestTask task = QuestTask.builder().quest(quest).build();

        assertFalse(service.update(task));
    }

    @Test
    void update_questTask_shouldReturnFalseIfThePlayerIsNotLoggedIn() {
        when(client.getGameState()).thenReturn(GameState.STARTING);

        QuestTask task = QuestTask.builder().build();

        assertFalse(service.update(task));
    }

    @Test
    void update_questTask_shouldReturnFalseIfNotClientThread() {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.isClientThread()).thenReturn(false);

        QuestTask task = QuestTask.builder().build();

        assertFalse(service.update(task));
    }

    @Test
    void update_itemTask_shouldReturnTrueIfTheAcquiredItemsExceedsTheExpected() {
        when(itemCache.getTotalQuantity(314)).thenReturn(100);

        ItemTask task = ItemTask.builder().itemId(314).acquired(0).quantity(100).build();

        assertTrue(service.update(task));
        assertEquals(Status.COMPLETED, task.getStatus());
    }

    @Test
    void update_itemTask_shouldReturnTrueIfTheAcquiredItemsHasChanged() {
        when(itemCache.getTotalQuantity(314)).thenReturn(99);

        ItemTask task = ItemTask.builder().itemId(314).status(Status.IN_PROGRESS).acquired(1).quantity(100).build();

        assertTrue(service.update(task));
        assertEquals(Status.IN_PROGRESS, task.getStatus());
    }

    @Test
    void update_itemTask_shouldReturnFalseIfTheAcquiredItemsHasNotChanged() {
        when(itemCache.getTotalQuantity(314)).thenReturn(0);

        ItemTask task = ItemTask.builder().itemId(314).acquired(0).quantity(100).build();

        assertFalse(service.update(task));
        assertEquals(Status.NOT_STARTED, task.getStatus());
    }

    // -------------------- Kill Count --------------------
    //
    // Kill count tasks don't poll the client directly (there's no RuneLite API
    // for "kills so far on this NPC"). Instead, they're recomputed from
    // KillCountManager's in-memory KillCountData, which onActorDeath updates
    // as kills come in. These tests stub killCountManager directly rather than
    // the client, since the client is not involved in this task type's update path.

    /**
     * If KillCountManager hasn't loaded data yet (e.g. player not fully logged
     * in), the task should not be touched at all and should stay NOT_STARTED.
     */
    @Test
    void update_killCountTask_shouldReturnFalseWhenManagerNotLoaded() {
        when(killCountManager.isLoaded()).thenReturn(false);

        KillCountTask task = KillCountTask.builder().npcId(1).npcName("Goblin").targetKills(10).build();

        assertFalse(service.update(task));
        assertEquals(Status.NOT_STARTED, task.getStatus());
    }

    /**
     * Zero recorded kills against the task's NPC ID should leave the task
     * NOT_STARTED and report no change.
     */
    @Test
    void update_killCountTask_shouldRemainNotStartedWhenZeroKills() {
        when(killCountManager.isLoaded()).thenReturn(true);
        when(killCountManager.getData()).thenReturn(dataWithKills(1, 0));

        KillCountTask task = KillCountTask.builder().npcId(1).npcName("Goblin").targetKills(10).build();

        assertFalse(service.update(task));
        assertEquals(Status.NOT_STARTED, task.getStatus());
    }

    /**
     * Some kills recorded but below the target should move the task to
     * IN_PROGRESS and report a change.
     */
    @Test
    void update_killCountTask_shouldSetInProgressWhenPartialKills() {
        when(killCountManager.isLoaded()).thenReturn(true);
        when(killCountManager.getData()).thenReturn(dataWithKills(1, 5));

        KillCountTask task = KillCountTask.builder().npcId(1).npcName("Goblin").targetKills(10).build();

        assertTrue(service.update(task));
        assertEquals(Status.IN_PROGRESS, task.getStatus());
    }

    /**
     * Recorded kills exactly matching the target should mark the task COMPLETED.
     */
    @Test
    void update_killCountTask_shouldCompleteWhenTargetMet() {
        when(killCountManager.isLoaded()).thenReturn(true);
        when(killCountManager.getData()).thenReturn(dataWithKills(1, 10));

        KillCountTask task = KillCountTask.builder().npcId(1).npcName("Goblin").targetKills(10).build();

        assertTrue(service.update(task));
        assertEquals(Status.COMPLETED, task.getStatus());
    }

    /**
     * Recorded kills beyond the target should still mark the task COMPLETED
     * (not overflow or error) — mirrors ItemTask's "exceeds expected" case.
     */
    @Test
    void update_killCountTask_shouldCompleteWhenCountExceedsTarget() {
        when(killCountManager.isLoaded()).thenReturn(true);
        when(killCountManager.getData()).thenReturn(dataWithKills(1, 999));

        KillCountTask task = KillCountTask.builder().npcId(1).npcName("Goblin").targetKills(10).build();

        assertTrue(service.update(task));
        assertEquals(Status.COMPLETED, task.getStatus());
    }

    /**
     * KillCountData may track kills for many NPC IDs at once (one player can
     * have several KC tasks active). A task must only read the count for its
     * own npcId and ignore kills recorded against other NPCs.
     */
    @Test
    void update_killCountTask_shouldNotCountKillsForDifferentNpc() {
        KillCountData data = new KillCountData();
        data.increment(999); // kills on NPC 999, not NPC 1
        when(killCountManager.isLoaded()).thenReturn(true);
        when(killCountManager.getData()).thenReturn(data);

        KillCountTask task = KillCountTask.builder().npcId(1).npcName("Goblin").targetKills(1).build();

        assertFalse(service.update(task));
        assertEquals(Status.NOT_STARTED, task.getStatus());
    }

    /**
     * Calling update() twice with unchanged kill data should report no change
     * on the second call. This matters because the plugin calls update() on
     * every onActorDeath for every incomplete KC task, so this needs to be a
     * cheap no-op when nothing relevant happened.
     */
    @Test
    void update_killCountTask_shouldReturnFalseOnSecondCallWithSameCount() {
        KillCountData data = dataWithKills(1, 5);
        when(killCountManager.isLoaded()).thenReturn(true);
        when(killCountManager.getData()).thenReturn(data);

        KillCountTask task = KillCountTask.builder().npcId(1).npcName("Goblin").targetKills(10).build();
        service.update(task); // first call: NOT_STARTED -> IN_PROGRESS

        assertFalse(service.update(task)); // second call: same data, no change
    }
}