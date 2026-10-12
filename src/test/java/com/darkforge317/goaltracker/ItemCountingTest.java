package com.darkforge317.goaltracker;

import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ItemCountingTest
{
    private GoalTrackerPlugin plugin;
    private Client client;
    private ItemManager itemManager;

    @BeforeEach
    void setUp()
    {
        client = mock(Client.class);
        itemManager = mock(ItemManager.class);
        plugin = new GoalTrackerPlugin(client, itemManager);
    }

    @Test
    void countsCraftedItemsOnce()
    {
        itemName(100, "Blisterwood stake");
        container(InventoryID.INV, new Item(100, 5));
        assertEquals(5, count(100, "Blisterwood stake"));
        container(InventoryID.INV, new Item(100, 10));
        assertEquals(10, count(100, "Blisterwood stake"));
    }

    @Test
    void openingBankCountsEachContainerOnce()
    {
        itemName(100, "Maple logs");
        container(InventoryID.INV, new Item(100, 3));
        container(InventoryID.BANK, new Item(100, 20));
        assertEquals(23, count(100, "Maple logs"));
        container(InventoryID.BANK, new Item(100, 20));
        assertEquals(23, count(100, "Maple logs"));
    }

    @Test
    void includesNotedAndDegradedVariantsWithoutDuplicatingBase()
    {
        itemName(100, "Torag's platelegs");
        itemName(101, "Torag's platelegs 75");
        itemName(102, "Torag's platelegs");
        container(InventoryID.WORN, new Item(101, 1));
        container(InventoryID.BANK, new Item(100, 2));
        container(InventoryID.INV, new Item(102, 3));
        assertEquals(6, count(100, "Torag's platelegs"));
    }

    @Test
    void retainsExactIdFallbackForMissingOrDifferentNames()
    {
        itemName(100, "Maple logs");
        container(InventoryID.BANK, new Item(100, 7));
        assertEquals(7, count(100, null));
        assertEquals(7, count(100, "Old item name"));
    }

    @Test
    void retainsExactIdFallbackWhenCompositionLookupFails()
    {
        when(itemManager.getItemComposition(100)).thenThrow(new IllegalStateException());
        container(InventoryID.BANK, new Item(100, 7));
        assertEquals(7, count(100, "Maple logs"));
    }

    private void itemName(int id, String name)
    {
        ItemComposition composition = mock(ItemComposition.class);
        when(composition.getName()).thenReturn(name);
        when(itemManager.getItemComposition(id)).thenReturn(composition);
    }

    private void container(int id, Item... items)
    {
        ItemContainer container = mock(ItemContainer.class);
        when(container.getItems()).thenReturn(items);
        when(client.getItemContainer(id)).thenReturn(container);
        plugin.refreshContainerCache(id);
    }

    private int count(int id, String name)
    {
        return plugin.countHeldEquivalent(id, name);
    }
}
