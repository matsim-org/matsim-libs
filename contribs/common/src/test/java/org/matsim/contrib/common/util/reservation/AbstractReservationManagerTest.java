/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.common.util.reservation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author nkuehnel
 */
public class AbstractReservationManagerTest {
    
    private TestReservationManager reservationManager;
    private TestResource resource1;
    private TestResource resource2;
    private TestConsumer consumer1;
    private TestConsumer consumer2;
    private TestConsumer consumer3;
    
    @BeforeEach
    public void setUp() {
        reservationManager = new TestReservationManager();
        resource1 = new TestResource(Id.create("resource1", TestResource.class));
        resource2 = new TestResource(Id.create("resource2", TestResource.class));
        consumer1 = new TestConsumer("consumer1");
        consumer2 = new TestConsumer("consumer2");
        consumer3 = new TestConsumer("consumer3");
    }
    
    @Test
    public void testIsAvailable_NoReservations() {
        // When no reservations exist, resource should be available
        assertThat(reservationManager.isAvailable(resource1, consumer1, 100, 200)).isTrue();
    }
    
    @Test
    public void testIsAvailable_WithCapacityZero() {
        // When capacity is zero, resource should not be available
        resource1.capacity = 0;
        assertThat(reservationManager.isAvailable(resource1, consumer1, 100, 200)).isFalse();
    }
    
    @Test
    public void testIsAvailable_WithExistingReservation_NoOverlap() {
        // Add a reservation for 100-200
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation = 
                reservationManager.addReservation(resource1, consumer2, 100, 200);
        assertThat(reservation).isPresent();
        
        // Test if time ranges that don't overlap are available
        assertThat(reservationManager.isAvailable(resource1, consumer1, 0, 100)).isTrue(); // Ends exactly at start
        assertThat(reservationManager.isAvailable(resource1, consumer1, 200, 300)).isTrue(); // Starts exactly at end
    }
    
    @Test
    public void testIsAvailable_WithExistingReservation_WithOverlap() {
        // Add a reservation for 100-200
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation = 
                reservationManager.addReservation(resource1, consumer2, 100, 200);
        assertThat(reservation).isPresent();
        
        // Test if time ranges that overlap are not available
        assertThat(reservationManager.isAvailable(resource1, consumer1, 50, 150)).isFalse(); // Overlap at start
        assertThat(reservationManager.isAvailable(resource1, consumer1, 150, 250)).isFalse(); // Overlap at end
        assertThat(reservationManager.isAvailable(resource1, consumer1, 100, 200)).isFalse(); // Exact same range
        assertThat(reservationManager.isAvailable(resource1, consumer1, 50, 250)).isFalse(); // Completely contains
        assertThat(reservationManager.isAvailable(resource1, consumer1, 120, 180)).isFalse(); // Completely inside
    }
    
    @Test
    public void testIsAvailable_WithExistingReservation_SameConsumer() {
        // Add a reservation for consumer1
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation = 
                reservationManager.addReservation(resource1, consumer1, 100, 200);
        assertThat(reservation).isPresent();
        
        // Same consumer should be able to make overlapping reservations
        assertThat(reservationManager.isAvailable(resource1, consumer1, 150, 250)).isTrue();
    }
    
    @Test
    public void testIsAvailable_WithExistingReservation_DifferentResource() {
        // Add a reservation for resource1
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation = 
                reservationManager.addReservation(resource1, consumer1, 100, 200);
        assertThat(reservation).isPresent();
        
        // Different resource should be available
        assertThat(reservationManager.isAvailable(resource2, consumer2, 100, 200)).isTrue();
    }
    
    @Test
    public void testIsAvailable_WithExistingReservation_WithCapacity() {
        // Set capacity to 2
        resource1.capacity = 2;
        
        // Add first reservation
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation1 = 
                reservationManager.addReservation(resource1, consumer1, 100, 200);
        assertThat(reservation1).isPresent();
        
        // Resource should still be available for another consumer
        assertThat(reservationManager.isAvailable(resource1, consumer2, 100, 200)).isTrue();
        
        // Add second reservation
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation2 = 
                reservationManager.addReservation(resource1, consumer2, 100, 200);
        assertThat(reservation2).isPresent();
        
        // Resource should no longer be available for a third consumer
        TestConsumer consumer3 = new TestConsumer("consumer3");
        assertThat(reservationManager.isAvailable(resource1, consumer3, 100, 200)).isFalse();
    }
    
    @Test
    public void testAddReservation() {
        // Add a reservation and verify it was added successfully
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation = 
                reservationManager.addReservation(resource1, consumer1, 100, 200);
        
        assertThat(reservation).isPresent();
        assertThat(reservation.get().resource()).isEqualTo(resource1);
        assertThat(reservation.get().consumer()).isEqualTo(consumer1);
        assertThat(reservation.get().startTime()).isEqualTo(100);
        assertThat(reservation.get().endTime()).isEqualTo(200);
    }
    
    @Test
    public void testAddReservation_NotAvailable() {
        // Add a reservation
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation1 = 
                reservationManager.addReservation(resource1, consumer1, 100, 200);
        assertThat(reservation1).isPresent();
        
        // Try to add an overlapping reservation for a different consumer
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation2 = 
                reservationManager.addReservation(resource1, consumer2, 150, 250);
        assertThat(reservation2).isEmpty();
    }
    
    @Test
    public void testRemoveReservation() {
        // Add a reservation
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation = 
                reservationManager.addReservation(resource1, consumer1, 100, 200);
        assertThat(reservation).isPresent();
        
        // Remove the reservation
        boolean removed = reservationManager.removeReservation(resource1.getId(), reservation.get().reservationId());
        assertThat(removed).isTrue();
        
        // Verify the resource is available again
        assertThat(reservationManager.isAvailable(resource1, consumer2, 100, 200)).isTrue();
    }
    
    @Test
    public void testRemoveReservation_NonExistent() {
        // Try to remove a non-existent reservation
        boolean removed = reservationManager.removeReservation(
                resource1.getId(), 
                Id.create("nonexistent", ReservationManager.Reservation.class)
        );
        assertThat(removed).isFalse();
    }
    
    @Test
    public void testUpdateReservation() {
        // Add a reservation
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation = 
                reservationManager.addReservation(resource1, consumer1, 100, 200);
        assertThat(reservation).isPresent();
        
        // Update the reservation
        boolean updated = reservationManager.updateReservation(
                resource1.getId(), 
                reservation.get().reservationId(), 
                150, 
                250
        );
        assertThat(updated).isTrue();
        
        // Verify the old time range is now available
        assertThat(reservationManager.isAvailable(resource1, consumer2, 100, 150)).isTrue();
        
        // Verify the new time range is not available
        assertThat(reservationManager.isAvailable(resource1, consumer2, 200, 250)).isFalse();
    }
    
    @Test
    public void testUpdateReservation_NotAvailable() {
        // Add a reservation for consumer1
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation1 = 
                reservationManager.addReservation(resource1, consumer1, 100, 200);
        assertThat(reservation1).isPresent();
        
        // Add a reservation for consumer2
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation2 = 
                reservationManager.addReservation(resource1, consumer2, 300, 400);
        assertThat(reservation2).isPresent();
        
        // Try to update consumer1's reservation to overlap with consumer2's
        boolean updated = reservationManager.updateReservation(
                resource1.getId(), 
                reservation1.get().reservationId(), 
                200, 
                350
        );
        assertThat(updated).isFalse();
        
        // Verify the original reservation is still intact
        assertThat(reservationManager.isAvailable(resource1, new TestConsumer("consumer3"), 100, 200)).isFalse();
        assertThat(reservationManager.isAvailable(resource1, new TestConsumer("consumer3"), 200, 300)).isTrue();
    }
    
    @Test
    public void testFindReservation_ByResourceAndConsumer() {
        // Add a reservation
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> addedReservation = 
                reservationManager.addReservation(resource1, consumer1, 100, 200);
        assertThat(addedReservation).isPresent();
        
        // Find the reservation by resource and consumer
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> foundReservation = 
                reservationManager.findReservation(resource1, consumer1, 150);
        
        assertThat(foundReservation).isPresent();
        assertThat(foundReservation.get().reservationId()).isEqualTo(addedReservation.get().reservationId());
    }
    
    @Test
    public void testFindReservation_ByResourceAndConsumer_OutsideTimeRange() {
        // Add a reservation
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> addedReservation = 
                reservationManager.addReservation(resource1, consumer1, 100, 200);
        assertThat(addedReservation).isPresent();
        
        // Try to find the reservation at a time outside its range
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> foundReservation = 
                reservationManager.findReservation(resource1, consumer1, 50);
        
        assertThat(foundReservation).isEmpty();
    }
    
    @Test
    public void testFindReservation_ById() {
        // Add a reservation
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> addedReservation = 
                reservationManager.addReservation(resource1, consumer1, 100, 200);
        assertThat(addedReservation).isPresent();
        
        // Find the reservation by ID
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> foundReservation = 
                reservationManager.findReservation(resource1.getId(), addedReservation.get().reservationId());
        
        assertThat(foundReservation).isPresent();
        assertThat(foundReservation.get().reservationId()).isEqualTo(addedReservation.get().reservationId());
    }
    
    @Test
    public void testFindReservation_ById_NonExistent() {
        // Try to find a non-existent reservation by ID
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> foundReservation = 
                reservationManager.findReservation(
                        resource1.getId(), 
                        Id.create("nonexistent", ReservationManager.Reservation.class)
                );
        
        assertThat(foundReservation).isEmpty();
    }
    
    @Test
    public void testFindReservation_NonExistentResource() {
        // Try to find a reservation for a resource that has no reservations
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> foundReservation = 
                reservationManager.findReservation(
                        resource2.getId(), 
                        Id.create("anyId", ReservationManager.Reservation.class)
                );
        
        assertThat(foundReservation).isEmpty();
    }
    
    /**
     * Test that demonstrates the unique consumer counting logic.
     * Multiple overlapping reservations from the same consumer are counted only once.
     */
    @Test
    public void testUniqueConsumerCounting() {
        // Set resource capacity to 2
        resource1.capacity = 2;
        
        // Add first reservation for consumer1
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation1 = 
                reservationManager.addReservation(resource1, consumer1, 100, 200);
        assertThat(reservation1).isPresent();
        
        // Add second overlapping reservation for consumer1
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation2 = 
                reservationManager.addReservation(resource1, consumer1, 150, 250);
        assertThat(reservation2).isPresent();
        
        // Add reservation for consumer2
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation3 = 
                reservationManager.addReservation(resource1, consumer2, 120, 180);
        assertThat(reservation3).isPresent();
        
        // With capacity of 2 and 2 unique consumers (consumer1, consumer2) with overlapping reservations,
        // consumer3 should NOT be able to make a reservation in the overlapping time range
        assertThat(reservationManager.isAvailable(resource1, consumer3, 160, 180)).isFalse();
        
        // But consumer3 should be able to make a reservation in a non-overlapping time range
        assertThat(reservationManager.isAvailable(resource1, consumer3, 300, 350)).isTrue();
        
        // Add reservation for consumer3 in the non-overlapping time range
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation4 = 
                reservationManager.addReservation(resource1, consumer3, 300, 350);
        assertThat(reservation4).isPresent();
    }
    
    @Test
    public void testOverlappingReservationsForDifferentTimeRanges() {
        // Set resource capacity to 2
        resource1.capacity = 2;
        
        // Add first reservation for consumer1
        reservationManager.addReservation(resource1, consumer1, 100, 200);
        
        // Add second non-overlapping reservation for consumer1
        reservationManager.addReservation(resource1, consumer1, 300, 400);
        
        // Add reservation for consumer2
        reservationManager.addReservation(resource1, consumer2, 150, 250);
        
        // Consumer3 should be able to reserve at 350-450 (overlapping with consumer1's second reservation)
        assertThat(reservationManager.isAvailable(resource1, consumer3, 350, 450)).isTrue();
        reservationManager.addReservation(resource1, consumer3, 350, 450);
        
        // Consumer3 should NOT be able to reserve at 175-225 (would exceed capacity)
        assertThat(reservationManager.isAvailable(resource1, consumer3, 175, 225)).isFalse();
    }
    
    @Test
    public void testRemoveReservationRestoringCapacity() {
        // Set resource capacity to 2
        resource1.capacity = 2;
        
        // Add reservations for consumer1 and consumer2
        reservationManager.addReservation(resource1, consumer1, 100, 200);
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation2 = 
                reservationManager.addReservation(resource1, consumer2, 150, 250);
        
        // Add another reservation for consumer1 (doesn't affect capacity since we count unique consumers)
        reservationManager.addReservation(resource1, consumer1, 130, 230);
        
        // Consumer3 should not be able to reserve in the time range where both consumer1 and consumer2 have reservations
        assertThat(reservationManager.isAvailable(resource1, consumer3, 160, 180)).isFalse();
        
        // Remove consumer2's reservation
        assertThat(reservation2).isPresent(); // Ensure reservation exists
        reservationManager.removeReservation(resource1.getId(), reservation2.get().reservationId());
        
        // Now consumer3 should be able to reserve
        assertThat(reservationManager.isAvailable(resource1, consumer3, 160, 220)).isTrue();
    }

    @Test
    public void testCleanReservations() {
        // Add reservations
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation1 = 
                reservationManager.addReservation(resource1, consumer1, 100, 200);
        Optional<ReservationManager.ReservationInfo<TestResource, TestConsumer>> reservation2 = 
                reservationManager.addReservation(resource2, consumer2, 300, 400);
        
        assertThat(reservation1).isPresent();
        assertThat(reservation2).isPresent();
        
        // Clean reservations
        reservationManager.cleanReservations();
        
        // Verify all resources are available again
        assertThat(reservationManager.isAvailable(resource1, consumer2, 100, 200)).isTrue();
        assertThat(reservationManager.isAvailable(resource2, consumer1, 300, 400)).isTrue();
    }

    // Test class implementations
    
    private static class TestResource implements Identifiable<TestResource> {
        private final Id<TestResource> id;
        private int capacity = 1;
        
        public TestResource(Id<TestResource> id) {
            this.id = id;
        }
        
        @Override
        public Id<TestResource> getId() {
            return id;
        }
    }
    
    private record TestConsumer(String id) {
        @Override
        public String toString() {
            return "TestConsumer(" + id + ")";
        }
    }
    
    private static class TestReservationManager extends AbstractReservationManager<TestResource, TestConsumer> {
        @Override
        public int getCapacity(TestResource resource) {
            return resource.capacity;
        }
        
        // Make protected method accessible for testing
        @Override
        protected void cleanReservations() {
            super.cleanReservations();
        }
    }
}