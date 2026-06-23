package com.toofifty.goaltracker.models.task;

import com.toofifty.goaltracker.models.enums.TaskType;
import com.toofifty.goaltracker.models.enums.Status;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.runelite.api.Skill;
import net.runelite.api.Client;

@Setter
@Getter
@SuperBuilder
/**
 * Task representing reaching a target level in a skill.
 * Stores the RuneLite Skill and required level.
 */
public final class SkillLevelTask extends Task
{
    private Skill skill;
    private int targetSkillLevel;
    private int currentSkillLevel;

    @Override
    public String getDisplayName()
    {
        return String.format("Reach level %d %s", targetSkillLevel, skill.getName());
    }

    @Override
    public String toString()
    {
        return String.format("%s %s", targetSkillLevel, skill.getName());
    }

    @Override
    public TaskType getType()
    {
        return TaskType.SKILL_LEVEL;
    }

    /**
     * Re-evaluate the player's level in the skill and update the Task.status field.
     * Safe to call on login and whenever relevant varbits/varps change.
     */
    public void refreshStatus(final Client client)
    {
        if (client == null || skill == null)
        {
            return;
        }


        currentSkillLevel = client.getRealSkillLevel(skill);


        if(hasReachedTargetLevel())
        {
            setStatus(Status.COMPLETED);
        }
        // This covers cases where players log into a separate character that no longer meets the task requirements
        else {
            setStatus(Status.NOT_STARTED);
        }
    }

    /**
     * Whether the player has reached the target level for this task.
     * @return <ul>
     *     <li>{@code true} - Target level <b>HAS</b> been reached</li>
     *     <li>{@code false} - Target level <b>HAS NOT</b> been reached</li>
     * </ul>
     */
    public boolean hasReachedTargetLevel()
    {
        return (currentSkillLevel >= targetSkillLevel);
    }

    /**
     * Whether the player would reach the target level for this task given the level provided.
     * @param level The level to check against
     * @return <ul>
     *     <li>{@code true} - Target level <b>HAS</b> been reached</li>
     *     <li>{@code false} - Target level <b>HAS NOT</b> been reached</li>
     * </ul>
     */
    public boolean wouldReachTargetLevel(int level)
    {
        return (level >= targetSkillLevel);
    }
}
