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
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
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
	public static final String OFFER_REJECTED_CAUSE = "offer_rejected";

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
	private final PassengerStopDurationProvider stopDurationProvider;
	private final RequestFleetFilter requestFleetFilter;

	public DefaultUnplannedRequestInserter(DrtConfigGroup drtCfg, Fleet fleet, MobsimTimer mobsimTimer,
                                           EventsManager eventsManager, RequestInsertionScheduler insertionScheduler,
                                           VehicleEntry.EntryFactory vehicleEntryFactory, DrtInsertionSearch insertionSearch,
                                           DrtRequestInsertionRetryQueue insertionRetryQueue, DrtOfferAcceptor drtOfferAcceptor,
                                           ForkJoinPool forkJoinPool, PassengerStopDurationProvider stopDurationProvider, RequestFleetFilter requestFleetFilter) {
		this(drtCfg.getMode(), fleet, mobsimTimer::getTimeOfDay, eventsManager, insertionScheduler, vehicleEntryFactory,
				insertionRetryQueue, insertionSearch, drtOfferAcceptor, forkJoinPool, stopDurationProvider, requestFleetFilter);
	}

	@VisibleForTesting
	DefaultUnplannedRequestInserter(String mode, Fleet fleet, DoubleSupplier timeOfDay, EventsManager eventsManager,
                                    RequestInsertionScheduler insertionScheduler, VehicleEntry.EntryFactory vehicleEntryFactory,
                                    DrtRequestInsertionRetryQueue insertionRetryQueue, DrtInsertionSearch insertionSearch,
                                    DrtOfferAcceptor drtOfferAcceptor, ForkJoinPool forkJoinPool, PassengerStopDurationProvider stopDurationProvider, RequestFleetFilter requestFleetFilter) {
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
		this.stopDurationProvider = stopDurationProvider;
        this.requestFleetFilter = requestFleetFilter;
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
		Collection<VehicleEntry> filteredFleet = requestFleetFilter.filter(req, vehicleEntries, now);
		Optional<InsertionWithDetourData> best = insertionSearch.findBestInsertion(req,
				Collections.unmodifiableCollection(filteredFleet));
		if (best.isEmpty()) {
			retryOrReject(req, now, NO_INSERTION_FOUND_CAUSE);
		} else {
			InsertionWithDetourData insertion = best.get();

			// accept offered drt ride
			var acceptedRequest = drtOfferAcceptor.acceptDrtOffer(req,
					insertion.detourTimeInfo.pickupDetourInfo.departureTime,
					insertion.detourTimeInfo.dropoffDetourInfo.arrivalTime);

			if(acceptedRequest.isPresent()) {
				var vehicle = insertion.insertion.vehicleEntry.vehicle;
				var pickupDropoffTaskPair = insertionScheduler.scheduleRequest(acceptedRequest.get(), insertion);

				VehicleEntry newVehicleEntry = vehicleEntryFactory.create(vehicle, now);
				if (newVehicleEntry != null) {
					vehicleEntries.put(vehicle.getId(), newVehicleEntry);
				} else {
					vehicleEntries.remove(vehicle.getId());
				}

				double expectedPickupTime = pickupDropoffTaskPair.pickupTask.getBeginTime();
				expectedPickupTime = Math.max(expectedPickupTime, acceptedRequest.get().getEarliestStartTime());
				expectedPickupTime += stopDurationProvider.calcPickupDuration(vehicle, req);

				double expectedDropoffTime = pickupDropoffTaskPair.dropoffTask.getBeginTime();
				expectedDropoffTime += stopDurationProvider.calcDropoffDuration(vehicle, req);

				eventsManager.processEvent(
						new PassengerRequestScheduledEvent(now, mode, req.getId(), req.getPassengerIds(), vehicle.getId(),
								expectedPickupTime, expectedDropoffTime));
			} else {
				retryOrReject(req, now, OFFER_REJECTED_CAUSE);
			}
		}
	}

	private void retryOrReject(DrtRequest req, double now, String cause) {
		if (!insertionRetryQueue.tryAddFailedRequest(req, now)) {
			eventsManager.processEvent(
					new PassengerRequestRejectedEvent(now, mode, req.getId(), req.getPassengerIds(),
							cause));
			log.debug("No insertion found for drt request "
					+ req
					+ " with passenger ids="
					+ req.getPassengerIds().stream().map(Object::toString).collect(Collectors.joining(","))
					+ " fromLinkId="
					+ req.getFromLink().getId());
		}
	}
}
