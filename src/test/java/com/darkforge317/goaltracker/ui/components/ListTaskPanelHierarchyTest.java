package com.darkforge317.goaltracker.ui.components;

import com.darkforge317.goaltracker.models.ActionHistory;
import com.darkforge317.goaltracker.models.task.ManualTask;
import com.darkforge317.goaltracker.models.task.Task;
import com.darkforge317.goaltracker.ui.TaskInteractionState;
import com.darkforge317.goaltracker.utils.ReorderableList;
import com.darkforge317.goaltracker.utils.TaskHierarchyUtils;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListTaskPanelHierarchyTest
{
    @Test
    void setParentMovesEntireSubtreeNextToParentAndUndoRedoRestoresState()
    {
        ManualTask parent = task("Parent");
        ManualTask unrelated = task("Unrelated");
        ManualTask moved = task("Moved");
        ManualTask child = task("Moved child");
        parent(child, moved);
        ReorderableList<Task> tasks = ReorderableList.from(Arrays.asList(parent, unrelated, moved, child));
        TaskHierarchyUtils.normalizeIndentLevels(tasks);
        ActionHistory history = new ActionHistory();
        ListTaskPanel panel = panel(tasks, moved, history);

        panel.changeParent(parent.getTaskId());
        assertEquals(Arrays.asList(parent, moved, child, unrelated), tasks);
        assertEquals(parent.getTaskId(), moved.getParentTaskId());
        assertEquals(moved.getTaskId(), child.getParentTaskId());
        assertEquals(Arrays.asList(0, 1, 2, 0), indentLevels(tasks));

        history.undo();
        assertEquals(Arrays.asList(parent, unrelated, moved, child), tasks);
        assertNull(moved.getParentTaskId());
        assertEquals(moved.getTaskId(), child.getParentTaskId());

        history.redo();
        assertEquals(Arrays.asList(parent, moved, child, unrelated), tasks);
        assertEquals(parent.getTaskId(), moved.getParentTaskId());
    }

    @Test
    void removeParentPlacesOrphanAfterOldParentsRemainingSubtreeBeforeNextSibling()
    {
        ManualTask a = task("A");
        ManualTask b1 = task("B1");
        ManualTask b2 = task("B2");
        ManualTask c1 = task("C1");
        ManualTask c2 = task("C2");
        ManualTask c2Child = task("C2 child");
        ManualTask b3 = task("B3");
        ManualTask b4 = task("B4");
        parent(b1, a);
        parent(b2, a);
        parent(c1, b2);
        parent(c2, b2);
        parent(c2Child, c2);
        parent(b3, a);
        parent(b4, a);
        ReorderableList<Task> tasks = ReorderableList.from(Arrays.asList(a, b1, b2, c1, c2, c2Child, b3, b4));
        TaskHierarchyUtils.normalizeIndentLevels(tasks);
        ActionHistory history = new ActionHistory();
        ListTaskPanel panel = panel(tasks, c2, history);

        panel.removeParent();
        assertEquals(Arrays.asList(a, b1, b2, c1, c2, c2Child, b3, b4), tasks);
        assertEquals(a.getTaskId(), c2.getParentTaskId());
        assertEquals(c2.getTaskId(), c2Child.getParentTaskId());
        assertEquals(1, c2.getIndentLevel());
        assertEquals(2, c2Child.getIndentLevel());

        history.undo();
        assertEquals(b2.getTaskId(), c2.getParentTaskId());
        assertEquals(Arrays.asList(a, b1, b2, c1, c2, c2Child, b3, b4), tasks);
        history.redo();
        assertEquals(a.getTaskId(), c2.getParentTaskId());
        assertEquals(Arrays.asList(a, b1, b2, c1, c2, c2Child, b3, b4), tasks);
    }

    @Test
    void setParentRejectsSelfCurrentParentAndDescendantTargets()
    {
        ManualTask root = task("root");
        ManualTask child = task("child");
        ManualTask grandchild = task("grandchild");
        parent(child, root);
        parent(grandchild, child);
        ReorderableList<Task> tasks = ReorderableList.from(Arrays.asList(root, child, grandchild));
        TaskHierarchyUtils.normalizeIndentLevels(tasks);
        ListTaskPanel panel = panel(tasks, child, null);

        panel.changeParent(child.getTaskId());
        panel.changeParent(root.getTaskId());
        panel.changeParent(grandchild.getTaskId());

        assertEquals(root.getTaskId(), child.getParentTaskId());
        assertEquals(Arrays.asList(root, child, grandchild), tasks);
    }

    private static ListTaskPanel panel(ReorderableList<Task> tasks, Task task, ActionHistory history)
    {
        ListTaskPanel panel = new ListTaskPanel(tasks, task, new JPanel(), new TaskInteractionState());
        panel.setActionHistory(history);
        return panel;
    }

    private static ManualTask task(String name)
    {
        return ManualTask.builder().description(name).build();
    }

    private static void parent(Task task, Task parent)
    {
        task.setParentTaskId(parent.getTaskId());
    }

    private static List<Integer> indentLevels(List<Task> tasks)
    {
        return Arrays.asList(tasks.get(0).getIndentLevel(), tasks.get(1).getIndentLevel(),
                tasks.get(2).getIndentLevel(), tasks.get(3).getIndentLevel());
    }
}
