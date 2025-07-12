/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */


package org.matsim.contrib.zone.skims;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class SparseTravelTimeCacheTest {

    private Map<AdaptiveTravelTimeMatrixImpl.IntSparseKey, Double> cache;

    @BeforeEach
    void setUp() {
        cache = new ConcurrentHashMap<>();
    }

    @Test
    void testPutAndGetTravelTime() {
        int fromIndex = 123;
        int toIndex = 456;
        int timeBin = 5;
        double travelTime = 321.5;

        AdaptiveTravelTimeMatrixImpl.IntSparseKey key = new AdaptiveTravelTimeMatrixImpl.IntSparseKey(fromIndex, toIndex, timeBin);
        cache.put(key, travelTime);

        assertTrue(cache.containsKey(key), "Key should exist in cache");
        assertEquals(travelTime, cache.get(key), 1e-6, "Travel time should match");
    }

    @Test
    void testDifferentBinsAreDifferentKeys() {
        AdaptiveTravelTimeMatrixImpl.IntSparseKey key1 = new AdaptiveTravelTimeMatrixImpl.IntSparseKey(1, 2, 3);
        AdaptiveTravelTimeMatrixImpl.IntSparseKey key2 = new AdaptiveTravelTimeMatrixImpl.IntSparseKey(1, 2, 4);

        cache.put(key1, 100.0);
        cache.put(key2, 200.0);

        assertNotEquals(key1, key2, "Keys with different bins should not be equal");
        assertEquals(100.0, cache.get(key1), 1e-6);
        assertEquals(200.0, cache.get(key2), 1e-6);
    }

    @Test
    void testSymmetry() {
        AdaptiveTravelTimeMatrixImpl.IntSparseKey key1 = new AdaptiveTravelTimeMatrixImpl.IntSparseKey(10, 20, 1);
        AdaptiveTravelTimeMatrixImpl.IntSparseKey key2 = new AdaptiveTravelTimeMatrixImpl.IntSparseKey(20, 10, 1);

        cache.put(key1, 50.0);
        cache.put(key2, 75.0);

        assertNotEquals(key1, key2, "Direction matters: keys should not be equal");
        assertEquals(50.0, cache.get(key1), 1e-6);
        assertEquals(75.0, cache.get(key2), 1e-6);
    }

	@Test
	void testBitPackingAndUnpacking() {
		int fromIndex = 123456;
		int toIndex = 654321;
		int timeBin = 17;

		AdaptiveTravelTimeMatrixImpl.IntSparseKey key = new AdaptiveTravelTimeMatrixImpl.IntSparseKey(fromIndex, toIndex, timeBin);

		assertEquals(fromIndex, key.getFromIndex(), "fromIndex should match after unpacking");
		assertEquals(toIndex, key.getToIndex(), "toIndex should match after unpacking");
		assertEquals(timeBin, key.getTimeBin(), "timeBin should match after unpacking");
	}

}
