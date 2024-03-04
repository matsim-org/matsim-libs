/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.optimizer.insertion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.matsim.contrib.drt.optimizer.insertion.DefaultUnplannedRequestInserter.NO_INSERTION_FOUND_CAUSE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.*;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.DrtRequestInsertionRetryParams;
import org.matsim.contrib.drt.optimizer.DrtRequestInsertionRetryQueue;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.passenger.DrtOfferAcceptor;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler.PickupDropoffTaskPair;
import org.matsim.contrib.drt.stops.StaticPassengerStopDurationProvider;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.testcases.fakes.FakeLink;
import org.mockito.ArgumentCaptor;

import com.google.common.collect.ImmutableMap;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DefaultUnplannedRequestInserterTest {
	private static final String mode = "DRT_MODE";

	private final DrtRequest request1 = request("r1", "from1", "to1");
	private final AcceptedDrtRequest acceptedDrtRequest1 = AcceptedDrtRequest.createFromOriginalRequest(request1);

	private final EventsManager eventsManager = mock(EventsManager.class);

	@RegisterExtension
	public final ForkJoinPoolExtension forkJoinPoolExtension = new ForkJoinPoolExtension();

	@Test
	void nothingToSchedule() {
		var fleet = fleet(vehicle("1"));
		var unplannedRequests = requests();
		double now = 15;
		VehicleEntry.EntryFactory entryFactory = null;//should not be used
		DrtRequestInsertionRetryQueue retryQueue = new DrtRequestInsertionRetryQueue(
				new DrtRequestInsertionRetryParams());//retry OFF, empty queue
		DrtInsertionSearch insertionSearch = null;//should not be used
		RequestInsertionScheduler insertionScheduler = null;//should not be used

		newInserter(fleet, now, entryFactory, retryQueue, insertionSearch,
				insertionScheduler).scheduleUnplannedRequests(unplannedRequests);
	}

	@Test
	void notScheduled_rejected() {
		var fleet = fleet();//no vehicles -> impossible to schedule
		var unplannedRequests = requests(request1);
		double now = 15;
		VehicleEntry.EntryFactory entryFactory = null;//should not be used
		DrtRequestInsertionRetryQueue retryQueue = new DrtRequestInsertionRetryQueue(
				new DrtRequestInsertionRetryParams());//retry OFF, empty queue
		DrtInsertionSearch insertionSearch = //
				(drtRequest, vEntries) -> drtRequest == request1 && vEntries.isEmpty() ?
						Optional.empty() :
						fail("Undefined");
		RequestInsertionScheduler insertionScheduler = null;//should not be used

		//call insertion
		newInserter(fleet, now, entryFactory, retryQueue, insertionSearch,
				insertionScheduler).scheduleUnplannedRequests(unplannedRequests);

		//request is removed from unplanned requests
		assertThat(unplannedRequests).isEmpty();

		//make sure the retry queue is empty (retry is OFF)
		assertThat(retryQueue.getRequestsToRetryNow(Double.POSITIVE_INFINITY)).isEmpty();

		//ensure rejection event is emitted
		ArgumentCaptor<PassengerRequestRejectedEvent> captor = ArgumentCaptor.forClass(
				PassengerRequestRejectedEvent.class);
		verify(eventsManager, times(1)).processEvent(captor.capture());
		assertThat(captor.getValue()).isEqualToComparingFieldByField(
				new PassengerRequestRejectedEvent(now, mode, request1.getId(), request1.getPassengerIds(),
						NO_INSERTION_FOUND_CAUSE));
	}

	@Test
	void notScheduled_addedToRetry() {
		var fleet = fleet();//no vehicles -> impossible to schedule
		var unplannedRequests = requests(request1);
		double now = 15;
		int retryInterval = 10;
		VehicleEntry.EntryFactory entryFactory = null;//should not be used
		var drtRequestInsertionRetryParams = new DrtRequestInsertionRetryParams();
		drtRequestInsertionRetryParams.maxRequestAge = Double.POSITIVE_INFINITY;
		drtRequestInsertionRetryParams.retryInterval = retryInterval;
		DrtRequestInsertionRetryQueue retryQueue = new DrtRequestInsertionRetryQueue(
				drtRequestInsertionRetryParams);//retry ON, empty queue
		DrtInsertionSearch insertionSearch = //
				(drtRequest, vEntries) -> drtRequest == request1 && vEntries.isEmpty() ?
						Optional.empty() :
						fail("Undefined");
		RequestInsertionScheduler insertionScheduler = null;//should not be used

		//call insertion
		newInserter(fleet, now, entryFactory, retryQueue, insertionSearch,
				insertionScheduler).scheduleUnplannedRequests(unplannedRequests);

		//request is removed from unplanned requests
		assertThat(unplannedRequests).isEmpty();

		//make sure the retry queue contains an updated copy of request1 pending retry at time 25 (retry is ON)
		assertThat(retryQueue.getRequestsToRetryNow(now + retryInterval - 1)).isEmpty();
		assertThat(retryQueue.getRequestsToRetryNow(now + retryInterval)).usingRecursiveFieldByFieldElementComparator()
				.containsExactly(DrtRequest.newBuilder(request1)
						.latestStartTime(request1.getLatestStartTime() + retryInterval)
						.latestArrivalTime(request1.getLatestArrivalTime() + retryInterval)
						.build());

		//ensure rejection event is NOT emitted
		verify(eventsManager, times(0)).processEvent(any());
	}

	@Test
	void firstRetryOldRequest_thenHandleNewRequest() {
		var fleet = fleet();//no vehicles -> impossible to schedule
		var unplannedRequests = requests(request1);
		double now = 15;
		int retryInterval = 10;
		VehicleEntry.EntryFactory entryFactory = null;//should not be used

		var drtRequestInsertionRetryParams = new DrtRequestInsertionRetryParams();
		drtRequestInsertionRetryParams.maxRequestAge = Double.POSITIVE_INFINITY;
		drtRequestInsertionRetryParams.retryInterval = retryInterval;
		DrtRequestInsertionRetryQueue retryQueue = new DrtRequestInsertionRetryQueue(
				drtRequestInsertionRetryParams);//retry ON
		var oldRequest = request("r0", "from0", "to0");
		retryQueue.tryAddFailedRequest(oldRequest, now - retryInterval);// will be retried at time 15
		DrtInsertionSearch insertionSearch = (drtRequest, vEntries) -> Optional.empty();

		RequestInsertionScheduler insertionScheduler = null;//should not be used

		//call insertion
		newInserter(fleet, now, entryFactory, retryQueue, insertionSearch,
				insertionScheduler).scheduleUnplannedRequests(unplannedRequests);

		//request is removed from unplanned requests
		assertThat(unplannedRequests).isEmpty();

		//make sure the retry queue contains an updated copy of request1 pending retry at time 25 (retry is ON)
		assertThat(retryQueue.getRequestsToRetryNow(now + retryInterval - 1)).isEmpty();
		assertThat(retryQueue.getRequestsToRetryNow(now + retryInterval)).usingElementComparatorIgnoringFields(
				"latestStartTime", "latestArrivalTime").containsExactly(oldRequest, request1);

		//ensure rejection event is NOT emitted
		verify(eventsManager, times(0)).processEvent(any());
	}

	@Test
	void acceptedRequest() {
		var vehicle1 = vehicle("1");
		var fleet = fleet(vehicle1);
		var unplannedRequests = requests(request1);
		double now = 15;

		var vehicle1Entry = new VehicleEntry(vehicle1, null, null, null, null, 0);
		var createEntryCounter = new MutableInt();
		VehicleEntry.EntryFactory entryFactory = (vehicle, currentTime) -> {
			//make sure the right arguments are passed
			assertThat(vehicle).isSameAs(vehicle1);
			assertThat(currentTime).isEqualTo(now);
			createEntryCounter.increment();
			return vehicle1Entry;
		};

		DrtRequestInsertionRetryQueue retryQueue = new DrtRequestInsertionRetryQueue(
				new DrtRequestInsertionRetryParams());//retry OFF, empty queue

		DrtInsertionSearch insertionSearch = (drtRequest, vEntries) -> drtRequest == request1 ?
				Optional.of(new InsertionWithDetourData(
						new InsertionGenerator.Insertion(vEntries.iterator().next(), null, null), null,
						new InsertionDetourTimeCalculator.DetourTimeInfo(
								mock(InsertionDetourTimeCalculator.PickupDetourInfo.class),
								mock(InsertionDetourTimeCalculator.DropoffDetourInfo.class)))) :
				fail("request1 expected");

		double pickupEndTime = now + 20;
		double dropoffBeginTime = now + 40;
		RequestInsertionScheduler insertionScheduler = (request, insertion) -> {
			var pickupTask = new DefaultDrtStopTask(pickupEndTime - 10, pickupEndTime, request1.getFromLink());
			pickupTask.addPickupRequest(acceptedDrtRequest1);
			var dropoffTask = new DefaultDrtStopTask(dropoffBeginTime, dropoffBeginTime + 10, request1.getToLink());
			dropoffTask.addPickupRequest(acceptedDrtRequest1);
			return new PickupDropoffTaskPair(pickupTask, dropoffTask);
		};

		//call insertion
		newInserter(fleet, now, entryFactory, retryQueue, insertionSearch,
				insertionScheduler).scheduleUnplannedRequests(unplannedRequests);

		//request is removed from unplanned requests
		assertThat(unplannedRequests).isEmpty();

		//make sure the retry queue is empty (retry is OFF)
		assertThat(retryQueue.getRequestsToRetryNow(Double.POSITIVE_INFINITY)).isEmpty();

		//ensure request scheduled event is emitted
		ArgumentCaptor<PassengerRequestScheduledEvent> captor = ArgumentCaptor.forClass(
				PassengerRequestScheduledEvent.class);
		verify(eventsManager, times(1)).processEvent(captor.capture());
		assertThat(captor.getValue()).isEqualToComparingFieldByField(
				new PassengerRequestScheduledEvent(now, mode, request1.getId(), request1.getPassengerIds(),
						vehicle1.getId(), pickupEndTime, dropoffBeginTime));

		//vehicle entry was created twice:
		// 1 - via constructing vehicle entries (before scheduling)
		// 2 - via updating vehicle entries (after scheduling)
		assertThat(createEntryCounter.getValue()).isEqualTo(2);
	}

	private Collection<DrtRequest> requests(DrtRequest... requests) {
		return new ArrayList<>(Arrays.asList(requests));//returned collection needs to be modifiable
	}

	private Fleet fleet(DvrpVehicle... vehicles) {
		var map = Arrays.stream(vehicles).collect(ImmutableMap.toImmutableMap(Identifiable::getId, v -> v));
		return () -> map;
	}

	private DvrpVehicle vehicle(String vehicleId) {
		var id = Id.create(vehicleId, DvrpVehicle.class);
		var vehicle = mock(DvrpVehicle.class);
		when(vehicle.getId()).thenReturn(id);
		return vehicle;
	}

	private DrtRequest request(String id, String fromLinkId, String toLinkId) {
		return DrtRequest.newBuilder()
				.id(Id.create(id, Request.class))
				.passengerIds(List.of(Id.createPersonId(id)))
				.fromLink(link(fromLinkId))
				.toLink(link(toLinkId))
				.mode(mode)
				.build();
	}

	private DefaultUnplannedRequestInserter newInserter(Fleet fleet, double now,
			VehicleEntry.EntryFactory vehicleEntryFactory, DrtRequestInsertionRetryQueue insertionRetryQueue,
			DrtInsertionSearch insertionSearch, RequestInsertionScheduler insertionScheduler) {
		return new DefaultUnplannedRequestInserter(mode, fleet, () -> now, eventsManager, insertionScheduler,
				vehicleEntryFactory, insertionRetryQueue, insertionSearch, DrtOfferAcceptor.DEFAULT_ACCEPTOR,
				forkJoinPoolExtension.forkJoinPool, StaticPassengerStopDurationProvider.of(10.0, 0.0));
	}

	private Link link(String id) {
		return new FakeLink(Id.createLinkId(id));
	}
}
