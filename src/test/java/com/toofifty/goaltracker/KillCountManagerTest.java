package com.toofifty.goaltracker;

import com.google.gson.Gson;
import com.toofifty.goaltracker.models.KillCountData;
import net.runelite.client.config.ConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KillCountManagerTest
{
    private static final String CONFIG_GROUP = "goaltracker";
    private static final long ACCOUNT_HASH = 123456789L;
    private static final String CONFIG_KEY = "killcounts_" + ACCOUNT_HASH;

    @Mock
    private ConfigManager configManager;

    @InjectMocks
    private KillCountManager manager;

    @BeforeEach
    void setUp() throws Exception
    {
        // Same pattern as GoalSerializerTest — inject real Gson via reflection
        Field gsonField = KillCountManager.class.getDeclaredField("gson");
        gsonField.setAccessible(true);
        gsonField.set(manager, new Gson());
    }

    // -------------------- load --------------------

    @Test
    void load_shouldStartFreshWhenNoDataExists()
    {
        when(configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY)).thenReturn(null);

        manager.load(ACCOUNT_HASH);

        assertTrue(manager.isLoaded());
        assertEquals(0, manager.getData().getKillCount(1));
    }

    @Test
    void load_shouldStartFreshWhenStoredJsonIsBlank()
    {
        when(configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY)).thenReturn("   ");

        manager.load(ACCOUNT_HASH);

        assertTrue(manager.isLoaded());
        assertEquals(0, manager.getData().getKillCount(1));
    }

    @Test
    void load_shouldRestorePersistedKillCounts()
    {
        KillCountData existing = new KillCountData();
        existing.increment(42);
        existing.increment(42);
        String json = new Gson().toJson(existing);
        when(configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY)).thenReturn(json);

        manager.load(ACCOUNT_HASH);

        assertEquals(2, manager.getData().getKillCount(42));
    }

    @Test
    void load_shouldStartFreshOnCorruptJson()
    {
        when(configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY)).thenReturn("{not valid json{{");

        manager.load(ACCOUNT_HASH);

        assertTrue(manager.isLoaded());
        assertEquals(0, manager.getData().getKillCount(1));
    }

    // -------------------- isLoaded --------------------

    @Test
    void isLoaded_shouldReturnFalseBeforeLoad()
    {
        assertFalse(manager.isLoaded());
    }

    @Test
    void isLoaded_shouldReturnFalseAfterUnload()
    {
        when(configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY)).thenReturn(null);
        manager.load(ACCOUNT_HASH);

        manager.unload();

        assertFalse(manager.isLoaded());
    }

    // -------------------- recordKill --------------------

    @Test
    void recordKill_shouldReturnOneOnFirstKill()
    {
        when(configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY)).thenReturn(null);
        manager.load(ACCOUNT_HASH);

        assertEquals(1, manager.recordKill(99));
    }

    @Test
    void recordKill_shouldAccumulateKills()
    {
        when(configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY)).thenReturn(null);
        manager.load(ACCOUNT_HASH);

        manager.recordKill(99);
        manager.recordKill(99);

        assertEquals(3, manager.recordKill(99));
    }

    @Test
    void recordKill_shouldReturnZeroWhenNotLoaded()
    {
        assertEquals(0, manager.recordKill(99));
    }

    @Test
    void recordKill_shouldPersistAfterEachKill()
    {
        when(configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY)).thenReturn(null);
        manager.load(ACCOUNT_HASH);

        manager.recordKill(99);

        verify(configManager, atLeastOnce())
                .setConfiguration(eq(CONFIG_GROUP), eq(CONFIG_KEY), anyString());
    }

    @Test
    void recordKill_shouldTrackDifferentNpcsSeparately()
    {
        when(configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY)).thenReturn(null);
        manager.load(ACCOUNT_HASH);

        manager.recordKill(100);
        manager.recordKill(100);
        manager.recordKill(200);

        assertEquals(2, manager.getData().getKillCount(100));
        assertEquals(1, manager.getData().getKillCount(200));
    }

    // -------------------- unload --------------------

    @Test
    void unload_shouldSaveBeforeClearing()
    {
        when(configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY)).thenReturn(null);
        manager.load(ACCOUNT_HASH);
        manager.recordKill(7);
        clearInvocations(configManager); // reset so we only count the unload save

        manager.unload();

        verify(configManager).setConfiguration(eq(CONFIG_GROUP), eq(CONFIG_KEY), anyString());
    }

    @Test
    void unload_shouldBeIdempotentWhenNotLoaded()
    {
        assertDoesNotThrow(() -> manager.unload());
    }

    // -------------------- round-trip --------------------

    @Test
    void killCounts_shouldSurviveUnloadAndReload()
    {
        final String[] savedJson = {null};
        when(configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY))
                .thenAnswer(inv -> savedJson[0]);
        doAnswer(inv -> { savedJson[0] = inv.getArgument(2); return null; })
                .when(configManager).setConfiguration(eq(CONFIG_GROUP), eq(CONFIG_KEY), anyString());

        manager.load(ACCOUNT_HASH);
        manager.recordKill(55);
        manager.recordKill(55);
        manager.unload();

        manager.load(ACCOUNT_HASH);

        assertEquals(2, manager.getData().getKillCount(55));
    }
}
