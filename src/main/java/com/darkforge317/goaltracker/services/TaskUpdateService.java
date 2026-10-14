package com.darkforge317.goaltracker.services;

import com.darkforge317.goaltracker.ItemCache;
import com.darkforge317.goaltracker.models.enums.Status;
import com.darkforge317.goaltracker.models.task.CombatLevelTask;
import com.darkforge317.goaltracker.models.task.*;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.events.StatChanged;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Updates task status based on current client state and events.
 */
@Singleton
public final class TaskUpdateService
{
    @Inject private Client client;
    @Inject private ItemCache itemCache;

    /**
     * Dispatch update for a generic task, returning true if status/values changed.
     */
    public boolean update(Task task)
    {
        switch (task.getType())
        {
            case SKILL_LEVEL: return update((SkillLevelTask) task);
            case SKILL_XP:    return update((SkillXpTask) task);
            case COMBAT_LEVEL: return update((CombatLevelTask) task);
            case QUEST_POINTS: return update((QuestPointsTask) task);
            case QUEST:       return update((QuestTask) task);
            case ITEM:        return update((ItemTask) task);
            default:          return false;
        }
    }

    // -------------------- Skill Level --------------------

    /** Returns true if the cached combat level or task status changed. */
    public boolean update(CombatLevelTask task)
    {
        if (client.getGameState() != GameState.LOGGED_IN || !client.isClientThread())
        {
            return false;
        }
        Player player = client.getLocalPlayer();
        if (player == null)
        {
            return false;
        }
        double combatLevel = Experience.getCombatLevelPrecise(
            client.getRealSkillLevel(Skill.ATTACK),
            client.getRealSkillLevel(Skill.STRENGTH),
            client.getRealSkillLevel(Skill.DEFENCE),
            client.getRealSkillLevel(Skill.HITPOINTS),
            client.getRealSkillLevel(Skill.MAGIC),
            client.getRealSkillLevel(Skill.RANGED),
            client.getRealSkillLevel(Skill.PRAYER));
        return update(task, combatLevel);
    }

    /** Update from a combat level already read on the client thread. */
    public boolean update(CombatLevelTask task, double combatLevel)
    {
        double oldCombatLevel = task.getCurrentCombatLevel();
        Status oldStatus = task.getStatus();
        task.setCurrentCombatLevel(combatLevel);
        task.setStatus(task.wouldReachTargetLevel(combatLevel) ? Status.COMPLETED : Status.NOT_STARTED);
        return oldCombatLevel != task.getCurrentCombatLevel() || oldStatus != task.getStatus();
    }

    /** Read the current Quest point total from the logged-in client. */
    @SuppressWarnings("deprecation")
    public boolean update(QuestPointsTask task)
    {
        if (client.getGameState() != GameState.LOGGED_IN || !client.isClientThread())
        {
            return false;
        }
        return update(task, client.getVarpValue(VarPlayer.QUEST_POINTS));
    }

    /** Update from the Quest point total already read on the client thread. */
    public boolean update(QuestPointsTask task, int questPoints)
    {
        int oldQuestPoints = task.getCurrentQuestPoints();
        Status oldStatus = task.getStatus();
        task.setCurrentQuestPoints(Math.max(0, questPoints));
        task.setStatus(task.wouldReachTarget(task.getCurrentQuestPoints())
            ? Status.COMPLETED
            : (task.getCurrentQuestPoints() > 0 ? Status.IN_PROGRESS : Status.NOT_STARTED));
        return oldQuestPoints != task.getCurrentQuestPoints() || oldStatus != task.getStatus();
    }

    /** Returns true if an update has occurred. */
    public boolean update(SkillLevelTask task)
    {
        if (client.getGameState() != GameState.LOGGED_IN || !client.isClientThread())
        {
            return false;
        }
        return update(task, client.getRealSkillLevel(task.getSkill()));
    }

    /** Returns true if an update has occurred (event-driven). */
    public boolean update(SkillLevelTask task, StatChanged event)
    {
        if (event.getSkill() != task.getSkill())
        {
            return false;
        }
        return update(task, event.getLevel());
    }

    /** Returns true if an update has occurred given a specific level. */
    public boolean update(SkillLevelTask task, int level)
    {
        final Status oldStatus = task.getStatus();
        task.setCurrentSkillLevel(level);
        task.setStatus(level >= task.getTargetSkillLevel() ? Status.COMPLETED : Status.NOT_STARTED);
        return oldStatus != task.getStatus();
    }

    // -------------------- Skill XP --------------------

    /** Returns true if an update has occurred. */
    public boolean update(SkillXpTask task)
    {
        if (client.getGameState() != GameState.LOGGED_IN || !client.isClientThread())
        {
            return false;
        }
        return update(task, client.getSkillExperience(task.getSkill()));
    }

    /** Returns true if an update has occurred (event-driven). */
    public boolean update(SkillXpTask task, StatChanged event)
    {
        if (event.getSkill() != task.getSkill())
        {
            return false;
        }
        return update(task, event.getXp());
    }

    /** Returns true if an update has occurred given a specific XP value. */
    public boolean update(SkillXpTask task, int xp)
    {
        final Status oldStatus = task.getStatus();

        // Store the live player experience in the task's cache
        task.setCurrentSkillXp(xp);

        task.setStatus(xp >= task.getTargetSkillXp() ? Status.COMPLETED : Status.NOT_STARTED);
        return oldStatus != task.getStatus();
    }

    // -------------------- Quest --------------------

    /** Returns true if an update has occurred. */
    public boolean update(QuestTask task)
    {
        if (client.getGameState() != GameState.LOGGED_IN || !client.isClientThread())
        {
            return false;
        }
        final Status oldStatus = task.getStatus();
        task.setStatus(Status.fromQuestState(task.getQuest().getState(client)));
        return oldStatus != task.getStatus();
    }

    // -------------------- Item --------------------

    /** Returns true if an update has occurred. */
    public boolean update(ItemTask task)
    {
        final int oldAcquired = task.getAcquired();
        final Status oldStatus = task.getStatus();

        task.setAcquired(Math.min(itemCache.getTotalQuantity(task.getItemId()), task.getQuantity()));

        task.setStatus(
            task.getAcquired() >= task.getQuantity()
                ? Status.COMPLETED
                : (task.getAcquired() > 0 ? Status.IN_PROGRESS : Status.NOT_STARTED)
        );

        return oldAcquired != task.getAcquired() || oldStatus != task.getStatus();
    }
}
