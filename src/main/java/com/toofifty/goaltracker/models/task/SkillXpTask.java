package com.toofifty.goaltracker.models.task;

import com.toofifty.goaltracker.models.enums.Status;
import com.toofifty.goaltracker.models.enums.TaskType;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.runelite.api.Client;
import net.runelite.api.Skill;

@Getter
@Setter
@SuperBuilder
/**
 * Task representing earning a target XP amount in a skill.
 * Stores the RuneLite Skill and required XP.
 */
public final class SkillXpTask extends Task
{
    private Skill skill;
    private int currentSkillXp;
    private int targetSkillXp;

    @Override
    public String toString()
    {
        return String.format("%d %s XP", targetSkillXp, skill.getName());
    }

    @Override
    public String getDisplayName()
    {
        return String.format("%d %s XP", targetSkillXp, skill.getName());
    }

    @Override
    public TaskType getType()
    {
        return TaskType.SKILL_XP;
    }

    /**
     * Re-evaluate the player's Xp in the skill and update the Task.status field.
     * Safe to call on login and whenever relevant varbits/varps change.
     */
    public void refreshStatus(final Client client)
    {
        if (client == null || skill == null)
        {
            return;
        }

        currentSkillXp = client.getSkillExperience(skill);

        if(hasReachedTargetLevel())
        {
            setStatus(Status.COMPLETED);
        }
    }

    /**
     * Whether the player has reached the target Xp for this task.
     * @return <ul>
     *     <li>{@code true} - Target Xp <b>HAS</b> been reached</li>
     *     <li>{@code false} - Target Xp <b>HAS NOT</b> been reached</li>
     * </ul>
     */
    public boolean hasReachedTargetLevel()
    {
        return (currentSkillXp >= targetSkillXp);
    }

    /**
     * Whether the player would reach the target Xp for this task given the xp provided.
     * @param xp The Xp value to check against
     * @return <ul>
     *     <li>{@code true} - Target Xp <b>HAS</b> been reached</li>
     *     <li>{@code false} - Target Xp <b>HAS NOT</b> been reached</li>
     * </ul>
     */
    public boolean wouldReachTargetLevel(int xp)
    {
        return (xp >= targetSkillXp);
    }
}
