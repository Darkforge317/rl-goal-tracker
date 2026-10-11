package com.darkforge317.goaltracker;

import com.darkforge317.goaltracker.models.Goal;
import com.darkforge317.goaltracker.models.task.SkillXpTask;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import org.junit.jupiter.api.Test;
import java.awt.Color;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EditedTaskRefreshTest
{
    @Test void completedTargetNotifiesAgainAfterEditingBack() throws Exception
    {
        Client client = mock(Client.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getSkillExperience(Skill.FISHING)).thenReturn(1000);
        GoalTrackerPlugin plugin = new GoalTrackerPlugin(client, null);
        ClientThread thread = mock(ClientThread.class);
        ChatMessageManager messages = mock(ChatMessageManager.class);
        GoalTrackerConfig config = mock(GoalTrackerConfig.class);
        when(config.completionMessageColor()).thenReturn(Color.WHITE);
        inject(plugin, "clientThread", thread);
        inject(plugin, "chatMessageManager", messages);
        inject(plugin, "config", config);
        // Run client work synchronously to exercise the edit completion check.
        doAnswer(call -> {
            Runnable work = call.getArgument(0);
            work.run();
            return null;
        }).when(thread).invokeLater(any(Runnable.class));
        inject(plugin, "goalManager", mock(GoalManager.class));
        inject(plugin, "uiStatusManager", mock(TaskUIStatusManager.class));
        SkillXpTask task = SkillXpTask.builder().skill(Skill.FISHING).targetSkillXp(500).build();
        Goal goal = Goal.builder().build();
        plugin.refreshEditedTask(task, goal);
        assertTrue(task.isDone());
        assertTrue(task.isNotified());
        task.setTargetSkillXp(2000);
        task.setNotified(false);
        plugin.refreshEditedTask(task, goal);
        assertFalse(task.isDone());
        task.setTargetSkillXp(500);
        task.setNotified(false);
        plugin.refreshEditedTask(task, goal);
        assertTrue(task.isDone());
        verify(messages, times(2)).queue(any(QueuedMessage.class));
        javax.swing.SwingUtilities.invokeAndWait(() -> {});
    }

    private static void inject(Object target, String name, Object value) throws Exception
    {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
