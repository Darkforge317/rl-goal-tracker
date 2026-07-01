package com.toofifty.goaltracker;

import com.google.gson.Gson;
import com.toofifty.goaltracker.models.KillCountData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages per-character NPC kill count persistence.
 *
 * Data is stored in RuneLite's ConfigManager under a key scoped to the
 * player's account hash. Because RuneLite config is linked to the player's
 * RuneLite account, this syncs automatically across devices.
 *
 * Lifecycle:
 *   load(accountHash)  — call on LOGGED_IN
 *   recordKill(npcId)  — call on onActorDeath
 *   unload()           — call on LOGGING_OUT / HOPPING
 */
@Slf4j
@Singleton
public final class KillCountManager
{
    private static final String CONFIG_GROUP = "goaltracker";
    private static final String KC_KEY_PREFIX = "killcounts_";

    @Inject private ConfigManager configManager;
    @Inject private Gson gson;

    private KillCountData currentData;
    private String currentKey;

    /**
     * Load (or initialise) KC data for the given character.
     * Safe to call multiple times — reloads fresh each time.
     */
    public void load(long accountHash)
    {
        currentKey = KC_KEY_PREFIX + accountHash;
        String json = configManager.getConfiguration(CONFIG_GROUP, currentKey);

        if (json == null || json.isBlank())
        {
            log.debug("No KC data found for accountHash={}, starting fresh.", accountHash);
            currentData = new KillCountData();
        }
        else
        {
            try
            {
                currentData = gson.fromJson(json, KillCountData.class);
                log.debug("Loaded KC data for accountHash={}: {} NPC IDs tracked.",
                        accountHash, currentData.getKills().size());
            }
            catch (Exception e)
            {
                log.warn("Failed to deserialise KC data — resetting. accountHash={}", accountHash, e);
                currentData = new KillCountData();
            }
        }
    }

    /**
     * Persist current data and clear in-memory state.
     * Call on logout or world hop.
     */
    public void unload()
    {
        save();
        currentData = null;
        currentKey = null;
    }

    /**
     * Increment the kill count for a given NPC ID and immediately persist.
     * @return the new kill count, or 0 if not loaded
     */
    public int recordKill(int npcId)
    {
        log.info("GoalTracker: recordKill({}) starting...", npcId);

        if (currentData == null)
        {
            log.warn("recordKill called before load() — ignoring npcId={}", npcId);
            return 0;
        }
        int newCount = currentData.increment(npcId);
        save();
        log.debug("save() returned. Kill recorded npcId={} newCount={}", npcId, newCount);
        return newCount;
    }

    /** @return current KC data, or null if not loaded */
    public KillCountData getData()
    {
        log.debug("GoalTracker: KC Manager's getData() was called. currentData={}", currentData);
        return currentData;
    }

    public boolean isLoaded()
    {
        log.info("GoalTracker: KC Manager's isLoaded() was called. currentData exists={}", currentData != null);
        return currentData != null;
    }

    private void save()
    {
        log.info("GoalTracker: Starting KC Save...");
        if (currentData == null || currentKey == null)
        {
            log.info("GoalTracker: save() had no currentData or currentKey. currentData={} and currentKey={}", currentData, currentKey);
            return;
        }
        log.info("GoalTracker: Running setConfiguration...");
        log.info("GoalTracker: CONFIG_GROUP={}, currentKey={}, currentDataJson={}",CONFIG_GROUP,currentKey, gson.toJson(currentData));
        configManager.setConfiguration(CONFIG_GROUP, currentKey, gson.toJson(currentData));
        log.info("GoalTracker: setConfiguration finished. Saved configuration is: {}", configManager.getConfiguration(CONFIG_GROUP, currentKey));
    }
}