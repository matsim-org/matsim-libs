package org.matsim.contrib.drt.optimizer.insertion.parallel;/* *********************************************************************** *
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
import com.google.common.base.Verify;
import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
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
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
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

public class ParallelUnplannedRequestInserter implements UnplannedRequestInserter, MobsimEngine, MobsimBeforeCleanupListener {
	private static final Logger LOG = LogManager.getLogger(ParallelUnplannedRequestInserter.class);
	private Double lastProcessingTime;
	private static final Comparator<DrtRequest> drtRequestComparator = Comparator.comparingDouble(DrtRequest::getSubmissionTime).thenComparing(req -> req.getId().toString());
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
	private final SortedSet<DrtRequest> noSolutions = new ConcurrentSkipListSet<>(drtRequestComparator);
	private final Queue<DrtRequest> tmpQueue = new ConcurrentLinkedQueue<>();
	private final VehicleEntryPartitioner vehicleEntryPartitioner;
	private final RequestsPartitioner requestsPartitioner;
	private final DrtRequestInsertionRetryQueue insertionRetryQueue;
	private final List<DataRecord> dataRecordsLog = new ArrayList<>();
	private final DrtParallelInserterParams drtParallelInserterParams;
	private final MatsimServices matsimServices;

	public long nConflicting = 0;
	public long nNonConflicting = 0;

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
		this.requestsPartitioner = requestsPartitioner;
		this.vehicleEntryPartitioner = vehicleEntryPartitioner;
		this.collectionPeriod = drtParallelInserterParams.getCollectionPeriod();
		this.drtParallelInserterParams = drtParallelInserterParams;
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
		this.workers = getRequestInsertWorker(drtParallelInserterParams.getMaxPartitions());
		this.maxIter = drtParallelInserterParams.getMaxIterations();
		this.insertionRetryQueue = insertionRetryQueue;
		this.matsimServices = matsimServices;
	}

	List<RequestInsertWorker> getRequestInsertWorker(int n) {
		List<RequestInsertWorker> workers = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			RequestInsertWorker requestInsertWorker = new RequestInsertWorker(
				requestFleetFilter,
				insertionSearch.get(),
				solutions,
				noSolutions);
			workers.add(requestInsertWorker);
		}
		return workers;
	}

	@Override
	public void scheduleUnplannedRequests(Collection<DrtRequest> unplannedRequests) {
		var it = unplannedRequests.iterator();
		while (it.hasNext()) {
			this.tmpQueue.add(it.next());
			it.remove();
		}
	}

	public static boolean validateUniqueRequests(List<Collection<RequestData>> partitions) {
		Set<Id<Request>> seen = new HashSet<>();
		for (Collection<RequestData> partition : partitions) {
			for (RequestData data : partition) {
				if (!seen.add(data.getDrtRequest().getId())) {
					throw new IllegalStateException("Duplicate DrtRequest found across partitions: " + data.getDrtRequest().getId());
				}
			}
		}
		return true; // All requests are unique per partition
	}


	private void solve(double now, Map<Id<DvrpVehicle>, VehicleEntry> entries, RequestsPartitioner requestsPartitioner, VehicleEntryPartitioner partitioner) {
		List<ForkJoinTask<?>> tasks = new ArrayList<>();

		int requests = this.tmpQueue.size();
		// Do not create more requestPartitions than available vehicles
		// e.g. 60 req / 20 req/min --> 3 active partitions but only 2 available veh right now
		int maxPartitions = Math.max(1, Math.min(this.workers.size(), entries.size())); // at least one partition

		List<Collection<RequestData>> requestsPartitions = requestsPartitioner.partition(this.tmpQueue, maxPartitions, this.collectionPeriod);
		if (drtParallelInserterParams.isLogThreadActivity()) {
			int activePartitions = (int) requestsPartitions.stream()
				.filter(partition -> partition != null && !partition.isEmpty())
				.count();
			this.dataRecordsLog.add(new DataRecord(now, (int) (requests / this.collectionPeriod * 60), activePartitions));
		}

		// The number of request partitions indicate the number of required vehicle partitions
		List<Map<Id<DvrpVehicle>, VehicleEntry>> vehiclePartitions = partitioner.partition(entries, requestsPartitions);

		Verify.verify(
			vehiclePartitions.size() == requestsPartitions.size(),
			"Mismatch between number of vehicle entry vehiclePartitions (%s) and requestsPartitions (%s)",
			vehiclePartitions.size(), requestsPartitions.size()
		);

		validateUniqueRequests(requestsPartitions);

		// We only could use the number of maxPartitions
		for (int i = 0; i < maxPartitions; i++) {
			var worker = this.workers.get(i);
			Collection<RequestData> requestDataPartition = requestsPartitions.get(i);
			Map<Id<DvrpVehicle>, VehicleEntry> vehiclePartition = vehiclePartitions.get(i);
			tasks.add(inserterExecutorService.submit(() -> worker.process(now, requestDataPartition, vehiclePartition)));
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


	private Map<Id<DvrpVehicle>, VehicleEntry> calculateVehicleEntries(double now, Collection<DvrpVehicle> vehicles) {
		return Collections.unmodifiableMap(
			inserterExecutorService.submit(() ->
				vehicles
					.parallelStream()
					.map(v -> vehicleEntryFactory.create(v, now))
					.filter(Objects::nonNull)
					.collect(Collectors.toMap(
						e -> e.vehicle.getId(), e -> e
					))
			).join()
		);
	}

	record ConsolidationResult(List<RequestData> toBeScheduled, Collection<DrtRequest> toBeRejected) {
	}

	ConsolidationResult consolidate() {

		Set<DrtRequest> allRejection = this.noSolutions;
		ResolvedConflicts resolvedConflicts = resolve(this.solutions);

		this.nConflicting += resolvedConflicts.conflicts.size();
		this.nNonConflicting += resolvedConflicts.noConflicts.size();

		// Remaining conflicts, add up into allRejection
		allRejection.addAll(
			resolvedConflicts.conflicts.stream()
				.map(RequestData::getDrtRequest)
				.toList()
		);


		this.workers.forEach(RequestInsertWorker::clean);
		return new ConsolidationResult(resolvedConflicts.noConflicts, allRejection);
	}

	record ResolvedConflicts(List<RequestData> noConflicts, List<RequestData> conflicts) {
	}

	ResolvedConflicts resolve(Map<Id<DvrpVehicle>, SortedSet<RequestData>> data) {
		List<RequestData> noConflicts = new ArrayList<>();
		List<RequestData> conflicts = new ArrayList<>();

		for (var requestDataList : data.values()) {
			if (requestDataList.isEmpty()) continue;

			var iterator = requestDataList.iterator();
			var bestSolution = iterator.next();
			noConflicts.add(bestSolution);

			while (iterator.hasNext()) {
				conflicts.add(iterator.next());
			}
		}

		return new ResolvedConflicts(noConflicts, conflicts);
	}


	Optional<DvrpVehicle> schedule(RequestData requestData, double now) {
		var req = requestData.getDrtRequest();
		var insertion = requestData.getSolution().insertion().get();

		double dropoffDuration = insertion.detourTimeInfo.dropoffDetourInfo.requestDropoffTime -
			insertion.detourTimeInfo.dropoffDetourInfo.vehicleArrivalTime;

		var acceptedRequest = drtOfferAcceptor.acceptDrtOffer(req,
			insertion.detourTimeInfo.pickupDetourInfo.requestPickupTime,
			insertion.detourTimeInfo.dropoffDetourInfo.requestDropoffTime,
			dropoffDuration);

		if (acceptedRequest.isPresent()) {
			var vehicle = insertion.insertion.vehicleEntry.vehicle;
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

		if (this.lastProcessingTime == null) {
			this.lastProcessingTime = time;
		}

		if ((time - lastProcessingTime) >= collectionPeriod) {
			handleInsertionRetryQueue(time); // Add now also elements from the insertionRetryQueue

			// Solve requests the first time
			// At this point, we need to generate vehicleEntries for all vehicles
			Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries = calculateVehicleEntries(time, this.fleet.getVehicles().values());


			lastProcessingTime = time;

			SortedSet<DrtRequest> toBeRejected = new TreeSet<>(drtRequestComparator); // ensure time order
			// Retry conflicts
			Integer lastUnsolvedConflicts = null;
			int scheduled = 0;
			for (int i = 0; i < this.maxIter; i++) {
				solve(time, vehicleEntries, this.requestsPartitioner, this.vehicleEntryPartitioner);
				ConsolidationResult consolidationResult = consolidate();

				// Schedule and clear
				List<RequestData> toBeScheduled = consolidationResult.toBeScheduled;
				Set<DvrpVehicle> scheduledVehicles = toBeScheduled.stream()
					.map(r -> schedule(r, time))
					.flatMap(Optional::stream)
					.collect(Collectors.toSet());
				this.solutions.clear(); // Clean after having them scheduled!
				scheduled += toBeScheduled.size();

				// Prepare for next round
				toBeRejected.addAll(consolidationResult.toBeRejected);
				this.noSolutions.clear(); // Clean after having them added for next iterations!

				if (toBeRejected.isEmpty()
					|| (lastUnsolvedConflicts != null && toBeRejected.size() == lastUnsolvedConflicts) // not getting better
					|| i == this.maxIter - 1) { // reached iter limit
					LOG.debug("Stopped with rejections #{} ", toBeRejected.size());
					break;
				}

				// Update vehicle entries for next round
				vehicleEntries = updateVehicleEntries(time, vehicleEntries, scheduledVehicles);
				lastUnsolvedConflicts = toBeRejected.size();
				this.scheduleUnplannedRequests(toBeRejected);
			}
			// Clean workers ultimately
			toBeRejected.forEach(s -> retryOrReject(s, time, NO_INSERTION_FOUND_CAUSE));
			LOG.debug("Scheduled requests #{} ", scheduled);

			this.workers.forEach(RequestInsertWorker::clean);
		}
	}

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		plotDataRecordLogWithDualAxis();
		dumpDataRecordLog();
		inserterExecutorService.shutdown();
		LOG.info("Avg. conflict share {} ", nConflicting / (double) (nConflicting + nNonConflicting));
	}

	public void plotDataRecordLogWithDualAxis() {
		if (!this.drtParallelInserterParams.isLogThreadActivity()) return;


		XYSeries densitySeries = new XYSeries("Requests Density");

		XYSeries partitionsSeries = new XYSeries("Active Partitions");

		for (DataRecord record : dataRecordsLog) {
			densitySeries.add(record.time, record.requestsDensityPerMinute);
			partitionsSeries.add(record.time, record.activePartitions);
		}

		XYSeriesCollection densityDataset = new XYSeriesCollection(densitySeries);
		NumberAxis densityAxis = new NumberAxis("Requests Density [req/min]");
		XYPlot densityPlot = new XYPlot(densityDataset, null, densityAxis, null);
		XYLineAndShapeRenderer densityRenderer = new XYLineAndShapeRenderer(true, false); // Linien ohne Punkte
		densityPlot.setRenderer(densityRenderer);


		XYSeriesCollection partitionsDataset = new XYSeriesCollection(partitionsSeries);

		NumberAxis partitionsAxis = new NumberAxis("Active Partitions");
		partitionsAxis.setAutoRangeIncludesZero(false);
		partitionsAxis.setLowerBound(1);
		partitionsAxis.setUpperBound(this.drtParallelInserterParams.getMaxPartitions() + 1);

		XYPlot partitionsPlot = new XYPlot(partitionsDataset, null, partitionsAxis, null);
		XYLineAndShapeRenderer partitionsRenderer = new XYLineAndShapeRenderer(true, false);
		partitionsPlot.setRenderer(partitionsRenderer);


		NumberAxis timeAxis = new NumberAxis("Time [s]");
		CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(timeAxis);
		combinedPlot.add(densityPlot, 1);
		combinedPlot.add(partitionsPlot, 1);

		var chart = new JFreeChart("Active Partitions Over Time", JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, true);


		String filename = matsimServices.getControllerIO().getIterationFilename(
			matsimServices.getIterationNumber(), mode + "_partitionActivity.png"
		);

		try {
			ChartUtils.saveChartAsPNG(new File(filename), chart, 900, 600);
		} catch (IOException e) {
			LOG.error("Failed to write chart image", e);
		}
	}

	void dumpDataRecordLog() {
		if (!this.drtParallelInserterParams.isLogThreadActivity()) return;

		String sep = matsimServices.getConfig().global().getDefaultDelimiter();
		String header = String.join(sep, "time", "requestsDensityPerMinute", "activePartitions");
		String filename = matsimServices.getControllerIO().getIterationFilename(
			matsimServices.getIterationNumber(), mode + "_dataRecordsLog.csv.gz"
		);

		try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {
			writer.write(header);
			writer.newLine();

			for (DataRecord record : dataRecordsLog) {
				writer.write(String.join(sep,
					String.valueOf(record.time),
					String.valueOf(record.requestsDensityPerMinute),
					String.valueOf(record.activePartitions)
				));
				writer.newLine();
			}

		} catch (IOException ex) {
			LOG.error("Failed to write dataRecordsLog", ex);
		}
	}


	record DataRecord(double time, int requestsDensityPerMinute, int activePartitions) {
	}


}
