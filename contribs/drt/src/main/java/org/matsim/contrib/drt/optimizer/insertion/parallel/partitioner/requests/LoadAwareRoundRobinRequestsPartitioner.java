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


package org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.requests;

import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.RequestData;
import org.matsim.contrib.drt.passenger.DrtRequest;

import java.util.*;

/**
 * A {@link RequestsPartitioner} implementation that distributes DRT requests across partitions
 * using a load-aware round-robin strategy.
 * <p>
 * Unlike the basic round-robin approach, this implementation dynamically adjusts the number
 * of active partitions based on the current request load and a configurable {@link PartitionScalingFunction}.
 * This allows the system to scale the number of partitions up or down depending on demand,
 * improving resource efficiency and responsiveness.
 * <p>
 * Requests are wrapped in {@link RequestData} and assigned cyclically to the active partitions.
 * The internal counter ensures a consistent distribution across multiple invocations.
 * <p>
 * After partitioning, the original collection of unplanned requests is cleared.
 *
 * @author Steffen Axer
 */
public class LoadAwareRoundRobinRequestsPartitioner implements RequestsPartitioner {

	private final PartitionScalingFunction scalingFunction;
	private long counter = 0;

	public LoadAwareRoundRobinRequestsPartitioner(PartitionScalingFunction scalingFunction) {
		this.scalingFunction = scalingFunction;
	}

	@Override
	public List<Collection<RequestData>> partition(Collection<DrtRequest> unplannedRequests, int n, double collectionPeriod) {
		List<Collection<RequestData>> partitions = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			partitions.add(new ArrayList<>());
		}

		int requestCount = unplannedRequests.size();
		int activePartitions = scalingFunction.computeActivePartitions(n, requestCount, collectionPeriod);

		Iterator<DrtRequest> it = unplannedRequests.iterator();
		while (it.hasNext()) {
			DrtRequest request = it.next();
			int partitionIndex = (int) (counter % activePartitions);
			partitions.get(partitionIndex).add(new RequestData(request));
			it.remove();
			counter++;
		}

		return partitions;
	}

	/**
	 * Calculates the number of active partitions as a function of totalNumberOfPartitions,
	 * and request density per minute. This scaling function adds for every additional 20 rides/minute
	 * an additional partition. Each partition is managed by an own thread.
	 */
	public static PartitionScalingFunction getDefaultPartitionScalingFunction() {
		return (totalNumberOfPartitions, requests, period) -> {
			double requestsPerMinute = requests * (60.0 / period);
			return Math.min(totalNumberOfPartitions, Math.max(1, (int) (requestsPerMinute / 20) + 1));
		};
	}
}

