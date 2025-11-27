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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link VehicleEntryPartitioner} implementation that distributes vehicle entries
 * across active request partitions using a shifting round-robin strategy.
 * <p>
 * Similar to {@link RoundRobinVehicleEntryPartitioner}, this class assigns vehicles
 * to partitions in a cyclic manner. However, it introduces a dynamic shift in the
 * starting partition index on each invocation, based on an internal call counter.
 * <p>
 * This shifting mechanism helps to avoid systematic biases in vehicle assignment
 * over time, promoting a more even distribution of vehicles across partitions
 * in long-running simulations.
 * <p>
 * Only partitions with at least one request are considered active and included
 * in the distribution. Vehicles are sorted by ID to ensure deterministic behavior.
 *
 * @author Steffen Axer
 */
public class ShiftingRoundRobinVehicleEntryPartitioner implements VehicleEntryPartitioner {

	private final AtomicInteger callCounter = new AtomicInteger(0);

	@Override
	public List<Map<Id<DvrpVehicle>, VehicleEntry>> partition(
		Map<Id<DvrpVehicle>, VehicleEntry> entries,
		List<Collection<RequestData>> requestsPartitions) {


		int n = requestsPartitions.size();
		List<Map<Id<DvrpVehicle>, VehicleEntry>> partitions = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			partitions.add(new HashMap<>());
		}

		List<Integer> activePartitionIndices = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			if (!requestsPartitions.get(i).isEmpty()) {
				activePartitionIndices.add(i);
			}
		}

		if (activePartitionIndices.isEmpty()) {
			return partitions;
		}

		List<Map.Entry<Id<DvrpVehicle>, VehicleEntry>> sortedEntries = new ArrayList<>(entries.entrySet());
		sortedEntries.sort(Map.Entry.comparingByKey(Comparator.comparing(Id::toString)));

		int shift = callCounter.getAndIncrement() % activePartitionIndices.size();
		int index = 0;
		for (Map.Entry<Id<DvrpVehicle>, VehicleEntry> entry : sortedEntries) {
			int partitionIndex = activePartitionIndices.get((index + shift) % activePartitionIndices.size());
			partitions.get(partitionIndex).put(entry.getKey(), entry.getValue());
			index++;
		}

		return partitions;
	}
}

