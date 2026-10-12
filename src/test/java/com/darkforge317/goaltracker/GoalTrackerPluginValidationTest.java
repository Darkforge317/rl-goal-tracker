package com.darkforge317.goaltracker;

import com.darkforge317.goaltracker.models.enums.Status;
import com.darkforge317.goaltracker.models.enums.TaskType;
import com.darkforge317.goaltracker.models.task.ItemTask;
import com.darkforge317.goaltracker.models.task.QuestTask;
import com.darkforge317.goaltracker.models.task.SkillLevelTask;
import com.darkforge317.goaltracker.models.task.SkillXpTask;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoalTrackerPluginValidationTest
{
    @Test
    void refreshAllTasks_shouldValidateSkillsXpQuestsAndCachedItems()
    {
        Client client = mock(Client.class);
        ClientThread clientThread = mock(ClientThread.class);
        ChatMessageManager chatMessageManager = mock(ChatMessageManager.class);
        GoalTrackerConfig config = mock(GoalTrackerConfig.class);
        GoalManager goalManager = mock(GoalManager.class);
        TaskUIStatusManager uiStatusManager = mock(TaskUIStatusManager.class);
        when(config.completionMessageColor()).thenReturn(Color.GREEN);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getRealSkillLevel(Skill.ATTACK)).thenReturn(70);
        when(client.getSkillExperience(Skill.STRENGTH)).thenReturn(500_000);

        Quest quest = mock(Quest.class);
        when(quest.getState(client)).thenReturn(QuestState.FINISHED);

        SkillLevelTask skillTask = SkillLevelTask.builder()
                .skill(Skill.ATTACK)
                .targetSkillLevel(60)
                .build();
        SkillXpTask xpTask = SkillXpTask.builder()
                .skill(Skill.STRENGTH)
                .targetSkillXp(400_000)
                .build();
        QuestTask questTask = QuestTask.builder().quest(quest).build();
        ItemTask itemTask = ItemTask.builder()
                .itemId(314)
                .itemName("Feather")
                .quantity(3)
                .build();
        itemTask.setInventoryQuantity(InventoryID.BANK, 2);
        itemTask.setInventoryQuantity(InventoryID.INV, 1);

        when(goalManager.getIncompleteTasksByType(TaskType.SKILL_LEVEL)).thenReturn(List.of(skillTask));
        when(goalManager.getIncompleteTasksByType(TaskType.SKILL_XP)).thenReturn(List.of(xpTask));
        when(goalManager.getIncompleteTasksByType(TaskType.QUEST)).thenReturn(List.of(questTask));
        when(goalManager.getIncompleteTasksByType(TaskType.ITEM)).thenReturn(List.of(itemTask));
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(clientThread).invokeLater(any(Runnable.class));

        GoalTrackerPlugin plugin = new GoalTrackerPlugin(
                client,
                clientThread,
                chatMessageManager,
                config,
                goalManager,
                uiStatusManager);

        plugin.refreshAllTasks(null);

        assertEquals(Status.COMPLETED, skillTask.getStatus());
        assertEquals(Status.COMPLETED, xpTask.getStatus());
        assertEquals(Status.COMPLETED, questTask.getStatus());
        assertEquals(Status.COMPLETED, itemTask.getStatus());
        assertEquals(3, itemTask.getAcquired());
    }
}
