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
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.optimizer.insertion.RequestFleetFilter;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.RequestData;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
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

	public RequestInsertWorker(
		RequestFleetFilter requestFleetFilter,
		DrtInsertionSearch insertionSearch,
		Map<Id<DvrpVehicle>, SortedSet<RequestData>> solutions, SortedSet<DrtRequest> noSolutions) {
		this.requestFleetFilter = requestFleetFilter;
		this.insertionSearch = insertionSearch;
		this.solutions = solutions;
		this.noSolutions = noSolutions;
	}

	public int getUnplannedRequestCount()
	{
		return this.unplannedRequests.size();
	}


	public int getPlannedRequestCount()
	{
		return this.noSolutions.size() + solutions.values().stream().mapToInt(Set::size).sum();
	}

	private static SortedSet<RequestData> createTreeSet()
	{
		return new ConcurrentSkipListSet<>(new TreeSet<>(REQUEST_DATA_COMPARATOR));
	}

	private void findInsertion(RequestData requestData, Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries, double now) {
		DrtRequest req = requestData.getDrtRequest();
		Collection<VehicleEntry> filteredFleet = requestFleetFilter.filter(req, vehicleEntries, now);
		Optional<InsertionWithDetourData> best = insertionSearch.findBestInsertion(req, Collections.unmodifiableCollection(filteredFleet));

		if (best.isEmpty()) {
			this.noSolutions.add(requestData.getDrtRequest());
		} else {
			InsertionWithDetourData insertion = best.get();
			requestData.setSolution(new RequestData.InsertionRecord(best));
			this.solutions.computeIfAbsent(insertion.insertion.vehicleEntry.vehicle.getId(), k -> createTreeSet()).add(requestData);
		}
	}


	void process(double now, Collection<RequestData> requestDataPartition, Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries) {
		this.unplannedRequests.addAll(requestDataPartition);

		while (!unplannedRequests.isEmpty()) {
			findInsertion(unplannedRequests.poll(), vehicleEntries, now);
		}

	}


	public void clean() {
		this.unplannedRequests.clear();
	}
}
