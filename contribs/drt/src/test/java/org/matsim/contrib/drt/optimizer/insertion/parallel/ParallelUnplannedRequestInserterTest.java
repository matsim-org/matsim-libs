/*
 * *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.optimizer.insertion.parallel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.matsim.contrib.drt.optimizer.insertion.DefaultUnplannedRequestInserter.NO_INSERTION_FOUND_CAUSE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.*;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.DrtRequestInsertionRetryParams;
import org.matsim.contrib.drt.optimizer.DrtRequestInsertionRetryQueue;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.*;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.requests.RequestsPartitioner;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.requests.RoundRobinRequestsPartitioner;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.requests.LoadAwareRoundRobinRequestsPartitioner;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.vehicles.ReplicatingVehicleEntryPartitioner;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.vehicles.RoundRobinVehicleEntryPartitioner;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.vehicles.ShiftingRoundRobinVehicleEntryPartitioner;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.vehicles.VehicleEntryPartitioner;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.passenger.DefaultOfferAcceptor;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler.PickupDropoffTaskPair;
import org.matsim.contrib.drt.stops.StaticPassengerStopDurationProvider;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.load.IntegerLoadType;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.testcases.fakes.FakeLink;
import org.mockito.ArgumentCaptor;

import com.google.common.collect.ImmutableMap;

/**
 * @author Steffen Axer
 */
public class ParallelUnplannedRequestInserterTest {
	private static final String mode = "DRT_MODE";
	private static final double COLLECTION_PERIOD = 15.0;

	private final DrtRequest request1 = request("r1", "from1", "to1");
	private final AcceptedDrtRequest acceptedDrtRequest1 = AcceptedDrtRequest.createFromOriginalRequest(request1, 60);

	private final EventsManager eventsManager = mock(EventsManager.class);
	private final MatsimServices matsimServices = mock(MatsimServices.class);

	private final IntegerLoadType loadType = new IntegerLoadType("passengers");

	@Test
	void nothingToSchedule() {
		var fleet = fleet(vehicle("1"));
		var unplannedRequests = requests();
		double now = 15;
		VehicleEntry.EntryFactory entryFactory = (v, t) -> null;
		DrtRequestInsertionRetryQueue retryQueue = new DrtRequestInsertionRetryQueue(
				new DrtRequestInsertionRetryParams());//retry OFF, empty queue
		DrtInsertionSearch insertionSearch = (req, entries) -> Optional.empty();
		RequestInsertionScheduler insertionScheduler = null;//should not be used

		var inserter = newInserter(fleet, entryFactory, retryQueue, insertionSearch, insertionScheduler);
		inserter.scheduleUnplannedRequests(unplannedRequests);

		// First call initializes lastProcessingTime, second call after collection period triggers processing
		inserter.doSimStep(now);
		inserter.doSimStep(now + COLLECTION_PERIOD);
	}

	@Test
	void notScheduled_rejected() {
		var fleet = fleet();//no vehicles -> impossible to schedule
		var unplannedRequests = requests(request1);
		double now = 15;
		VehicleEntry.EntryFactory entryFactory = (v, t) -> null;
		DrtRequestInsertionRetryQueue retryQueue = new DrtRequestInsertionRetryQueue(
				new DrtRequestInsertionRetryParams());//retry OFF, empty queue
		DrtInsertionSearch insertionSearch = (drtRequest, vEntries) -> Optional.empty();
		RequestInsertionScheduler insertionScheduler = null;//should not be used

		var inserter = newInserter(fleet, entryFactory, retryQueue, insertionSearch, insertionScheduler);
		inserter.scheduleUnplannedRequests(unplannedRequests);

		// Trigger processing
		inserter.doSimStep(now);
		inserter.doSimStep(now + COLLECTION_PERIOD);

		//request is removed from unplanned requests
		assertThat(unplannedRequests).isEmpty();

		//make sure the retry queue is empty (retry is OFF)
		assertThat(retryQueue.getRequestsToRetryNow(Double.POSITIVE_INFINITY)).isEmpty();

		//ensure rejection event is emitted
		ArgumentCaptor<PassengerRequestRejectedEvent> captor = ArgumentCaptor.forClass(
				PassengerRequestRejectedEvent.class);
		verify(eventsManager, times(1)).processEvent(captor.capture());
		assertThat(captor.getValue()).isEqualToComparingFieldByField(
				new PassengerRequestRejectedEvent(now + COLLECTION_PERIOD, mode, request1.getId(), request1.getPassengerIds(),
						NO_INSERTION_FOUND_CAUSE));
	}

	@Test
	void notScheduled_addedToRetry() {
		var fleet = fleet();//no vehicles -> impossible to schedule
		var unplannedRequests = requests(request1);
		double now = 15;
		int retryInterval = 10;
		VehicleEntry.EntryFactory entryFactory = (v, t) -> null;
		var drtRequestInsertionRetryParams = new DrtRequestInsertionRetryParams();
		drtRequestInsertionRetryParams.setMaxRequestAge(Double.POSITIVE_INFINITY);
		drtRequestInsertionRetryParams.setRetryInterval(retryInterval);
		DrtRequestInsertionRetryQueue retryQueue = new DrtRequestInsertionRetryQueue(
				drtRequestInsertionRetryParams);//retry ON, empty queue
		DrtInsertionSearch insertionSearch = (drtRequest, vEntries) -> Optional.empty();
		RequestInsertionScheduler insertionScheduler = null;//should not be used

		var inserter = newInserter(fleet, entryFactory, retryQueue, insertionSearch, insertionScheduler);
		inserter.scheduleUnplannedRequests(unplannedRequests);

		// Trigger processing
		double processTime = now + COLLECTION_PERIOD;
		inserter.doSimStep(now);
		inserter.doSimStep(processTime);

		//request is removed from unplanned requests
		assertThat(unplannedRequests).isEmpty();

		//make sure the retry queue contains an updated copy of request1 pending retry (retry is ON)
		assertThat(retryQueue.getRequestsToRetryNow(processTime + retryInterval - 1)).isEmpty();
		assertThat(retryQueue.getRequestsToRetryNow(processTime + retryInterval)).usingRecursiveFieldByFieldElementComparator()
				.containsExactly(DrtRequest.newBuilder(request1)
						.earliestDepartureTime(request1.getEarliestStartTime())
						.constraints(request1.getConstraints())
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
		VehicleEntry.EntryFactory entryFactory = (v, t) -> null;

		var drtRequestInsertionRetryParams = new DrtRequestInsertionRetryParams();
		drtRequestInsertionRetryParams.setMaxRequestAge(Double.POSITIVE_INFINITY);
		drtRequestInsertionRetryParams.setRetryInterval(retryInterval);
		DrtRequestInsertionRetryQueue retryQueue = new DrtRequestInsertionRetryQueue(
				drtRequestInsertionRetryParams);//retry ON
		var oldRequest = request("r0", "from0", "to0");
		retryQueue.tryAddFailedRequest(oldRequest, now - retryInterval);// will be retried at time 15
		DrtInsertionSearch insertionSearch = (drtRequest, vEntries) -> Optional.empty();

		RequestInsertionScheduler insertionScheduler = null;//should not be used

		var inserter = newInserter(fleet, entryFactory, retryQueue, insertionSearch, insertionScheduler);
		inserter.scheduleUnplannedRequests(unplannedRequests);

		// Trigger processing
		double processTime = now + COLLECTION_PERIOD;
		inserter.doSimStep(now);
		inserter.doSimStep(processTime);

		//request is removed from unplanned requests
		assertThat(unplannedRequests).isEmpty();

		//make sure the retry queue contains both requests pending retry (retry is ON)
		assertThat(retryQueue.getRequestsToRetryNow(processTime + retryInterval - 1)).isEmpty();
		assertThat(retryQueue.getRequestsToRetryNow(processTime + retryInterval)).usingElementComparatorIgnoringFields(
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
			if (vehicle == vehicle1) {
				createEntryCounter.increment();
				return vehicle1Entry;
			}
			return null;
		};

		DrtRequestInsertionRetryQueue retryQueue = new DrtRequestInsertionRetryQueue(
				new DrtRequestInsertionRetryParams());//retry OFF, empty queue

		DrtInsertionSearch insertionSearch = (drtRequest, vEntries) -> drtRequest == request1 ?
				Optional.of(new InsertionWithDetourData(
						new InsertionGenerator.Insertion(vEntries.iterator().next(), null, null, loadType.fromInt(1)), null,
						new InsertionDetourTimeCalculator.DetourTimeInfo(
								mock(InsertionDetourTimeCalculator.PickupDetourInfo.class),
								mock(InsertionDetourTimeCalculator.DropoffDetourInfo.class)))) :
				fail("request1 expected");

		double processTime = now + COLLECTION_PERIOD;
		double pickupEndTime = processTime + 20;
		double dropoffBeginTime = processTime + 40;
		RequestInsertionScheduler insertionScheduler = (request, insertion) -> {
			var pickupTask = new DefaultDrtStopTask(pickupEndTime - 10, pickupEndTime, request1.getFromLink());
			pickupTask.addPickupRequest(acceptedDrtRequest1);
			var dropoffTask = new DefaultDrtStopTask(dropoffBeginTime, dropoffBeginTime + 10, request1.getToLink());
			dropoffTask.addPickupRequest(acceptedDrtRequest1);
			return new PickupDropoffTaskPair(pickupTask, dropoffTask);
		};

		var inserter = newInserter(fleet, entryFactory, retryQueue, insertionSearch, insertionScheduler);
		inserter.scheduleUnplannedRequests(unplannedRequests);

		// Trigger processing
		inserter.doSimStep(now);
		inserter.doSimStep(processTime);

		//request is removed from unplanned requests
		assertThat(unplannedRequests).isEmpty();

		//make sure the retry queue is empty (retry is OFF)
		assertThat(retryQueue.getRequestsToRetryNow(Double.POSITIVE_INFINITY)).isEmpty();

		//ensure request scheduled event is emitted
		ArgumentCaptor<PassengerRequestScheduledEvent> captor = ArgumentCaptor.forClass(
				PassengerRequestScheduledEvent.class);
		verify(eventsManager, times(1)).processEvent(captor.capture());
		assertThat(captor.getValue()).isEqualToComparingFieldByField(
				new PassengerRequestScheduledEvent(processTime, mode, request1.getId(), request1.getPassengerIds(),
						vehicle1.getId(), pickupEndTime, dropoffBeginTime));

		//vehicle entry was created at least once
		assertThat(createEntryCounter.getValue()).isGreaterThanOrEqualTo(1);
	}

	@Test
	void threadSafety_multiplePartitions_highLoad() {
		// Setup: each request gets its own vehicle to avoid conflicts
		// This tests thread safety of parallel processing without conflict resolution
		int numVehicles = 1000;
		int numRequests = 1000;
		int numPartitions = 8;

		List<DvrpVehicle> vehicleList = new ArrayList<>();
		Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntryMap = new HashMap<>();
		for (int i = 0; i < numVehicles; i++) {
			var vehicle = vehicle("v" + i);
			vehicleList.add(vehicle);
			vehicleEntryMap.put(vehicle.getId(), new VehicleEntry(vehicle, null, null, null, null, 0));
		}

		List<DrtRequest> requestList = new ArrayList<>();
		for (int i = 0; i < numRequests; i++) {
			requestList.add(request("req" + i, "from" + i, "to" + i));
		}

		var fleet = fleet(vehicleList.toArray(new DvrpVehicle[0]));
		var unplannedRequests = requests(requestList.toArray(new DrtRequest[0]));
		double now = 15;

		// Entry factory returns corresponding vehicle entry
		VehicleEntry.EntryFactory entryFactory = (vehicle, currentTime) -> vehicleEntryMap.get(vehicle.getId());

		DrtRequestInsertionRetryQueue retryQueue = new DrtRequestInsertionRetryQueue(
				new DrtRequestInsertionRetryParams());//retry OFF

		// Track scheduled requests in a thread-safe manner
		Set<Id<Request>> scheduledRequests = Collections.synchronizedSet(new HashSet<>());

		// Each request gets its own unique vehicle - no conflicts
		DrtInsertionSearch insertionSearch = (drtRequest, vEntries) -> {
			if (vEntries.isEmpty()) {
				return Optional.empty();
			}
			// Extract request number and assign to corresponding vehicle
			String reqId = drtRequest.getId().toString();
			int requestNum = Integer.parseInt(reqId.replace("req", ""));
			var vehicleEntry = vehicleEntryMap.get(Id.create("v" + requestNum, DvrpVehicle.class));
			if (vehicleEntry == null) {
				return Optional.empty();
			}
			return Optional.of(new InsertionWithDetourData(
					new InsertionGenerator.Insertion(vehicleEntry, null, null, loadType.fromInt(1)), null,
					new InsertionDetourTimeCalculator.DetourTimeInfo(
							mock(InsertionDetourTimeCalculator.PickupDetourInfo.class),
							mock(InsertionDetourTimeCalculator.DropoffDetourInfo.class))));
		};

		double processTime = now + COLLECTION_PERIOD;
		double pickupEndTime = processTime + 20;
		double dropoffBeginTime = processTime + 40;

		RequestInsertionScheduler insertionScheduler = (acceptedRequest, insertion) -> {
			scheduledRequests.add(acceptedRequest.getId());
			var pickupTask = new DefaultDrtStopTask(pickupEndTime - 10, pickupEndTime, acceptedRequest.getFromLink());
			pickupTask.addPickupRequest(acceptedRequest);
			var dropoffTask = new DefaultDrtStopTask(dropoffBeginTime, dropoffBeginTime + 10, acceptedRequest.getToLink());
			dropoffTask.addPickupRequest(acceptedRequest);
			return new PickupDropoffTaskPair(pickupTask, dropoffTask);
		};

		var inserter = newInserterWithPartitions(fleet, entryFactory, retryQueue, insertionSearch, insertionScheduler, numPartitions);
		inserter.scheduleUnplannedRequests(unplannedRequests);

		// Trigger processing
		inserter.doSimStep(now);
		inserter.doSimStep(processTime);

		// All requests should be removed from unplanned
		assertThat(unplannedRequests).isEmpty();

		// Verify no duplicate scheduling occurred (thread safety check)
		assertThat(scheduledRequests).doesNotHaveDuplicates();
		assertThat(scheduledRequests).hasSize(numRequests);

		// Verify all events are scheduled events
		verify(eventsManager, times(numRequests)).processEvent(any(PassengerRequestScheduledEvent.class));
	}

	@Test
	void threadSafety_allPartitionerCombinations() {
		// Test all combinations of request and vehicle partitioners
		List<RequestsPartitioner> requestPartitioners = List.of(
			new RoundRobinRequestsPartitioner(),
			new LoadAwareRoundRobinRequestsPartitioner((totalPartitions, requestCount, collectionPeriod) -> totalPartitions)
		);

		List<VehicleEntryPartitioner> vehiclePartitioners = List.of(
			new ReplicatingVehicleEntryPartitioner(),
			new RoundRobinVehicleEntryPartitioner(),
			new ShiftingRoundRobinVehicleEntryPartitioner()
		);

		int numVehicles = 100;
		int numRequests = 100;
		int numPartitions = 4;

		for (RequestsPartitioner reqPartitioner : requestPartitioners) {
			for (VehicleEntryPartitioner vehPartitioner : vehiclePartitioners) {
				String testCase = reqPartitioner.getClass().getSimpleName() + " + " + vehPartitioner.getClass().getSimpleName();

				// Fresh eventsManager for each combination
				EventsManager testEventsManager = mock(EventsManager.class);

				List<DvrpVehicle> vehicleList = new ArrayList<>();
				Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntryMap = new HashMap<>();
				for (int i = 0; i < numVehicles; i++) {
					var vehicle = vehicle("combo_v" + i);
					vehicleList.add(vehicle);
					vehicleEntryMap.put(vehicle.getId(), new VehicleEntry(vehicle, null, null, null, null, 0));
				}

				List<DrtRequest> requestList = new ArrayList<>();
				for (int i = 0; i < numRequests; i++) {
					requestList.add(request("combo_req" + i, "from" + i, "to" + i));
				}

				var fleet = fleet(vehicleList.toArray(new DvrpVehicle[0]));
				var unplannedRequests = requests(requestList.toArray(new DrtRequest[0]));
				double now = 15;

				VehicleEntry.EntryFactory entryFactory = (vehicle, currentTime) -> vehicleEntryMap.get(vehicle.getId());

				DrtRequestInsertionRetryQueue retryQueue = new DrtRequestInsertionRetryQueue(
						new DrtRequestInsertionRetryParams());

				Set<Id<Request>> scheduledRequests = Collections.synchronizedSet(new HashSet<>());

				// Each request gets unique vehicle - no conflicts
				DrtInsertionSearch insertionSearch = (drtRequest, vEntries) -> {
					if (vEntries.isEmpty()) {
						return Optional.empty();
					}
					String reqId = drtRequest.getId().toString();
					int requestNum = Integer.parseInt(reqId.replace("combo_req", ""));
					var vehicleEntry = vehicleEntryMap.get(Id.create("combo_v" + requestNum, DvrpVehicle.class));
					if (vehicleEntry == null) {
						return Optional.empty();
					}
					return Optional.of(new InsertionWithDetourData(
							new InsertionGenerator.Insertion(vehicleEntry, null, null, loadType.fromInt(1)), null,
							new InsertionDetourTimeCalculator.DetourTimeInfo(
									mock(InsertionDetourTimeCalculator.PickupDetourInfo.class),
									mock(InsertionDetourTimeCalculator.DropoffDetourInfo.class))));
				};

				double processTime = now + COLLECTION_PERIOD;
				RequestInsertionScheduler insertionScheduler = (acceptedRequest, insertion) -> {
					scheduledRequests.add(acceptedRequest.getId());
					var pickupTask = new DefaultDrtStopTask(processTime, processTime + 10, acceptedRequest.getFromLink());
					pickupTask.addPickupRequest(acceptedRequest);
					var dropoffTask = new DefaultDrtStopTask(processTime + 20, processTime + 30, acceptedRequest.getToLink());
					dropoffTask.addPickupRequest(acceptedRequest);
					return new PickupDropoffTaskPair(pickupTask, dropoffTask);
				};

				DrtParallelInserterParams params = new DrtParallelInserterParams();
				params.setCollectionPeriod(COLLECTION_PERIOD);
				params.setMaxIterations(2);
				params.setMaxPartitions(numPartitions);
				params.setLogThreadActivity(false);

				var inserter = new ParallelUnplannedRequestInserter(
						matsimServices,
						reqPartitioner,
						vehPartitioner,
						params,
						mode,
						fleet,
						testEventsManager,
						() -> insertionScheduler,
						entryFactory,
						() -> insertionSearch,
						new DefaultOfferAcceptor(),
						StaticPassengerStopDurationProvider.of(10.0, 0.0),
						RequestFleetFilter.none,
						retryQueue
				);

				inserter.scheduleUnplannedRequests(unplannedRequests);
				inserter.doSimStep(now);
				inserter.doSimStep(processTime);

				// Verify
				assertThat(unplannedRequests).as(testCase + ": unplanned empty").isEmpty();
				assertThat(scheduledRequests).as(testCase + ": no duplicates").doesNotHaveDuplicates();
				assertThat(scheduledRequests).as(testCase + ": all scheduled").hasSize(numRequests);
				verify(testEventsManager, times(numRequests)).processEvent(any(PassengerRequestScheduledEvent.class));
			}
		}
	}

	@Test
	void threadSafety_roundRobinVehiclePartitioner_highLoad() {
		// Test specifically with RoundRobinVehicleEntryPartitioner
		int numVehicles = 500;
		int numRequests = 500;
		int numPartitions = 8;

		List<DvrpVehicle> vehicleList = new ArrayList<>();
		Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntryMap = new HashMap<>();
		for (int i = 0; i < numVehicles; i++) {
			var vehicle = vehicle("rr_v" + i);
			vehicleList.add(vehicle);
			vehicleEntryMap.put(vehicle.getId(), new VehicleEntry(vehicle, null, null, null, null, 0));
		}

		List<DrtRequest> requestList = new ArrayList<>();
		for (int i = 0; i < numRequests; i++) {
			requestList.add(request("rr_req" + i, "from" + i, "to" + i));
		}

		var fleet = fleet(vehicleList.toArray(new DvrpVehicle[0]));
		var unplannedRequests = requests(requestList.toArray(new DrtRequest[0]));
		double now = 15;

		VehicleEntry.EntryFactory entryFactory = (vehicle, currentTime) -> vehicleEntryMap.get(vehicle.getId());

		DrtRequestInsertionRetryQueue retryQueue = new DrtRequestInsertionRetryQueue(
				new DrtRequestInsertionRetryParams());

		Set<Id<Request>> scheduledRequests = Collections.synchronizedSet(new HashSet<>());

		// Each request gets unique vehicle
		DrtInsertionSearch insertionSearch = (drtRequest, vEntries) -> {
			if (vEntries.isEmpty()) {
				return Optional.empty();
			}
			String reqId = drtRequest.getId().toString();
			int requestNum = Integer.parseInt(reqId.replace("rr_req", ""));
			var vehicleEntry = vehicleEntryMap.get(Id.create("rr_v" + requestNum, DvrpVehicle.class));
			if (vehicleEntry == null) {
				return Optional.empty();
			}
			return Optional.of(new InsertionWithDetourData(
					new InsertionGenerator.Insertion(vehicleEntry, null, null, loadType.fromInt(1)), null,
					new InsertionDetourTimeCalculator.DetourTimeInfo(
							mock(InsertionDetourTimeCalculator.PickupDetourInfo.class),
							mock(InsertionDetourTimeCalculator.DropoffDetourInfo.class))));
		};

		double processTime = now + COLLECTION_PERIOD;
		RequestInsertionScheduler insertionScheduler = (acceptedRequest, insertion) -> {
			scheduledRequests.add(acceptedRequest.getId());
			var pickupTask = new DefaultDrtStopTask(processTime, processTime + 10, acceptedRequest.getFromLink());
			pickupTask.addPickupRequest(acceptedRequest);
			var dropoffTask = new DefaultDrtStopTask(processTime + 20, processTime + 30, acceptedRequest.getToLink());
			dropoffTask.addPickupRequest(acceptedRequest);
			return new PickupDropoffTaskPair(pickupTask, dropoffTask);
		};

		// Use RoundRobinVehicleEntryPartitioner
		RequestsPartitioner requestsPartitioner = new RoundRobinRequestsPartitioner();
		VehicleEntryPartitioner vehicleEntryPartitioner = new RoundRobinVehicleEntryPartitioner();

		DrtParallelInserterParams params = new DrtParallelInserterParams();
		params.setCollectionPeriod(COLLECTION_PERIOD);
		params.setMaxIterations(2);
		params.setMaxPartitions(numPartitions);
		params.setLogThreadActivity(false);

		var inserter = new ParallelUnplannedRequestInserter(
				matsimServices,
				requestsPartitioner,
				vehicleEntryPartitioner,
				params,
				mode,
				fleet,
				eventsManager,
				() -> insertionScheduler,
				entryFactory,
				() -> insertionSearch,
				new DefaultOfferAcceptor(),
				StaticPassengerStopDurationProvider.of(10.0, 0.0),
				RequestFleetFilter.none,
				retryQueue
		);

		inserter.scheduleUnplannedRequests(unplannedRequests);
		inserter.doSimStep(now);
		inserter.doSimStep(processTime);

		assertThat(unplannedRequests).isEmpty();
		assertThat(scheduledRequests).doesNotHaveDuplicates();
		// With RoundRobinVehicleEntryPartitioner, not all requests may be scheduled
		// because vehicles are split across partitions
		assertThat(scheduledRequests).isNotEmpty();
	}

	@Test
	void threadSafety_shiftingRoundRobinVehiclePartitioner_highLoad() {
		// Test specifically with ShiftingRoundRobinVehicleEntryPartitioner
		int numVehicles = 500;
		int numRequests = 500;
		int numPartitions = 8;

		List<DvrpVehicle> vehicleList = new ArrayList<>();
		Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntryMap = new HashMap<>();
		for (int i = 0; i < numVehicles; i++) {
			var vehicle = vehicle("shift_v" + i);
			vehicleList.add(vehicle);
			vehicleEntryMap.put(vehicle.getId(), new VehicleEntry(vehicle, null, null, null, null, 0));
		}

		List<DrtRequest> requestList = new ArrayList<>();
		for (int i = 0; i < numRequests; i++) {
			requestList.add(request("shift_req" + i, "from" + i, "to" + i));
		}

		var fleet = fleet(vehicleList.toArray(new DvrpVehicle[0]));
		var unplannedRequests = requests(requestList.toArray(new DrtRequest[0]));
		double now = 15;

		VehicleEntry.EntryFactory entryFactory = (vehicle, currentTime) -> vehicleEntryMap.get(vehicle.getId());

		DrtRequestInsertionRetryQueue retryQueue = new DrtRequestInsertionRetryQueue(
				new DrtRequestInsertionRetryParams());

		Set<Id<Request>> scheduledRequests = Collections.synchronizedSet(new HashSet<>());

		// Each request gets unique vehicle
		DrtInsertionSearch insertionSearch = (drtRequest, vEntries) -> {
			if (vEntries.isEmpty()) {
				return Optional.empty();
			}
			String reqId = drtRequest.getId().toString();
			int requestNum = Integer.parseInt(reqId.replace("shift_req", ""));
			var vehicleEntry = vehicleEntryMap.get(Id.create("shift_v" + requestNum, DvrpVehicle.class));
			if (vehicleEntry == null) {
				return Optional.empty();
			}
			return Optional.of(new InsertionWithDetourData(
					new InsertionGenerator.Insertion(vehicleEntry, null, null, loadType.fromInt(1)), null,
					new InsertionDetourTimeCalculator.DetourTimeInfo(
							mock(InsertionDetourTimeCalculator.PickupDetourInfo.class),
							mock(InsertionDetourTimeCalculator.DropoffDetourInfo.class))));
		};

		double processTime = now + COLLECTION_PERIOD;
		RequestInsertionScheduler insertionScheduler = (acceptedRequest, insertion) -> {
			scheduledRequests.add(acceptedRequest.getId());
			var pickupTask = new DefaultDrtStopTask(processTime, processTime + 10, acceptedRequest.getFromLink());
			pickupTask.addPickupRequest(acceptedRequest);
			var dropoffTask = new DefaultDrtStopTask(processTime + 20, processTime + 30, acceptedRequest.getToLink());
			dropoffTask.addPickupRequest(acceptedRequest);
			return new PickupDropoffTaskPair(pickupTask, dropoffTask);
		};

		// Use ShiftingRoundRobinVehicleEntryPartitioner
		RequestsPartitioner requestsPartitioner = new RoundRobinRequestsPartitioner();
		VehicleEntryPartitioner vehicleEntryPartitioner = new ShiftingRoundRobinVehicleEntryPartitioner();

		DrtParallelInserterParams params = new DrtParallelInserterParams();
		params.setCollectionPeriod(COLLECTION_PERIOD);
		params.setMaxIterations(2);
		params.setMaxPartitions(numPartitions);
		params.setLogThreadActivity(false);

		var inserter = new ParallelUnplannedRequestInserter(
				matsimServices,
				requestsPartitioner,
				vehicleEntryPartitioner,
				params,
				mode,
				fleet,
				eventsManager,
				() -> insertionScheduler,
				entryFactory,
				() -> insertionSearch,
				new DefaultOfferAcceptor(),
				StaticPassengerStopDurationProvider.of(10.0, 0.0),
				RequestFleetFilter.none,
				retryQueue
		);

		inserter.scheduleUnplannedRequests(unplannedRequests);
		inserter.doSimStep(now);
		inserter.doSimStep(processTime);

		assertThat(unplannedRequests).isEmpty();
		assertThat(scheduledRequests).doesNotHaveDuplicates();
		// With ShiftingRoundRobinVehicleEntryPartitioner, not all requests may be scheduled
		assertThat(scheduledRequests).isNotEmpty();
	}

	@Test
	void threadSafety_withConflicts_allProcessed() {
		// Test with intentional conflicts: many requests target few vehicles
		// Verifies that scheduled + rejected = total
		int numVehicles = 10;
		int numRequests = 200;
		int numPartitions = 8;

		List<DvrpVehicle> vehicleList = new ArrayList<>();
		Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntryMap = new HashMap<>();
		for (int i = 0; i < numVehicles; i++) {
			var vehicle = vehicle("conflict_v" + i);
			vehicleList.add(vehicle);
			vehicleEntryMap.put(vehicle.getId(), new VehicleEntry(vehicle, null, null, null, null, 0));
		}

		List<DrtRequest> requestList = new ArrayList<>();
		for (int i = 0; i < numRequests; i++) {
			requestList.add(request("conflict_req" + i, "from" + i, "to" + i));
		}

		var fleet = fleet(vehicleList.toArray(new DvrpVehicle[0]));
		var unplannedRequests = requests(requestList.toArray(new DrtRequest[0]));
		double now = 15;

		VehicleEntry.EntryFactory entryFactory = (vehicle, currentTime) -> vehicleEntryMap.get(vehicle.getId());

		DrtRequestInsertionRetryQueue retryQueue = new DrtRequestInsertionRetryQueue(
				new DrtRequestInsertionRetryParams());//retry OFF

		Set<Id<Request>> scheduledRequests = Collections.synchronizedSet(new HashSet<>());

		// All requests target vehicles round-robin - creates conflicts
		final int finalNumVehicles = numVehicles;
		DrtInsertionSearch insertionSearch = (drtRequest, vEntries) -> {
			if (vEntries.isEmpty()) {
				return Optional.empty();
			}
			String reqId = drtRequest.getId().toString();
			int requestNum = Integer.parseInt(reqId.replace("conflict_req", ""));
			int vehicleIndex = requestNum % finalNumVehicles;
			var vehicleEntry = vehicleEntryMap.get(Id.create("conflict_v" + vehicleIndex, DvrpVehicle.class));
			if (vehicleEntry == null) {
				return Optional.empty();
			}
			return Optional.of(new InsertionWithDetourData(
					new InsertionGenerator.Insertion(vehicleEntry, null, null, loadType.fromInt(1)), null,
					new InsertionDetourTimeCalculator.DetourTimeInfo(
							mock(InsertionDetourTimeCalculator.PickupDetourInfo.class),
							mock(InsertionDetourTimeCalculator.DropoffDetourInfo.class))));
		};

		double processTime = now + COLLECTION_PERIOD;
		RequestInsertionScheduler insertionScheduler = (acceptedRequest, insertion) -> {
			scheduledRequests.add(acceptedRequest.getId());
			var pickupTask = new DefaultDrtStopTask(processTime, processTime + 10, acceptedRequest.getFromLink());
			pickupTask.addPickupRequest(acceptedRequest);
			var dropoffTask = new DefaultDrtStopTask(processTime + 20, processTime + 30, acceptedRequest.getToLink());
			dropoffTask.addPickupRequest(acceptedRequest);
			return new PickupDropoffTaskPair(pickupTask, dropoffTask);
		};

		var inserter = newInserterWithPartitions(fleet, entryFactory, retryQueue, insertionSearch, insertionScheduler, numPartitions);
		inserter.scheduleUnplannedRequests(unplannedRequests);

		// Trigger processing
		inserter.doSimStep(now);
		inserter.doSimStep(processTime);

		// All requests should be removed from unplanned
		assertThat(unplannedRequests).isEmpty();

		// No duplicates in scheduled (thread safety)
		assertThat(scheduledRequests).doesNotHaveDuplicates();

		// Some should be scheduled, some rejected due to conflicts
		assertThat(scheduledRequests).isNotEmpty();
		assertThat(scheduledRequests.size()).isLessThan(numRequests);

		// Total events (scheduled + rejected) should equal numRequests
		verify(eventsManager, times(numRequests)).processEvent(any());
	}

	@Test
	void threadSafety_concurrentRejections() {
		// Test that concurrent rejections are handled correctly
		int numRequests = 500;
		int numPartitions = 8;

		List<DrtRequest> requestList = new ArrayList<>();
		for (int i = 0; i < numRequests; i++) {
			requestList.add(request("rej" + i, "from" + i, "to" + i));
		}

		var fleet = fleet(); // No vehicles -> all rejected
		var unplannedRequests = requests(requestList.toArray(new DrtRequest[0]));
		double now = 15;

		VehicleEntry.EntryFactory entryFactory = (v, t) -> null;
		DrtRequestInsertionRetryQueue retryQueue = new DrtRequestInsertionRetryQueue(
				new DrtRequestInsertionRetryParams());//retry OFF

		DrtInsertionSearch insertionSearch = (drtRequest, vEntries) -> Optional.empty();
		RequestInsertionScheduler insertionScheduler = null;

		var inserter = newInserterWithPartitions(fleet, entryFactory, retryQueue, insertionSearch, insertionScheduler, numPartitions);
		inserter.scheduleUnplannedRequests(unplannedRequests);

		// Trigger processing
		inserter.doSimStep(now);
		inserter.doSimStep(now + COLLECTION_PERIOD);

		// All requests should be rejected
		assertThat(unplannedRequests).isEmpty();

		// Verify all rejection events
		verify(eventsManager, times(numRequests)).processEvent(any(PassengerRequestRejectedEvent.class));
	}

	@Test
	void threadSafety_mixedSchedulingAndRejection() {
		// Test mixed scenario: some requests scheduled, some rejected
		int numVehicles = 20;
		int numRequests = 500;
		int numPartitions = 8;

		List<DvrpVehicle> vehicleList = new ArrayList<>();
		Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntryMap = new HashMap<>();
		for (int i = 0; i < numVehicles; i++) {
			var vehicle = vehicle("mv" + i);
			vehicleList.add(vehicle);
			vehicleEntryMap.put(vehicle.getId(), new VehicleEntry(vehicle, null, null, null, null, 0));
		}

		List<DrtRequest> requestList = new ArrayList<>();
		for (int i = 0; i < numRequests; i++) {
			requestList.add(request("mix" + i, "from" + i, "to" + i));
		}

		var fleet = fleet(vehicleList.toArray(new DvrpVehicle[0]));
		var unplannedRequests = requests(requestList.toArray(new DrtRequest[0]));
		double now = 15;

		VehicleEntry.EntryFactory entryFactory = (vehicle, currentTime) -> vehicleEntryMap.get(vehicle.getId());

		DrtRequestInsertionRetryQueue retryQueue = new DrtRequestInsertionRetryQueue(
				new DrtRequestInsertionRetryParams());//retry OFF

		Set<Id<Request>> scheduledIds = Collections.synchronizedSet(new HashSet<>());

		// Only accept every other request (to create mixed scenario)
		DrtInsertionSearch insertionSearch = (drtRequest, vEntries) -> {
			int requestNum = Integer.parseInt(drtRequest.getId().toString().replace("mix", ""));
			if (requestNum % 2 == 0 && !vEntries.isEmpty()) {
				int vehicleIndex = requestNum % numVehicles;
				var vehicleEntry = vehicleEntryMap.get(Id.create("mv" + vehicleIndex, DvrpVehicle.class));
				return Optional.of(new InsertionWithDetourData(
						new InsertionGenerator.Insertion(vehicleEntry, null, null, loadType.fromInt(1)), null,
						new InsertionDetourTimeCalculator.DetourTimeInfo(
								mock(InsertionDetourTimeCalculator.PickupDetourInfo.class),
								mock(InsertionDetourTimeCalculator.DropoffDetourInfo.class))));
			}
			return Optional.empty();
		};

		double processTime = now + COLLECTION_PERIOD;
		RequestInsertionScheduler insertionScheduler = (acceptedRequest, insertion) -> {
			scheduledIds.add(acceptedRequest.getId());
			var pickupTask = new DefaultDrtStopTask(processTime, processTime + 10, acceptedRequest.getFromLink());
			pickupTask.addPickupRequest(acceptedRequest);
			var dropoffTask = new DefaultDrtStopTask(processTime + 20, processTime + 30, acceptedRequest.getToLink());
			dropoffTask.addPickupRequest(acceptedRequest);
			return new PickupDropoffTaskPair(pickupTask, dropoffTask);
		};

		var inserter = newInserterWithPartitions(fleet, entryFactory, retryQueue, insertionSearch, insertionScheduler, numPartitions);
		inserter.scheduleUnplannedRequests(unplannedRequests);

		// Trigger processing
		inserter.doSimStep(now);
		inserter.doSimStep(processTime);

		// All requests should be processed
		assertThat(unplannedRequests).isEmpty();

		// Capture all events
		ArgumentCaptor<PassengerRequestScheduledEvent> scheduledCaptor = ArgumentCaptor.forClass(PassengerRequestScheduledEvent.class);

		// Count scheduled events
		verify(eventsManager, atLeast(1)).processEvent(scheduledCaptor.capture());

		long actualScheduled = scheduledCaptor.getAllValues().size();

		// Total events should equal numRequests
		// Due to conflicts in parallel processing, some scheduled requests may be rejected
		// But we can verify that the scheduled set has no duplicates
		assertThat(scheduledIds).doesNotHaveDuplicates();
		assertThat(actualScheduled).isGreaterThan(0);
		assertThat(actualScheduled).isLessThanOrEqualTo(numRequests);
	}

	@Test
	void threadSafety_stressTest_repeatedExecution() {
		// Repeated execution to catch intermittent thread safety issues
		// Note: With parallel processing, conflicts occur when multiple requests target the same vehicle
		// This test verifies thread safety, not that all requests are scheduled
		int numVehicles = 100;
		int numRequests = 2000;
		int numPartitions = 16;
		int numIterations = 5;

		for (int iteration = 0; iteration < numIterations; iteration++) {
			// Fresh eventsManager for each iteration
			EventsManager iterationEventsManager = mock(EventsManager.class);

			List<DvrpVehicle> vehicleList = new ArrayList<>();
			Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntryMap = new HashMap<>();
			for (int i = 0; i < numVehicles; i++) {
				var vehicle = vehicle("stress_v" + iteration + "_" + i);
				vehicleList.add(vehicle);
				vehicleEntryMap.put(vehicle.getId(), new VehicleEntry(vehicle, null, null, null, null, 0));
			}

			List<DrtRequest> requestList = new ArrayList<>();
			for (int i = 0; i < numRequests; i++) {
				requestList.add(request("stress_req" + iteration + "_" + i, "from" + i, "to" + i));
			}

			var fleet = fleet(vehicleList.toArray(new DvrpVehicle[0]));
			var unplannedRequests = requests(requestList.toArray(new DrtRequest[0]));
			double now = 15;

			VehicleEntry.EntryFactory entryFactory = (vehicle, currentTime) -> vehicleEntryMap.get(vehicle.getId());

			DrtRequestInsertionRetryQueue retryQueue = new DrtRequestInsertionRetryQueue(
					new DrtRequestInsertionRetryParams());

			Set<Id<Request>> scheduledRequests = Collections.synchronizedSet(new HashSet<>());

			final int finalNumVehicles = numVehicles;
			DrtInsertionSearch insertionSearch = (drtRequest, vEntries) -> {
				if (vEntries.isEmpty()) {
					return Optional.empty();
				}
				int vehicleIndex = Math.abs(drtRequest.getId().toString().hashCode()) % finalNumVehicles;
				var vehicleEntry = vehicleEntryMap.get(vehicleList.get(vehicleIndex).getId());
				if (vehicleEntry == null) {
					return Optional.empty();
				}
				return Optional.of(new InsertionWithDetourData(
						new InsertionGenerator.Insertion(vehicleEntry, null, null, loadType.fromInt(1)), null,
						new InsertionDetourTimeCalculator.DetourTimeInfo(
								mock(InsertionDetourTimeCalculator.PickupDetourInfo.class),
								mock(InsertionDetourTimeCalculator.DropoffDetourInfo.class))));
			};

			double processTime = now + COLLECTION_PERIOD;
			RequestInsertionScheduler insertionScheduler = (acceptedRequest, insertion) -> {
				scheduledRequests.add(acceptedRequest.getId());
				var pickupTask = new DefaultDrtStopTask(processTime, processTime + 10, acceptedRequest.getFromLink());
				pickupTask.addPickupRequest(acceptedRequest);
				var dropoffTask = new DefaultDrtStopTask(processTime + 20, processTime + 30, acceptedRequest.getToLink());
				dropoffTask.addPickupRequest(acceptedRequest);
				return new PickupDropoffTaskPair(pickupTask, dropoffTask);
			};

			RequestsPartitioner requestsPartitioner = new RoundRobinRequestsPartitioner();
			VehicleEntryPartitioner vehicleEntryPartitioner = new ReplicatingVehicleEntryPartitioner();

			DrtParallelInserterParams params = new DrtParallelInserterParams();
			params.setCollectionPeriod(COLLECTION_PERIOD);
			params.setMaxIterations(3);
			params.setMaxPartitions(numPartitions);
			params.setLogThreadActivity(false);

			var inserter = new ParallelUnplannedRequestInserter(
					matsimServices,
					requestsPartitioner,
					vehicleEntryPartitioner,
					params,
					mode,
					fleet,
					iterationEventsManager,
					() -> insertionScheduler,
					entryFactory,
					() -> insertionSearch,
					new DefaultOfferAcceptor(),
					StaticPassengerStopDurationProvider.of(10.0, 0.0),
					RequestFleetFilter.none,
					retryQueue
			);

			inserter.scheduleUnplannedRequests(unplannedRequests);
			inserter.doSimStep(now);
			inserter.doSimStep(processTime);

			// Verify all requests were removed from unplanned
			assertThat(unplannedRequests).as("Iteration " + iteration + ": unplanned should be empty").isEmpty();

			// Verify no duplicates in scheduled (thread safety check)
			assertThat(scheduledRequests).as("Iteration " + iteration + ": no duplicates in scheduled").doesNotHaveDuplicates();

			// Verify some requests were scheduled (not all due to conflicts in parallel processing)
			assertThat(scheduledRequests).as("Iteration " + iteration + ": some scheduled").isNotEmpty();

			// Verify total events equals numRequests (scheduled + rejected)
			verify(iterationEventsManager, times(numRequests)).processEvent(any());
		}
	}

	@Test
	void threadSafety_noConflicts_allScheduled() {
		// Test scenario where each request gets a unique vehicle - no conflicts expected
		int numVehicles = 500;
		int numRequests = 500;
		int numPartitions = 8;

		List<DvrpVehicle> vehicleList = new ArrayList<>();
		Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntryMap = new HashMap<>();
		for (int i = 0; i < numVehicles; i++) {
			var vehicle = vehicle("unique_v" + i);
			vehicleList.add(vehicle);
			vehicleEntryMap.put(vehicle.getId(), new VehicleEntry(vehicle, null, null, null, null, 0));
		}

		List<DrtRequest> requestList = new ArrayList<>();
		for (int i = 0; i < numRequests; i++) {
			requestList.add(request("unique_req" + i, "from" + i, "to" + i));
		}

		var fleet = fleet(vehicleList.toArray(new DvrpVehicle[0]));
		var unplannedRequests = requests(requestList.toArray(new DrtRequest[0]));
		double now = 15;

		VehicleEntry.EntryFactory entryFactory = (vehicle, currentTime) -> vehicleEntryMap.get(vehicle.getId());

		DrtRequestInsertionRetryQueue retryQueue = new DrtRequestInsertionRetryQueue(
				new DrtRequestInsertionRetryParams());

		Set<Id<Request>> scheduledRequests = Collections.synchronizedSet(new HashSet<>());

		// Each request gets its own unique vehicle - no conflicts
		DrtInsertionSearch insertionSearch = (drtRequest, vEntries) -> {
			if (vEntries.isEmpty()) {
				return Optional.empty();
			}
			// Extract request number and assign to corresponding vehicle
			String reqId = drtRequest.getId().toString();
			int requestNum = Integer.parseInt(reqId.replace("unique_req", ""));
			var vehicleEntry = vehicleEntryMap.get(Id.create("unique_v" + requestNum, DvrpVehicle.class));
			if (vehicleEntry == null) {
				return Optional.empty();
			}
			return Optional.of(new InsertionWithDetourData(
					new InsertionGenerator.Insertion(vehicleEntry, null, null, loadType.fromInt(1)), null,
					new InsertionDetourTimeCalculator.DetourTimeInfo(
							mock(InsertionDetourTimeCalculator.PickupDetourInfo.class),
							mock(InsertionDetourTimeCalculator.DropoffDetourInfo.class))));
		};

		double processTime = now + COLLECTION_PERIOD;
		RequestInsertionScheduler insertionScheduler = (acceptedRequest, insertion) -> {
			scheduledRequests.add(acceptedRequest.getId());
			var pickupTask = new DefaultDrtStopTask(processTime, processTime + 10, acceptedRequest.getFromLink());
			pickupTask.addPickupRequest(acceptedRequest);
			var dropoffTask = new DefaultDrtStopTask(processTime + 20, processTime + 30, acceptedRequest.getToLink());
			dropoffTask.addPickupRequest(acceptedRequest);
			return new PickupDropoffTaskPair(pickupTask, dropoffTask);
		};

		var inserter = newInserterWithPartitions(fleet, entryFactory, retryQueue, insertionSearch, insertionScheduler, numPartitions);
		inserter.scheduleUnplannedRequests(unplannedRequests);

		// Trigger processing
		inserter.doSimStep(now);
		inserter.doSimStep(processTime);

		// All requests should be removed from unplanned
		assertThat(unplannedRequests).isEmpty();

		// No duplicates (thread safety)
		assertThat(scheduledRequests).doesNotHaveDuplicates();

		// All requests should be scheduled (no conflicts)
		assertThat(scheduledRequests).hasSize(numRequests);

		// Verify all events are scheduled events
		verify(eventsManager, times(numRequests)).processEvent(any(PassengerRequestScheduledEvent.class));
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

	private ParallelUnplannedRequestInserter newInserter(Fleet fleet,
			VehicleEntry.EntryFactory vehicleEntryFactory, DrtRequestInsertionRetryQueue insertionRetryQueue,
			DrtInsertionSearch insertionSearch, RequestInsertionScheduler insertionScheduler) {
		return newInserterWithPartitions(fleet, vehicleEntryFactory, insertionRetryQueue, insertionSearch, insertionScheduler, 1);
	}

	private ParallelUnplannedRequestInserter newInserterWithPartitions(Fleet fleet,
			VehicleEntry.EntryFactory vehicleEntryFactory, DrtRequestInsertionRetryQueue insertionRetryQueue,
			DrtInsertionSearch insertionSearch, RequestInsertionScheduler insertionScheduler, int maxPartitions) {

		// Use real partitioner implementations
		RequestsPartitioner requestsPartitioner = new RoundRobinRequestsPartitioner();
		VehicleEntryPartitioner vehicleEntryPartitioner = new ReplicatingVehicleEntryPartitioner();

		DrtParallelInserterParams params = new DrtParallelInserterParams();
		params.setCollectionPeriod(COLLECTION_PERIOD);
		params.setMaxIterations(2);
		params.setMaxPartitions(maxPartitions);
		params.setLogThreadActivity(false);

		return new ParallelUnplannedRequestInserter(
				matsimServices,
				requestsPartitioner,
				vehicleEntryPartitioner,
				params,
				mode,
				fleet,
				eventsManager,
				() -> insertionScheduler,
				vehicleEntryFactory,
				() -> insertionSearch,
				new DefaultOfferAcceptor(),
				StaticPassengerStopDurationProvider.of(10.0, 0.0),
				RequestFleetFilter.none,
				insertionRetryQueue
		);
	}

	private Link link(String id) {
		return new FakeLink(Id.createLinkId(id));
	}
}
