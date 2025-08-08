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
 * A {@link RequestsPartitioner} implementation that distributes incoming DRT requests
 * across a fixed number of partitions using a round-robin strategy.
 * <p>
 * Each request is wrapped in a {@link RequestData} object and assigned to a partition
 * in a cyclic manner based on an internal counter. This ensures an even and deterministic
 * distribution of requests, regardless of their content or origin.
 * <p>
 * The partitioning is stateful: the internal counter is preserved across multiple calls,
 * which helps maintain balanced distribution over time.
 * <p>
 * After partitioning, the original collection of unplanned requests is cleared.
 *
 * @author Steffen Axer
 */

public class RoundRobinRequestsPartitioner implements RequestsPartitioner {
	private long counter = 0;

    @Override
    public List<Collection<RequestData>> partition(Collection<DrtRequest> unplannedRequests, int n, double collectionPeriod) {
        List<Collection<RequestData>> partitions = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            partitions.add(new ArrayList<>());
        }

        Iterator<DrtRequest> it = unplannedRequests.iterator();
        while (it.hasNext()) {
            DrtRequest request = it.next();
            int partitionIndex = (int) counter % n;
            partitions.get(partitionIndex).add(new RequestData(request));
            it.remove();
            counter++;
        }

        return partitions;
    }
}
