package com.darkforge317.goaltracker.models.task;

import com.darkforge317.goaltracker.models.enums.TaskType;
import net.runelite.api.gameval.InventoryID;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemTaskTest {

    @Test
    void toString_shouldSupportItems() {
        ItemTask task = ItemTask.builder().quantity(1).itemName("Feather").build();

        assertEquals("Feather", task.toString());
    }

    @Test
    void toString_shouldSupportMultipleItems() {
        ItemTask task = ItemTask.builder().quantity(100).itemName("Feather").build();

        assertEquals("100 x Feather", task.toString());
    }

    @Test
    void toString_shouldSupportHavingSomeItemsAcquired() {
        ItemTask task = ItemTask.builder().acquired(50).quantity(100).itemName("Feather").build();

        assertEquals("50/100 x Feather", task.toString());
    }

    @Test
    void toString_shouldDisplayBaseNameForChargedItemVariants() {
        ItemTask task = ItemTask.builder().acquired(94).quantity(100).itemName("Super Energy(4)").build();

        assertEquals("94/100 x Super Energy", task.toString());
        assertEquals("Super Energy", task.getDisplayName());
    }

    @Test
    void toString_shouldSupportTheAcquiredItemsExceedingTheQuantity() {
        ItemTask task = ItemTask.builder().acquired(150).quantity(100).itemName("Feather").build();

        assertEquals("100 x Feather", task.toString());
    }

    @Test
    void getType_shouldReturnItem() {
        ItemTask task = ItemTask.builder().build();

        assertEquals(TaskType.ITEM, task.getType());
    }

    @Test
    void inventoryQuantities_shouldBeStoredAndSummedPerContainer() {
        ItemTask task = ItemTask.builder().acquired(5496).build();

        task.setInventoryQuantity(InventoryID.BANK, 5495);
        task.setInventoryQuantity(InventoryID.INV, 1);

        assertEquals(5495, task.getInventoryQuantity(InventoryID.BANK));
        assertEquals(1, task.getInventoryQuantity(InventoryID.INV));
        assertEquals(5496, task.getTotalInventoryQuantity());
    }

    @Test
    void inventoryQuantities_shouldSurviveSerialization() {
        ItemTask task = ItemTask.builder().acquired(5496).build();
        task.setInventoryQuantity(InventoryID.BANK, 5495);
        task.setInventoryQuantity(InventoryID.INV, 1);

        ItemTask restored = RuneLiteAPI.GSON.fromJson(RuneLiteAPI.GSON.toJson(task), ItemTask.class);

        assertEquals(5495, restored.getInventoryQuantity(InventoryID.BANK));
        assertEquals(1, restored.getInventoryQuantity(InventoryID.INV));
        assertEquals(5496, restored.getTotalInventoryQuantity());
    }
}
