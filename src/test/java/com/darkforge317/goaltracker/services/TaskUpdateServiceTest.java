package com.darkforge317.goaltracker.services;

import com.darkforge317.goaltracker.models.task.Task;
import com.darkforge317.goaltracker.models.task.SkillLevelTask;
import com.darkforge317.goaltracker.models.task.SkillXpTask;
import com.darkforge317.goaltracker.models.task.QuestTask;
import com.darkforge317.goaltracker.models.task.ItemTask;
import com.darkforge317.goaltracker.models.task.ManualTask;
import com.darkforge317.goaltracker.models.task.CombatLevelTask;
import com.darkforge317.goaltracker.models.task.QuestPointsTask;

import com.darkforge317.goaltracker.ItemCache;
import com.darkforge317.goaltracker.models.enums.Status;
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
    private Player player;

    @Mock
    private StatChanged statChangedEvent;

    @InjectMocks
    TaskUpdateService service;

    @Test
    void update_shouldDynamicallyMapTasks() {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.isClientThread()).thenReturn(true);
        when(client.getRealSkillLevel(Skill.ATTACK)).thenReturn(99);
        when(client.getSkillExperience(Skill.ATTACK)).thenReturn(1234);
        when(quest.getState(client)).thenReturn(QuestState.FINISHED);
        when(itemCache.getTotalQuantity(314)).thenReturn(100);

        assertTrue(service.update((Task) SkillLevelTask.builder().skill(Skill.ATTACK).targetSkillLevel(90).build()));
        assertTrue(service.update((Task) SkillXpTask.builder().skill(Skill.ATTACK).targetSkillXp(1234).build()));
        assertTrue(service.update((Task) QuestTask.builder().quest(quest).build()));
        assertTrue(service.update((Task) ItemTask.builder().itemId(314).acquired(0).quantity(100).build()));
    }

    @Test
    void update_combatLevelTask_shouldCacheLevelAndCompleteTarget()
    {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.isClientThread()).thenReturn(true);
        when(client.getLocalPlayer()).thenReturn(player);
        when(client.getRealSkillLevel(Skill.ATTACK)).thenReturn(80);
        when(client.getRealSkillLevel(Skill.STRENGTH)).thenReturn(80);
        when(client.getRealSkillLevel(Skill.DEFENCE)).thenReturn(80);
        when(client.getRealSkillLevel(Skill.HITPOINTS)).thenReturn(80);
        when(client.getRealSkillLevel(Skill.MAGIC)).thenReturn(80);
        when(client.getRealSkillLevel(Skill.RANGED)).thenReturn(80);
        when(client.getRealSkillLevel(Skill.PRAYER)).thenReturn(80);
        double currentCombatLevel = Experience.getCombatLevelPrecise(80, 80, 80, 80, 80, 80, 80);
        CombatLevelTask task = CombatLevelTask.builder().targetCombatLevel(currentCombatLevel).build();

        assertTrue(service.update((Task) task));
        assertEquals(currentCombatLevel, task.getCurrentCombatLevel(), 0.000001);
        assertEquals(Status.COMPLETED, task.getStatus());
    }

    @Test
    void update_combatLevelTask_shouldSaveProgressChangesBeforeCompletion()
    {
        CombatLevelTask task = CombatLevelTask.builder().targetCombatLevel(100)
            .currentCombatLevel(83).status(Status.NOT_STARTED).build();

        assertTrue(service.update(task, 84.75));
        assertEquals(84.75, task.getCurrentCombatLevel(), 0.000001);
        assertEquals(Status.NOT_STARTED, task.getStatus());
        assertFalse(service.update(task, 84.75));
    }

    @Test
    void update_combatLevelTask_shouldKeepCachedLevelWhenPlayerIsUnavailable()
    {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.isClientThread()).thenReturn(true);
        when(client.getLocalPlayer()).thenReturn(null);
        CombatLevelTask task = CombatLevelTask.builder().targetCombatLevel(85)
            .currentCombatLevel(84).status(Status.NOT_STARTED).build();

        assertFalse(service.update(task));
        assertEquals(84, task.getCurrentCombatLevel());
        assertEquals(Status.NOT_STARTED, task.getStatus());
    }

    @Test
    void update_questPointsTask_shouldCacheProgressAndCompleteTheTarget()
    {
        QuestPointsTask task = QuestPointsTask.builder().targetQuestPoints(200)
            .currentQuestPoints(50).status(Status.IN_PROGRESS).build();

        assertTrue(service.update(task, 199));
        assertEquals(199, task.getCurrentQuestPoints());
        assertEquals(Status.IN_PROGRESS, task.getStatus());
        assertFalse(service.update(task, 199));

        assertTrue(service.update(task, 200));
        assertEquals(Status.COMPLETED, task.getStatus());
    }

    @Test
    void update_questPointsTask_shouldNotRetainNegativeProgress()
    {
        QuestPointsTask task = QuestPointsTask.builder().targetQuestPoints(200)
            .currentQuestPoints(10).status(Status.IN_PROGRESS).build();

        assertTrue(service.update(task, -1));
        assertEquals(0, task.getCurrentQuestPoints());
        assertEquals(Status.NOT_STARTED, task.getStatus());
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

        SkillLevelTask task = SkillLevelTask.builder().skill(Skill.ATTACK).targetSkillLevel(90).build();

        assertTrue(service.update(task));
        assertEquals(Status.COMPLETED, task.getStatus());
    }

    @Test
    void update_skillLevelTask_shouldReturnFalseIfWeAreNotLoggedIn() {
        when(client.getGameState()).thenReturn(GameState.STARTING);

        SkillLevelTask task = SkillLevelTask.builder().skill(Skill.ATTACK).targetSkillLevel(90).build();

        assertFalse(service.update(task));
    }

    @Test
    void update_skillLevelTask_shouldSupportStatChangedEvents() {
        when(statChangedEvent.getSkill()).thenReturn(Skill.ATTACK);
        when(statChangedEvent.getLevel()).thenReturn(99);

        SkillLevelTask task = SkillLevelTask.builder().skill(Skill.ATTACK).targetSkillLevel(90).build();

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
        SkillLevelTask task = SkillLevelTask.builder().skill(Skill.ATTACK).targetSkillLevel(90).build();

        assertTrue(service.update(task, 99));
        assertEquals(Status.COMPLETED, task.getStatus());
    }

    @Test
    void update_skillLevelTask_shouldReturnFalseIfThePlayerLevelDoesNotExceedTheGoal() {
        SkillLevelTask task = SkillLevelTask.builder().skill(Skill.ATTACK).targetSkillLevel(99).build();

        assertFalse(service.update(task, 90));
        assertEquals(Status.NOT_STARTED, task.getStatus());
    }

    @Test
    void update_skillXpTask_shouldSupportLookingUpThePlayersLevel() {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.isClientThread()).thenReturn(true);
        when(client.getSkillExperience(Skill.ATTACK)).thenReturn(1234);

        SkillXpTask task = SkillXpTask.builder().skill(Skill.ATTACK).targetSkillXp(1234).build();

        assertTrue(service.update(task));
        assertEquals(Status.COMPLETED, task.getStatus());
    }

    @Test
    void update_skillXpTask_shouldReturnFalseIfWeAreNotLoggedIn() {
        when(client.getGameState()).thenReturn(GameState.STARTING);

        SkillXpTask task = SkillXpTask.builder().skill(Skill.ATTACK).targetSkillXp(1234).build();

        assertFalse(service.update(task));
    }

    @Test
    void update_skillXpTask_shouldSupportStatChangedEvents() {
        when(statChangedEvent.getSkill()).thenReturn(Skill.ATTACK);
        when(statChangedEvent.getXp()).thenReturn(1234);

        SkillXpTask task = SkillXpTask.builder().skill(Skill.ATTACK).targetSkillXp(1234).build();

        assertTrue(service.update(task, statChangedEvent));
        assertEquals(Status.COMPLETED, task.getStatus());
    }

    @Test
    void update_skillXpTask_shouldIgnoreStatChangedEventsForTheWrongSkill() {
        when(statChangedEvent.getSkill()).thenReturn(Skill.AGILITY);

        SkillXpTask task = SkillXpTask.builder().skill(Skill.ATTACK).targetSkillXp(1234).build();

        assertFalse(service.update(task, statChangedEvent));
    }

    @Test
    void update_skillXpTask_shouldReturnTrueIfThePlayerXPExceedsTheGoal() {
        SkillXpTask task = SkillXpTask.builder().skill(Skill.ATTACK).targetSkillXp(1234).build();

        assertTrue(service.update(task, 1234));
        assertEquals(Status.COMPLETED, task.getStatus());
    }

    @Test
    void update_skillXPTask_shouldReturnFalseIfThePlayerXPDoesNotExceedTheGoal() {
        SkillXpTask task = SkillXpTask.builder().skill(Skill.ATTACK).targetSkillXp(1234).build();

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
}
