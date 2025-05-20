package org.matsim.contrib.drt.extension.operations.operationFacilities;

import org.apache.commons.lang.math.IntRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import static org.junit.jupiter.api.Assertions.*;

public class CapacityManagerTest {
    private static final double HORIZON = 5.0;
    private static final int CAPACITY = 1;
    private OperationFacilityImpl.CapacityManager cm;
    private Id<DvrpVehicle> v1;
    private Id<DvrpVehicle> v2;

    @BeforeEach
    void setUp() {
        cm = new OperationFacilityImpl.CapacityManager(CAPACITY, HORIZON);
        v1 = Id.create("v1", DvrpVehicle.class);
        v2 = Id.create("v2", DvrpVehicle.class);
    }

    @Test
    void testSingleShiftBreakReservation() {
        // v1 reserves [0,2]
        assertTrue(cm.registerShiftBreak(v1, new IntRange(0, 2)));
        // v2 cannot reserve same range since capacity=1
        assertFalse(cm.registerShiftBreak(v2, new IntRange(0, 2)));
    }

    @Test
    void testNonOverlappingShiftBreaks() {
        assertTrue(cm.registerShiftBreak(v1, new IntRange(0, 0)));
        assertTrue(cm.registerShiftBreak(v2, new IntRange(1, 1)));
    }

    @Test
    void testUpdateShiftBreakOverridesOldReservation() {
        // first reservation
        assertTrue(cm.registerShiftBreak(v1, new IntRange(0, 2)));
        // second reservation for same vehicle replaces old one
        assertTrue(cm.registerShiftBreak(v1, new IntRange(3, 4)));
        // now [0,2] is freed, so v2 can reserve inside [1,1]
        assertTrue(cm.registerShiftBreak(v2, new IntRange(1, 1)));
    }

    @Test
    void testParkingAndBreakInterference() {
        // parking out-of-shift acts on same capacity
        assertTrue(cm.registerParkingOutOfShift(v1, new IntRange(0, 2)));
        // shift break cannot overlap parking
        assertFalse(cm.registerShiftBreak(v2, new IntRange(1, 1)));
    }

    @Test
    void testUpdateParkingOutOfShift() {
        // initial parking
        assertTrue(cm.registerParkingOutOfShift(v1, new IntRange(0, 2)));
        // update parking time frees old and uses new
        assertTrue(cm.registerParkingOutOfShift(v1, new IntRange(3, 4)));
        // now [0,2] is free
        assertTrue(cm.registerShiftBreak(v2, new IntRange(1, 1)));
    }

    @Test
    void testBoundaryClipping() {
        // request beyond horizon trimmed to [0,4]
        assertTrue(cm.registerShiftBreak(v1, new IntRange(-10, 10)));
        // any further reservation inside [0,4] should fail
        assertFalse(cm.registerShiftBreak(v2, new IntRange(2, 2)));
    }

    @Test
    void testZeroLengthRange() {
        // zero-length window [3,3]
        assertTrue(cm.registerShiftBreak(v1, new IntRange(3, 3)));
        // same second cannot be reused
        assertFalse(cm.registerShiftBreak(v2, new IntRange(3, 3)));
        // but adjacent second is free
        assertTrue(cm.registerShiftBreak(v2, new IntRange(4, 4)));
    }

    @Test
    void testMaximumHorizonEdge() {
        // reserving exactly at horizon-1 should succeed
        int lastSec = (int) HORIZON - 1;
        assertTrue(cm.registerShiftBreak(v1, new IntRange(lastSec, lastSec)));
        // out-of-bounds start clamps
        assertFalse(cm.registerShiftBreak(v2, new IntRange(lastSec, lastSec + 10)));
    }
}
