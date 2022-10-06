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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.function.DoubleSupplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.optimizer.DrtRequestInsertionRetryQueue;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.passenger.DrtOfferAcceptor;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author michalm
 */
public class DefaultUnplannedRequestInserter implements UnplannedRequestInserter {
	private static final Logger log = LogManager.getLogger(DefaultUnplannedRequestInserter.class);
	public static final String NO_INSERTION_FOUND_CAUSE = "no_insertion_found";

	private final String mode;
	private final Fleet fleet;
	private final DoubleSupplier timeOfDay;
	private final EventsManager eventsManager;
	private final RequestInsertionScheduler insertionScheduler;
	private final VehicleEntry.EntryFactory vehicleEntryFactory;
	private final DrtInsertionSearch insertionSearch;
	private final DrtRequestInsertionRetryQueue insertionRetryQueue;
	private final DrtOfferAcceptor drtOfferAcceptor;
	private final ForkJoinPool forkJoinPool;

	public DefaultUnplannedRequestInserter(DrtConfigGroup drtCfg, Fleet fleet, MobsimTimer mobsimTimer,
			EventsManager eventsManager, RequestInsertionScheduler insertionScheduler,
			VehicleEntry.EntryFactory vehicleEntryFactory, DrtInsertionSearch insertionSearch,
			DrtRequestInsertionRetryQueue insertionRetryQueue, DrtOfferAcceptor drtOfferAcceptor,
			ForkJoinPool forkJoinPool) {
		this(drtCfg.getMode(), fleet, mobsimTimer::getTimeOfDay, eventsManager, insertionScheduler, vehicleEntryFactory,
				insertionRetryQueue, insertionSearch, drtOfferAcceptor, forkJoinPool);
	}

	@VisibleForTesting
	DefaultUnplannedRequestInserter(String mode, Fleet fleet, DoubleSupplier timeOfDay, EventsManager eventsManager,
			RequestInsertionScheduler insertionScheduler, VehicleEntry.EntryFactory vehicleEntryFactory,
			DrtRequestInsertionRetryQueue insertionRetryQueue, DrtInsertionSearch insertionSearch,
			DrtOfferAcceptor drtOfferAcceptor, ForkJoinPool forkJoinPool) {
		this.mode = mode;
		this.fleet = fleet;
		this.timeOfDay = timeOfDay;
		this.eventsManager = eventsManager;
		this.insertionScheduler = insertionScheduler;
		this.vehicleEntryFactory = vehicleEntryFactory;
		this.insertionRetryQueue = insertionRetryQueue;
		this.insertionSearch = insertionSearch;
		this.drtOfferAcceptor = drtOfferAcceptor;
		this.forkJoinPool = forkJoinPool;
	}

	@Override
	public void scheduleUnplannedRequests(Collection<DrtRequest> unplannedRequests) {
		double now = timeOfDay.getAsDouble();

		List<DrtRequest> requestsToRetry = insertionRetryQueue.getRequestsToRetryNow(now);
		if (unplannedRequests.isEmpty() && requestsToRetry.isEmpty()) {
			return;
		}

		var vehicleEntries = forkJoinPool.submit(() -> fleet.getVehicles()
				.values()
				.parallelStream()
				.map(v -> vehicleEntryFactory.create(v, now))
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(e -> e.vehicle.getId(), e -> e))).join();

		//first retry scheduling old requests
		requestsToRetry.forEach(req -> scheduleUnplannedRequest(req, vehicleEntries, now));

		//then schedule new requests
		for (var reqIter = unplannedRequests.iterator(); reqIter.hasNext(); ) {
			scheduleUnplannedRequest(reqIter.next(), vehicleEntries, now);
			reqIter.remove();
		}
	}

	private void scheduleUnplannedRequest(DrtRequest req, Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries,
			double now) {
		Optional<InsertionWithDetourData> best = insertionSearch.findBestInsertion(req,
				Collections.unmodifiableCollection(vehicleEntries.values()));
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
			InsertionWithDetourData insertion = best.get();

			// accept offered drt ride
			var acceptedRequest = drtOfferAcceptor.acceptDrtOffer(req,
					insertion.detourTimeInfo.pickupDetourInfo.departureTime,
					insertion.detourTimeInfo.dropoffDetourInfo.arrivalTime);

			var vehicle = insertion.insertion.vehicleEntry.vehicle;
			var pickupDropoffTaskPair = insertionScheduler.scheduleRequest(acceptedRequest.get(), insertion);

			VehicleEntry newVehicleEntry = vehicleEntryFactory.create(vehicle, now);
			if (newVehicleEntry != null) {
				vehicleEntries.put(vehicle.getId(), newVehicleEntry);
			} else {
				vehicleEntries.remove(vehicle.getId());
			}

			eventsManager.processEvent(
					new PassengerRequestScheduledEvent(now, mode, req.getId(), req.getPassengerId(), vehicle.getId(),
							pickupDropoffTaskPair.pickupTask.getEndTime(),
							pickupDropoffTaskPair.dropoffTask.getBeginTime()));
		}
	}
}
