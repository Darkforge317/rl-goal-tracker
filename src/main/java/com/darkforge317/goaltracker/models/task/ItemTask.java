package com.darkforge317.goaltracker.models.task;

import com.google.gson.annotations.SerializedName;
import com.darkforge317.goaltracker.models.enums.Status;
import com.darkforge317.goaltracker.models.enums.TaskType;
import com.darkforge317.goaltracker.utils.ItemNameNormalizer;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@SuperBuilder
/**
 * Task representing acquisition of an item.
 * Tracks item ID, name, desired quantity, acquired count, and cached icon.
 */
public final class ItemTask extends Task
{
    private transient BufferedImage cachedIcon;
    @Builder.Default
    @SerializedName("quantity")
    private int quantity = 1;
    @Builder.Default
    @SerializedName("acquired")
    private int acquired = 0;
    @SerializedName("item_id")
    private int itemId;
    @SerializedName("item_name")
    private String itemName;
    @SerializedName("inventory_quantities")
    private Map<Integer, Integer> inventoryQuantities;

    public int getInventoryQuantity(final int inventoryId)
    {
        return inventoryQuantities == null ? 0 : inventoryQuantities.getOrDefault(inventoryId, 0);
    }

    public boolean hasInventoryQuantityCache()
    {
        return inventoryQuantities != null && !inventoryQuantities.isEmpty();
    }

    public boolean setInventoryQuantity(final int inventoryId, final int count)
    {
        if (inventoryQuantities == null)
        {
            inventoryQuantities = new HashMap<>();
        }
        final Integer previous = inventoryQuantities.put(inventoryId, Math.max(0, count));
        return previous == null || previous != Math.max(0, count);
    }

    public int getTotalInventoryQuantity()
    {
        if (inventoryQuantities == null)
        {
            return acquired;
        }
        return inventoryQuantities.values().stream().mapToInt(count -> count == null ? 0 : count).sum();
    }

    /**
     * Refreshes the displayed acquired quantity without changing a manually selected status.
     */
    public void updateAcquiredFromCount(final int count)
    {
        this.acquired = Math.max(0, Math.min(count, Math.max(1, quantity)));
    }

    /**
     * Recompute acquired count and task status from a current item count.
     * @param count how many of this item are currently held (inventory/equipment/bank, as defined by the caller)
     * @return true if either acquired or status changed; false otherwise
     */
    public boolean recomputeFromCount(final int count)
    {
        final int oldAcquired = this.acquired;
        final Status oldStatus = getStatus();

        this.acquired = Math.max(0, Math.min(count, Math.max(1, quantity)));

        if (this.acquired >= this.quantity)
        {
            setStatus(Status.COMPLETED);
        }
        else if (this.acquired > 0)
        {
            setStatus(Status.IN_PROGRESS);
        }
        else
        {
            setStatus(Status.NOT_STARTED);
        }

        return oldAcquired != this.acquired || oldStatus != getStatus();
    }

    @Override
    public String toString()
    {
        final String displayItemName = ItemNameNormalizer.normalizeItemNameVariants(itemName);
        if (quantity == 1) {
            return displayItemName;
        }

        if (acquired > 0 && acquired < quantity) {
            return String.format("%d/%d x %s", acquired, quantity, displayItemName);
        }

        return String.format("%d x %s", quantity, displayItemName);
    }

    @Override
    public String getDisplayName()
    {
        return ItemNameNormalizer.normalizeItemNameVariants(itemName);
    }

    @Override
    public TaskType getType()
    {
        return TaskType.ITEM;
    }
}
