package com.darkforge317.goaltracker.ui.components;

import com.darkforge317.goaltracker.models.enums.Status;
import com.darkforge317.goaltracker.models.task.ItemTask;
import net.runelite.api.gameval.InventoryID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListTaskPanelTest
{
    @Test
    void markingItemTaskIncomplete_refreshesProgressWithoutRestoringCompletedStatus()
    {
        ItemTask task = ItemTask.builder()
                .quantity(3)
                .acquired(0)
                .status(Status.COMPLETED)
                .build();
        task.setInventoryQuantity(InventoryID.BANK, 2);
        task.setInventoryQuantity(InventoryID.INV, 1);

        ListTaskPanel.applyStatus(task, Status.NOT_STARTED);

        assertEquals(3, task.getAcquired());
        assertEquals(Status.NOT_STARTED, task.getStatus());

        task.setInventoryQuantity(InventoryID.INV, 0);
        task.recomputeFromCount(task.getTotalInventoryQuantity());

        assertEquals(2, task.getAcquired());
        assertEquals(Status.IN_PROGRESS, task.getStatus());
    }
}
