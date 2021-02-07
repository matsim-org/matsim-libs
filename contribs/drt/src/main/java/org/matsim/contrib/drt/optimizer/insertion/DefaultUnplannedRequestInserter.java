/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.function.DoubleSupplier;

import org.apache.log4j.Logger;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author michalm
 */
public class DefaultUnplannedRequestInserter implements UnplannedRequestInserter {
	private static final Logger log = Logger.getLogger(DefaultUnplannedRequestInserter.class);
	public static final String NO_INSERTION_FOUND_CAUSE = "no_insertion_found";

	private final String mode;
	private final Fleet fleet;
	private final DoubleSupplier timeOfDay;
	private final EventsManager eventsManager;
	private final RequestInsertionScheduler insertionScheduler;
	private final VehicleData.EntryFactory vehicleDataEntryFactory;
	private final DrtRequestInsertionRetryQueue insertionRetryQueue;

	private final ForkJoinPool forkJoinPool;
	private final DrtInsertionSearch<PathData> insertionSearch;

	public DefaultUnplannedRequestInserter(DrtConfigGroup drtCfg, Fleet fleet, MobsimTimer mobsimTimer,
			EventsManager eventsManager, RequestInsertionScheduler insertionScheduler,
			VehicleData.EntryFactory vehicleDataEntryFactory, DrtInsertionSearch<PathData> insertionSearch,
			ForkJoinPool forkJoinPool) {
		this(drtCfg.getMode(), fleet, mobsimTimer::getTimeOfDay, eventsManager, insertionScheduler,
				vehicleDataEntryFactory, new DrtRequestInsertionRetryQueue(drtCfg.getDrtRequestInsertionRetryParams().
						orElse(new DrtRequestInsertionRetryParams())), forkJoinPool, insertionSearch);
	}

	@VisibleForTesting
	DefaultUnplannedRequestInserter(String mode, Fleet fleet, DoubleSupplier timeOfDay, EventsManager eventsManager,
			RequestInsertionScheduler insertionScheduler, VehicleData.EntryFactory vehicleDataEntryFactory,
			DrtRequestInsertionRetryQueue insertionRetryQueue, ForkJoinPool forkJoinPool,
			DrtInsertionSearch<PathData> insertionSearch) {
		this.mode = mode;
		this.fleet = fleet;
		this.timeOfDay = timeOfDay;
		this.eventsManager = eventsManager;
		this.insertionScheduler = insertionScheduler;
		this.vehicleDataEntryFactory = vehicleDataEntryFactory;
		this.insertionRetryQueue = insertionRetryQueue;
		this.forkJoinPool = forkJoinPool;
		this.insertionSearch = insertionSearch;
	}

	@Override
	public void scheduleUnplannedRequests(Collection<DrtRequest> unplannedRequests) {
		double now = timeOfDay.getAsDouble();

		List<DrtRequest> requestsToRetry = insertionRetryQueue.getRequestsToRetryNow(now);
		if (unplannedRequests.isEmpty() && requestsToRetry.isEmpty()) {
			return;
		}

		VehicleData vData = new VehicleData(now, fleet.getVehicles().values().stream(), vehicleDataEntryFactory,
				forkJoinPool);

		//first retry scheduling old requests
		requestsToRetry.forEach(req -> scheduleUnplannedRequest(req, vData, now));

		//then schedule new requests
		for (var reqIter = unplannedRequests.iterator(); reqIter.hasNext(); ) {
			scheduleUnplannedRequest(reqIter.next(), vData, now);
			reqIter.remove();
		}
	}

	private void scheduleUnplannedRequest(DrtRequest req, VehicleData vData, double now) {
		Optional<InsertionWithDetourData<PathData>> best = insertionSearch.findBestInsertion(req, vData.getEntries());
		if (best.isEmpty()) {
			if (!insertionRetryQueue.tryAddFailedRequest(req, now)) {
				eventsManager.processEvent(
						new PassengerRequestRejectedEvent(now, mode, req.getId(), req.getPassengerId(),
								NO_INSERTION_FOUND_CAUSE));
				log.debug("No insertion found for drt request "
						+ req
						+ " from passenger id="
						+ req.getPassengerId()
						+ " fromLinkId="
						+ req.getFromLink().getId());
			}
		} else {
			InsertionWithDetourData<PathData> insertion = best.get();
			var pickupDropoffTaskPair = insertionScheduler.scheduleRequest(req, insertion);
			vData.updateEntry(insertion.getVehicleEntry().vehicle);
			eventsManager.processEvent(new PassengerRequestScheduledEvent(now, mode, req.getId(), req.getPassengerId(),
					insertion.getVehicleEntry().vehicle.getId(), pickupDropoffTaskPair.pickupTask.getEndTime(),
					pickupDropoffTaskPair.dropoffTask.getBeginTime()));
		}
	}
}
