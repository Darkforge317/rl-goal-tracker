package com.toofifty.goaltracker.ui;

import com.toofifty.goaltracker.GoalTrackerPlugin;
import com.toofifty.goaltracker.models.ActionHistory;
import com.toofifty.goaltracker.models.Goal;
import com.toofifty.goaltracker.models.ToggleCompleteAction;
import com.toofifty.goaltracker.models.enums.Status;
import com.toofifty.goaltracker.models.task.ManualTask;
import com.toofifty.goaltracker.models.task.Task;
import com.toofifty.goaltracker.services.TaskIconService;
import com.toofifty.goaltracker.ui.components.ListItemPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Locale;

import static com.toofifty.goaltracker.utils.Constants.STATUS_TO_COLOR;

/**
 * UI component for rendering a single Task row inside a Goal.
 * Shows icon, title (editable for manual tasks), right-click context menu,
 * and handles refreshing on task updates.
 */
public final class TaskItemContent extends JPanel implements Refreshable
{
    private static final int INDENT_PER_LEVEL = 12; // pixels per indent level

    private final Task task;
    private final Goal goal;
    private final TaskIconService iconService;
    private final JLabel titleLabel = new JLabel() {
        @Override
        public String getToolTipText(MouseEvent event) {
            if (event != null && (event.getModifiersEx() & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0) {
                return "Shift-click to remove task and children";
            }
            return super.getToolTipText();
        }
    };
    private final JTextField titleEdit = new JTextField();
    private final JPanel titleStack = new JPanel(new CardLayout());
    private final JLabel iconLabel = new JLabel();
    private JPanel iconWrapper;
    private boolean titleEditable;

    private final GoalTrackerPlugin plugin;
    private ActionHistory actionHistory;

    // Custom Trash Cursor Assets
    private static final Cursor DEFAULT_PANEL_CURSOR = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    private static final Cursor TRASH_CURSOR;

    static {
        Cursor tempCursor;
        try {
            java.awt.image.BufferedImage trashImg = net.runelite.client.util.ImageUtil.loadImageResource(TaskItemContent.class, "/trash.png");
            Point hotspot = new Point(0, 0);
            tempCursor = java.awt.Toolkit.getDefaultToolkit().createCustomCursor(trashImg, hotspot, "TrashCursor");
        } catch (Exception e) {
            tempCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
        TRASH_CURSOR = tempCursor;
    }

    TaskItemContent(GoalTrackerPlugin plugin, Goal goal, Task task)
    {
        super(new BorderLayout());
        this.plugin = plugin;
        this.task = task;
        this.goal = goal;
        iconService = plugin.getTaskIconService();

        titleLabel.setPreferredSize(new Dimension(0, 24));
        titleLabel.setBorder(null);
        titleLabel.setOpaque(false);
        titleEdit.setBorder(null);
        titleEdit.setOpaque(false);
        titleEdit.setDragEnabled(true);

        titleStack.setOpaque(false);
        titleStack.add(titleLabel, "label");
        titleStack.add(titleEdit, "edit");
        add(titleStack, BorderLayout.CENTER);

        iconWrapper = new JPanel(new BorderLayout());
        iconWrapper.setBorder(new EmptyBorder(4, 0, 0, 4));
        iconWrapper.add(iconLabel, BorderLayout.NORTH);
        add(iconWrapper, BorderLayout.WEST);

        plugin.getUiStatusManager().addRefresher(task, this::refresh);

        this.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { updateTitleLabel(); }
        });

        titleEditable = (task instanceof ManualTask);
        if (titleEditable) {
            titleLabel.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { enterEdit(); }
            });
            titleEdit.addActionListener(e -> exitEdit(true));
            titleEdit.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusLost(java.awt.event.FocusEvent e) { exitEdit(true); }
            });
        }

        // Right-click to toggle completion with ActionHistory
        MouseAdapter contextMenuListener = new MouseAdapter()
        {
            private void showMenuIfNeeded(MouseEvent e)
            {
                if (!(e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)))
                {
                    return;
                }
                // Prefer the parent ListItemPanel context menu (move up/down/remove, etc.)
                Component src = (Component) e.getSource();
                JComponent listItem = (JComponent) SwingUtilities.getAncestorOfClass(ListItemPanel.class, src);
                if (listItem != null && listItem.getComponentPopupMenu() != null)
                {
                    Point p = SwingUtilities.convertPoint(src, e.getPoint(), listItem);
                    listItem.getComponentPopupMenu().show(listItem, p.x, p.y);
                    return;
                }

                // Fallback: show simple toggle menu if no parent popup menu is available
                boolean currentlyComplete = task.getStatus() == Status.COMPLETED;
                String label = currentlyComplete ? "Mark as Incomplete" : "Mark as Completed";

                JPopupMenu menu = new JPopupMenu();
                JMenuItem toggle = new JMenuItem(label);
                toggle.addActionListener(a -> {
                    ToggleCompleteAction act = new ToggleCompleteAction(task, currentlyComplete, !currentlyComplete);
                    act.redo();
                    if (actionHistory != null)
                    {
                        actionHistory.push(act);
                    }
                    plugin.getUiStatusManager().refresh(goal);
                });
                menu.add(toggle);
                Component invoker = (Component) e.getSource();
                menu.show(invoker, e.getX(), e.getY());
            }

            @Override public void mousePressed(MouseEvent e) { showMenuIfNeeded(e); }
            @Override public void mouseReleased(MouseEvent e) { showMenuIfNeeded(e); }
        };

        // Attach listener to multiple components to make right-click reliable across platforms
        this.addMouseListener(contextMenuListener);
        titleStack.addMouseListener(contextMenuListener);
        titleLabel.addMouseListener(contextMenuListener);
        titleEdit.addMouseListener(contextMenuListener);
        iconLabel.addMouseListener(contextMenuListener);

        // Track the Shift key toggling entirely within the Swing thread context
        KeyEventDispatcher shiftWatcher = new KeyEventDispatcher()
        {
            private boolean shiftWasDown = false;

            @Override
            public boolean dispatchKeyEvent(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_SHIFT)
                {
                    boolean isShiftDown = e.isShiftDown();
                    // Only fire an update when the state actually cuts over (press or release)
                    if (isShiftDown != shiftWasDown)
                    {
                        shiftWasDown = isShiftDown;
                        // Pass the exact shift state safely to the EDT loop
                        SwingUtilities.invokeLater(() -> forceTooltipUpdate(isShiftDown));
                    }
                }
                return false;
            }
        };


        // Bind the listener when this component is physically displayed on screen, and unbind to prevent leaks
        this.addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override
            public void ancestorAdded(javax.swing.event.AncestorEvent event) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(shiftWatcher);
            }

            @Override
            public void ancestorRemoved(javax.swing.event.AncestorEvent event) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(shiftWatcher);
            }

            @Override
            public void ancestorMoved(javax.swing.event.AncestorEvent event) {}
        });

        // Ensure that cursor styles actively adapt whenever the mouse slides onto or off a row layout
        MouseAdapter rowHoverCursorAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                boolean isShiftCurrentlyDown = (e.getModifiersEx() & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0;
                if (isShiftCurrentlyDown) {
                    updateAllChildCursors(TRASH_CURSOR);
                } else {
                    updateAllChildCursors(DEFAULT_PANEL_CURSOR);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                PointerInfo pi = MouseInfo.getPointerInfo();
                if (pi != null) {
                    Point mousePos = pi.getLocation();
                    SwingUtilities.convertPointFromScreen(mousePos, TaskItemContent.this);

                    if (!TaskItemContent.this.contains(mousePos)) {
                        updateAllChildCursors(DEFAULT_PANEL_CURSOR);
                    }
                } else {
                    updateAllChildCursors(DEFAULT_PANEL_CURSOR);
                }
            }
        };

        this.addMouseListener(rowHoverCursorAdapter);
        titleStack.addMouseListener(rowHoverCursorAdapter);
        titleLabel.addMouseListener(rowHoverCursorAdapter);
        titleEdit.addMouseListener(rowHoverCursorAdapter);
        iconLabel.addMouseListener(rowHoverCursorAdapter);
        if (iconWrapper != null) {
            iconWrapper.addMouseListener(rowHoverCursorAdapter);
        }
    }

    private void updateAllChildCursors(Cursor cursor) {
        this.setCursor(cursor);
        titleStack.setCursor(cursor);
        titleLabel.setCursor(cursor);
        titleEdit.setCursor(cursor);
        iconLabel.setCursor(cursor);
        if (iconWrapper != null) {
            iconWrapper.setCursor(cursor);
        }
    }

    private void forceTooltipUpdate(boolean isShiftActive)
    {
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) return;

        Point mousePos = pointerInfo.getLocation();
        SwingUtilities.convertPointFromScreen(mousePos, titleLabel);

        // Only process if the cursor is resting inside our text label coordinates
        if (titleLabel.contains(mousePos))
        {
            long now = System.currentTimeMillis();
            int modifiers = isShiftActive ? java.awt.event.InputEvent.SHIFT_DOWN_MASK : 0;
            if (isShiftActive) {
                titleLabel.setToolTipText("Shift-click to remove task and children");
                updateAllChildCursors(TRASH_CURSOR);
            } else {
                String full = task.toString();
                titleLabel.setToolTipText((full == null || full.isEmpty()) ? null : full);
                updateAllChildCursors(DEFAULT_PANEL_CURSOR);
            }
            MouseEvent moveEvent = new MouseEvent(titleLabel, MouseEvent.MOUSE_MOVED, now, modifiers, mousePos.x, mousePos.y, 0, false);
            ToolTipManager.sharedInstance().mouseMoved(moveEvent);
        }
    }

    public void setActionHistory(ActionHistory history)
    {
        this.actionHistory = history;
    }

    @Override
    public void refresh()
    {
        titleLabel.setForeground(STATUS_TO_COLOR.get(task.getStatus()));
        updateTitleLabel();

        int level = Math.max(0, task.getIndentLevel());
        // Shift rendering so level 0 = 0px, level 1 = 0px, level 2 = 12px, etc.
        // This avoids the first child appearing with an extra indent when prereqs are added.
        int indent = (level <= 1) ? 0 : (level - 1) * INDENT_PER_LEVEL;

        iconLabel.setIcon(iconService.get(task));
        // Apply indent to the wrapper instead of the label to avoid double padding
        iconLabel.setBorder(new EmptyBorder(0, 0, 0, 0));
        iconWrapper.setBorder(new EmptyBorder(4, indent, 0, 4));

        revalidate();
    }

    @Override
    public void setBackground(Color bg)
    {
        super.setBackground(bg);
        for (Component component : getComponents()) {
            component.setBackground(bg);
        }
    }

    private void updateTitleLabel()
    {
        String full = task.toString();
        titleLabel.setToolTipText((full == null || full.isEmpty()) ? null : full);
        if (getWidth() <= 0) { titleLabel.setText(full); return; }

        int insets = 0;
        if (getBorder() != null) {
            Insets ins = getBorder().getBorderInsets(this);
            insets = (ins.left + ins.right);
        }
        int iconW = iconWrapper != null ? iconWrapper.getPreferredSize().width : 0;
        int gap = 8;
        int avail = Math.max(16, getWidth() - insets - iconW - gap);

        FontMetrics fm = titleLabel.getFontMetrics(titleLabel.getFont());
        if (full == null) full = "";
        if (fm.stringWidth(full) <= avail) { titleLabel.setText(full); return; }

        String ellipsis = "…";
        int lo = 0, hi = full.length();
        int cut = hi;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            String candidate = full.substring(0, Math.max(0, mid)) + ellipsis;
            if (fm.stringWidth(candidate) <= avail) { cut = mid; lo = mid + 1; }
            else { hi = mid - 1; }
        }
        titleLabel.setText(full.substring(0, Math.max(0, cut)) + ellipsis);
    }

    private void enterEdit()
    {
        if (!titleEditable) return;
        titleEdit.setText(task.toString());
        ((CardLayout) titleStack.getLayout()).show(titleStack, "edit");
        titleEdit.requestFocusInWindow();
        titleEdit.selectAll();
    }

    private void exitEdit(boolean save)
    {
        if (!titleEditable) { ((CardLayout) titleStack.getLayout()).show(titleStack, "label"); return; }
        if (save) {
            String newText = titleEdit.getText();
            if (newText != null && task instanceof ManualTask) {
                ((ManualTask) task).setDescription(newText);
            }
        }
        ((CardLayout) titleStack.getLayout()).show(titleStack, "label");
        updateTitleLabel();
        plugin.getUiStatusManager().refresh(goal);
    }

    public Task getTask()
    {
        return task;
    }

    /**
     * Try to invoke the same context menu action as right-click -> "Add prerequisites".
     * @return true if the action was found and invoked, false otherwise
     */
    public boolean addPrereqsFromContext()
    {
        JComponent listItem = (JComponent) SwingUtilities.getAncestorOfClass(ListItemPanel.class, this);
        if (listItem == null || listItem.getComponentPopupMenu() == null)
        {
            return false;
        }

        JPopupMenu menu = listItem.getComponentPopupMenu();
        // Accept several label variants (case-insensitive)
        String[] targets = new String[] {
                "add prerequisites", "add pre-reqs", "add prereqs", "prerequisites"
        };

        return clickMenuItemByLabels(menu.getSubElements(), targets);
    }

    // Recursively search menu/submenus for a matching label and click it.
    private static boolean clickMenuItemByLabels(MenuElement[] items, String[] needles)
    {
        for (MenuElement me : items)
        {
            if (me instanceof JMenuItem)
            {
                JMenuItem it = (JMenuItem) me;
                String txt = it.getText();
                if (txt != null)
                {
                    String lower = txt.toLowerCase(Locale.ROOT).trim();
                    for (String needle : needles)
                    {
                        if (lower.contains(needle))
                        {
                            it.doClick();
                            return true;
                        }
                    }
                }
            }
            // Recurse into submenus and containers
            MenuElement[] children = me.getSubElements();
            if (children != null && children.length > 0 && clickMenuItemByLabels(children, needles))
            {
                return true;
            }
        }
        return false;
    }
}
