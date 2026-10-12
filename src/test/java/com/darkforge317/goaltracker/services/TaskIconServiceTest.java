package com.darkforge317.goaltracker.services;

import com.darkforge317.goaltracker.models.task.Task;
import com.darkforge317.goaltracker.models.task.ManualTask;
import com.darkforge317.goaltracker.models.task.QuestTask;
import com.darkforge317.goaltracker.models.task.SkillLevelTask;
import com.darkforge317.goaltracker.models.task.SkillXpTask;
import com.darkforge317.goaltracker.models.task.ItemTask;

import com.darkforge317.goaltracker.models.enums.Status;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskIconServiceTest {
    @Mock
    private ItemManager itemManager;

    @Mock
    private SkillIconManager skillIconManager;

    @InjectMocks
    TaskIconService service;

    @Mock
    AsyncBufferedImage image;

    @BeforeEach
    public void init() {
        when(image.getScaledInstance(anyInt(), anyInt(), anyInt())).thenReturn(image);
    }

    @Test
    void get_shouldSupportManualTasks() {
        Task task = ManualTask.builder().status(Status.NOT_STARTED).build();

        assertEquals(TaskIconService.CROSS_MARK_ICON, service.get(task));
    }

    @Test
    void get_shouldSupportCompletedManualTasks() {
        Task task = ManualTask.builder().status(Status.COMPLETED).build();

        assertEquals(TaskIconService.CHECK_MARK_ICON, service.get(task));
    }

    @Test
    void get_shouldSupportNotStartedQuestTasks() {
        Task task = QuestTask.builder().status(Status.NOT_STARTED).build();

        assertEquals(TaskIconService.QUEST_ICON, service.get(task));
    }

    @Test
    void get_shouldSupportCompletedQuestTasks() {
        Task task = QuestTask.builder().status(Status.COMPLETED).build();

        assertEquals(TaskIconService.QUEST_COMPLETE_ICON, service.get(task));
    }

    @Test
    void get_shouldSupportSkillLevelTasks() {
        when(skillIconManager.getSkillImage(Skill.ATTACK)).thenReturn(image);

        Task task = SkillLevelTask.builder().skill(Skill.ATTACK).build();

        assertEquals(ImageIcon.class, service.get(task).getClass());
        verify(skillIconManager).getSkillImage(Skill.ATTACK);
    }

    @Test
    void get_shouldSupportSkillXPTasks() {
        when(skillIconManager.getSkillImage(Skill.ATTACK)).thenReturn(image);

        Task task = SkillXpTask.builder().skill(Skill.ATTACK).build();

        assertEquals(ImageIcon.class, service.get(task).getClass());
        verify(skillIconManager).getSkillImage(Skill.ATTACK);
    }

    @Test
    void get_shouldSupportItemTasks() {
        when(itemManager.getImage(314)).thenReturn(image);

        Task task = ItemTask.builder().itemId(314).build();

        assertEquals(ImageIcon.class, service.get(task).getClass());
        verify(itemManager).getImage(314);
    }

    @Test
    void get_shouldRequestItemIconsFromSidebarThread() {
        when(itemManager.getImage(314)).thenReturn(image);

        Task task = ItemTask.builder().itemId(314).build();

        assertEquals(ImageIcon.class, service.get(task).getClass());
        verify(itemManager).getImage(314);
        org.junit.jupiter.api.Assertions.assertNotSame(TaskIconService.UNKNOWN_ICON, service.get(task));
    }
    @Test
    void updateIcon_shouldRefreshAfterLoadingWithoutOverwritingNewerTask() throws Exception {
        AsyncBufferedImage pending = new AsyncBufferedImage(null, 32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        when(itemManager.getImage(314)).thenReturn(pending);
        Task task = ItemTask.builder().itemId(314).build();
        JLabel label = new JLabel();
        Icon[] initial = new Icon[1];
        SwingUtilities.invokeAndWait(() -> {
            service.updateIcon(task, label);
            initial[0] = label.getIcon();
        });
        pending.loaded();
        SwingUtilities.invokeAndWait(() -> {
            org.junit.jupiter.api.Assertions.assertNotSame(initial[0], label.getIcon());
            assertEquals(16, label.getIcon().getIconWidth());
        });

        AsyncBufferedImage second = new AsyncBufferedImage(null, 32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        when(itemManager.getImage(315)).thenReturn(second);
        SwingUtilities.invokeAndWait(() -> {
            service.updateIcon(ItemTask.builder().itemId(315).build(), label);
            service.updateIcon(ManualTask.builder().status(Status.NOT_STARTED).build(), label);
        });
        second.loaded();
        SwingUtilities.invokeAndWait(() -> assertEquals(TaskIconService.CROSS_MARK_ICON, label.getIcon()));
    }
}