package org.matsim.contrib.drt.extension.operations.operationFacilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CapacityManagerTest {
    private static final double HORIZON = 5.0;
    private static final int CAPACITY = 1;
    private OperationFacilityImpl.CapacityManager cm;
    private Id<DvrpVehicle> v1;
    private Id<DvrpVehicle> v2;

    @BeforeEach
    void setUp() {
        cm = new OperationFacilityImpl.CapacityManager(null, CAPACITY, HORIZON);
        v1 = Id.create("v1", DvrpVehicle.class);
        v2 = Id.create("v2", DvrpVehicle.class);
    }

    @Test
    void testInterference1() {
        // v1 reserves [0,2]
        assertTrue(cm.registerVehicle(v1, 0, 2).isPresent());
        // v2 cannot reserve same range since capacity=1
        assertFalse(cm.registerVehicle(v2, 0, 2).isPresent());
    }

    @Test
    void testInterference2() {
        assertTrue(cm.registerVehicle(v1, 0, 2).isPresent());
        assertFalse(cm.registerVehicle(v2, 1, 1).isPresent());
    }

    @Test
    void testInterference3() {
        assertTrue(cm.registerVehicle(v1, 0, 2).isPresent());
        assertTrue(cm.registerVehicle(v2, 2, 3).isPresent());
    }

    @Test
    void testNonOverlappingReservations1() {
        assertTrue(cm.registerVehicle(v1, 0, 1).isPresent());
        assertTrue(cm.registerVehicle(v2, 2, 3).isPresent());
    }

    @Test
    void testNonOverlappingReservations2() {
        assertTrue(cm.registerVehicle(v1, 0, 2).isPresent());
        assertTrue(cm.registerVehicle(v2, 2, 4).isPresent());
    }

    @Test
    void testUpdate() {
        // initial parking
        Optional<OperationFacility.Registration> registration = cm.registerVehicle(v1, 0, 2);
        assertTrue(registration.isPresent());
        // update parking time frees old and uses new
        assertTrue(cm.deregister(registration.get().registrationId()));
        // now [0,2] is free
        assertTrue(cm.registerVehicle(v2, 1, 2).isPresent());
    }

    @Test
    void testBoundaryClipping() {
        // request beyond horizon trimmed to [0,4]
        assertTrue(cm.registerVehicle(v1, -10, 10).isPresent());
        // any further reservation inside [0,4] should fail
        assertFalse(cm.registerVehicle(v2, 2, 2).isPresent());
    }

    @Test
    void testZeroLengthRange() {
        assertFalse(cm.registerVehicle(v1, 3, 3).isPresent());
    }

    @Test
    void testMaximumHorizonEdge() {
        // reserving exactly at horizon-1 should succeed
        int lastSec = (int) HORIZON - 1;
        assertTrue(cm.registerVehicle(v1, 0, lastSec).isPresent());
    }

    @Test
    void testMaximumHorizonEdgeClamped() {
        // reserving exactly at horizon-1 should succeed
        int lastSec = (int) HORIZON - 1;
        assertTrue(cm.registerVehicle(v2, 0, lastSec + 10).isPresent());
    }
}
