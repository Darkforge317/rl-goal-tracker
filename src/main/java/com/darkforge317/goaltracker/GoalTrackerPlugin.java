package com.darkforge317.goaltracker;


import com.darkforge317.goaltracker.models.enums.TaskType;
import com.darkforge317.goaltracker.models.Goal;
import com.darkforge317.goaltracker.models.task.ItemTask;
import com.darkforge317.goaltracker.models.task.QuestTask;
import com.darkforge317.goaltracker.models.task.SkillLevelTask;
import com.darkforge317.goaltracker.models.task.SkillXpTask;
import com.darkforge317.goaltracker.models.task.Task;
import com.darkforge317.goaltracker.services.ChangelogService;
import com.darkforge317.goaltracker.services.KeyInputService;
import com.darkforge317.goaltracker.services.PanelService;
import com.darkforge317.goaltracker.services.TaskIconService;
import com.darkforge317.goaltracker.services.TaskUpdateService;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
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
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    public GoalTrackerPlugin()
    {
        // RuneLite requires this no-argument constructor; tests use the overload below to inject mocks without reflection.
    }

    GoalTrackerPlugin(Client client, ItemManager itemManager)
    {
        this.client = client;
        this.itemManager = itemManager;
    }

    GoalTrackerPlugin(Client client, ClientThread clientThread, ChatMessageManager chatMessageManager,
                      GoalTrackerConfig config, GoalManager goalManager, TaskUIStatusManager uiStatusManager)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.chatMessageManager = chatMessageManager;
        this.config = config;
        this.goalManager = goalManager;
        this.uiStatusManager = uiStatusManager;
    }

    public static final int[] PLAYER_INVENTORIES = {
            InventoryID.INV,
            InventoryID.WORN,
            InventoryID.BANK,
            InventoryID.SEED_VAULT,
            InventoryID.INV_GROUP_TEMP,
            // Emits change events during transfers to/from Group Ironman shared storage.
            // This temporary player-side container is a transfer snapshot, not extra held items.
            InventoryID.INV_PLAYER_TEMP
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
    private final ChangelogService changelogService = new ChangelogService();

    @Getter final PanelService panelService = new PanelService(this);

    @Getter
    @Inject
    private TaskUpdateService taskUpdateService;

    @Getter
    @Inject
    private TaskIconService taskIconService;

    @Inject
    private KeyManager keyManager;

    @Inject
    private KeyInputService keyInputService;

    public KeyInputService getKeyInputService() { return keyInputService; }

    @Getter
    @Inject
    private TaskUIStatusManager uiStatusManager;

    @Getter
    @Inject
    private GoalManager goalManager;

    private NavigationButton uiNavigationButton;

    @Setter
    private boolean validateAll = true;

    private boolean warmedIcons = false;

    // Debounced UI refresh timer (coalesces many varbit changes into one repaint)
    private Timer uiRefreshTimer;

    private static final List<Integer> TRACKED_INVENTORIES = List.of(
            InventoryID.INV,
            InventoryID.WORN,
            InventoryID.BANK,
            InventoryID.SEED_VAULT,
            InventoryID.INV_GROUP_TEMP
    );

    // Per-container cached counts, keyed by normalized item name and by raw item ID.
    // Rebuilt only for the single container that actually changed (see refreshContainerCache),
    // instead of rescanning all 5 containers on every ItemContainerChanged event.
    private final Map<Integer, Map<String, Integer>> containerNameCounts = new HashMap<>();
    private final Map<Integer, Map<Integer, Integer>> containerIdCounts = new HashMap<>();
    private final Map<Integer, Boolean> itemPlaceholderCache = new HashMap<>();
    private final Set<Integer> initializedContainerCaches = new HashSet<>();
    private final Set<Integer> pendingContainerRefreshes = new HashSet<>();
    private boolean containerRefreshPending;

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
        if (uiRefreshTimer != null && uiRefreshTimer.isRunning())
        {
            uiRefreshTimer.stop();
        }
        uiRefreshTimer = new Timer(delayMs, e -> SwingUtilities.invokeLater(panelService::refreshCurrent));
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
            itemManager.getImage(ItemID.GRIM_WITCH_TODOLIST);

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
        // Listens to keyboard state with net.runelite.client.input.KeyManager,
        // currently used for Shift-Click task removal
        keyManager.registerKeyListener(keyInputService);

        // Defensive guards to avoid NPEs during test bootstrap if DI bindings are missing
        if (goalManager == null || itemCache == null || itemManager == null || clientToolbar == null)
        {
            log.warn("GoalTrackerPlugin: skipping full startup because a dependency was null. goalManager={}, itemCache={}, itemManager={}, toolbar={}",
                    goalManager != null, itemCache != null, itemManager != null, clientToolbar != null);
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
            if (client.getGameState() == GameState.LOGGED_IN)
            {
                for (int inv : TRACKED_INVENTORIES)
                {
                    if (canReadContainerNow(inv))
                    {
                        refreshContainerCache(inv);
                    }
                }
                refreshItemTasks();
            }
        });

        panelService.registerGoalsChangedListener();

        ChangelogService.ChangelogEntry newestEntry = changelogService.getNewestEntry();

        // If the newest changelog hasn't been seen by the player yet
        if (newestEntry != null && !newestEntry.getVersion().equals(config.highestChangelogVersionSeen()))
        {
            // Show them the newest changelog
            panelService.showChangelogPanel(newestEntry, true, false);
        }
        else
        {
            // Otherwise, show them the home panel
            panelService.showHome();
        }

        final AsyncBufferedImage icon = itemManager.getImage(ItemID.GRIM_WITCH_TODOLIST);
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
                        .panel(panelService)
                        .build();

                clientToolbar.addNavigation(uiNavigationButton);
            });
        }

        // Preload item icons at plugin startup so they are visible immediately
        warmItemIcons();
        warmedIcons = true; // avoid re-warming on first login tick
    }

    @Override
    protected void shutDown()
    {
        // Removes the keyboard state listener when the plugin is disabled
        keyManager.unregisterKeyListener(keyInputService);

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
        panelService.refreshCurrent();
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
                for (int inv : TRACKED_INVENTORIES)
                {
                    if (canReadContainerNow(inv))
                    {
                        refreshContainerCache(inv);
                    }
                }
                refreshItemTasks();

                // Give the UI a moment to settle after data updates before repainting the panel
                schedulePanelRefresh(200);

                // Stop the refresh frame loop
                return true;
            });
        }
    }

    private volatile boolean questRefreshPending = false;

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        // Quest progress often updates via varbits/varps. Bank UI (and other interfaces)
        // also fire unrelated varbit changes frequently, so without this guard a burst of
        // rapid, unrelated varbit changes would queue many redundant refreshQuestTasks()
        // calls back-to-back on the client thread instead of just one.
        if (!questRefreshPending)
        {
            questRefreshPending = true;
            clientThread.invokeLater(() -> {
                questRefreshPending = false;
                refreshQuestTasks();
            });
        }

        // Debounce UI refresh during rapid quest varbit updates
        schedulePanelRefresh(750);
    }

    public void onGoalUpdatedCallback(Goal goal)
    {
        goalManager.save();
    }

    public void onTaskAddedCallback(Task task)
    {
        clientThread.invokeLater(() -> {
            taskUpdateService.update(task);
            if (task instanceof ItemTask)
            {
                ItemTask itemTask = (ItemTask) task;
                updateTaskInventoryCounts(itemTask);
                itemTask.recomputeFromCount(itemTask.getTotalInventoryQuantity());
            }
            goalManager.save();
            if (task.getStatus().isCompleted()) {
                notifyTask(task);
            }
            SwingUtilities.invokeLater(() -> uiStatusManager.refresh(task));
        });
    }

    public void onTaskUpdatedCallback(Task task)
    {
        goalManager.save();
    }

    /**
     * Re-evaluates all incomplete item tasks against the current cached container counts.
     * Callers must ensure relevant container caches are already up to date.
     */
    private void refreshItemTasks()
    {
        List<ItemTask> itemTasks = goalManager.getIncompleteTasksByType(TaskType.ITEM);
        boolean cacheChanged = false;
        for (ItemTask task : itemTasks)
        {
            final int itemId = task.getItemId();
            if (itemId <= 0)
            {
                continue;
            }

            cacheChanged |= updateTaskInventoryCounts(task);
            final int count = task.getTotalInventoryQuantity();
            final boolean changed = task.recomputeFromCount(count);
            cacheChanged |= changed;
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

        if (cacheChanged)
        {
            goalManager.save();
        }
    }

    boolean updateTaskInventoryCounts(final ItemTask task)
    {
        boolean changed = false;
        if (!task.hasInventoryQuantityCache())
        {
            int knownCount = 0;
            for (int inventory : TRACKED_INVENTORIES)
            {
                final int count = initializedContainerCaches.contains(inventory)
                        ? countHeldEquivalentInContainer(inventory, task.getItemId(), task.getItemName())
                        : 0;
                changed |= task.setInventoryQuantity(inventory, count);
                knownCount += count;
            }

            // Older saves only stored the aggregate. Keep any quantity we cannot
            // yet attribute to an accessible container as a bank snapshot until
            // that container is next read.
            final int unassignedCount = Math.max(0, task.getAcquired() - knownCount);
            if (unassignedCount > 0)
            {
                int fallbackInventory = InventoryID.BANK;
                if (initializedContainerCaches.contains(fallbackInventory))
                {
                    for (int inventory : TRACKED_INVENTORIES)
                    {
                        if (!initializedContainerCaches.contains(inventory))
                        {
                            fallbackInventory = inventory;
                            break;
                        }
                    }
                }
                changed |= task.setInventoryQuantity(
                        fallbackInventory,
                        task.getInventoryQuantity(fallbackInventory) + unassignedCount);
            }
        }

        for (int inventory : initializedContainerCaches)
        {
            changed |= task.setInventoryQuantity(
                    inventory,
                    countHeldEquivalentInContainer(inventory, task.getItemId(), task.getItemName()));
        }
        return changed;
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        // Only react to the player's inventory/equipment/bank-like containers
        if (!isPlayerInventoryContainer(event.getContainerId()))
        {
            return;
        }

        final Integer changedInventory = inventoryIdFromContainerId(event.getContainerId());
        if (changedInventory == null)
        {
            return;
        }

        pendingContainerRefreshes.addAll(containersToRefreshTogether(changedInventory, isBankOpen()));

        if (!containerRefreshPending)
        {
            containerRefreshPending = true;
            clientThread.invokeLater(() -> {
                Set<Integer> inventoriesToRefresh = new HashSet<>(pendingContainerRefreshes);
                pendingContainerRefreshes.clear();
                containerRefreshPending = false;

                refreshContainerCaches(inventoriesToRefresh);

                refreshItemTasks();
                schedulePanelRefresh(400);
            });
        }

    }

    static Set<Integer> containersToRefreshTogether(int changedInventory, boolean bankOpen)
    {
        Set<Integer> inventories = new HashSet<>();
        if (changedInventory == InventoryID.BANK
                || (changedInventory == InventoryID.INV && bankOpen))
        {
            // Bank transfers update both containers. Refresh both after the client
            // finishes processing this event batch to avoid combining stale counts.
            inventories.add(InventoryID.INV);
            inventories.add(InventoryID.BANK);
        }
        else if (changedInventory == InventoryID.INV)
        {
            // Inventory changes outside the bank (for example GE collections) must
            // not replace the last known bank snapshot with an empty closed-bank cache.
            inventories.add(InventoryID.INV);
        }
        else if (changedInventory == InventoryID.INV_PLAYER_TEMP)
        {
            // This transfer snapshot is only an event source. Refresh the two
            // actual holdings without caching or counting the temporary container.
            inventories.add(InventoryID.INV);
            inventories.add(InventoryID.INV_GROUP_TEMP);
        }
        else if (changedInventory == InventoryID.INV_GROUP_TEMP)
        {
            // Shared-storage transfers can also report the shared container directly.
            inventories.add(InventoryID.INV);
            inventories.add(InventoryID.INV_GROUP_TEMP);
        }
        else
        {
            inventories.add(changedInventory);
        }
        return inventories;
    }

    private boolean isBankOpen()
    {
        final Widget bankWidget = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
        return bankWidget != null && !bankWidget.isHidden();
    }

    boolean canReadContainerNow(final int inventoryId)
    {
        if (inventoryId == InventoryID.BANK)
        {
            return isBankOpen();
        }
        if (inventoryId == InventoryID.INV_GROUP_TEMP)
        {
            return isWidgetVisible(InterfaceID.SharedBank.FRAME);
        }
        if (inventoryId == InventoryID.SEED_VAULT)
        {
            return isWidgetVisible(InterfaceID.SeedVault.UNIVERSE);
        }
        return true;
    }

    private boolean isWidgetVisible(final int widgetId)
    {
        final Widget widget = client.getWidget(widgetId);
        return widget != null && !widget.isHidden();
    }

    void refreshContainerCaches(Set<Integer> inventoriesToRefresh)
    {
        for (int inventory : TRACKED_INVENTORIES)
        {
            if (inventoriesToRefresh.contains(inventory))
            {
                refreshContainerCache(inventory);
            }
        }
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
    void refreshContainerCache(final int inventoryId)
    {
        final Map<String, Integer> nameCounts = new HashMap<>();
        final Map<Integer, Integer> idCounts = new HashMap<>();

        final ItemContainer container = client.getItemContainer(inventoryId);
        if (container == null)
        {
            return;
        }

        for (Item i : container.getItems())
        {
            if (i == null || i.getId() <= 0 || isPlaceholderItem(i.getId()))
            {
                continue;
            }
            final int qty = i.getQuantity();
            if (qty <= 0)
            {
                // Bank placeholders retain the item ID but have no quantity.
                continue;
            }

            idCounts.merge(i.getId(), qty, Integer::sum);

            final String normalized = normalizedNameFor(i.getId());
            if (normalized != null)
            {
                nameCounts.merge(normalized, qty, Integer::sum);
            }
        }

        containerNameCounts.put(inventoryId, nameCounts);
        containerIdCounts.put(inventoryId, idCounts);
        initializedContainerCaches.add(inventoryId);
    }

    private boolean isPlaceholderItem(final int itemId)
    {
        return itemPlaceholderCache.computeIfAbsent(itemId, id -> {
            try
            {
                return itemManager.getItemComposition(id).getPlaceholderTemplateId() != -1;
            }
            catch (Exception ex)
            {
                return false;
            }
        });
    }

    private static Integer inventoryIdFromContainerId(final int containerId)
    {
        for (int inv : PLAYER_INVENTORIES)
        {
            if (inv == containerId)
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
        for (int inv : TRACKED_INVENTORIES)
        {
            final Map<Integer, Integer> idCounts = containerIdCounts.get(inv);
            if (idCounts != null)
            {
                total += idCounts.getOrDefault(itemId, 0);
            }
        }
        return total;
    }

    int countHeldEquivalent(final int targetItemId, final String targetItemName)
    {
        // Fallback to ID-only counting if we don't have a name
        if (targetItemName == null || targetItemName.isEmpty() || itemManager == null)
        {
            return countHeld(targetItemId);
        }

        int total = 0;
        for (int inv : TRACKED_INVENTORIES)
        {
            total += countHeldEquivalentInContainer(inv, targetItemId, targetItemName);
        }
        return total;
    }

    private int countHeldEquivalentInContainer(final int inventoryId,
                                                final int targetItemId,
                                                final String targetItemName)
    {
        if (targetItemName == null || targetItemName.isEmpty() || itemManager == null)
        {
            final Map<Integer, Integer> idCounts = containerIdCounts.get(inventoryId);
            return idCounts == null ? 0 : idCounts.getOrDefault(targetItemId, 0);
        }

        final String baseName = normalizeBarrowsName(targetItemName);
        final Map<String, Integer> nameCounts = containerNameCounts.get(inventoryId);
        int total = nameCounts == null ? 0 : nameCounts.getOrDefault(baseName, 0);
        // Name counts already include the exact ID when its name matches. Only
        // fall back to the ID when that item was not included in the name total.
        if (!baseName.equals(normalizedNameFor(targetItemId)))
        {
            final Map<Integer, Integer> idCounts = containerIdCounts.get(inventoryId);
            if (idCounts != null)
            {
                total += idCounts.getOrDefault(targetItemId, 0);
            }
        }
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

    /** Recheck an edited target, including targets that were previously completed. */
    public void refreshEditedTask(Task task, com.darkforge317.goaltracker.models.Goal goal)
    {
        refreshEditedTask(task, goal, task.getStatus().isCompleted());
    }

    /** Recheck an edited target and notify only when the edit changes it from incomplete to complete. */
    public void refreshEditedTask(Task task, com.darkforge317.goaltracker.models.Goal goal, boolean wasCompleted)
    {
        clientThread.invokeLater(() -> {
            if (client.getGameState() == GameState.LOGGED_IN)
            {
                if (task instanceof SkillLevelTask) ((SkillLevelTask) task).refreshStatus(client);
                else if (task instanceof SkillXpTask) ((SkillXpTask) task).refreshStatus(client);
                else if (task instanceof ItemTask)
                {
                    ItemTask item = (ItemTask) task;
                    final int count = item.hasInventoryQuantityCache()
                            ? item.getTotalInventoryQuantity()
                            : countHeldEquivalent(item.getItemId(), item.getItemName());
                    item.recomputeFromCount(count);
                }
            }
            if (client.getGameState() == GameState.LOGGED_IN && !wasCompleted
                && task.getStatus().isCompleted() && !task.isNotified())
            {
                notifyTask(task);
                task.setNotified(true);
            }
            SwingUtilities.invokeLater(() -> {
                goalManager.save();
                uiStatusManager.refresh(task);
                uiStatusManager.refresh(goal);
            });
        });
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
