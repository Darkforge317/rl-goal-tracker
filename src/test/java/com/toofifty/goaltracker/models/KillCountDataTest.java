package com.toofifty.goaltracker.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KillCountDataTest
{
    @Test
    void getKillCount_shouldReturnZeroForUnknownNpc()
    {
        KillCountData data = new KillCountData();

        assertEquals(0, data.getKillCount(1234));
    }

    @Test
    void increment_shouldReturnOneOnFirstKill()
    {
        KillCountData data = new KillCountData();

        assertEquals(1, data.increment(1234));
    }

    @Test
    void increment_shouldAccumulateKillsForSameNpc()
    {
        KillCountData data = new KillCountData();

        data.increment(1234);
        data.increment(1234);
        int result = data.increment(1234);

        assertEquals(3, result);
    }

    @Test
    void increment_shouldTrackDifferentNpcsSeparately()
    {
        KillCountData data = new KillCountData();

        data.increment(1111);
        data.increment(1111);
        data.increment(2222);

        assertEquals(2, data.getKillCount(1111));
        assertEquals(1, data.getKillCount(2222));
    }

    @Test
    void getKillCount_shouldReturnCurrentCountAfterIncrements()
    {
        KillCountData data = new KillCountData();

        data.increment(5);
        data.increment(5);

        assertEquals(2, data.getKillCount(5));
    }
}
