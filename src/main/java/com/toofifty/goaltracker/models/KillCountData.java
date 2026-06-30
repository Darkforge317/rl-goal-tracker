package com.toofifty.goaltracker.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-character NPC kill counts, keyed by NPC ID.
 * Stored in RuneLite config under a key scoped to the account hash,
 * so it persists across sessions and syncs across devices automatically.
 */
@Getter
@Setter
@NoArgsConstructor
public class KillCountData
{
    /** npcId -> cumulative kill count */
    private Map<Integer, Integer> kills = new HashMap<>();

    public int getKillCount(int npcId)
    {
        return kills.getOrDefault(npcId, 0);
    }

    /**
     * Increment the kill count for the given NPC ID.
     * @return the new kill count after incrementing
     */
    public int increment(int npcId)
    {
        int newCount = getKillCount(npcId) + 1;
        kills.put(npcId, newCount);
        return newCount;
    }
}