package com.darkforge317.goaltracker.models.task;

import com.darkforge317.goaltracker.models.enums.Status;
import com.darkforge317.goaltracker.models.enums.TaskType;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A task representing a target combat level. */
@Getter
@Setter
@SuperBuilder
public final class CombatLevelTask extends Task
{
    @SerializedName("combat_level")
    private double targetCombatLevel;

    /** Last observed combat level, persisted so progress survives logout and world hopping. */
    @SerializedName("current_combat_level")
    private double currentCombatLevel;

    @Override
    public String getDisplayName()
    {
        return String.format("Reach combat level %s", formatCombatLevel(targetCombatLevel));
    }

    @Override
    public String toString()
    {
        return String.format("%s Combat", formatCombatLevel(targetCombatLevel));
    }

    @Override
    public TaskType getType()
    {
        return TaskType.COMBAT_LEVEL;
    }

    public boolean hasReachedTargetLevel()
    {
        return currentCombatLevel >= targetCombatLevel;
    }

    public boolean wouldReachTargetLevel(double combatLevel)
    {
        double displayedCombatLevel = BigDecimal.valueOf(combatLevel)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
        return displayedCombatLevel >= targetCombatLevel;
    }

    private static String formatCombatLevel(double level)
    {
        return BigDecimal.valueOf(level).stripTrailingZeros().toPlainString();
    }

}
