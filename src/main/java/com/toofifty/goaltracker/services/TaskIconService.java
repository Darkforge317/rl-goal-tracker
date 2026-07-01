package com.toofifty.goaltracker.services;

import com.toofifty.goaltracker.GoalTrackerPlugin;
import com.toofifty.goaltracker.models.task.*;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Resolves small 16x16 icons for tasks (items, skills, quests, manual checks).
 */
@Singleton
public final class TaskIconService
{
    private static final int ICON_SIZE = 16;

    public static final ImageIcon CROSS_MARK_ICON;
    public static final ImageIcon CHECK_MARK_ICON;
    public static final ImageIcon QUEST_ICON;
    public static final ImageIcon QUEST_COMPLETE_ICON;
    public static final ImageIcon KC_ICON;
    public static final ImageIcon KC_COMPLETE_ICON;
    public static final ImageIcon UNKNOWN_ICON;

    static
    {
        CROSS_MARK_ICON = new ImageIcon(
            ImageUtil.loadImageResource(GoalTrackerPlugin.class, "/cross_mark.png")
        );
        CHECK_MARK_ICON = new ImageIcon(
            ImageUtil.loadImageResource(GoalTrackerPlugin.class, "/check_mark.png")
        );
        QUEST_ICON = new ImageIcon(
            ImageUtil.loadImageResource(GoalTrackerPlugin.class, "/quest_icon.png")
        );
        QUEST_COMPLETE_ICON = new ImageIcon(
            ImageUtil.loadImageResource(GoalTrackerPlugin.class, "/quest_complete.png")
        );
        KC_ICON = new ImageIcon(
            ImageUtil.loadImageResource(GoalTrackerPlugin.class, "/kc_skull.png")
        );
        KC_COMPLETE_ICON = new ImageIcon(
                ImageUtil.loadImageResource(GoalTrackerPlugin.class, "/kc_skull_complete.png")
        );
        UNKNOWN_ICON = new ImageIcon(
            ImageUtil.loadImageResource(GoalTrackerPlugin.class, "/question_mark.png")
        );
    }

    @Inject private ItemManager itemManager;
    @Inject private SkillIconManager skillIconManager;
    @Inject private Client client;

    public ImageIcon get(Task task)
    {
        if (task instanceof ManualTask)
        {
            return get((ManualTask) task);
        }
        if (task instanceof QuestTask)
        {
            return get((QuestTask) task);
        }
        if (task instanceof KillCountTask)
        {
            return get((KillCountTask) task);
        }

        BufferedImage image = null;
        if (task instanceof SkillLevelTask)
        {
            image = get((SkillLevelTask) task);
        }
        else if (task instanceof SkillXpTask)
        {
            image = get((SkillXpTask) task);
        }
        else if (task instanceof ItemTask)
        {
            image = get((ItemTask) task);
        }

        if (image != null)
        {
            return iconify(image);
        }

        return UNKNOWN_ICON;
    }

    public ImageIcon get(ManualTask task)
    {
        return task.isDone() ? CHECK_MARK_ICON : CROSS_MARK_ICON;
    }

    public BufferedImage get(SkillLevelTask task)
    {
        return skillIconManager.getSkillImage(task.getSkill());
    }

    public BufferedImage get(SkillXpTask task)
    {
        return skillIconManager.getSkillImage(task.getSkill());
    }

    public ImageIcon get(QuestTask task)
    {
        return task.isDone() ? QUEST_COMPLETE_ICON : QUEST_ICON;
    }

    public BufferedImage get(ItemTask task)
    {
        if (task.getCachedIcon() == null && client.isClientThread())
        {
            task.setCachedIcon(itemManager.getImage(task.getItemId()));
        }
        return task.getCachedIcon();
    }

    public ImageIcon get(KillCountTask task)
    {
        return task.isDone() ? KC_COMPLETE_ICON : KC_ICON;
    }

    private ImageIcon iconify(BufferedImage img)
    {
        Image scaled = img.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }
}
