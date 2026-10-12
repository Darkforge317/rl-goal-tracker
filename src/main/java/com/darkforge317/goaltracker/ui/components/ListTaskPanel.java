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
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Task row panel with context menu (move, indent, toggle),
 * cascading child handling, and shift-click remove support.
 */
public final class ListTaskPanel extends ListItemPanel<Task>
{
    private static ParentSelectionMode activeParentSelection;

    private TaskItemContent taskContent;
    private Color parentSelectionOverlay;

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

    private void setParentSelectionOverlay(Color color)
    {
        parentSelectionOverlay = color;
        repaint();
    }

    @Override
    public void paint(Graphics graphics)
    {
        super.paint(graphics);
        if (parentSelectionOverlay != null)
        {
            Graphics2D overlay = (Graphics2D) graphics.create();
            overlay.setColor(parentSelectionOverlay);
            overlay.fillRect(0, 0, getWidth(), getHeight());
            overlay.dispose();
        }
    }

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
            if (candidate != item && !descendants.contains(candidate)) candidates.add(candidate);
        JMenuItem setParent = new JMenuItem("Set parent...");
        setParent.setEnabled(!candidates.isEmpty());
        setParent.addActionListener(e -> startParentSelection(candidates));
        popupMenu.add(setParent);
        if (item.getParentTaskId() != null) {
            JMenuItem removeParent = new JMenuItem("Remove parent");
            removeParent.addActionListener(e -> removeParent());
            popupMenu.add(removeParent);
        }
    }

    private void startParentSelection(List<Task> candidates)
    {
        if (activeParentSelection != null) activeParentSelection.cancel();
        activeParentSelection = new ParentSelectionMode(this, candidates);
        activeParentSelection.start();
    }

    private boolean isValidParentCandidate(Task candidate)
    {
        return candidate != item
                && !TaskHierarchyUtils.getDescendants(list, item).contains(candidate);
    }

    /** Temporary window-level interaction that lets the player choose a visible task as parent. */
    private static final class ParentSelectionMode implements AWTEventListener, java.awt.KeyEventDispatcher
    {
        private static final Cursor ADD_CURSOR = createAddCursor();
        private static final Cursor INVALID_CURSOR = createInvalidCursor();
        private static final Color VALID_HIGHLIGHT = new Color(40, 200, 70, 80);
        private static final Color INVALID_HIGHLIGHT = new Color(220, 45, 45, 85);

        private final ListTaskPanel source;
        private final List<Task> candidates;
        private ListTaskPanel hovered;
        private final Map<Component, Cursor> sourceCursors;
        private final String sourceToolTip;
        private Map<Component, Cursor> hoveredCursors;
        private boolean active;

        private ParentSelectionMode(ListTaskPanel source, List<Task> candidates)
        {
            this.source = source;
            this.candidates = new ArrayList<>(candidates);
            this.sourceCursors = captureCursors(source);
            this.sourceToolTip = source.getToolTipText();
        }

        private void start()
        {
            active = true;
            Toolkit.getDefaultToolkit().addAWTEventListener(this,
                    AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
            if (source.taskContent != null) source.taskContent.setParentSelectionMode(true);
            setCursorRecursively(source, ADD_CURSOR);
            source.setToolTipText("Choose a task to become the parent. Right-click or press Escape to cancel.");
        }

        @Override
        public void eventDispatched(AWTEvent event)
        {
            if (!active || !(event instanceof MouseEvent)) return;
            MouseEvent mouse = (MouseEvent) event;
            Component component = mouse.getSource() instanceof Component ? (Component) mouse.getSource() : null;
            ListTaskPanel target = component instanceof ListTaskPanel ? (ListTaskPanel) component
                    : component == null ? null
                    : (ListTaskPanel) SwingUtilities.getAncestorOfClass(ListTaskPanel.class, component);

            if (mouse.getID() == MouseEvent.MOUSE_MOVED || mouse.getID() == MouseEvent.MOUSE_DRAGGED)
            {
                setHovered(target != null && target.list == source.list ? target : null);
                return;
            }

            if (mouse.getID() != MouseEvent.MOUSE_PRESSED) return;
            if (SwingUtilities.isRightMouseButton(mouse))
            {
                mouse.consume();
                cancel();
            }
            else if (SwingUtilities.isLeftMouseButton(mouse))
            {
                mouse.consume();
                if (target != null && target.list == source.list && candidates.contains(target.item)
                        && source.isValidParentCandidate(target.item))
                {
                    ListTaskPanel selected = target;
                    finish();
                    source.changeParent(selected.item.getTaskId());
                }
            }
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event)
        {
            if (active && event.getID() == KeyEvent.KEY_PRESSED && event.getKeyCode() == KeyEvent.VK_ESCAPE)
            {
                event.consume();
                cancel();
                return true;
            }
            return false;
        }

        private void setHovered(ListTaskPanel next)
        {
            if (hovered == next) return;
            if (hovered != null)
            {
                hovered.setParentSelectionOverlay(null);
                if (hovered.taskContent != null) hovered.taskContent.setParentSelectionMode(false);
                restoreCursors(hoveredCursors);
            }
            hovered = next;
            if (hovered != null)
            {
                boolean valid = candidates.contains(hovered.item) && source.isValidParentCandidate(hovered.item);
                if (hovered.taskContent != null) hovered.taskContent.setParentSelectionMode(true);
                hovered.setParentSelectionOverlay(valid ? VALID_HIGHLIGHT : INVALID_HIGHLIGHT);
                hoveredCursors = captureAndSetCursors(hovered, valid ? ADD_CURSOR : INVALID_CURSOR);
            }
        }

        private void cancel()
        {
            finish();
        }

        private void finish()
        {
            if (!active) return;
            active = false;
            setHovered(null);
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
            restoreCursors(sourceCursors);
            if (source.taskContent != null) source.taskContent.setParentSelectionMode(false);
            source.setToolTipText(sourceToolTip);
            if (activeParentSelection == this) activeParentSelection = null;
        }

        private static Map<Component, Cursor> captureAndSetCursors(Component root, Cursor cursor)
        {
            Map<Component, Cursor> originals = captureCursors(root);
            setCursorRecursively(root, cursor);
            return originals;
        }

        private static Map<Component, Cursor> captureCursors(Component root)
        {
            Map<Component, Cursor> cursors = new IdentityHashMap<>();
            captureCursors(root, cursors);
            return cursors;
        }

        private static void captureCursors(Component component, Map<Component, Cursor> cursors)
        {
            cursors.put(component, component.getCursor());
            if (component instanceof Container)
                for (Component child : ((Container) component).getComponents()) captureCursors(child, cursors);
        }

        private static void setCursorRecursively(Component component, Cursor cursor)
        {
            component.setCursor(cursor);
            if (component instanceof Container)
                for (Component child : ((Container) component).getComponents()) setCursorRecursively(child, cursor);
        }

        private static void restoreCursors(Map<Component, Cursor> cursors)
        {
            if (cursors != null) cursors.forEach(Component::setCursor);
        }

        private static Cursor createAddCursor()
        {
            return createSymbolCursor(false);
        }

        private static Cursor createInvalidCursor()
        {
            return createSymbolCursor(true);
        }

        private static Cursor createSymbolCursor(boolean invalid)
        {
            try
            {
                Dimension size = Toolkit.getDefaultToolkit().getBestCursorSize(24, 24);
                if (size.width == 0 || size.height == 0) return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
                BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                graphics.setColor(new Color(20, 30, 20));
                if (invalid)
                {
                    graphics.setStroke(new BasicStroke(5, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
                    graphics.drawLine(5, 5, 18, 18);
                    graphics.drawLine(18, 5, 5, 18);
                    graphics.setColor(new Color(245, 55, 55));
                    graphics.setStroke(new BasicStroke(3, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
                    graphics.drawLine(5, 5, 18, 18);
                    graphics.drawLine(18, 5, 5, 18);
                }
                else
                {
                    graphics.fillRect(10, 2, 5, 19);
                    graphics.fillRect(2, 10, 19, 5);
                    graphics.setColor(new Color(60, 230, 90));
                    graphics.fillRect(11, 3, 3, 17);
                    graphics.fillRect(3, 11, 17, 3);
                }
                graphics.dispose();
                return Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(12, 12),
                        invalid ? "InvalidParentCursor" : "SetParentCursor");
            }
            catch (RuntimeException e)
            {
                return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
            }
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
            if (newParent == null || !isValidParentCandidate(newParent)) return;
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
