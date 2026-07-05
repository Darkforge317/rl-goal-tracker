package com.toofifty.goaltracker.models.task;

import com.google.gson.annotations.SerializedName;
import com.toofifty.goaltracker.models.enums.Status;
import com.toofifty.goaltracker.models.enums.TaskType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
/**
 * Base class for all goal tasks.
 * Provides status tracking, indent level management, and type identification.
 */
public abstract class Task
{
    @Builder.Default
    @SerializedName(value = "status", alternate = {"previous_result"})
    private Status status = Status.NOT_STARTED;

    @Builder.Default
    @SerializedName("has_been_notified")
    private boolean notified = false;

    @Builder.Default
    @SerializedName("indent_level")
    private int indentLevel = 0;

    // The maximum indent we'll allow. Currently, 15 levels in total (0-14)
    private static final int MAX_INDENT_LEVEL = 14;

    public boolean isDone() {
        return Status.COMPLETED.equals(this.status);
    }

    public void indent() {
        if (indentLevel >= MAX_INDENT_LEVEL) return;

        indentLevel += 1;
    }

    public void unindent() {
        if (isNotIndented()) return;

        indentLevel -= 1;
    }

    public boolean isIndented() {
        return indentLevel > 0;
    }

    public boolean isNotIndented() {
        return !isIndented();
    }

    public boolean isFullyIndented() {
        return indentLevel >= MAX_INDENT_LEVEL;
    }

    public boolean isNotFullyIndented() {
        return !isFullyIndented();
    }


    @Override
    abstract public String toString();

    /**
     * Returns a human-readable name for the task.
     */
    public abstract String getDisplayName();

    abstract public TaskType getType();
}
