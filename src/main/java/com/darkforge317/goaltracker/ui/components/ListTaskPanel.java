package com.darkforge317.goaltracker.ui.components;

import com.darkforge317.goaltracker.models.ActionHistory;
import com.darkforge317.goaltracker.models.ReorderTaskAction;
import com.darkforge317.goaltracker.models.RemoveTasksAction;
import com.darkforge317.goaltracker.models.enums.Status;
import com.darkforge317.goaltracker.models.task.QuestTask;
import com.darkforge317.goaltracker.models.task.Task;
import com.darkforge317.goaltracker.ui.TaskItemContent;
import com.darkforge317.goaltracker.utils.QuestRequirements;
import com.darkforge317.goaltracker.utils.ReorderableList;
import com.darkforge317.goaltracker.utils.TaskHierarchyUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Task row panel with context menu (move, indent, toggle),
 * cascading child handling, and shift-click remove support.
 */
public final class ListTaskPanel extends ListItemPanel<Task>
{
    private TaskItemContent taskContent;

    private Consumer<Task> parentChangedListener;

    private ActionHistory history;

    private final MouseAdapter shiftClickRemoveListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.isShiftDown() && SwingUtilities.isLeftMouseButton(e)) {
                removeItem.doClick();
            }
        }
    };

    private void addShiftRemoveListenerRecursive(Component c)
    {
        if (taskContent != null && c == taskContent.getAddChildButton()) return;
        c.addMouseListener(shiftClickRemoveListener);
        if (c instanceof Container)
        {
            for (Component child : ((Container) c).getComponents())
            {
                addShiftRemoveListenerRecursive(child);
            }
        }
    }

    public ListTaskPanel(ReorderableList<Task> list, Task item, JComponent parent)
    {
        super(list, item, parent);

        // Allow shift-click to remove this item and all its indented children
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.isShiftDown() && e.getButton() == MouseEvent.BUTTON1) {
                    // Delegate to the Remove action (which cascades children and notifies listeners)
                    removeItem.doClick();
                }
            }
        });
        // Apply the same shift-click removal listener to all nested components
        addShiftRemoveListenerRecursive(this);
    }

    public void refreshMenu()
    {
        super.refresh();
        popupMenu.removeAll();
        javax.swing.JMenu moveMenu = new javax.swing.JMenu("Move");
        boolean hasMove = false;
        if (!list.isFirst(item)) {
            for (var l : moveUp.getActionListeners()) moveUp.removeActionListener(l);
            moveUp.addActionListener(e -> {
                int oldIndex = list.indexOf(item);
                int newIndex = moveItemTo(Math.max(0, oldIndex - 1));
                if (newIndex < 0) return;
                if (history != null) {
                    history.push(new ReorderTaskAction(list, item, oldIndex, newIndex));
                }
                refreshParentList();
            });
            moveMenu.add(moveUp);
            hasMove = true;
        }
        if (!list.isLast(item)) {
            for (var l : moveDown.getActionListeners()) moveDown.removeActionListener(l);
            moveDown.addActionListener(e -> {
                int oldIndex = list.indexOf(item);
                int newIndex = moveItemTo(Math.min(list.size() - 1, oldIndex + 1));
                if (newIndex < 0) return;
                if (history != null) {
                    history.push(new ReorderTaskAction(list, item, oldIndex, newIndex));
                }
                refreshParentList();
            });
            moveMenu.add(moveDown);
            hasMove = true;
        }
        if (!list.isFirst(item)) {
            for (var l : moveToTop.getActionListeners()) moveToTop.removeActionListener(l);
            moveToTop.addActionListener(e -> {
                int oldIndex = list.indexOf(item);
                int newIndex = moveItemTo(0);
                if (newIndex < 0) return;
                if (history != null) {
                    history.push(new ReorderTaskAction(list, item, oldIndex, 0));
                }
                refreshParentList();
            });
            moveMenu.add(moveToTop);
            hasMove = true;
        }
        if (!list.isLast(item)) {
            for (var l : moveToBottom.getActionListeners()) moveToBottom.removeActionListener(l);
            moveToBottom.addActionListener(e -> {
                int oldIndex = list.indexOf(item);
                int newIndex = moveItemTo(list.size() - 1);
                if (newIndex < 0) return;
                if (history != null) {
                    history.push(new ReorderTaskAction(list, item, oldIndex, newIndex));
                }
                refreshParentList();
            });
            moveMenu.add(moveToBottom);
            hasMove = true;
        }
        if (hasMove) {
            popupMenu.add(moveMenu);
        }

        addParentMenuItems();

        String toggleLabel = "Mark as " + (item.getStatus() == Status.COMPLETED ? "Incomplete" : "Completed");
        JMenuItem toggleStatusItem = new JMenuItem(toggleLabel);
        toggleStatusItem.addActionListener(e -> {
            // Determine new status for the root item
            Status newStatus = (item.getStatus() == Status.COMPLETED ? Status.NOT_STARTED : Status.COMPLETED);

            java.util.List<Task> affected = TaskHierarchyUtils.getSubtree(list, item);
            java.util.List<Status> oldStatuses = new java.util.ArrayList<>();
            for (Task affectedTask : affected) oldStatuses.add(affectedTask.getStatus());

            ActionHistory.Action act = new ActionHistory.Action() {
                @Override public void undo() {
                    for (int i = 0; i < affected.size(); i++) {
                        affected.get(i).setStatus(oldStatuses.get(i));
                    }
                }
                @Override public void redo() {
                    for (Task t : affected) {
                        t.setStatus(newStatus);
                    }
                }
            };

            if (history != null) {
                act.redo();          // apply change now
                history.push(act);   // record for undo/redo
            } else {
                // Fallback if no history injected
                for (Task t : affected) {
                    t.setStatus(newStatus);
                }
            }

            if (taskContent != null) {
                taskContent.refresh();
            }
            refreshParentList();
        });
        popupMenu.add(toggleStatusItem);

        // Add quest pre-reqs menu item only if the quest actually has prereqs
        if (item instanceof QuestTask) {
            QuestTask questTask = (QuestTask) item;
            int baseIndent = item.getIndentLevel();
            // Gather existing direct/descendant children under this quest to avoid duplicates
            java.util.Set<String> existingKeys = new java.util.HashSet<>();
            for (Task child : TaskHierarchyUtils.getDescendants(list, item)) {
                existingKeys.add(child.getClass().getName() + "|" + child.toString());
            }
            var rawPrereqs = QuestRequirements.getRequirements(questTask.getQuest(), baseIndent);
            java.util.List<Task> missingPrereqs = new java.util.ArrayList<>();
            if (rawPrereqs != null) {
                for (Task p : rawPrereqs) {
                    String key = p.getClass().getName() + "|" + p.toString();
                    if (!existingKeys.contains(key)) {
                        missingPrereqs.add(p);
                    }
                }
            }
            if (!missingPrereqs.isEmpty()) {
                JMenuItem prereqItem = new JMenuItem("Add pre-reqs");
                prereqItem.addActionListener(e -> {
                    // Recompute and filter again at click time
                    var raw = QuestRequirements.getRequirements(questTask.getQuest(), baseIndent);
                    if (raw != null) {
                        // Refresh existing keys in case the list changed since menu was built
                        java.util.Set<String> currentKeys = new java.util.HashSet<>();
                        for (Task child : TaskHierarchyUtils.getDescendants(list, item)) {
                            currentKeys.add(child.getClass().getName() + "|" + child.toString());
                        }
                        java.util.List<Task> filtered = new java.util.ArrayList<>();
                        for (Task t : raw) {
                            String key = t.getClass().getName() + "|" + t.toString();
                            if (!currentKeys.contains(key)) {
                                filtered.add(t);
                            }
                        }
                        if (!filtered.isEmpty()) {
                            TaskHierarchyUtils.initializeFromIndent(filtered, baseIndent, item);
                            int index = list.indexOf(item);
                            for (Task prereq : filtered) {
                                list.add(index + 1, prereq);
                                index++;
                            }
                            TaskHierarchyUtils.normalizeIndentLevels(list);
                            refreshParentList();
                        }
                    }
                });
                popupMenu.add(prereqItem);
            }
        }

        // Make the Remove action also delete all indented children of this item
        for (var l : removeItem.getActionListeners()) {
            removeItem.removeActionListener(l);
        }
        removeItem.addActionListener(e -> {
            removeSubtree();
        });

        removeItem.setText("<html>Remove <span style='font-size: smaller; color: gray;'>(Shift+Left Click)</span></html>");
        popupMenu.add(removeItem);
    }

    public void onParentChanged(Consumer<Task> listener) { this.parentChangedListener = listener; }

    public void setTaskContent(TaskItemContent taskContent) {
        this.taskContent = taskContent;
        if (taskContent != null) {
            addShiftRemoveListenerRecursive(taskContent);
            addMouseDragEventForwarderRecursive(taskContent);
        }
    }

    @Override
    protected void addMouseDragEventForwarderRecursive(Component component)
    {
        if (taskContent != null && component == taskContent.getAddChildButton()) return;
        super.addMouseDragEventForwarderRecursive(component);
    }

    public void setActionHistory(ActionHistory history) {
        this.history = history;
    }

    /**
     * Appends Task-specific context menu items.
     */
    @Override
    protected void buildAdditionalMenu()
    {
        javax.swing.JMenu moveMenu = new javax.swing.JMenu("Move");
        boolean hasMove = false;
        // Rewire generic move actions to include ActionHistory entries
        if (!list.isFirst(item)) {
            for (var l : moveUp.getActionListeners()) moveUp.removeActionListener(l);
            moveUp.addActionListener(e -> {
                int oldIndex = list.indexOf(item);
                int newIndex = moveItemTo(Math.max(0, oldIndex - 1));
                if (newIndex < 0) return;
                if (history != null) {
                    history.push(new ReorderTaskAction(list, item, oldIndex, newIndex));
                }
                refreshParentList();
            });
            moveMenu.add(moveUp);
            hasMove = true;
        }
        if (!list.isLast(item)) {
            for (var l : moveDown.getActionListeners()) moveDown.removeActionListener(l);
            moveDown.addActionListener(e -> {
                int oldIndex = list.indexOf(item);
                int newIndex = moveItemTo(Math.min(list.size() - 1, oldIndex + 1));
                if (newIndex < 0) return;
                if (history != null) {
                    history.push(new ReorderTaskAction(list, item, oldIndex, newIndex));
                }
                refreshParentList();
            });
            moveMenu.add(moveDown);
            hasMove = true;
        }
        if (!list.isFirst(item)) {
            for (var l : moveToTop.getActionListeners()) moveToTop.removeActionListener(l);
            moveToTop.addActionListener(e -> {
                int oldIndex = list.indexOf(item);
                int newIndex = moveItemTo(0);
                if (newIndex < 0) return;
                if (history != null) {
                    history.push(new ReorderTaskAction(list, item, oldIndex, 0));
                }
                refreshParentList();
            });
            moveMenu.add(moveToTop);
            hasMove = true;
        }
        if (!list.isLast(item)) {
            for (var l : moveToBottom.getActionListeners()) moveToBottom.removeActionListener(l);
            moveToBottom.addActionListener(e -> {
                int oldIndex = list.indexOf(item);
                int newIndex = moveItemTo(list.size() - 1);
                if (newIndex < 0) return;
                if (history != null) {
                    history.push(new ReorderTaskAction(list, item, oldIndex, newIndex));
                }
                refreshParentList();
            });
            moveMenu.add(moveToBottom);
            hasMove = true;
        }
        if (hasMove) {
            popupMenu.add(moveMenu);
        }

        addParentMenuItems();

        // Toggle Completed/Incomplete for this task and its descendants
        String toggleLabel = "Mark as " + (item.getStatus() == Status.COMPLETED ? "Incomplete" : "Completed");
        JMenuItem toggleStatusItem = new JMenuItem(toggleLabel);
        toggleStatusItem.addActionListener(e -> {
            Status newStatus = (item.getStatus() == Status.COMPLETED ? Status.NOT_STARTED : Status.COMPLETED);
            java.util.List<Task> affected = TaskHierarchyUtils.getSubtree(list, item);
            java.util.List<Status> oldStatuses = new java.util.ArrayList<>();
            for (Task affectedTask : affected) oldStatuses.add(affectedTask.getStatus());
            ActionHistory.Action act = new ActionHistory.Action() {
                @Override public void undo() { for (int i = 0; i < affected.size(); i++) affected.get(i).setStatus(oldStatuses.get(i)); }
                @Override public void redo() { for (Task t : affected) t.setStatus(newStatus); }
            };
            if (history != null) { act.redo(); history.push(act); }
            else { for (Task t : affected) t.setStatus(newStatus); }
            if (taskContent != null) taskContent.refresh();
            refreshParentList();
        });
        popupMenu.add(toggleStatusItem);

        // Quest pre-reqs (if available)
        if (item instanceof QuestTask) {
            QuestTask questTask = (QuestTask) item;
            int baseIndent = item.getIndentLevel();
            java.util.Set<String> existingKeys = new java.util.HashSet<>();
            for (Task child : TaskHierarchyUtils.getDescendants(list, item)) {
                existingKeys.add(child.getClass().getName() + "|" + child.toString());
            }
            var rawPrereqs = QuestRequirements.getRequirements(questTask.getQuest(), baseIndent);
            java.util.List<Task> missingPrereqs = new java.util.ArrayList<>();
            if (rawPrereqs != null) {
                for (Task p : rawPrereqs) {
                    String key = p.getClass().getName() + "|" + p.toString();
                    if (!existingKeys.contains(key)) missingPrereqs.add(p);
                }
            }
            if (!missingPrereqs.isEmpty()) {
                JMenuItem prereqItem = new JMenuItem("Add pre-reqs");
                prereqItem.addActionListener(e -> {
                    var raw = QuestRequirements.getRequirements(questTask.getQuest(), baseIndent);
                    if (raw != null) {
                        java.util.Set<String> currentKeys = new java.util.HashSet<>();
                        for (Task child : TaskHierarchyUtils.getDescendants(list, item)) {
                            currentKeys.add(child.getClass().getName() + "|" + child.toString());
                        }
                        java.util.List<Task> filtered = new java.util.ArrayList<>();
                        for (Task t : raw) {
                            String key = t.getClass().getName() + "|" + t.toString();
                            if (!currentKeys.contains(key)) filtered.add(t);
                        }
                        if (!filtered.isEmpty()) {
                            TaskHierarchyUtils.initializeFromIndent(filtered, baseIndent, item);
                            int insertIndex = list.indexOf(item);
                            for (Task prereq : filtered) {
                                list.add(insertIndex + 1, prereq);
                                insertIndex++;
                            }
                            TaskHierarchyUtils.normalizeIndentLevels(list);
                            refreshParentList();
                        }
                    }
                });
                popupMenu.add(prereqItem);
            }
        }

        // Rewire Remove to cascade children and update label
        for (var l : removeItem.getActionListeners()) removeItem.removeActionListener(l);
        removeItem.addActionListener(e -> removeSubtree());
        removeItem.setText("<html>Remove <span style='font-size: smaller; color: gray;'>(Shift+Left Click)</span></html>");
        // Ensure remove is at the bottom: remove and re-add it
        popupMenu.remove(removeItem);
        popupMenu.add(removeItem);
    }

    private void refreshParentList()
    {
        Container parent = SwingUtilities.getAncestorOfClass(ListPanel.class, this);
        if (parent instanceof ListPanel) {
            ((ListPanel<?>) parent).tryBuildList();
            ((ListPanel<?>) parent).refresh();
        } else {
            // Fallback
            revalidate();
            repaint();
        }
    }

    private void addParentMenuItems()
    {
        java.util.List<Task> descendants = TaskHierarchyUtils.getDescendants(list, item);
        java.util.List<Task> candidates = new java.util.ArrayList<>();
        for (Task candidate : list)
            if (candidate != item && !candidate.isCollapsed()
                    && !TaskHierarchyUtils.hasCollapsedAncestor(list, candidate)
                    && !descendants.contains(candidate)
                    && TaskHierarchyUtils.canSetParent(list, item, candidate)) candidates.add(candidate);
        JMenuItem setParent = new JMenuItem("Set parent...");
        setParent.setEnabled(!candidates.isEmpty());
        setParent.addActionListener(e -> {
            Object selected = JOptionPane.showInputDialog(this, "Choose a parent task:", "Set parent",
                    JOptionPane.PLAIN_MESSAGE, null, candidates.toArray(), TaskHierarchyUtils.getParent(list, item));
            if (selected instanceof Task) changeParent(((Task) selected).getTaskId());
        });
        popupMenu.add(setParent);
        if (item.getParentTaskId() != null) {
            JMenuItem removeParent = new JMenuItem("Remove parent");
            removeParent.addActionListener(e -> removeParent());
            popupMenu.add(removeParent);
        }
    }

    private void removeParent()
    {
        Task oldParent = TaskHierarchyUtils.getParent(list, item);
        Task newParent = oldParent == null ? null : TaskHierarchyUtils.getParent(list, oldParent);
        changeParent(newParent == null ? null : newParent.getTaskId(), oldParent, true);
    }

    private void changeParent(String newParentId)
    {
        changeParent(newParentId, null, false);
    }

    private void changeParent(String newParentId, Task positionAnchor, boolean afterAnchorSubtree)
    {
        String oldParentId = item.getParentTaskId();
        if (java.util.Objects.equals(oldParentId, newParentId)) return;
        Task newParent = null;
        if (newParentId != null)
        {
            for (Task candidate : list)
                if (newParentId.equals(candidate.getTaskId())) { newParent = candidate; break; }
            if (newParent == null || !TaskHierarchyUtils.canSetParent(list, item, newParent)) return;
        }
        final Task destinationParent = newParent;
        java.util.List<Task> oldOrder = new java.util.ArrayList<>(list);
        java.util.List<Task> subtree = TaskHierarchyUtils.getSubtree(list, item);
        ActionHistory.Action action = new ActionHistory.Action() {
            @Override public void undo() {
                item.setParentTaskId(oldParentId);
                list.clear();
                list.addAll(oldOrder);
                TaskHierarchyUtils.normalizeIndentLevels(list);
            }
            @Override public void redo() {
                item.setParentTaskId(newParentId);
                if (destinationParent != null || positionAnchor != null)
                {
                    list.removeAll(subtree);
                    int insertIndex;
                    if (afterAnchorSubtree && positionAnchor != null)
                    {
                        int anchorIndex = list.indexOf(positionAnchor);
                        insertIndex = anchorIndex < 0 ? list.size() : anchorIndex + 1;
                        for (Task descendant : TaskHierarchyUtils.getDescendants(list, positionAnchor))
                        {
                            insertIndex = Math.max(insertIndex, list.indexOf(descendant) + 1);
                        }
                    }
                    else if (destinationParent != null)
                    {
                        insertIndex = list.indexOf(destinationParent) + 1;
                        for (Task descendant : TaskHierarchyUtils.getDescendants(list, destinationParent))
                            insertIndex = Math.max(insertIndex, list.indexOf(descendant) + 1);
                    }
                    else insertIndex = list.size();
                    list.addAll(insertIndex, subtree);
                }
                TaskHierarchyUtils.normalizeIndentLevels(list);
            }
        };
        if (history != null) { action.redo(); history.push(action); }
        else action.redo();
        if (parentChangedListener != null) parentChangedListener.accept(item);
        refreshParentList();
    }

    private void removeSubtree()
    {
        int removedIndex = list.indexOf(item);
        java.util.List<Task> subtree = TaskHierarchyUtils.getSubtree(list, item);
        if (subtree.isEmpty()) return;

        if (history != null)
        {
            RemoveTasksAction action = new RemoveTasksAction(list, subtree);
            action.redo();
            history.push(action);
        }
        else
        {
            for (int i = subtree.size() - 1; i >= 0; i--) list.remove(subtree.get(i));
        }

        if (this.removedWithIndexListener != null) this.removedWithIndexListener.accept(item, removedIndex);
        if (this.removedListener != null) this.removedListener.accept(item);
        refreshParentList();
    }

    /** Moves this row unless that would place it under a collapsed ancestor. */
    private int moveItemTo(int requestedIndex)
    {
        int oldIndex = list.indexOf(item);
        if (oldIndex < 0) return -1;

        list.remove(oldIndex);
        int newIndex = Math.max(0, Math.min(requestedIndex, list.size()));
        list.add(newIndex, item);
        if (TaskHierarchyUtils.hasCollapsedAncestor(list, item))
        {
            list.remove(newIndex);
            list.add(oldIndex, item);
            return -1;
        }
        return newIndex;
    }
    /**
     * Programmatically add quest prerequisites using the same logic as the context menu action.
     * This lets callers (e.g., presets) trigger prereq insertion without simulating a right-click.
     */
    public static void addPrereqsForTask(ReorderableList<Task> list, Task item)
    {
        if (!(item instanceof QuestTask)) {
            return;
        }
        QuestTask questTask = (QuestTask) item;
        int baseIndent = item.getIndentLevel();

        // Gather existing descendants under this task to avoid duplicates
        java.util.Set<String> existingKeys = new java.util.HashSet<>();
        for (Task child : TaskHierarchyUtils.getDescendants(list, item)) {
            existingKeys.add(child.getClass().getName() + "|" + child.toString());
        }

        java.util.List<Task> raw = QuestRequirements.getRequirements(questTask.getQuest(), baseIndent);
        if (raw == null || raw.isEmpty()) {
            return;
        }

        // Filter out any already-present entries
        java.util.List<Task> filtered = new java.util.ArrayList<>();
        for (Task t : raw) {
            String key = t.getClass().getName() + "|" + t.toString();
            if (!existingKeys.contains(key)) {
                filtered.add(t);
            }
        }

        if (filtered.isEmpty()) {
            return;
        }

        TaskHierarchyUtils.initializeFromIndent(filtered, baseIndent, item);

        // Insert directly after the parent item, preserving order from QuestRequirements
        int insertIndex = list.indexOf(item);
        for (Task prereq : filtered) {
            list.add(insertIndex + 1, prereq);
            insertIndex++;
        }
        TaskHierarchyUtils.normalizeIndentLevels(list);
    }
}
