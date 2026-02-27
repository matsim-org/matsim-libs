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

import com.google.common.base.Verify;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.RequestData;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;

import java.util.*;

/**
 * Immutable container for a validated partition of requests and vehicles.
 * <p>
 * This record ensures that request and vehicle partitions are consistent and validated
 * at construction time (fail-fast). Once created, the partitions cannot be modified.
 * <p>
 * Validation includes:
 * <ul>
 *   <li>Equal number of request and vehicle partitions</li>
 *   <li>No duplicate requests across partitions</li>
 * </ul>
 *
 * @param requestPartitions  List of request partitions, one per worker
 * @param vehiclePartitions  List of vehicle partitions, one per worker
 * @param activePartitionCount Number of non-empty request partitions
 * @param originalRequestCount Number of requests before partitioning
 *
 * @author Steffen Axer
 */
public record PartitionContext(
	List<Collection<RequestData>> requestPartitions,
	List<Map<Id<DvrpVehicle>, VehicleEntry>> vehiclePartitions,
	int activePartitionCount,
	int originalRequestCount
) {
	/**
	 * Compact constructor with validation.
	 */
	public PartitionContext {
		Objects.requireNonNull(requestPartitions, "requestPartitions must not be null");
		Objects.requireNonNull(vehiclePartitions, "vehiclePartitions must not be null");

		Verify.verify(
			requestPartitions.size() == vehiclePartitions.size(),
			"Mismatch between number of request partitions (%s) and vehicle partitions (%s)",
			requestPartitions.size(), vehiclePartitions.size()
		);

		validateUniqueRequests(requestPartitions);

		// Make immutable
		requestPartitions = List.copyOf(requestPartitions);
		vehiclePartitions = List.copyOf(vehiclePartitions);
	}

	/**
	 * @return Number of partitions (same for requests and vehicles)
	 */
	public int size() {
		return requestPartitions.size();
	}

	/**
	 * @return true if there are no requests to process
	 */
	public boolean isEmpty() {
		return originalRequestCount == 0;
	}

	/**
	 * Gets the request partition for a specific worker index.
	 */
	public Collection<RequestData> getRequestPartition(int index) {
		return requestPartitions.get(index);
	}

	/**
	 * Gets the vehicle partition for a specific worker index.
	 */
	public Map<Id<DvrpVehicle>, VehicleEntry> getVehiclePartition(int index) {
		return vehiclePartitions.get(index);
	}

	private static void validateUniqueRequests(List<Collection<RequestData>> partitions) {
		Set<Id<Request>> seen = new HashSet<>();
		for (Collection<RequestData> partition : partitions) {
			for (RequestData data : partition) {
				if (!seen.add(data.getDrtRequest().getId())) {
					throw new IllegalStateException(
						"Duplicate DrtRequest found across partitions: " + data.getDrtRequest().getId()
					);
				}
			}
		}
	}
}
