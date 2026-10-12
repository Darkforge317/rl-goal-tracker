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
        if (baseParent == null)
        {
            normalizeIndentLevels(tasks);
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

    /**
     * Returns legal insertion indices after removing the moved task's visible subtree.
     * Child tasks may be reordered only at sibling-subtree boundaries; roots may be
     * reordered only between complete hierarchy blocks.
     */
    public static List<Integer> getValidInsertionSlots(List<Task> tasks, List<Task> visibleItems, Task movedTask)
    {
        List<Task> draggedSubtree = getSubtree(tasks, movedTask);
        List<Task> remaining = new ArrayList<>(visibleItems);
        remaining.removeAll(draggedSubtree);

        Task parent = getParent(tasks, movedTask);
        List<Integer> validSlots = new ArrayList<>();
        if (parent != null)
        {
            int parentIndex = remaining.indexOf(parent);
            if (parentIndex < 0) return java.util.Collections.singletonList(0);
            validSlots.add(parentIndex + 1);
            for (Task sibling : getChildren(tasks, parent))
            {
                if (draggedSubtree.contains(sibling)) continue;
                int siblingSubtreeEnd = remaining.indexOf(sibling);
                for (Task descendant : getDescendants(tasks, sibling))
                {
                    int index = remaining.indexOf(descendant);
                    if (index >= 0) siblingSubtreeEnd = Math.max(siblingSubtreeEnd, index);
                }
                if (siblingSubtreeEnd >= 0) validSlots.add(siblingSubtreeEnd + 1);
            }
            java.util.Collections.sort(validSlots);
            return validSlots;
        }

        for (int slot = 0; slot <= remaining.size(); slot++)
        {
            boolean insideHierarchy = false;
            for (Task possibleParent : remaining)
            {
                int parentIndex = remaining.indexOf(possibleParent);
                if (parentIndex >= slot) continue;
                int lastDescendantIndex = parentIndex;
                for (Task descendant : getDescendants(tasks, possibleParent))
                {
                    int index = remaining.indexOf(descendant);
                    if (index >= 0) lastDescendantIndex = Math.max(lastDescendantIndex, index);
                }
                if (slot <= lastDescendantIndex)
                {
                    insideHierarchy = true;
                    break;
                }
            }
            if (!insideHierarchy) validSlots.add(slot);
        }
        return validSlots;
    }

    public static boolean hasChildren(List<Task> tasks, Task parent)
    {
        return !getChildren(tasks, parent).isEmpty();
    }

    /** Keeps the stored display indentation in sync with the explicit parent hierarchy. */
    public static void normalizeIndentLevels(List<Task> tasks)
    {
        ensureTaskIds(tasks);
        Set<String> visited = new HashSet<>();
        for (Task task : tasks)
        {
            if (getParent(tasks, task) == null)
            {
                assignIndentLevels(tasks, task, 0, visited);
            }
        }
        // Recover gracefully from malformed cyclic parent data by treating remaining tasks as roots.
        for (Task task : tasks)
        {
            if (!visited.contains(task.getTaskId())) assignIndentLevels(tasks, task, 0, visited);
        }
    }

    private static void assignIndentLevels(List<Task> tasks, Task task, int indent, Set<String> visited)
    {
        if (!visited.add(task.getTaskId())) return;
        task.setIndentLevel(indent);
        for (Task child : getChildren(tasks, task))
        {
            assignIndentLevels(tasks, child, indent + 1, visited);
        }
    }

    public static boolean canSetParent(List<Task> tasks, Task task, Task parent)
    {
        if (task == parent || getDescendants(tasks, task).contains(parent)) return false;
        int parentDepth = 0;
        Set<String> ancestors = new HashSet<>();
        Task current = parent;
        while (current != null)
        {
            if (!ancestors.add(current.getTaskId())) return false;
            Task ancestor = getParent(tasks, current);
            if (ancestor != null) parentDepth++;
            current = ancestor;
        }
        return parentDepth + 1 + getSubtreeHeight(tasks, task, new HashSet<>()) <= Task.MAX_INDENT_LEVEL;
    }

    private static int getSubtreeHeight(List<Task> tasks, Task task, Set<String> visited)
    {
        if (!visited.add(task.getTaskId())) return 0;
        int height = 0;
        for (Task child : getChildren(tasks, task))
        {
            height = Math.max(height, 1 + getSubtreeHeight(tasks, child, visited));
        }
        visited.remove(task.getTaskId());
        return height;
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
