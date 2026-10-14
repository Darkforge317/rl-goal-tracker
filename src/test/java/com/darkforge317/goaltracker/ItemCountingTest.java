package com.darkforge317.goaltracker;

import com.darkforge317.goaltracker.models.task.ItemTask;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void withdrawingFromBankRefreshesBothSidesBeforeCounting()
    {
        itemName(100, "Feathers");
        AtomicReference<Item[]> bankContents = new AtomicReference<>(new Item[] {new Item(100, 5400)});
        AtomicReference<Item[]> inventoryContents = new AtomicReference<>(new Item[0]);
        ItemContainer bank = mock(ItemContainer.class);
        ItemContainer inventory = mock(ItemContainer.class);
        when(bank.getItems()).thenAnswer(ignored -> bankContents.get());
        when(inventory.getItems()).thenAnswer(ignored -> inventoryContents.get());
        when(client.getItemContainer(InventoryID.BANK)).thenReturn(bank);
        when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);

        plugin.refreshContainerCaches(Set.of(InventoryID.BANK, InventoryID.INV));
        assertEquals(5400, count(100, "Feathers"));

        // After withdrawing the feathers, RuneLite has moved them from bank to inventory.
        bankContents.set(new Item[0]);
        inventoryContents.set(new Item[] {new Item(100, 5400)});

        plugin.refreshContainerCaches(GoalTrackerPlugin.containersToRefreshTogether(InventoryID.INV, true));
        assertEquals(5400, count(100, "Feathers"));
    }

    @Test
    void transferringFromGroupStorageRefreshesBothSidesWithoutCountingTransferSnapshot()
    {
        itemName(100, "Feathers");
        AtomicReference<Item[]> sharedStorageContents = new AtomicReference<>(new Item[] {new Item(100, 5400)});
        AtomicReference<Item[]> inventoryContents = new AtomicReference<>(new Item[0]);
        ItemContainer sharedStorage = mock(ItemContainer.class);
        ItemContainer inventory = mock(ItemContainer.class);
        when(sharedStorage.getItems()).thenAnswer(ignored -> sharedStorageContents.get());
        when(inventory.getItems()).thenAnswer(ignored -> inventoryContents.get());
        when(client.getItemContainer(InventoryID.INV_GROUP_TEMP)).thenReturn(sharedStorage);
        when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);

        plugin.refreshContainerCaches(Set.of(InventoryID.INV_GROUP_TEMP, InventoryID.INV));
        assertEquals(5400, count(100, "Feathers"));

        // The shared-storage transfer removes the item from one counted container
        // and adds it to the inventory. INV_PLAYER_TEMP may also emit a change event,
        // but that transient snapshot is deliberately not counted as another holding.
        sharedStorageContents.set(new Item[0]);
        inventoryContents.set(new Item[] {new Item(100, 5400)});

        Set<Integer> refreshSet = GoalTrackerPlugin.containersToRefreshTogether(InventoryID.INV_PLAYER_TEMP, false);
        assertEquals(Set.of(InventoryID.INV, InventoryID.INV_GROUP_TEMP), refreshSet);
        plugin.refreshContainerCaches(refreshSet);
        assertEquals(5400, count(100, "Feathers"));
    }

    @Test
    void bankPlaceholderDoesNotCountAsAnItem()
    {
        itemName(100, "Feathers");
        itemName(101, "Feathers");
        ItemComposition placeholderComposition = mock(ItemComposition.class);
        when(placeholderComposition.getName()).thenReturn("Feathers");
        when(placeholderComposition.getPlaceholderTemplateId()).thenReturn(14401);
        when(itemManager.getItemComposition(101)).thenReturn(placeholderComposition);
        AtomicReference<Item[]> bankContents = new AtomicReference<>(new Item[] {new Item(100, 5495)});
        AtomicReference<Item[]> inventoryContents = new AtomicReference<>(new Item[0]);
        ItemContainer bank = mock(ItemContainer.class);
        ItemContainer inventory = mock(ItemContainer.class);
        when(bank.getItems()).thenAnswer(ignored -> bankContents.get());
        when(inventory.getItems()).thenAnswer(ignored -> inventoryContents.get());
        when(client.getItemContainer(InventoryID.BANK)).thenReturn(bank);
        when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);

        plugin.refreshContainerCaches(Set.of(InventoryID.BANK, InventoryID.INV));
        assertEquals(5495, count(100, "Feathers"));

        // RuneLite represents the placeholder as its own item variant, with a positive quantity.
        bankContents.set(new Item[] {new Item(101, 1)});
        inventoryContents.set(new Item[] {new Item(100, 5495)});
        plugin.refreshContainerCaches(GoalTrackerPlugin.containersToRefreshTogether(InventoryID.BANK, false));

        assertEquals(5495, count(100, "Feathers"));
    }

    @Test
    void inventoryChangesWhileBankIsClosedPreserveCachedBankCounts()
    {
        itemName(100, "Feathers");
        container(InventoryID.BANK, new Item(100, 5495));
        AtomicReference<Item[]> inventoryContents = new AtomicReference<>(new Item[] {new Item(100, 1)});
        ItemContainer inventory = mock(ItemContainer.class);
        when(inventory.getItems()).thenAnswer(ignored -> inventoryContents.get());
        when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);
        plugin.refreshContainerCache(InventoryID.INV);

        assertEquals(5496, count(100, "Feathers"));

        // A GE collection/cancellation updates inventory while the bank is closed.
        inventoryContents.set(new Item[] {new Item(100, 2)});
        Set<Integer> refreshSet = GoalTrackerPlugin.containersToRefreshTogether(InventoryID.INV, false);
        assertEquals(Set.of(InventoryID.INV), refreshSet);
        plugin.refreshContainerCaches(refreshSet);

        assertEquals(5497, count(100, "Feathers"));
    }

    @Test
    void itemTaskKeepsPerInventoryCountsWhenOnlyInventoryRefreshes()
    {
        itemName(100, "Feathers");
        container(InventoryID.BANK, new Item(100, 5495));
        container(InventoryID.INV, new Item(100, 1));
        ItemTask task = ItemTask.builder().itemId(100).itemName("Feathers").quantity(6000).acquired(5496).build();

        plugin.updateTaskInventoryCounts(task);
        task.recomputeFromCount(task.getTotalInventoryQuantity());
        assertEquals(5496, task.getAcquired());

        // Simulate a GE event: only the inventory changes; the closed bank is not refreshed.
        container(InventoryID.INV, new Item(100, 2));
        plugin.updateTaskInventoryCounts(task);
        task.recomputeFromCount(task.getTotalInventoryQuantity());

        assertEquals(5495, task.getInventoryQuantity(InventoryID.BANK));
        assertEquals(2, task.getInventoryQuantity(InventoryID.INV));
        assertEquals(5497, task.getAcquired());
    }

    @Test
    void loginRefreshCanPreservePersistedBankQuantityUntilBankIsOpen()
    {
        assertFalse(plugin.canReadContainerNow(InventoryID.BANK));
        assertTrue(plugin.canReadContainerNow(InventoryID.INV));

        itemName(100, "Feathers");
        ItemTask task = ItemTask.builder().itemId(100).itemName("Feathers").quantity(6000).acquired(5496).build();
        task.setInventoryQuantity(InventoryID.BANK, 5495);
        task.setInventoryQuantity(InventoryID.INV, 1);

        container(InventoryID.INV, new Item(100, 1));
        plugin.updateTaskInventoryCounts(task);

        assertEquals(5495, task.getInventoryQuantity(InventoryID.BANK));
        assertEquals(1, task.getInventoryQuantity(InventoryID.INV));
        assertEquals(5496, task.getTotalInventoryQuantity());
    }

    @Test
    void legacyItemTaskPreservesUnattributedQuantityAsBankCache()
    {
        itemName(100, "Feathers");
        container(InventoryID.INV, new Item(100, 1));
        ItemTask legacyTask = ItemTask.builder()
                .itemId(100)
                .itemName("Feathers")
                .quantity(6000)
                .acquired(5496)
                .build();

        plugin.updateTaskInventoryCounts(legacyTask);

        assertEquals(1, legacyTask.getInventoryQuantity(InventoryID.INV));
        assertEquals(5495, legacyTask.getInventoryQuantity(InventoryID.BANK));
        assertEquals(5496, legacyTask.getTotalInventoryQuantity());
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
    void includesChargedItemVariantsAndUnchargedBase()
    {
        itemName(100, "Amulet of glory (6)");
        itemName(101, "Amulet of glory (4)");
        itemName(102, "Amulet of glory");
        container(InventoryID.WORN, new Item(100, 1));
        container(InventoryID.BANK, new Item(101, 1));
        container(InventoryID.INV, new Item(102, 2));

        assertEquals(4, count(100, "Amulet of glory (6)"));
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
        when(composition.getPlaceholderTemplateId()).thenReturn(-1);
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
