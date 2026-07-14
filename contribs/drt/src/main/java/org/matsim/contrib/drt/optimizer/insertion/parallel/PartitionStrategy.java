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

package org.matsim.contrib.drt.optimizer.insertion.parallel;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.RequestData;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.requests.RequestsPartitioner;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.vehicles.VehicleEntryPartitioner;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Strategy for creating consistent request and vehicle partitions.
 * <p>
 * This class encapsulates the partitioning logic, coordinating between
 * {@link RequestsPartitioner} and {@link VehicleEntryPartitioner} to create
 * a validated {@link PartitionContext}.
 *
 * @author Steffen Axer
 */
public class PartitionStrategy {

	private final RequestsPartitioner requestsPartitioner;
	private final VehicleEntryPartitioner vehicleEntryPartitioner;
	private final int maxWorkers;
	private final double collectionPeriod;

	public PartitionStrategy(
		RequestsPartitioner requestsPartitioner,
		VehicleEntryPartitioner vehicleEntryPartitioner,
		int maxWorkers,
		double collectionPeriod
	) {
		this.requestsPartitioner = requestsPartitioner;
		this.vehicleEntryPartitioner = vehicleEntryPartitioner;
		this.maxWorkers = maxWorkers;
		this.collectionPeriod = collectionPeriod;
	}

	/**
	 * Creates a validated partition context from the given requests and vehicle entries.
	 *
	 * @param requests       Queue of requests to partition (will be consumed/modified)
	 * @param vehicleEntries Map of available vehicle entries
	 * @return Validated partition context
	 */
	public PartitionContext createPartitions(
		Queue<DrtRequest> requests,
		Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries
	) {
		int originalRequestCount = requests.size();
		int maxPartitions = Math.max(1, Math.min(maxWorkers, vehicleEntries.size()));

		List<Collection<RequestData>> requestPartitions =
			requestsPartitioner.partition(requests, maxPartitions, collectionPeriod);

		List<Map<Id<DvrpVehicle>, VehicleEntry>> vehiclePartitions =
			vehicleEntryPartitioner.partition(vehicleEntries, requestPartitions);

		int activePartitionCount = (int) requestPartitions.stream()
			.filter(p -> p != null && !p.isEmpty())
			.count();

		return new PartitionContext(
			requestPartitions,
			vehiclePartitions,
			activePartitionCount,
			originalRequestCount
		);
	}

	public int getMaxWorkers() {
		return maxWorkers;
	}

	public double getCollectionPeriod() {
		return collectionPeriod;
	}
}
