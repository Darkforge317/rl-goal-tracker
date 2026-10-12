package com.darkforge317.goaltracker.ui.components;

import com.darkforge317.goaltracker.ui.Refreshable;
import com.darkforge317.goaltracker.utils.ReorderableList;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Scrollable container that renders and manages a list of items with optional placeholder text.
 * Supports reordering, removal, and refresh of child panels.
 */
public final class ListPanel<T> extends JScrollPane implements Refreshable
{
    private final GroupedDragAndDropReorderPane listPanel = new GroupedDragAndDropReorderPane();

    private final ReorderableList<T> reorderableList;
    private final BiFunction<JComponent, T, ListItemPanel<T>> renderItem;

    private final Map<T, ListItemPanel<T>> itemPanelMap = new HashMap<>();

    private int gap = 2;
    private JComponent placeholder = new JLabel("Nothing interesting happens.");
    private Consumer<T> updatedListener;

    private int rowLeftInset = 0;
    private int rowRightInset = 0;
    private Predicate<T> rowVisibilityFilter = t -> true;
    private BiPredicate<List<T>, T> reorderValidator = (order, item) -> true;
    private Function<T, T> hiddenRowAnchor = null;
    private BiConsumer<List<T>, List<T>> reorderListener;
    private Function<T, List<T>> dragGroupProvider = Collections::singletonList;
    private BiFunction<List<T>, T, List<Integer>> dragBoundsProvider = (items, moved) -> {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i <= items.size(); i++) slots.add(i);
        return slots;
    };

    public ListPanel(
            ReorderableList<T> reorderableList,
            BiFunction<JComponent, T, ListItemPanel<T>> renderItem
    ) {
        super();
        this.reorderableList = reorderableList;
        this.renderItem = renderItem;

        listPanel.setGroupResolver(component -> {
            T moved = ((ListItemPanel<T>) component).item;
            List<T> groupItems = dragGroupProvider.apply(moved);
            List<Component> groupComponents = new ArrayList<>();
            for (Component candidate : listPanel.getComponents())
                if (candidate instanceof ListItemPanel && groupItems.contains(((ListItemPanel<T>) candidate).item))
                    groupComponents.add(candidate);
            return groupComponents;
        });
        listPanel.setBoundsResolver((component, rows) -> {
            T moved = ((ListItemPanel<T>) component).item;
            List<T> visibleItems = new ArrayList<>();
            for (Component row : rows) visibleItems.add(((ListItemPanel<T>) row).item);
            return dragBoundsProvider.apply(visibleItems, moved);
        });


        listPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        // Supports padding. Previously had 10px left/right padding, but introduction
        // of a horizontal scroll panel in GoalPanel.java caused the padding to look out of place
        listPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        listPanel.addDragListener(component -> {
                T updatedItem = ((ListItemPanel<T>)component).item;

                // Capture hidden rows and keep them anchored to their visible owner
                // when the visible rows are reordered.
                List<T> oldOrder = new ArrayList<>(reorderableList);
                Map<T, List<T>> hiddenFollowersOf = new HashMap<>();
                T currentVisible = null;
                for (T t : oldOrder) {
                    if (rowVisibilityFilter.test(t)) {
                        currentVisible = t;
                        hiddenFollowersOf.computeIfAbsent(t, ignored -> new ArrayList<>());
                    } else {
                        T anchor = hiddenRowAnchor == null ? currentVisible : hiddenRowAnchor.apply(t);
                        if (anchor != null) hiddenFollowersOf.computeIfAbsent(anchor, ignored -> new ArrayList<>()).add(t);
                    }
                }

                List<T> newVisibleOrder = new ArrayList<>();
                for (Component c : listPanel.getOrderedComponents()) {
                    if (c instanceof ListItemPanel) newVisibleOrder.add(((ListItemPanel<T>) c).item);
                }
                List<T> newFullOrder = new ArrayList<>();
                for (T visible : newVisibleOrder) {
                    newFullOrder.add(visible);
                    List<T> followers = hiddenFollowersOf.get(visible);
                    if (followers != null) newFullOrder.addAll(followers);
                }

                if (!reorderValidator.test(newFullOrder, updatedItem)) {
                    refresh();
                    return;
                }
                reorderableList.clear();
                reorderableList.addAll(newFullOrder);
                if (!oldOrder.equals(newFullOrder) && reorderListener != null)
                    reorderListener.accept(new ArrayList<>(oldOrder), new ArrayList<>(newFullOrder));
                if (updatedListener != null) updatedListener.accept(updatedItem);
                refresh();
            });

        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        wrapperPanel.add(listPanel, BorderLayout.NORTH);

        setBorder(new EmptyBorder(0, 0, 0, 0));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
        getVerticalScrollBar().setBorder(new EmptyBorder(0, 4, 0, 0));

        setViewportView(wrapperPanel);

        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
        tryBuildList();
    }

    public void setGap(int gap)
    {
        this.gap = gap;
        setBorder(new EmptyBorder(0, 0, 0, 0));
        tryBuildList();
    }

    public void setRowSideInsets(int left, int right)
    {
        this.rowLeftInset = Math.max(0, left);
        this.rowRightInset = Math.max(0, right);
        tryBuildList();
    }

    public void setPlaceholder(String placeholderText)
    {
        this.placeholder = new JLabel(placeholderText);
        tryBuildList();
    }

    public void setPlaceholder(JComponent placeholderComponent)
    {
        this.placeholder = placeholderComponent;
        tryBuildList();
    }

    public void setRowVisibilityFilter(Predicate<T> filter)
    {
        this.rowVisibilityFilter = filter != null ? filter : (t -> true);
        tryBuildList();
    }

    public void setReorderValidator(BiPredicate<List<T>, T> validator)
    {
        this.reorderValidator = validator != null ? validator : (order, item) -> true;
    }

    public void setHiddenRowAnchor(Function<T, T> anchor)
    {
        this.hiddenRowAnchor = anchor;
    }

    private List<ListItemPanel<T>> buildItemPanels()
    {
        return reorderableList
                .stream()
                .filter(rowVisibilityFilter)
                .map(this::buildItemPanel)
                .collect(Collectors.toList());
    }

    @Override
    public void refresh()
    {
        // Rebuild the list first in case the underlying data changed
        tryBuildList();

        // Then refresh all children
        for (Component component : listPanel.getComponents()) {
            if (component instanceof Refreshable) {
                ((Refreshable) component).refresh();
            }
        }
    }

    private ListItemPanel<T> buildItemPanel(T item)
    {
        if (itemPanelMap.containsKey(item)) {
            return itemPanelMap.get(item);
        }

        ListItemPanel<T> itemPanel = renderItem.apply(listPanel, item);

        itemPanel.onReordered((updatedItem) -> {
            tryBuildList();
            if (this.updatedListener != null) this.updatedListener.accept(updatedItem);
        });

        itemPanel.onRemoved((updatedItem) -> {
            tryBuildList();
            if (this.updatedListener != null) this.updatedListener.accept(updatedItem);
        });

        itemPanelMap.put(item, itemPanel);

        return itemPanel;
    }

    private GridBagConstraints getConstraints()
    {
        return getConstraints(0);
    }

    private GridBagConstraints getConstraints(int gridy)
    {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.gridy = gridy;
        constraints.gridx = 0;
        constraints.insets = new Insets(4, rowLeftInset, gap, rowRightInset);
        return constraints;
    }

    private void refreshChildMenus()
    {
        for (Component component : listPanel.getComponents()) {
            if (component instanceof ListItemPanel) {
                ((Refreshable) component).refresh();
            }
        }
    }

    /**
     * Build the initial list, if items are provided otherwise build a placeholder
     */
    public void tryBuildList()
    {
        itemPanelMap.clear();

        if (reorderableList.isEmpty()) {
            listPanel.removeAll();

            if (placeholder instanceof JLabel) {
                ((JLabel) placeholder).setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            }
            JPanel placeholderPanel = new JPanel();
            placeholderPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
            placeholderPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
            placeholderPanel.add(placeholder);
            listPanel.add(placeholderPanel, getConstraints());
        } else {
            listPanel.removeAll();

            GridBagConstraints constraints = getConstraints();
            buildItemPanels().forEach(component -> {
                listPanel.add(component, constraints);
                constraints.gridy++;
            });
        }

        refreshChildMenus();
        revalidate();
    }

    public void onUpdated(Consumer<T> listener) {
        this.updatedListener = listener;
    }

    public void onReordered(BiConsumer<List<T>, List<T>> listener)
    {
        this.reorderListener = listener;
    }

    public void setDragGroupProvider(Function<T, List<T>> provider)
    {
        dragGroupProvider = provider != null ? provider : Collections::singletonList;
    }

    public void setDragBoundsProvider(BiFunction<List<T>, T, List<Integer>> provider)
    {
        dragBoundsProvider = provider != null ? provider : (items, moved) -> {
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i <= items.size(); i++) slots.add(i);
            return slots;
        };
    }
}
