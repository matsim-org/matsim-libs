package org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.RequestData;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.vehicles.ShiftingRoundRobinVehicleEntryPartitioner;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ShiftingRoundRobinVehicleEntryPartitionerTest {

    @Test
    void testPartitioningConsistency() {
        ShiftingRoundRobinVehicleEntryPartitioner partitioner = new ShiftingRoundRobinVehicleEntryPartitioner();

        // Create 4 request partitions, with only 2 being non-empty
        List<Collection<RequestData>> requestPartitions = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            requestPartitions.add(new ArrayList<>());
        }
        requestPartitions.get(0).add(new RequestData(null));
        requestPartitions.get(1).add(new RequestData(null));

        // Create 6 vehicle entries
        Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            Id<DvrpVehicle> id = Id.create("veh" + i, DvrpVehicle.class);
            vehicleEntries.put(id, new VehicleEntry(null, null, null, null, null, 0));
        }

        List<Map<Id<DvrpVehicle>, VehicleEntry>> partitions = partitioner.partition(vehicleEntries, requestPartitions);

        // 1. Number of partitions must match number of request partitions
        assertEquals(requestPartitions.size(), partitions.size(), "Partition count mismatch");

        // 2. If a vehicle partition is empty, the corresponding request partition must be empty
        for (int i = 0; i < partitions.size(); i++) {
            if (partitions.get(i).isEmpty()) {
                assertTrue(requestPartitions.get(i).isEmpty(), "Vehicle partition is empty but request partition is not");
            }
        }

        // 3. Vehicles are only assigned to partitions with non-empty request sets
        for (int i = 0; i < partitions.size(); i++) {
            if (!partitions.get(i).isEmpty()) {
                assertFalse(requestPartitions.get(i).isEmpty(), "Vehicle assigned to empty request partition");
            }
        }
    }
}
