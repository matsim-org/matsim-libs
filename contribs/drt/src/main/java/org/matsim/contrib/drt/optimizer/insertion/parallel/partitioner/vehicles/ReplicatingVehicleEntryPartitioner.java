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


package org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.vehicles;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.RequestData;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.*;

/**
 * A {@link VehicleEntryPartitioner} implementation that replicates the entire set of vehicle entries
 * across all request partitions.
 * <p>
 * This strategy ensures that each partition has access to the full set of available vehicles,
 * allowing independent insertion searches without constraints from partition-specific vehicle subsets.
 * <p>
 * It is useful in scenarios where request partitioning is necessary for parallel processing,
 * but vehicle availability should remain global across all partitions.
 * <p>
 * Note: This approach may increase computational load due to redundant vehicle data across partitions.
 *
 * @author Steffen Axer
 */
public class ReplicatingVehicleEntryPartitioner implements VehicleEntryPartitioner {

    @Override
    public List<Map<Id<DvrpVehicle>, VehicleEntry>> partition(
		Map<Id<DvrpVehicle>, VehicleEntry> entries, List<Collection<RequestData>> requestsPartitions) {
		int n = requestsPartitions.size();
        List<Map<Id<DvrpVehicle>, VehicleEntry>> partitions = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            partitions.add(new HashMap<>(entries));
        }

        return partitions;
    }
}
