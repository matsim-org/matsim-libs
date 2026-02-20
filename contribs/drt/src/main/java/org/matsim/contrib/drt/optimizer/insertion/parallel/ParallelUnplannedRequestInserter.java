package org.matsim.contrib.drt.optimizer.insertion.parallel;
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


import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.contrib.drt.optimizer.DrtRequestInsertionRetryQueue;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.RequestFleetFilter;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.RequestData;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.requests.RequestsPartitioner;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.vehicles.VehicleEntryPartitioner;
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
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.dsim.DistributedMobsimEngine;
import org.matsim.core.mobsim.dsim.NodeSingleton;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.qsim.InternalInterface;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.matsim.contrib.drt.optimizer.insertion.DefaultUnplannedRequestInserter.NO_INSERTION_FOUND_CAUSE;
import static org.matsim.contrib.drt.optimizer.insertion.DefaultUnplannedRequestInserter.OFFER_REJECTED_CAUSE;

/**
 * A parallelized implementation of {@link UnplannedRequestInserter} for dynamic ride-sharing (DRT) systems.
 * <p>
 * This class partitions incoming unplanned DRT requests and available vehicle entries into multiple subsets,
 * which are then processed concurrently by a pool of {@link RequestInsertWorker} threads. The goal is to
 * efficiently find feasible insertions for ride requests into vehicle schedules while minimizing conflicts.
 * <p>
 * Key features:
 * <ul>
 *   <li>Uses {@link RequestsPartitioner} and {@link VehicleEntryPartitioner} to divide work across threads.</li>
 *   <li>Supports configurable collection periods and maximum conflict resolution iterations.</li>
 *   <li>Handles request retries and conflict resolution across multiple rounds.</li>
 *   <li>Schedules accepted requests and emits appropriate MATSim events for scheduled and rejected requests.</li>
 * </ul>
 * <p>
 * This inserter is designed for high-performance, large-scale DRT simulations where parallel processing
 * of insertion logic is essential for scalability.
 *
 * @author Steffen Axer
 */
@NodeSingleton
public class ParallelUnplannedRequestInserter implements UnplannedRequestInserter, DistributedMobsimEngine, MobsimBeforeCleanupListener {
	private static final Logger LOG = LogManager.getLogger(ParallelUnplannedRequestInserter.class);
	private Double lastProcessingTime;
	private final double collectionPeriod;
	private final String mode;
	private final Fleet fleet;
	private final EventsManager eventsManager;
	private final RequestInsertionScheduler insertionScheduler;
	private final VehicleEntry.EntryFactory vehicleEntryFactory;
	private final Provider<DrtInsertionSearch> insertionSearch;
	private final DrtOfferAcceptor drtOfferAcceptor;
	private final PassengerStopDurationProvider stopDurationProvider;
	private final RequestFleetFilter requestFleetFilter;
	private final List<RequestInsertWorker> workers;
	private final ForkJoinPool inserterExecutorService;
	private final int maxIter;
	private final Map<Id<DvrpVehicle>, SortedSet<RequestData>> solutions = new ConcurrentHashMap<>();
	private final SortedSet<DrtRequest> noSolutions = new ConcurrentSkipListSet<>(ConflictResolver.DRT_REQUEST_COMPARATOR);
	private final Queue<DrtRequest> tmpQueue = new ConcurrentLinkedQueue<>();
	private final PartitionStrategy partitionStrategy;
	private final DrtRequestInsertionRetryQueue insertionRetryQueue;
	private final PartitionActivityLogger activityLogger;
	private final ConflictResolver conflictResolver;
	private final PerformanceLogger performanceLogger;

	public ParallelUnplannedRequestInserter(MatsimServices matsimServices, RequestsPartitioner requestsPartitioner, VehicleEntryPartitioner vehicleEntryPartitioner, DrtParallelInserterParams drtParallelInserterParams, DrtConfigGroup drtCfg, Fleet fleet,
											EventsManager eventsManager, Provider<RequestInsertionScheduler> insertionSchedulerProvider,
											VehicleEntry.EntryFactory vehicleEntryFactory, Provider<DrtInsertionSearch> insertionSearch,
											DrtOfferAcceptor drtOfferAcceptor,
											PassengerStopDurationProvider stopDurationProvider, RequestFleetFilter requestFleetFilter,
											DrtRequestInsertionRetryQueue insertionRetryQueue) {
		this(matsimServices, requestsPartitioner, vehicleEntryPartitioner, drtParallelInserterParams, drtCfg.getMode(), fleet, eventsManager, insertionSchedulerProvider, vehicleEntryFactory,
			insertionSearch, drtOfferAcceptor, stopDurationProvider, requestFleetFilter, insertionRetryQueue);
	}

	@VisibleForTesting
	ParallelUnplannedRequestInserter(MatsimServices matsimServices, RequestsPartitioner requestsPartitioner, VehicleEntryPartitioner vehicleEntryPartitioner, DrtParallelInserterParams drtParallelInserterParams, String mode, Fleet fleet, EventsManager eventsManager,
									 Provider<RequestInsertionScheduler> insertionSchedulerProvider, VehicleEntry.EntryFactory vehicleEntryFactory, Provider<DrtInsertionSearch> insertionSearch,
									 DrtOfferAcceptor drtOfferAcceptor, PassengerStopDurationProvider stopDurationProvider, RequestFleetFilter requestFleetFilter, DrtRequestInsertionRetryQueue insertionRetryQueue) {
		this.collectionPeriod = drtParallelInserterParams.getCollectionPeriod();
		this.mode = mode;
		this.fleet = fleet;
		this.eventsManager = eventsManager;
		this.insertionScheduler = insertionSchedulerProvider.get();
		this.vehicleEntryFactory = vehicleEntryFactory;
		this.insertionSearch = insertionSearch;
		this.drtOfferAcceptor = drtOfferAcceptor;
		this.stopDurationProvider = stopDurationProvider;
		this.requestFleetFilter = requestFleetFilter;
		this.inserterExecutorService = new ForkJoinPool(drtParallelInserterParams.getMaxPartitions());
		this.workers = createWorkers(drtParallelInserterParams.getMaxPartitions());
		this.maxIter = drtParallelInserterParams.getMaxIterations();
		this.insertionRetryQueue = insertionRetryQueue;
		this.partitionStrategy = new PartitionStrategy(
			requestsPartitioner,
			vehicleEntryPartitioner,
			drtParallelInserterParams.getMaxPartitions(),
			drtParallelInserterParams.getCollectionPeriod()
		);
		this.activityLogger = new PartitionActivityLogger(matsimServices, mode, drtParallelInserterParams);
		this.conflictResolver = new ConflictResolver();
		this.performanceLogger = new PerformanceLogger(matsimServices, mode, drtParallelInserterParams);
	}

	private List<RequestInsertWorker> createWorkers(int n) {
		List<RequestInsertWorker> workerList = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			workerList.add(new RequestInsertWorker(requestFleetFilter, insertionSearch.get(), solutions, noSolutions));
		}
		return workerList;
	}

	@Override
	public void scheduleUnplannedRequests(Collection<DrtRequest> unplannedRequests) {
		var it = unplannedRequests.iterator();
		while (it.hasNext()) {
			this.tmpQueue.add(it.next());
			it.remove();
		}
	}

	private void solve(double now, Map<Id<DvrpVehicle>, VehicleEntry> entries) {
		PartitionContext partitions = partitionStrategy.createPartitions(tmpQueue, entries);

		activityLogger.record(now, partitions.originalRequestCount(), collectionPeriod, partitions.activePartitionCount());

		if (partitions.isEmpty()) {
			return;
		}

		submitWorkersForPartitions(now, partitions);
	}

	private void submitWorkersForPartitions(double now, PartitionContext partitions) {
		List<ForkJoinTask<?>> tasks = new ArrayList<>();

		for (int i = 0; i < partitions.size(); i++) {
			var worker = this.workers.get(i);
			var requestPartition = partitions.getRequestPartition(i);
			var vehiclePartition = partitions.getVehiclePartition(i);
			tasks.add(inserterExecutorService.submit(() -> worker.process(now, requestPartition, vehiclePartition)));
		}

		tasks.forEach(ForkJoinTask::join);
	}

	private Map<Id<DvrpVehicle>, VehicleEntry> updateVehicleEntries(
		double now,
		Map<Id<DvrpVehicle>, VehicleEntry> currentVehicleEntries,
		Set<DvrpVehicle> toBeUpdated) {

		Set<Id<DvrpVehicle>> toBeDeleted = toBeUpdated.stream()
			.map(Identifiable::getId)
			.collect(Collectors.toSet());

		Map<Id<DvrpVehicle>, VehicleEntry> newlyCreated = calculateVehicleEntries(now, toBeUpdated);


		Map<Id<DvrpVehicle>, VehicleEntry> updated = new HashMap<>();

		currentVehicleEntries.forEach((id, entry) -> {
			if (!toBeDeleted.contains(id)) {
				updated.put(id, entry);
			}
		});

		updated.putAll(newlyCreated);

		return Collections.unmodifiableMap(updated);
	}


	// Ordered map by vehicle id
	private Map<Id<DvrpVehicle>, VehicleEntry> calculateVehicleEntries(double now, Collection<DvrpVehicle> vehicles) {
		return Collections.unmodifiableMap(
			inserterExecutorService.submit(() ->
				vehicles
					.parallelStream()
					.map(v -> vehicleEntryFactory.create(v, now))
					.filter(Objects::nonNull)
					.collect(Collectors.toMap(
						e -> e.vehicle.getId(),
						e -> e,
						(e1, e2) -> e1,
						TreeMap::new
					))
			).join()
		);
	}

	ConflictResolver.ConsolidationResult consolidate() {
		return conflictResolver.consolidate(this.solutions, this.noSolutions);
	}

	Optional<DvrpVehicle> schedule(RequestData requestData, double now) {
		var req = requestData.getDrtRequest();
		var insertion = requestData.getSolution().insertion().get();

		var vehicle = insertion.insertion.vehicleEntry.vehicle;
		double pickupDuration = stopDurationProvider.calcPickupDuration(vehicle, req);
		double dropoffDuration = stopDurationProvider.calcDropoffDuration(vehicle, req);

		var acceptedRequest = drtOfferAcceptor.acceptDrtOffer(req,
			insertion.detourTimeInfo.pickupDetourInfo.requestPickupTime,
			insertion.detourTimeInfo.dropoffDetourInfo.requestDropoffTime,
			pickupDuration, dropoffDuration);

		if (acceptedRequest.isPresent()) {
			var pickupDropoffTaskPair = insertionScheduler.scheduleRequest(acceptedRequest.get(), insertion);

			double expectedPickupTime = pickupDropoffTaskPair.pickupTask.getBeginTime();
			expectedPickupTime = Math.max(expectedPickupTime, acceptedRequest.get().getEarliestStartTime());
			expectedPickupTime += stopDurationProvider.calcPickupDuration(vehicle, req);

			double expectedDropoffTime = pickupDropoffTaskPair.dropoffTask.getBeginTime();
			expectedDropoffTime += stopDurationProvider.calcDropoffDuration(vehicle, req);

			eventsManager.processEvent(
				new PassengerRequestScheduledEvent(now, mode, req.getId(), req.getPassengerIds(), vehicle.getId(),
					expectedPickupTime, expectedDropoffTime));
			return Optional.of(vehicle);
		} else {
			retryOrReject(req, now, OFFER_REJECTED_CAUSE);
			return Optional.empty();
		}
	}

	void retryOrReject(DrtRequest req, double now, String cause) {
		if (!insertionRetryQueue.tryAddFailedRequest(req, now)) {
			eventsManager.processEvent(
				new PassengerRequestRejectedEvent(now, mode, req.getId(), req.getPassengerIds(),
					cause));
			LOG.debug("No insertion found for drt request {} with passenger ids={} fromLinkId={}", req, req.getPassengerIds().stream().map(Object::toString).collect(Collectors.joining(",")), req.getFromLink().getId());
		}
	}

	@Override
	public void onPrepareSim() {

	}

	@Override
	public void afterSim() {

	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {

	}

	void handleInsertionRetryQueue(double now) {
		tmpQueue.addAll(insertionRetryQueue.getRequestsToRetryNow(now));
	}

	@Override
	public void doSimStep(double time) {
		if (!shouldProcess(time)) {
			return;
		}
		processRequests(time);
	}

	private boolean shouldProcess(double time) {
		if (this.lastProcessingTime == null) {
			this.lastProcessingTime = time;
		}
		if ((time - lastProcessingTime) < collectionPeriod) {
			return false;
		}
		lastProcessingTime = time;
		return true;
	}

	private void processRequests(double time) {
		handleInsertionRetryQueue(time);

		int totalRequests = tmpQueue.size();

		// Only create builders if performance logging is enabled
		PerformanceLogger.CycleRecordBuilder cycleBuilder = performanceLogger.isEnabled()
			? new PerformanceLogger.CycleRecordBuilder().simTime(time).maxPartitions(workers.size()).startCycle()
			: null;

		if (cycleBuilder != null) {
			cycleBuilder.startVehicleEntryCalc();
		}

		Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries = calculateVehicleEntries(time, this.fleet.getVehicles().values());

		if (cycleBuilder != null) {
			cycleBuilder.endVehicleEntryCalc()
				.totalRequests(totalRequests)
				.totalVehicles(vehicleEntries.size());
		}

		InsertionRoundResult result = runInsertionRounds(time, vehicleEntries, cycleBuilder);

		result.rejected().forEach(req -> retryOrReject(req, time, NO_INSERTION_FOUND_CAUSE));
		LOG.debug("Scheduled requests #{} ", result.scheduledCount());

		if (cycleBuilder != null) {
			cycleBuilder.totalScheduled(result.scheduledCount())
				.totalRejected(result.rejected().size())
				.endCycle();
			performanceLogger.recordCycle(cycleBuilder.build());
		}
	}

	/**
	 * Result of the insertion rounds.
	 *
	 * @param scheduledCount Total number of successfully scheduled requests
	 * @param rejected       Requests that could not be scheduled after all iterations
	 */
	record InsertionRoundResult(int scheduledCount, SortedSet<DrtRequest> rejected) {
	}

	private InsertionRoundResult runInsertionRounds(double time, Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries,
												   PerformanceLogger.CycleRecordBuilder cycleBuilder) {
		SortedSet<DrtRequest> finalRejections = new TreeSet<>(ConflictResolver.DRT_REQUEST_COMPARATOR);
		Integer lastUnsolvedConflicts = null;
		int scheduled = 0;

		for (int i = 0; i < this.maxIter; i++) {
			PerformanceLogger.IterationRecordBuilder iterBuilder = cycleBuilder != null
				? new PerformanceLogger.IterationRecordBuilder().iterationNumber(i)
				: null;

			// Solve phase
			if (iterBuilder != null) {
				iterBuilder.startSolve();
			}
			solve(time, vehicleEntries);
			if (iterBuilder != null) {
				iterBuilder.endSolve();

				// Capture worker statistics after solve
				int[] requestsPerWorker = new int[workers.size()];
				int[] vehiclesPerWorker = new int[workers.size()];
				long[] workerTimes = new long[workers.size()];
				int activePartitions = 0;
				for (int w = 0; w < workers.size(); w++) {
					requestsPerWorker[w] = workers.get(w).getLastRequestCount();
					vehiclesPerWorker[w] = workers.get(w).getLastVehicleCount();
					workerTimes[w] = workers.get(w).getLastProcessingTimeNanos();
					if (requestsPerWorker[w] > 0) {
						activePartitions++;
					}
				}
				iterBuilder.requestsPerWorker(requestsPerWorker)
					.vehiclesPerWorker(vehiclesPerWorker)
					.workerProcessingTimes(workerTimes);

				// Set active partitions on first iteration
				if (i == 0) {
					cycleBuilder.activePartitions(activePartitions);
				}
			}

			// Conflict resolution phase
			if (iterBuilder != null) {
				iterBuilder.startConflictResolution();
			}
			ConflictResolver.ConsolidationResult consolidationResult = consolidate();
			if (iterBuilder != null) {
				iterBuilder.endConflictResolution();
			}

			// Clean workers after conflict resolution (not part of timing)
			this.workers.forEach(RequestInsertWorker::clean);

			// Scheduling phase
			if (iterBuilder != null) {
				iterBuilder.startScheduling();
			}
			List<RequestData> toBeScheduled = consolidationResult.toBeScheduled();
			Set<DvrpVehicle> scheduledVehicles = scheduleRequests(toBeScheduled, time);
			if (iterBuilder != null) {
				iterBuilder.endScheduling();
			}

			this.solutions.clear();
			scheduled += toBeScheduled.size();

			// Collect rejections for this iteration
			Collection<DrtRequest> iterationRejections = consolidationResult.toBeRejected();
			this.noSolutions.clear();

			if (iterBuilder != null) {
				iterBuilder.conflicts(consolidationResult.conflictCount())
					.noSolutions(consolidationResult.noSolutionCount())
					.scheduled(toBeScheduled.size());
				cycleBuilder.addIteration(iterBuilder.build());
			}

			// Check termination conditions
			if (iterationRejections.isEmpty()
				|| (lastUnsolvedConflicts != null && iterationRejections.size() == lastUnsolvedConflicts)
				|| i == this.maxIter - 1) {
				LOG.debug("Stopped with rejections #{} ", iterationRejections.size());
				finalRejections.addAll(iterationRejections);
				break;
			}

			// Prepare next iteration - put rejections back into queue for retry
			vehicleEntries = updateVehicleEntries(time, vehicleEntries, scheduledVehicles);
			lastUnsolvedConflicts = iterationRejections.size();
			this.scheduleUnplannedRequests(iterationRejections);
		}

		return new InsertionRoundResult(scheduled, finalRejections);
	}

	private Set<DvrpVehicle> scheduleRequests(List<RequestData> toBeScheduled, double time) {
		return toBeScheduled.stream()
			.map(r -> schedule(r, time))
			.flatMap(Optional::stream)
			.collect(Collectors.toSet());
	}


	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		activityLogger.writeOutputs();
		performanceLogger.writeOutputs();
		inserterExecutorService.shutdown();
		LOG.info("Avg. conflict share {} ", conflictResolver.getAverageConflictShare());
	}



}
