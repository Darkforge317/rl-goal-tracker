package com.darkforge317.goaltracker.utils;

import com.darkforge317.goaltracker.models.task.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers for navigating parent/child relationships in the flat, indent-level-based
 * task list. A task's descendants are the consecutive tasks that follow it in the
 * list with a strictly greater indent level than it.
 */
public final class TaskHierarchyUtils
{
    private TaskHierarchyUtils() {}

    public static List<Task> getDescendants(List<Task> list, Task parent)
    {
        List<Task> descendants = new ArrayList<>();
        int index = list.indexOf(parent);
        if (index == -1) return descendants;

        int baseIndent = parent.getIndentLevel();
        for (int i = index + 1; i < list.size(); i++)
        {
            Task candidate = list.get(i);
            if (candidate.getIndentLevel() <= baseIndent) break;
            descendants.add(candidate);
        }
        return descendants;
    }

    public static boolean hasChildren(List<Task> list, Task parent)
    {
        int index = list.indexOf(parent);
        if (index == -1 || index + 1 >= list.size()) return false;
        return list.get(index + 1).getIndentLevel() > parent.getIndentLevel();
    }

    /**
     * True if any ancestor of the given task (walking up the flat list) is collapsed,
     * meaning this task should currently be hidden from the rendered list.
     */
    public static boolean hasCollapsedAncestor(List<Task> list, Task task)
    {
        int index = list.indexOf(task);
        if (index == -1) return false;

        int level = task.getIndentLevel();
        for (int i = index - 1; i >= 0 && level > 0; i--)
        {
            Task candidate = list.get(i);
            if (candidate.getIndentLevel() < level)
            {
                if (candidate.isCollapsed()) return true;
                level = candidate.getIndentLevel();
            }
        }
        return false;
    }
}