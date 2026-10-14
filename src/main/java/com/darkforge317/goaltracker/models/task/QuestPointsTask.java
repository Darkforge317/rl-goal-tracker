package com.darkforge317.goaltracker.models.task;

import com.google.gson.annotations.SerializedName;
import com.darkforge317.goaltracker.models.enums.TaskType;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A task representing a target number of Quest points. */
@Getter
@Setter
@SuperBuilder
public final class QuestPointsTask extends Task
{
    /** Current maximum QP listed by the OSRS Wiki; update when quests add more points. */
    public static final int MAX_QUEST_POINTS = 343;
    public static final int MIN_QUEST_POINTS = 0;

    @SerializedName("quest_points")
    private int targetQuestPoints;

    /** Last observed Quest point total, persisted so progress survives logout and world hopping. */
    @SerializedName("current_quest_points")
    private int currentQuestPoints;

    @Override
    public String getDisplayName()
    {
        return String.format("Reach %d Quest Points", targetQuestPoints);
    }

    @Override
    public String toString()
    {
        if (currentQuestPoints > 0 && currentQuestPoints < targetQuestPoints)
        {
            return String.format("%d/%d Quest Points", currentQuestPoints, targetQuestPoints);
        }
        return String.format("%d Quest Points", targetQuestPoints);
    }

    @Override
    public TaskType getType()
    {
        return TaskType.QUEST_POINTS;
    }

    public boolean hasReachedTarget()
    {
        return currentQuestPoints >= targetQuestPoints;
    }

    public boolean wouldReachTarget(int questPoints)
    {
        return questPoints >= targetQuestPoints;
    }
}
