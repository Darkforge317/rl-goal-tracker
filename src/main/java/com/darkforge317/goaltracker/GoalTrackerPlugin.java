package com.darkforge317.goaltracker;


import com.google.inject.Provides;
import com.darkforge317.goaltracker.models.enums.TaskType;
import com.darkforge317.goaltracker.models.task.ItemTask;
import com.darkforge317.goaltracker.models.task.QuestTask;
import com.darkforge317.goaltracker.models.task.SkillLevelTask;
import com.darkforge317.goaltracker.models.task.SkillXpTask;
import com.darkforge317.goaltracker.models.task.Task;
import com.darkforge317.goaltracker.services.TaskIconService;
import com.darkforge317.goaltracker.services.TaskUpdateService;
import com.darkforge317.goaltracker.ui.GoalTrackerPanel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.SessionOpen;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.chatbox.ChatboxItemSearch;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(name = "Goal Tracker", description = "Keep track of your goals and complete them automatically")
/**
 * Main entry point for the Goal Tracker plugin.
 * Handles lifecycle (startup/shutdown), UI registration, and listens for
 * RuneLite events to update tasks and goals automatically.
 */
public final class GoalTrackerPlugin extends Plugin
{
    public static final int[] PLAYER_INVENTORIES = {
            InventoryID.INVENTORY.getId(),
            InventoryID.EQUIPMENT.getId(),
            InventoryID.BANK.getId(),
            InventoryID.SEED_VAULT.getId(),
            InventoryID.GROUP_STORAGE.getId()
    };

    @Getter
    @Inject
    private Client client;

    @Getter
    @Inject
    private SkillIconManager skillIconManager;

    @Getter
    @Inject
    private ItemManager itemManager;

    @Getter
    @Inject
    private ChatboxItemSearch itemSearch;

    @Getter
    @Inject
    private ChatboxPanelManager chatboxPanelManager;

    @Inject
    private ClientToolbar clientToolbar;

    @Getter
    @Inject
    private ClientThread clientThread;

    @Getter
    @Inject
    private ItemCache itemCache;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Getter
    @Inject
    private GoalTrackerConfig config;

    @Getter
    @Inject
    private TaskUpdateService taskUpdateService;

    @Getter
    @Inject
    private TaskIconService taskIconService;

    @Getter
    @Inject
    private TaskUIStatusManager uiStatusManager;

    @Getter
    @Inject
    private GoalManager goalManager;

    @Inject
    private GoalTrackerPanel goalTrackerPanel;

    private NavigationButton uiNavigationButton;

    @Setter
    private boolean validateAll = true;

    private boolean warmedIcons = false;

    // Debounced UI refresh timer (coalesces many varbit changes into one repaint)
    private Timer uiRefreshTimer;

    private static final List<InventoryID> TRACKED_INVENTORIES = List.of(
            InventoryID.INVENTORY,
            InventoryID.EQUIPMENT,
            InventoryID.BANK,
            InventoryID.SEED_VAULT,
            InventoryID.GROUP_STORAGE
    );

    // Per-container cached counts, keyed by normalized item name and by raw item ID.
    // Rebuilt only for the single container that actually changed (see refreshContainerCache),
    // instead of rescanning all 5 containers on every ItemContainerChanged event.
    private final Map<InventoryID, Map<String, Integer>> containerNameCounts = new EnumMap<>(InventoryID.class);
    private final Map<InventoryID, Map<Integer, Integer>> containerIdCounts = new EnumMap<>(InventoryID.class);

    // Cached normalized display name per item ID, since ItemManager composition lookups
    // are relatively expensive and the same IDs repeat across containers/tasks/events.
    private final Map<Integer, String> itemNameCache = new HashMap<>();

    private static final Pattern TRAILING_NUMBER_PATTERN = Pattern.compile("\\s[0-9]{1,3}$");
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");

    private void notifyTask(Task task)
    {
        if (task == null) { return; }

        try {
            // Use existing config color for chat prefix
            final Color chosen = config.completionMessageColor();
            final String prefix = ColorUtil.wrapWithColorTag("Goal Tracker", chosen);

            final String msg = prefix + ": Completed task — " + task;

            chatMessageManager.queue(
                QueuedMessage.builder()
                    .type(ChatMessageType.GAMEMESSAGE)
                    .runeLiteFormattedMessage(msg)
                    .build()
            );
        }
        catch (Exception ex) {
            log.warn("notifyTask failed", ex);
        }
    }

    /**
     * Schedule a debounced refresh of the sidebar panel. If a refresh is already
     * scheduled, it will be replaced with the new delay. This prevents spammy
     * repainting during rapid varbit changes.
     */
    private void schedulePanelRefresh(final int delayMs)
    {
        if (goalTrackerPanel == null)
        {
            return;
        }
        if (uiRefreshTimer != null && uiRefreshTimer.isRunning())
        {
            uiRefreshTimer.stop();
        }
        uiRefreshTimer = new Timer(delayMs, e -> SwingUtilities.invokeLater(goalTrackerPanel::refresh));
        uiRefreshTimer.setRepeats(false);
        uiRefreshTimer.start();
    }

    /**
     * Preloads commonly used item icons so they render instantly in the UI.
     */
    public void warmItemIcons()
    {
        try
        {
            // Example: warm up the TODO_LIST icon at minimum
            itemManager.getImage(ItemID.TODO_LIST);

            // Warm up skill icons
            for (Skill skill : Skill.values())
            {
                skillIconManager.getSkillImage(skill);
            }
        }
        catch (Exception ex)
        {
            log.warn("warmItemIcons failed", ex);
        }
    }

    @Override
    protected void startUp()
    {
        // Defensive guards to avoid NPEs during test bootstrap if DI bindings are missing
        if (goalManager == null || itemCache == null || goalTrackerPanel == null || itemManager == null || clientToolbar == null)
        {
            log.warn("GoalTrackerPlugin: skipping full startup because a dependency was null. goalManager={}, itemCache={}, panel={}, itemManager={}, toolbar={}",
                    goalManager != null, itemCache != null, goalTrackerPanel != null, itemManager != null, clientToolbar != null);
            return;
        }

        try {
            goalManager.load();
            itemCache.load();
        } catch (Exception ex) {
            log.error("GoalTrackerPlugin: failed to load persisted state", ex);
        }

        // Populate initial container caches so item-task counts are accurate
        // immediately, before any ItemContainerChanged event has fired.
        // client.getItemContainer() must be called on the client thread.
        clientThread.invokeLater(() -> {
            for (InventoryID inv : TRACKED_INVENTORIES)
            {
                refreshContainerCache(inv);
            }
        });

        goalTrackerPanel.home();

        final AsyncBufferedImage icon = itemManager.getImage(ItemID.TODO_LIST);
        if (icon == null)
        {
            log.warn("GoalTrackerPlugin: icon was null; skipping sidebar button creation");
        }
        else
        {
            icon.onLoaded(() -> {
                uiNavigationButton = NavigationButton.builder()
                        .tooltip("Goal Tracker")
                        .icon(icon)
                        .priority(7)
                        .panel(goalTrackerPanel)
                        .build();

                clientToolbar.addNavigation(uiNavigationButton);
            });
        }

        goalTrackerPanel.onGoalUpdated((goal) -> goalManager.save());

        goalTrackerPanel.onTaskAdded((task) -> {
            // Send directly to the client thread to fetch live player stats
            clientThread.invokeLater(() -> {
                // Populate the live metrics into memory instantly upon creation
                taskUpdateService.update(task);

                // Perform the disk write safely on the background game thread
                goalManager.save();

                // If the task is instantly completed, notify the player
                if (task.getStatus().isCompleted()) {
                    notifyTask(task);
                }

                // Send to the UI thread to handle screen graphics
                SwingUtilities.invokeLater(() -> {
                    uiStatusManager.refresh(task);
                });
            });
        });

        goalTrackerPanel.onTaskUpdated((task) -> goalManager.save());

        // Preload item icons at plugin startup so they are visible immediately
        warmItemIcons();
        warmedIcons = true; // avoid re-warming on first login tick
    }

    @Override
    protected void shutDown()
    {
        if (uiNavigationButton != null)
        {
            clientToolbar.removeNavigation(uiNavigationButton);
            uiNavigationButton = null;
        }
    }

    @Subscribe
    public void onSessionOpen(SessionOpen event)
    {
        if (goalManager != null) {
            try { goalManager.load(); } catch (Exception ex) { log.error("Failed to load goals on session open", ex); }
        }
        if (goalTrackerPanel != null) {
            goalTrackerPanel.refresh();
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        boolean anyTaskChanged = false;

        // 1. Process Skill Level task updates
        List<SkillLevelTask> skillLevelTasks = goalManager.getIncompleteTasksByType(TaskType.SKILL_LEVEL);
        for (SkillLevelTask task : skillLevelTasks) {
            // If this skill level task did not receive a status change
            if (!taskUpdateService.update(task, event)) continue;
            // If the skill level task DID receive a status change
            else {
                anyTaskChanged = true;

                // Update the UI immediately to reflect the new status
                uiStatusManager.refresh(task);

                // If we completed the task, notify the player
                if (task.getStatus().isCompleted()) {
                    notifyTask(task);
                }
            }
        }

        // 2. Process Skill XP task updates
        List<SkillXpTask> skillXpTasks = goalManager.getIncompleteTasksByType(TaskType.SKILL_XP);
        for (SkillXpTask task : skillXpTasks) {
            // If this skill XP task did not receive a status change
            if (!taskUpdateService.update(task, event)) continue;
            // If the skill XP task DID receive a status change
            else {
                anyTaskChanged = true;

                // Update the UI immediately to reflect the new status
                uiStatusManager.refresh(task);

                // If we completed the task, notify the player
                if (task.getStatus().isCompleted()) {
                    notifyTask(task);
                }
            }
        }

        // Save once if any status changes occurred
        if (anyTaskChanged) {
            goalManager.save();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            // Defer task refreshes until the player's data is loaded in, preventing a race condition.
            // Would otherwise cause a check of level-0/0xp against tasks, invalidating our login check entirely.
            clientThread.invokeLater(() -> {

                // If player data hasn't loaded from the server yet, wait and try again next frame
                if (client.getRealSkillLevel(Skill.ATTACK) <= 0) { return false; }

                // Refresh tasks now that player data exists
                refreshQuestTasks();
                refreshSkillLevelTasks();
                refreshSkillXpTasks();

                // Container contents (bank/inventory/equipment/etc.) can differ from what was
                // cached at plugin startup, e.g. on character switch - rebuild all caches, then
                // re-evaluate item tasks against the fresh counts.
                for (InventoryID inv : TRACKED_INVENTORIES)
                {
                    refreshContainerCache(inv);
                }
                refreshItemTasks();

                // Give the UI a moment to settle after data updates before repainting the panel
                schedulePanelRefresh(200);

                // Stop the refresh frame loop
                return true;
            });
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        // Quest progress often updates via varbits/varps
        clientThread.invokeLater(() -> refreshQuestTasks());

        // Debounce UI refresh during rapid quest varbit updates
        schedulePanelRefresh(750);
    }

    /**
     * Re-evaluates all incomplete item tasks against the current cached container counts.
     * Callers must ensure relevant container caches are already up to date.
     */
    private void refreshItemTasks()
    {
        List<ItemTask> itemTasks = goalManager.getIncompleteTasksByType(TaskType.ITEM);
        for (ItemTask task : itemTasks)
        {
            final int itemId = task.getItemId();
            if (itemId <= 0)
            {
                continue;
            }

            final int count = countHeldEquivalent(itemId, task.getItemName());
            final boolean changed = task.recomputeFromCount(count);
            if (!changed)
            {
                continue;
            }

            uiStatusManager.refresh(task);
            if (task.getStatus().isCompleted())
            {
                notifyTask(task);
            }
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        // Only react to the player's inventory/equipment/bank-like containers
        if (!isPlayerInventoryContainer(event.getContainerId()))
        {
            return;
        }

        // Rebuild the cache for only the container that changed, not all 5.
        final InventoryID changedInventory = inventoryIdFromContainerId(event.getContainerId());
        if (changedInventory != null)
        {
            refreshContainerCache(changedInventory);
        }

        refreshItemTasks();

        // Debounce panel refresh; coalesce multiple rapid inventory changes
        schedulePanelRefresh(400);
    }

    private static boolean isPlayerInventoryContainer(int containerId)
    {
        for (int id : PLAYER_INVENTORIES)
        {
            if (id == containerId)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Rebuilds cached item counts (by ID and by normalized name) for a single container.
     * Called only when that specific container reports a change, instead of rescanning
     * all 5 tracked containers on every ItemContainerChanged event.
     */
    private void refreshContainerCache(final InventoryID inventoryId)
    {
        final Map<String, Integer> nameCounts = new HashMap<>();
        final Map<Integer, Integer> idCounts = new HashMap<>();

        final ItemContainer container = client.getItemContainer(inventoryId);
        if (container != null)
        {
            for (Item i : container.getItems())
            {
                if (i == null || i.getId() <= 0)
                {
                    continue;
                }
                final int qty = Math.max(1, i.getQuantity());

                idCounts.merge(i.getId(), qty, Integer::sum);

                final String normalized = normalizedNameFor(i.getId());
                if (normalized != null)
                {
                    nameCounts.merge(normalized, qty, Integer::sum);
                }
            }
        }

        containerNameCounts.put(inventoryId, nameCounts);
        containerIdCounts.put(inventoryId, idCounts);
    }

    private static InventoryID inventoryIdFromContainerId(final int containerId)
    {
        for (InventoryID inv : TRACKED_INVENTORIES)
        {
            if (inv.getId() == containerId)
            {
                return inv;
            }
        }
        return null;
    }

    /**
     * Cached normalized-name lookup for an item ID. Composition lookup + regex
     * normalization only happens once per item ID rather than once per item slot
     * per task per event.
     */
    private String normalizedNameFor(final int itemId)
    {
        return itemNameCache.computeIfAbsent(itemId, id -> {
            try
            {
                return normalizeBarrowsName(itemManager.getItemComposition(id).getName());
            }
            catch (Exception ex)
            {
                return null;
            }
        });
    }

    private int countHeld(final int itemId)
    {
        int total = 0;
        for (InventoryID inv : TRACKED_INVENTORIES)
        {
            final Map<Integer, Integer> idCounts = containerIdCounts.get(inv);
            if (idCounts != null)
            {
                total += idCounts.getOrDefault(itemId, 0);
            }
        }
        return total;
    }

    private int countHeldEquivalent(final int targetItemId, final String targetItemName)
    {
        // Fallback to ID-only counting if we don't have a name
        if (targetItemName == null || targetItemName.isEmpty() || itemManager == null)
        {
            return countHeld(targetItemId);
        }

        final String baseName = normalizeBarrowsName(targetItemName);
        int total = 0;

        for (InventoryID inv : TRACKED_INVENTORIES)
        {
            final Map<String, Integer> nameCounts = containerNameCounts.get(inv);
            if (nameCounts != null)
            {
                total += nameCounts.getOrDefault(baseName, 0);
            }
        }

        // Include the exact base ID too, in case some pieces don't use numeric suffixes
        total += countHeld(targetItemId);
        return total;
    }

    /**
     * Normalize Barrows item names so that degraded variants (e.g., "Torag's platelegs 100/75/50/25/0")
     * all map to the same base string (e.g., "Torag's platelegs").
     */
    private static String normalizeBarrowsName(final String raw)
    {
        if (raw == null)
        {
            return "";
        }
        // Strip trailing space+digits (e.g., " 100") and collapse double spaces
        String s = TRAILING_NUMBER_PATTERN.matcher(raw).replaceAll("").trim();
        s = MULTI_SPACE_PATTERN.matcher(s).replaceAll(" ");
        return s;
    }

    /**
     * Safe, multithreaded entry point to force a full data validation sweep across all task types.
     * Typically used after batch mutations like adding the quest prerequisites.
     */
    public void refreshAllTasks(Runnable onCompleteUIHandler)
    {
        // Force the execution to run safely on the OSRS client thread
        clientThread.invokeLater(() -> {
            refreshSkillLevelTasks();
            refreshQuestTasks();
            refreshSkillXpTasks();

            // If the caller provided a UI update script, bounce it back to the Swing thread
            if (onCompleteUIHandler != null)
            {
                javax.swing.SwingUtilities.invokeLater(onCompleteUIHandler);
            }
        });
    }

    private void refreshQuestTasks()
    {
        if (goalManager == null || client == null) return;
        List<QuestTask> questTasks = goalManager.getIncompleteTasksByType(TaskType.QUEST);
        for (QuestTask task : questTasks)
        {
            task.refreshStatus(client);
            uiStatusManager.refresh(task);
            if (task.getStatus().isCompleted())
            {
                notifyTask(task);
            }
        }
    }

    private void refreshSkillLevelTasks()
    {
        if (goalManager == null || client == null) return;
        List<SkillLevelTask> skillLevelTasks = goalManager.getIncompleteTasksByType(TaskType.SKILL_LEVEL);
        for (SkillLevelTask task : skillLevelTasks)
        {
            task.refreshStatus(client);
            uiStatusManager.refresh(task);
            if (task.getStatus().isCompleted())
            {
                notifyTask(task);
            }
        }
    }

    private void refreshSkillXpTasks()
    {
        if (goalManager == null || client == null) return;
        List<SkillXpTask> skillXpTasks = goalManager.getIncompleteTasksByType(TaskType.SKILL_XP);
        for (SkillXpTask task : skillXpTasks)
        {
            task.refreshStatus(client);
            uiStatusManager.refresh(task);
            if (task.getStatus().isCompleted())
            {
                notifyTask(task);
            }
        }
    }

    @Provides
    public GoalTrackerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(GoalTrackerConfig.class);
    }
}