package com.toofifty.goaltracker.models.task;

import com.google.gson.annotations.SerializedName;
import com.toofifty.goaltracker.models.enums.TaskType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@SuperBuilder
/**
 * Task representing reaching a target kill count for a specific NPC.
 * Kill counts are tracked via onActorDeath and persisted per character
 * in RuneLite config so they sync across devices.
 */
public final class KillCountTask extends Task
{
    @SerializedName("npc_id")
    private int npcId;

    @SerializedName("npc_name")
    private String npcName;

    @Builder.Default
    @SerializedName("target_kills")
    private int targetKills = 1;

    @Builder.Default
    @SerializedName("current_kills")
    private int currentKills = 0;

    /**
     * Update current kills from live KC data.
     * @param liveCount the current kill count from KillCountData
     * @return true if status changed
     */
    public boolean recomputeFromCount(int liveCount)
    {
        final int oldKills = this.currentKills;
        final com.toofifty.goaltracker.models.enums.Status oldStatus = getStatus();

        this.currentKills = Math.min(liveCount, targetKills);

        if (this.currentKills >= this.targetKills)
        {
            setStatus(com.toofifty.goaltracker.models.enums.Status.COMPLETED);
        }
        else if (this.currentKills > 0)
        {
            setStatus(com.toofifty.goaltracker.models.enums.Status.IN_PROGRESS);
        }
        else
        {
            setStatus(com.toofifty.goaltracker.models.enums.Status.NOT_STARTED);
        }

        return oldKills != this.currentKills || oldStatus != getStatus();
    }

    @Override
    public String toString()
    {
        if (currentKills > 0 && currentKills < targetKills)
        {
            return String.format("%d/%d %s kills", currentKills, targetKills, npcName);
        }
        return String.format("%d x %s kills", targetKills, npcName);
    }

    @Override
    public String getDisplayName()
    {
        return npcName + " KC";
    }

    @Override
    public TaskType getType()
    {
        return TaskType.KILL_COUNT;
    }
}