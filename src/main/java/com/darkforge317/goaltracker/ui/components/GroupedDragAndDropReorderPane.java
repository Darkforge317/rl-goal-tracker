package com.darkforge317.goaltracker.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DragSource;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Reorder pane that animates a contiguous group as one draggable block. */
final class GroupedDragAndDropReorderPane extends JLayeredPane
{
    @FunctionalInterface interface DragListener { void onDrag(Component root); }

    private Point dragStartPoint;
    private List<Component> draggingGroup;
    private int dragYOffset;
    private int dragIndex;
    private int minDragIndex;
    private int maxDragIndex;
    private int groupHeight;
    private int groupTop;
    private int rowGap;
    private Component dragRoot;
    private final java.util.Map<Component, Integer> dragOffsets = new java.util.IdentityHashMap<>();
    private Function<Component, List<Component>> groupResolver = component -> List.of(component);
    private BiFunction<Component, List<Component>, List<Integer>> boundsResolver = (component, rows) -> {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i <= rows.size(); i++) slots.add(i);
        return slots;
    };
    private final List<DragListener> dragListeners = new ArrayList<>();

    GroupedDragAndDropReorderPane()
    {
        setLayout(new GroupLayout());
        MouseAdapter mouse = new MouseAdapter()
        {
            @Override public void mousePressed(MouseEvent e)
            {
                if (SwingUtilities.isLeftMouseButton(e) && getComponentCount() > 1) dragStartPoint = e.getPoint();
            }
            @Override public void mouseDragged(MouseEvent e)
            {
                if (!SwingUtilities.isLeftMouseButton(e) || dragStartPoint == null) return;
                if (draggingGroup == null)
                {
                    if (e.getPoint().distance(dragStartPoint) > DragSource.getDragThreshold()) startDragging(e.getPoint());
                }
                else drag(e.getPoint());
            }
            @Override public void mouseReleased(MouseEvent e)
            {
                if (SwingUtilities.isLeftMouseButton(e)) finishDragging();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    void setGroupResolver(Function<Component, List<Component>> resolver) { groupResolver = resolver; }
    void setBoundsResolver(BiFunction<Component, List<Component>, List<Integer>> resolver) { boundsResolver = resolver; }
    void addDragListener(DragListener listener) { dragListeners.add(listener); }

    private List<Component> orderedDefaultComponents()
    {
        List<Component> components = new ArrayList<>(List.of(getComponentsInLayer(DEFAULT_LAYER)));
        components.sort(Comparator.comparingInt(Component::getY));
        return components;
    }

    private void startDragging(Point point)
    {
        Component root = null;
        for (Component candidate : getComponentsInLayer(DEFAULT_LAYER))
        {
            if (candidate.contains(dragStartPoint.x - candidate.getX(), dragStartPoint.y - candidate.getY()))
            {
                root = candidate;
                break;
            }
        }
        if (root == null) return;
        List<Component> all = orderedDefaultComponents();
        rowGap = DragInsertionGeometry.measureRowGap(all);
        List<Component> group = new ArrayList<>(groupResolver.apply(root));
        group.removeIf(component -> !all.contains(component));
        if (!group.contains(root)) group.add(root);
        group.sort(Comparator.comparingInt(Component::getY));
        if (group.isEmpty()) return;

        dragRoot = root;
        draggingGroup = group;
        groupTop = root.getY();
        dragYOffset = point.y - groupTop;
        groupHeight = group.get(group.size() - 1).getY() + group.get(group.size() - 1).getHeight() - groupTop;
        dragOffsets.clear();
        for (Component component : group) dragOffsets.put(component, component.getY() - groupTop);
        List<Component> remaining = new ArrayList<>(all);
        int insertion = 0;
        for (Component component : all)
        {
            if (group.contains(component))
            {
                if (component == group.get(0)) break;
            }
            else insertion++;
        }
        List<Integer> allowedSlots = allowedSlots(root, all, remaining.size());
        dragIndex = nearestSlot(insertion, allowedSlots);
        minDragIndex = allowedSlots.get(0);
        maxDragIndex = allowedSlots.get(allowedSlots.size() - 1);
        for (Component component : group) setLayer(component, DRAG_LAYER);
        moveGroup(point.y - dragYOffset);
        revalidate();
    }

    private void drag(Point point)
    {
        List<Component> remaining = orderedDefaultComponents();
        if (draggingGroup != null) remaining.removeAll(draggingGroup);
        List<Integer> allowedSlots = allowedSlots(dragRoot, orderedWithDraggingGroup(), remaining.size());
        minDragIndex = allowedSlots.get(0);
        maxDragIndex = allowedSlots.get(allowedSlots.size() - 1);

        int minY = insertionY(minDragIndex, remaining);
        int maxY = insertionY(maxDragIndex, remaining);
        int requestedTop = point.y - dragYOffset;
        moveGroup(DragInsertionGeometry.clamp(requestedTop, minY, Math.max(minY, maxY)));

        // Compare insertion slots to the dragged block's top, not the cursor's
        // raw y-coordinate. The cursor may be grabbed partway down the row (or
        // subtree), which otherwise biases slot selection toward later rows and
        // can make the dragged block jump over its siblings.
        int nextIndex = nearestSlotByY(requestedTop, allowedSlots, remaining);
        if (nextIndex != dragIndex)
        {
            dragIndex = nextIndex;
            revalidate();
        }
    }

    private List<Component> orderedWithDraggingGroup()
    {
        List<Component> result = orderedDefaultComponents();
        if (draggingGroup != null) result.addAll(Math.min(dragIndex, result.size()), draggingGroup);
        return result;
    }

    private int insertionY(int index, List<Component> remaining)
    {
        return DragInsertionGeometry.insertionY(index, dragIndex, remaining, groupHeight, rowGap);
    }

    private List<Integer> allowedSlots(Component root, List<Component> visibleRows, int remainingCount)
    {
        List<Integer> slots = boundsResolver.apply(root, visibleRows);
        if (slots == null || slots.isEmpty()) return List.of(0);
        List<Integer> valid = new ArrayList<>();
        for (Integer slot : slots)
            if (slot != null && slot >= 0 && slot <= remainingCount && !valid.contains(slot)) valid.add(slot);
        valid.sort(Integer::compareTo);
        return valid.isEmpty() ? List.of(0) : valid;
    }

    private int nearestSlot(int candidate, List<Integer> allowedSlots)
    {
        int best = allowedSlots.get(0);
        int distance = Math.abs(candidate - best);
        for (int slot : allowedSlots)
        {
            int nextDistance = Math.abs(candidate - slot);
            if (nextDistance < distance)
            {
                best = slot;
                distance = nextDistance;
            }
        }
        return best;
    }

    private int nearestSlotByY(int y, List<Integer> allowedSlots, List<Component> remaining)
    {
        return DragInsertionGeometry.nearestSlotByY(y, allowedSlots, dragIndex, remaining, groupHeight, rowGap);
    }

    private void moveGroup(int top)
    {
        groupTop = top;
        for (Component component : draggingGroup)
        {
            component.setLocation(0, top + dragOffsets.getOrDefault(component, 0));
        }
    }

    private void finishDragging()
    {
        if (draggingGroup != null)
        {
            Component root = dragRoot;
            List<Component> group = new ArrayList<>(draggingGroup);
            for (int i = 0; i < group.size(); i++) setLayer(group.get(i), DEFAULT_LAYER, dragIndex + i);
            draggingGroup = null;
            dragRoot = null;
            dragOffsets.clear();
            revalidate();
            for (DragListener listener : dragListeners) listener.onDrag(root);
        }
        dragStartPoint = null;
    }

    List<Component> getOrderedComponents()
    {
        List<Component> components = new ArrayList<>();
        for (Component component : getComponents())
            if (getLayer(component) == DEFAULT_LAYER) components.add(component);
        return components;
    }

    private final class GroupLayout extends javax.swing.BoxLayout
    {
        GroupLayout() { super(GroupedDragAndDropReorderPane.this, Y_AXIS); }

        @Override public void layoutContainer(Container target)
        {
            if (draggingGroup == null)
            {
                super.layoutContainer(target);
                return;
            }
            for (int i = 0; i < draggingGroup.size(); i++)
                setLayer(draggingGroup.get(i), DEFAULT_LAYER, dragIndex + i);
            super.layoutContainer(target);
            for (Component component : draggingGroup) setLayer(component, DRAG_LAYER);
            moveGroup(groupTop);
        }
    }
}
