package com.darkforge317.goaltracker.ui.inputs;

import com.darkforge317.goaltracker.GoalTrackerPlugin;
import com.darkforge317.goaltracker.models.Goal;
import com.darkforge317.goaltracker.models.task.ItemTask;
import com.darkforge317.goaltracker.ui.components.TextButton;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.ParseException;
import java.util.Locale;

/** Item selection and quantity preview, submitted explicitly with Add. */
public final class ItemTaskInput extends TaskInput
{
    private final JSpinner quantityField = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
    private final TextButton searchItemButton = new TextButton("Search...");
    private final JLabel selectedItemLabel = new JLabel();
    private final JPanel selectedItemPanel = new JPanel(new BorderLayout());
    private boolean searchOpen;
    private ItemComposition selectedItem;

    public ItemTaskInput(GoalTrackerPlugin plugin, Goal goal)
    {
        super(plugin, goal, "Item");
        searchItemButton.onClick(e -> {
            if (searchOpen) {
                plugin.getChatboxPanelManager().close();
                searchClosed();
                return;
            }
            if (plugin.getClient().getGameState() != GameState.LOGGED_IN) {
                JOptionPane.showMessageDialog(this, "You must be logged in to choose items",
                    "Item search", JOptionPane.ERROR_MESSAGE);
                return;
            }
            searchOpen = true;
            searchItemButton.setText("Close");
            plugin.getItemSearch()
                .tooltipText("Choose an item")
                .onItemSelected(this::setSelectedItem)
                .onClose(() -> SwingUtilities.invokeLater(this::searchClosed))
                .build();
            // Chatbox input receives keys from the game canvas, not the sidebar.
            plugin.getClient().getCanvas().requestFocusInWindow();
        });
        getInputRow().add(searchItemButton, BorderLayout.WEST);

        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(quantityField, "0");
        quantityField.setEditor(editor);
        editor.getTextField().setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(
            new JFormattedTextField.AbstractFormatter() {
                @Override
                public Object stringToValue(String text) throws ParseException
                {
                    return parseQuantity(text);
                }

                @Override
                public String valueToString(Object value)
                {
                    return value.toString();
                }
            }));
        editor.getTextField().setHorizontalAlignment(SwingConstants.RIGHT);
        quantityField.setToolTipText("Quantity (supports k and m)");
        quantityField.setPreferredSize(new Dimension(80, 24));
        getInputRow().add(quantityField, BorderLayout.CENTER);

        selectedItemPanel.setBorder(new EmptyBorder(0, 8, 0, 8));
        selectedItemPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        selectedItemPanel.add(selectedItemLabel, BorderLayout.CENTER);
        selectedItemPanel.add(new TextButton("X")
            .setMainColor(ColorScheme.PROGRESS_ERROR_COLOR)
            .onClick(e -> clearSelectedItem()), BorderLayout.EAST);
        // Keep long item names from crowding the quantity and Add controls.
        selectedItemPanel.setPreferredSize(new Dimension(110, 24));
    }

    static int parseQuantity(String text) throws ParseException
    {
        String value = text.trim().toLowerCase(Locale.ROOT);
        if (!value.matches("[0-9]+[km]?")) {
            throw new ParseException("Enter a positive quantity", 0);
        }
        long multiplier = value.endsWith("k") ? 1000 : value.endsWith("m") ? 1000000 : 1;
        if (multiplier != 1) {
            value = value.substring(0, value.length() - 1);
        }
        try {
            long number = Long.parseLong(value);
            if (number < 1 || number > Integer.MAX_VALUE / multiplier) {
                throw new NumberFormatException();
            }
            return (int) (number * multiplier);
        } catch (NumberFormatException e) {
            throw new ParseException("Quantity must be between 1 and " + Integer.MAX_VALUE, 0);
        }
    }

    private void searchClosed()
    {
        searchOpen = false;
        searchItemButton.setText("Search...");
    }

    private void setSelectedItem(Integer rawId)
    {
        plugin.getClientThread().invokeLater(() -> {
            ItemComposition item = plugin.getItemManager().getItemComposition(
                plugin.getItemManager().canonicalize(rawId));
            SwingUtilities.invokeLater(() -> {
                selectedItem = item;
                selectedItemLabel.setText(item.getName());
                selectedItemLabel.setToolTipText(item.getName());
                searchClosed();
                getInputRow().remove(searchItemButton);
                getInputRow().add(selectedItemPanel, BorderLayout.WEST);
                revalidate();
                repaint();
                ((JSpinner.DefaultEditor) quantityField.getEditor()).getTextField().requestFocusInWindow();
            });
        });
    }

    @Override
    protected void submit()
    {
        if (selectedItem == null) {
            return;
        }
        try {
            quantityField.commitEdit();
        } catch (ParseException e) {
            ((JSpinner.DefaultEditor) quantityField.getEditor()).getTextField().requestFocusInWindow();
            return;
        }
        addTask(ItemTask.builder()
            .itemId(selectedItem.getId())
            .itemName(selectedItem.getName())
            .quantity((Integer) quantityField.getValue())
            .build());
    }

    @Override
    protected void reset()
    {
        clearSelectedItem();
        quantityField.setValue(1);
    }

    private void clearSelectedItem()
    {
        selectedItem = null;
        getInputRow().remove(selectedItemPanel);
        getInputRow().add(searchItemButton, BorderLayout.WEST);
        searchClosed();
        revalidate();
        repaint();
    }
}
