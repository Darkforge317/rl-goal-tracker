package com.darkforge317.goaltracker.models;

import com.darkforge317.goaltracker.models.task.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

/** Undoable removal of a task and all of its explicit descendants. */
public final class RemoveTasksAction implements ActionHistory.Action
{
    private static final class Entry
    {
        private final Task task;
        private final int index;

        private Entry(Task task, int index)
        {
            this.task = task;
            this.index = index;
        }
    }

    private final List<Task> tasks;
    private final List<Entry> removed = new ArrayList<>();

    public RemoveTasksAction(List<Task> tasks, List<Task> removedTasks)
    {
        this.tasks = tasks;
        for (Task task : removedTasks)
        {
            int index = tasks.indexOf(task);
            if (index >= 0) removed.add(new Entry(task, index));
        }
    }

    @Override
    public void undo()
    {
        List<Entry> byIndex = new ArrayList<>(removed);
        byIndex.sort(Comparator.comparingInt(entry -> entry.index));
        for (Entry entry : byIndex)
        {
            if (!tasks.contains(entry.task))
            {
                tasks.add(Math.max(0, Math.min(entry.index, tasks.size())), entry.task);
            }
        }
    }

    @Override
    public void redo()
    {
        for (int i = removed.size() - 1; i >= 0; i--)
        {
            tasks.remove(removed.get(i).task);
        }
    }
}
