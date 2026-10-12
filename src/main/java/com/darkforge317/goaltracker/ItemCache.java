package com.darkforge317.goaltracker;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Map;

@Singleton
/**
 * Tracks player inventory contents across sessions.
 * Persists item totals and note-link mappings for offline use.
 */
public final class ItemCache
{
    private Map<Integer, Item[]> inventories;
    private Map<Integer, Integer> itemTotals;

    // mapping of itemIds to their noted counterparts, and vice versa
    // needs to be persisted to show correct quantities when not logged in
    private Map<Integer, Integer> itemNoteMap;

    @Inject
    private GoalTrackerConfig config;

    @Inject
    private Gson gson;

    @Inject
    private Client client;

    @Inject
    private ItemManager itemManager;

    public void save()
    {
        config.goalTrackerItemCache(gson.toJson(inventories));
        config.goalTrackerItemNoteMapCache(gson.toJson(itemNoteMap));
    }

    public void load()
    {
        inventories = readInventories(config.goalTrackerItemCache());
        itemNoteMap = readIntegerMap(config.goalTrackerItemNoteMapCache());

        calculateItemTotals();
    }

    private Map<Integer, Item[]> readInventories(String serialized)
    {
        Map<Integer, Item[]> result = new HashMap<>();
        JsonElement root = gson.fromJson(serialized, JsonElement.class);
        if (root == null || !root.isJsonObject()) return result;

        for (Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet())
        {
            result.put(Integer.parseInt(entry.getKey()), gson.fromJson(entry.getValue(), Item[].class));
        }
        return result;
    }

    private Map<Integer, Integer> readIntegerMap(String serialized)
    {
        Map<Integer, Integer> result = new HashMap<>();
        JsonElement root = gson.fromJson(serialized, JsonElement.class);
        if (root == null || !root.isJsonObject()) return result;

        JsonObject object = root.getAsJsonObject();
        for (Entry<String, JsonElement> entry : object.entrySet())
        {
            result.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsInt());
        }
        return result;
    }

    public void calculateItemTotals()
    {
        itemTotals = new HashMap<>();
        for (int inventory : inventories.keySet()) {
            for (Item item : inventories.get(inventory)) {
                itemTotals.put(item.getId(), itemTotals.getOrDefault(item.getId(), 0) + item.getQuantity());
            }
        }
    }

    public void update(int inventoryID, Item[] contents)
    {
        inventories.put(inventoryID, contents);

        calculateItemTotals();
    }

    public int getTotalQuantity(int itemId)
    {
        int notedItemId = getNotedId(itemId);

        if (notedItemId != -1) {
            return itemTotals.getOrDefault(itemId, 0) + itemTotals.getOrDefault(notedItemId, 0);
        }

        return itemTotals.getOrDefault(itemId, 0);
    }

    private int getNotedId(int itemId)
    {
        if (client.isClientThread() && !itemNoteMap.containsKey(itemId)) {
            itemNoteMap.put(itemId, itemManager.getItemComposition(itemId).getLinkedNoteId());
        }
        return itemNoteMap.getOrDefault(itemId, -1);
    }
}
