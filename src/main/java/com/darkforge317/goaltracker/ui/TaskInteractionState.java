package com.darkforge317.goaltracker.ui;

import com.darkforge317.goaltracker.models.task.Task;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The authoritative interaction mode for the task list in one goal panel.
 * Task controls read this state instead of independently interpreting key and mouse input.
 */
public final class TaskInteractionState
{
    public enum Mode
    {
        NORMAL,
        SET_PARENT,
        SHIFT_DELETE
    }

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private Mode mode = Mode.NORMAL;
    private Task parentSelectionSource;
    private boolean shiftDown;

    public Mode getMode()
    {
        return mode;
    }

    public Task getParentSelectionSource()
    {
        return parentSelectionSource;
    }

    public void addListener(Runnable listener)
    {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener)
    {
        listeners.remove(listener);
    }

    public void beginParentSelection(Task source)
    {
        parentSelectionSource = source;
        setMode(Mode.SET_PARENT);
    }

    public void endParentSelection()
    {
        parentSelectionSource = null;
        setMode(shiftDown ? Mode.SHIFT_DELETE : Mode.NORMAL);
    }

    public void setShiftDown(boolean down)
    {
        shiftDown = down;
        if (mode != Mode.SET_PARENT)
        {
            setMode(down ? Mode.SHIFT_DELETE : Mode.NORMAL);
        }
    }

    private void setMode(Mode next)
    {
        if (mode == next) return;
        mode = next;
        for (Runnable listener : listeners) listener.run();
    }
}
