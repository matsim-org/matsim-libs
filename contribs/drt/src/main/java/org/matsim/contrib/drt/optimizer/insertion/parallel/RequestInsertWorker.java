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
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.optimizer.insertion.RequestFleetFilter;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.RequestData;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.matsim.contrib.drt.optimizer.insertion.selective.RequestDataComparators.REQUEST_DATA_COMPARATOR;

/**
 * @author steffenaxer
 */
public class RequestInsertWorker {
	private final RequestFleetFilter requestFleetFilter;
	private final DrtInsertionSearch insertionSearch;
	private final Queue<RequestData> unplannedRequests = new ArrayDeque<>();
	private final Map<Id<DvrpVehicle>, SortedSet<RequestData>> solutions;
	private final SortedSet<DrtRequest> noSolutions;

	// Performance tracking
	private volatile long lastProcessingTimeNanos = 0;
	private volatile int lastRequestCount = 0;
	private volatile int lastVehicleCount = 0;

	public RequestInsertWorker(
		RequestFleetFilter requestFleetFilter,
		DrtInsertionSearch insertionSearch,
		Map<Id<DvrpVehicle>, SortedSet<RequestData>> solutions, SortedSet<DrtRequest> noSolutions) {
		this.requestFleetFilter = requestFleetFilter;
		this.insertionSearch = insertionSearch;
		this.solutions = solutions;
		this.noSolutions = noSolutions;
	}


	private static SortedSet<RequestData> createTreeSet()
	{
		return new ConcurrentSkipListSet<>(REQUEST_DATA_COMPARATOR);
	}

	private void findInsertion(RequestData requestData, Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries, double now) {
		DrtRequest req = requestData.getDrtRequest();
		Collection<VehicleEntry> filteredFleet = requestFleetFilter.filter(req, vehicleEntries, now);
		Optional<InsertionWithDetourData> best = insertionSearch.findBestInsertion(req, filteredFleet);

		if (best.isEmpty()) {
			this.noSolutions.add(requestData.getDrtRequest());
		} else {
			InsertionWithDetourData insertion = best.get();
			requestData.setSolution(new RequestData.InsertionRecord(best));
			this.solutions.computeIfAbsent(insertion.insertion.vehicleEntry.vehicle.getId(), _ -> createTreeSet()).add(requestData);
		}
	}


	void process(double now, Collection<RequestData> requestDataPartition, Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries) {
		long startTime = System.nanoTime();
		this.lastRequestCount = requestDataPartition.size();
		this.lastVehicleCount = vehicleEntries.size();

		this.unplannedRequests.addAll(requestDataPartition);

		while (!unplannedRequests.isEmpty()) {
			findInsertion(unplannedRequests.poll(), vehicleEntries, now);
		}

		this.lastProcessingTimeNanos = System.nanoTime() - startTime;
	}

	/**
	 * @return Processing time of the last process() call in nanoseconds
	 */
	public long getLastProcessingTimeNanos() {
		return lastProcessingTimeNanos;
	}

	/**
	 * @return Number of requests processed in the last process() call
	 */
	public int getLastRequestCount() {
		return lastRequestCount;
	}

	/**
	 * @return Number of vehicles available in the last process() call
	 */
	public int getLastVehicleCount() {
		return lastVehicleCount;
	}


	public void clean() {
		this.unplannedRequests.clear();
		this.lastProcessingTimeNanos = 0;
		this.lastRequestCount = 0;
		this.lastVehicleCount = 0;
	}
}
