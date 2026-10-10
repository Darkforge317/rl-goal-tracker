package com.darkforge317.goaltracker.utils;

import com.darkforge317.goaltracker.models.task.Task;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Helpers for task parentage, which is stored independently from display indentation. */
public final class TaskHierarchyUtils
{
    private TaskHierarchyUtils() {}

    /** Assigns identities and migrates a legacy flat indent hierarchy to explicit parent IDs. */
    public static void initializeFromIndent(List<Task> tasks)
    {
        initializeFromIndent(tasks, 0, null);
    }

    /** Assigns hierarchy for an inserted indent-based block under an existing parent. */
    public static void initializeFromIndent(List<Task> tasks, int baseIndent, Task baseParent)
    {
        ensureTaskIds(tasks);
        List<Task> ancestors = new ArrayList<>();
        for (Task task : tasks)
        {
            int indent = Math.max(0, task.getIndentLevel());
            task.setIndentLevel(indent);
            while (!ancestors.isEmpty()
                    && ancestors.get(ancestors.size() - 1).getIndentLevel() >= indent)
            {
                ancestors.remove(ancestors.size() - 1);
            }

            Task parent = ancestors.isEmpty()
                    ? (indent > baseIndent ? baseParent : null)
                    : ancestors.get(ancestors.size() - 1);
            task.setParentTaskId(parent == null ? null : parent.getTaskId());
            ancestors.add(task);
        }
    }

    public static void ensureTaskIds(List<Task> tasks)
    {
        for (Task task : tasks)
        {
            if (task.getTaskId() == null || task.getTaskId().isEmpty())
            {
                task.setTaskId(UUID.randomUUID().toString());
            }
        }
    }

    public static Task getParent(List<Task> tasks, Task task)
    {
        if (task.getParentTaskId() == null) return null;
        for (Task candidate : tasks)
        {
            if (task.getParentTaskId().equals(candidate.getTaskId())) return candidate;
        }
        return null;
    }

    public static List<Task> getChildren(List<Task> tasks, Task parent)
    {
        List<Task> children = new ArrayList<>();
        for (Task candidate : tasks)
        {
            if (parent.getTaskId() != null && parent.getTaskId().equals(candidate.getParentTaskId()))
            {
                children.add(candidate);
            }
        }
        return children;
    }

    /** Returns all descendants in their current list order, even when interleaved with other tasks. */
    public static List<Task> getDescendants(List<Task> tasks, Task parent)
    {
        List<Task> descendants = new ArrayList<>();
        String parentId = parent.getTaskId();
        if (parentId == null) return descendants;

        for (Task candidate : tasks)
        {
            if (candidate == parent) continue;
            if (hasAncestor(tasks, candidate, parentId)) descendants.add(candidate);
        }
        return descendants;
    }

    public static List<Task> getSubtree(List<Task> tasks, Task parent)
    {
        List<Task> subtree = new ArrayList<>();
        if (tasks.contains(parent)) subtree.add(parent);
        subtree.addAll(getDescendants(tasks, parent));
        return subtree;
    }

    public static boolean hasChildren(List<Task> tasks, Task parent)
    {
        return !getChildren(tasks, parent).isEmpty();
    }

    /** Returns the outermost collapsed ancestor that remains visible for this task. */
    public static Task getVisibleCollapsedAncestor(List<Task> tasks, Task task)
    {
        String parentId = task.getParentTaskId();
        Set<String> visited = new HashSet<>();
        Task outermostCollapsed = null;
        while (parentId != null && visited.add(parentId))
        {
            Task parent = findById(tasks, parentId);
            if (parent == null) break;
            if (parent.isCollapsed()) outermostCollapsed = parent;
            parentId = parent.getParentTaskId();
        }
        return outermostCollapsed;
    }

    public static boolean hasCollapsedAncestor(List<Task> tasks, Task task)
    {
        String parentId = task.getParentTaskId();
        Set<String> visited = new HashSet<>();
        while (parentId != null && visited.add(parentId))
        {
            Task parent = findById(tasks, parentId);
            if (parent == null) return false;
            if (parent.isCollapsed()) return true;
            parentId = parent.getParentTaskId();
        }
        return false;
    }

    private static boolean hasAncestor(List<Task> tasks, Task task, String ancestorId)
    {
        String parentId = task.getParentTaskId();
        Set<String> visited = new HashSet<>();
        while (parentId != null && visited.add(parentId))
        {
            if (ancestorId.equals(parentId)) return true;
            Task parent = findById(tasks, parentId);
            if (parent == null) return false;
            parentId = parent.getParentTaskId();
        }
        return false;
    }

    private static Task findById(List<Task> tasks, String taskId)
    {
        for (Task task : tasks)
        {
            if (taskId.equals(task.getTaskId())) return task;
        }
        return null;
    }
}
