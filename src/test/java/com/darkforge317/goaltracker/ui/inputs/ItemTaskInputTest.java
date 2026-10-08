package com.darkforge317.goaltracker.ui.inputs;

import com.darkforge317.goaltracker.GoalTrackerPlugin;
import com.darkforge317.goaltracker.models.Goal;
import com.darkforge317.goaltracker.models.task.ItemTask;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.Component;
import java.lang.reflect.Method;
import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemTaskInputTest
{
    @Test
    void validatesQuantityAndSuffixes() throws Exception
    {
        assertEquals(25000, ItemTaskInput.parseQuantity("25K"));
        assertEquals(2000000, ItemTaskInput.parseQuantity(" 2m "));
        assertEquals(Integer.MAX_VALUE, ItemTaskInput.parseQuantity("2147483647"));
        for (String value : new String[]{"", "0", "-1", "1.5", "abc", "2147483648", "2148m", "9223372036854775807k"}) {
            assertThrows(ParseException.class, () -> ItemTaskInput.parseQuantity(value));
        }
    }

    @Test
    void selectionWaitsForAddAndUsesEditedQuantity() throws Exception
    {
        GoalTrackerPlugin plugin = mock(GoalTrackerPlugin.class);
        ItemManager manager = mock(ItemManager.class);
        ClientThread thread = mock(ClientThread.class);
        ItemComposition item = mock(ItemComposition.class);
        when(plugin.getItemManager()).thenReturn(manager);
        when(plugin.getClientThread()).thenReturn(thread);
        when(manager.canonicalize(101)).thenReturn(100);
        when(manager.getItemComposition(100)).thenReturn(item);
        when(item.getId()).thenReturn(100);
        when(item.getName()).thenReturn("Test item");
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(thread).invokeLater(any(Runnable.class));
        Goal goal = Goal.builder().build();
        ItemTaskInput[] input = new ItemTaskInput[1];
        SwingUtilities.invokeAndWait(() -> input[0] = new ItemTaskInput(plugin, goal));
        Method select = ItemTaskInput.class.getDeclaredMethod("setSelectedItem", Integer.class);
        select.setAccessible(true);
        select.invoke(input[0], 101);
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(goal.getTasks().isEmpty());
            JSpinner quantity = null;
            for (Component component : input[0].getInputRow().getComponents()) {
                if (component instanceof JSpinner) quantity = (JSpinner) component;
            }
            assertNotNull(quantity);
            assertEquals(2, quantity.getNextValue());
            assertNull(quantity.getPreviousValue());
            ((JSpinner.DefaultEditor) quantity.getEditor()).getTextField().setText("3k");
            input[0].submit();
            assertEquals(1, goal.getTasks().size());
            ItemTask task = (ItemTask) goal.getTasks().get(0);
            assertEquals(100, task.getItemId());
            assertEquals(3000, task.getQuantity());
            assertEquals(1, quantity.getValue());
            input[0].submit();
            assertEquals(1, goal.getTasks().size());
        });
    }
}
