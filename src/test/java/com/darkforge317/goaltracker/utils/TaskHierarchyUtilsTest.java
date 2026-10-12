package com.darkforge317.goaltracker.utils;

import com.darkforge317.goaltracker.models.task.ManualTask;
import com.darkforge317.goaltracker.models.task.Task;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskHierarchyUtilsTest
{
    private static ManualTask task(String name, int indent)
    {
        return ManualTask.builder().description(name).indentLevel(indent).build();
    }

    private static void parent(Task child, Task parent)
    {
        child.setParentTaskId(parent == null ? null : parent.getTaskId());
    }

    @Test
    void initializeFromIndentCreatesParentsAndIdsAndNormalizesGaps()
    {
        ManualTask root = task("root", 3);
        ManualTask child = task("child", 7);
        ManualTask sibling = task("sibling", 4);
        ManualTask secondRoot = task("second root", 0);
        List<Task> tasks = Arrays.asList(root, child, sibling, secondRoot);

        TaskHierarchyUtils.initializeFromIndent(tasks);

        assertNotNull(root.getTaskId());
        assertEquals(root.getTaskId(), child.getParentTaskId());
        assertEquals(root.getTaskId(), sibling.getParentTaskId());
        assertNull(root.getParentTaskId());
        assertNull(secondRoot.getParentTaskId());
        assertEquals(Arrays.asList(0, 1, 1, 0), Arrays.asList(root.getIndentLevel(), child.getIndentLevel(),
                sibling.getIndentLevel(), secondRoot.getIndentLevel()));
    }

    @Test
    void initializeFromIndentBlockUsesSuppliedBaseParent()
    {
        ManualTask root = task("root", 0);
        ManualTask inserted = task("inserted", 2);
        ManualTask nested = task("nested", 3);
        TaskHierarchyUtils.initializeFromIndent(Arrays.asList(inserted, nested), 1, root);

        assertEquals(root.getTaskId(), inserted.getParentTaskId());
        assertEquals(inserted.getTaskId(), nested.getParentTaskId());
        assertEquals(2, inserted.getIndentLevel());
        assertEquals(3, nested.getIndentLevel());
    }

    @Test
    void getChildrenDescendantsAndSubtreeFollowExplicitParentageAndListOrder()
    {
        ManualTask root = task("root", 0);
        ManualTask childA = task("child A", 1);
        ManualTask grandchild = task("grandchild", 2);
        ManualTask childB = task("child B", 1);
        ManualTask unrelated = task("unrelated", 0);
        parent(childA, root);
        parent(grandchild, childA);
        parent(childB, root);
        List<Task> tasks = Arrays.asList(root, childA, grandchild, childB, unrelated);

        assertEquals(Arrays.asList(childA, childB), TaskHierarchyUtils.getChildren(tasks, root));
        assertEquals(Arrays.asList(childA, grandchild, childB), TaskHierarchyUtils.getDescendants(tasks, root));
        assertEquals(Arrays.asList(root, childA, grandchild, childB), TaskHierarchyUtils.getSubtree(tasks, root));
        assertTrue(TaskHierarchyUtils.hasChildren(tasks, root));
        assertFalse(TaskHierarchyUtils.hasChildren(tasks, unrelated));
    }

    @Test
    void normalizeIndentLevelsFollowsParentsRatherThanOldIndentValues()
    {
        ManualTask root = task("root", 9);
        ManualTask child = task("child", 2);
        ManualTask grandchild = task("grandchild", 12);
        parent(child, root);
        parent(grandchild, child);
        TaskHierarchyUtils.normalizeIndentLevels(Arrays.asList(root, child, grandchild));

        assertEquals(Arrays.asList(0, 1, 2), Arrays.asList(root.getIndentLevel(), child.getIndentLevel(), grandchild.getIndentLevel()));
    }

    @Test
    void normalizeIndentLevelsRecoversFromCycleAsRoots()
    {
        ManualTask first = task("first", 7);
        ManualTask second = task("second", 8);
        parent(first, second);
        parent(second, first);

        assertDoesNotThrow(() -> TaskHierarchyUtils.normalizeIndentLevels(Arrays.asList(first, second)));
        assertEquals(0, first.getIndentLevel());
        assertTrue(first.getIndentLevel() >= 0 && second.getIndentLevel() >= 0);
    }

    @Test
    void collapsedAncestorsAndVisibleCollapsedAnchorAreResolvedThroughNestedAncestors()
    {
        ManualTask root = task("root", 0);
        ManualTask middle = task("middle", 1);
        ManualTask leaf = task("leaf", 2);
        parent(middle, root);
        parent(leaf, middle);
        List<Task> tasks = Arrays.asList(root, middle, leaf);
        root.setCollapsed(true);
        middle.setCollapsed(true);

        assertTrue(TaskHierarchyUtils.hasCollapsedAncestor(tasks, leaf));
        assertSame(root, TaskHierarchyUtils.getVisibleCollapsedAncestor(tasks, leaf));
        root.setCollapsed(false);
        assertSame(middle, TaskHierarchyUtils.getVisibleCollapsedAncestor(tasks, leaf));
        middle.setCollapsed(false);
        assertFalse(TaskHierarchyUtils.hasCollapsedAncestor(tasks, leaf));
        assertNull(TaskHierarchyUtils.getVisibleCollapsedAncestor(tasks, leaf));
    }

    @Test
    void canSetParentRejectsTooDeepSubtreesAndAcceptsValidDepth()
    {
        List<Task> tasks = new ArrayList<>();
        ManualTask root = task("root", 0);
        tasks.add(root);
        Task parent = root;
        for (int i = 1; i <= Task.MAX_INDENT_LEVEL; i++)
        {
            ManualTask child = task("level " + i, i);
            parent(child, parent);
            tasks.add(child);
            parent = child;
        }
        ManualTask moved = task("moved", 0);
        tasks.add(moved);
        assertTrue(TaskHierarchyUtils.canSetParent(tasks, moved, root));
        assertFalse(TaskHierarchyUtils.canSetParent(tasks, moved, parent));
    }

    @Test
    void canSetParentRejectsSelfAndDescendantsToPreventCycles()
    {
        ManualTask root = task("root", 0);
        ManualTask child = task("child", 1);
        ManualTask grandchild = task("grandchild", 2);
        ManualTask unrelated = task("unrelated", 0);
        parent(child, root);
        parent(grandchild, child);
        List<Task> tasks = Arrays.asList(root, child, grandchild, unrelated);

        assertFalse(TaskHierarchyUtils.canSetParent(tasks, root, root));
        assertFalse(TaskHierarchyUtils.canSetParent(tasks, root, grandchild));
        assertTrue(TaskHierarchyUtils.canSetParent(tasks, grandchild, unrelated));
    }

    @Test
    void validChildSlotsNeverSplitSiblingFromItsDescendants()
    {
        ManualTask song = task("Song", 0);
        ManualTask mourning = task("Mourning", 1);
        ManualTask agility = task("Agility", 2);
        ManualTask history = task("History", 1);
        ManualTask priest = task("Priest", 2);
        parent(mourning, song);
        parent(agility, mourning);
        parent(history, song);
        parent(priest, history);
        List<Task> tasks = Arrays.asList(song, mourning, agility, history, priest);

        assertEquals(Arrays.asList(1, 3), TaskHierarchyUtils.getValidInsertionSlots(tasks, tasks, history));
        assertEquals(Arrays.asList(1, 3), TaskHierarchyUtils.getValidInsertionSlots(tasks, tasks, mourning));
    }

    @Test
    void validRootSlotsStayOutsideEveryHierarchyBlock()
    {
        ManualTask firstRoot = task("first root", 0);
        ManualTask child = task("child", 1);
        ManualTask secondRoot = task("second root", 0);
        ManualTask grandchild = task("grandchild", 2);
        ManualTask thirdRoot = task("third root", 0);
        parent(child, firstRoot);
        parent(grandchild, child);
        List<Task> tasks = Arrays.asList(firstRoot, child, grandchild, secondRoot, thirdRoot);

        assertEquals(Arrays.asList(0, 3, 4), TaskHierarchyUtils.getValidInsertionSlots(tasks, tasks, secondRoot));
    }

    @Test
    void validChildSlotsSupportCollapsedSiblingWithHiddenDescendants()
    {
        ManualTask root = task("root", 0);
        ManualTask collapsedSibling = task("collapsed", 1);
        ManualTask hiddenChild = task("hidden", 2);
        ManualTask moved = task("moved", 1);
        parent(collapsedSibling, root);
        parent(hiddenChild, collapsedSibling);
        parent(moved, root);
        collapsedSibling.setCollapsed(true);
        List<Task> all = Arrays.asList(root, collapsedSibling, hiddenChild, moved);
        List<Task> visible = Arrays.asList(root, collapsedSibling, moved);

        assertEquals(Arrays.asList(1, 2), TaskHierarchyUtils.getValidInsertionSlots(all, visible, moved));
    }
}
