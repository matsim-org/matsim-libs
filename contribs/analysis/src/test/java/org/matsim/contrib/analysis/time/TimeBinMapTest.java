package org.matsim.contrib.analysis.time;

import org.junit.Test;

import static org.junit.Assert.*;

public class TimeBinMapTest {

    @Test
    public void getTimeBin() {

        TimeBinMap<String, String> map = new TimeBinMap<>(10);

        TimeBinMap.TimeBin<String, String> bin = map.getTimeBin(15);

        assertEquals(10, bin.getStartTime(), 0.001);
    }

    @Test
    public void getTimeBinWithStartTime() {

        TimeBinMap<String, String> map = new TimeBinMap<>(10, 20);

        TimeBinMap.TimeBin<String, String> bin = map.getTimeBin(25);

        assertEquals(20, bin.getStartTime(), 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getTimeInvalidTime_exception() {

        TimeBinMap<String, String> map = new TimeBinMap<>(10, 20);

        TimeBinMap.TimeBin<String, String> bin = map.getTimeBin(19);

        assertEquals(20, bin.getStartTime(), 0.001);
    }

    @Test
    public void getMultipleTimeBins() {

        TimeBinMap<String, String> map = new TimeBinMap<>(10);

        TimeBinMap.TimeBin<String, String> first = map.getTimeBin(15);
        TimeBinMap.TimeBin<String, String> second = map.getTimeBin(111);
        TimeBinMap.TimeBin<String, String> third = map.getTimeBin(0);
        TimeBinMap.TimeBin<String, String> fourth = map.getTimeBin(30);


        assertEquals(10, first.getStartTime(), 0.001);
        assertEquals(110, second.getStartTime(), 0.001);
        assertEquals(0, third.getStartTime(), 0.001);
        assertEquals(30, fourth.getStartTime(), 0.001);
    }

    @Test
    public void getEndOfLastTimeBucket() {

        TimeBinMap<String, String> map = new TimeBinMap<>(10);

        map.getTimeBin(15);
        map.getTimeBin(111);
        map.getTimeBin(0);
        map.getTimeBin(30);

        assertEquals(120, map.getEndTimeOfLastBucket(), 0.0001);
    }

    @Test
    public void getAllTimeBins() {

        TimeBinMap<String, String> map = new TimeBinMap<>(10);

        map.getTimeBin(15);
        map.getTimeBin(111);
        map.getTimeBin(0);
        map.getTimeBin(30);

        assertEquals(4, map.getAllTimeBins().size());
    }

    @Test
    public void timeBin_putEntry_testContains_retrieve() {

        final String testKey = "some-key";
        final String testValue = "some-value";

        TimeBinMap<String, String> map = new TimeBinMap<>(10);

        TimeBinMap.TimeBin<String, String> bin = map.getTimeBin(15);

        assertFalse(bin.containsKey("any"));
        bin.put(testKey, testValue);
        assertTrue(bin.containsKey(testKey));
        assertEquals(testValue, bin.get(testKey));
    }
}
