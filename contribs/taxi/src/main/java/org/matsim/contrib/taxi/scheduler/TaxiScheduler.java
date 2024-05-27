/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.scheduler;

import static java.util.stream.Collectors.toList;
import static org.matsim.contrib.taxi.schedule.TaxiTaskBaseType.PICKUP;
import static org.matsim.contrib.taxi.schedule.TaxiTaskBaseType.getBaseTypeOrElseThrow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelDataImpl;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiDropoffTask;
import org.matsim.contrib.taxi.schedule.TaxiEmptyDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiOccupiedDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiPickupTask;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.schedule.TaxiTaskType;
import org.matsim.contrib.util.ExecutorServiceWithResource;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

import com.google.common.util.concurrent.Futures;

public class TaxiScheduler implements MobsimBeforeCleanupListener {
	protected final TaxiConfigGroup taxiCfg;
	private final Fleet fleet;
	private final TravelTime travelTime;
	private final LeastCostPathCalculator router;
	private final TaxiScheduleInquiry taxiScheduleInquiry;
	private final EventsManager eventsManager;
	private final MobsimTimer mobsimTimer;

	// Pre-computing paths (occupied drive tasks) when they are not needed immediately (destinationKnown is false):
	// In some configurations, the taxi optimiser may not know the destination until the passenger is picked up.
	// But we can obtain this information already on request submission and use it to pre-compute the origin-destination path in the background.
	// If sufficiently many threads are used, all such paths are found before the pickup simulation ends.
	// Consequently, QSim is not (directly) slowed down by computing these paths.
	private final ConcurrentMap<Id<Request>, Future<VrpPathWithTravelData>> pathFutures = new ConcurrentHashMap<>();
	private final ExecutorServiceWithResource<LeastCostPathCalculator> executorService;

	public TaxiScheduler(TaxiConfigGroup taxiCfg, Fleet fleet, TaxiScheduleInquiry taxiScheduleInquiry,
			TravelTime travelTime, Supplier<LeastCostPathCalculator> routerCreator, EventsManager eventsManager,
			MobsimTimer mobsimTimer) {
		this.taxiCfg = taxiCfg;
		this.fleet = fleet;
		this.taxiScheduleInquiry = taxiScheduleInquiry;
		this.travelTime = travelTime;
		this.eventsManager = eventsManager;
		this.mobsimTimer = mobsimTimer;

		router = routerCreator.get();

		executorService = taxiCfg.destinationKnown ?
				null :
				new ExecutorServiceWithResource<>(IntStream.range(0, taxiCfg.numberOfThreads)
						.mapToObj(i -> routerCreator.get())
						.collect(toList()));

		initFleet();
	}

	private void initFleet() {
		for (DvrpVehicle veh : fleet.getVehicles().values()) {
			veh.getSchedule()
					.addTask(new TaxiStayTask(veh.getServiceBeginTime(), veh.getServiceEndTime(), veh.getStartLink()));
		}
	}

	public TaxiScheduleInquiry getScheduleInquiry() {
		return taxiScheduleInquiry;
	}

	// =========================================================================================

	public void scheduleRequest(DvrpVehicle vehicle, DrtRequest request, VrpPathWithTravelData vrpPath) {
		Schedule schedule = vehicle.getSchedule();
		divertOrAppendDrive(schedule, vrpPath, TaxiEmptyDriveTask.TYPE);

		double pickupEndTime = Math.max(vrpPath.getArrivalTime(), request.getEarliestStartTime())
				+ taxiCfg.pickupDuration;
		schedule.addTask(new TaxiPickupTask(vrpPath.getArrivalTime(), pickupEndTime, request));

		Link reqFromLink = request.getFromLink();
		Link reqToLink = request.getToLink();
		final double dropoffStartTime;
		if (taxiCfg.destinationKnown) {
			// TODO use an estimate to set up the occupied drive task and then start computing the actual path in the background
			VrpPathWithTravelData path = calcPath(reqFromLink, reqToLink, pickupEndTime);
			appendOccupiedDriveAndDropoff(schedule, request, path);
			appendTasksAfterDropoff(vehicle);
			dropoffStartTime = path.getArrivalTime();
		} else {
			// pre-compute path for occupied drive; the occupied drive and subsequent tasks will be added after the pickup
			var pathFuture = executorService.submitCallable(
					router -> VrpPaths.calcAndCreatePath(reqFromLink, reqToLink, pickupEndTime, router, travelTime));
			pathFutures.put(request.getId(), pathFuture);
			dropoffStartTime = Double.NaN; // destination is unknown and so the arrival time
		}

		eventsManager.processEvent(
				new PassengerRequestScheduledEvent(mobsimTimer.getTimeOfDay(), request.getMode(), request.getId(),
						request.getPassengerIds(), vehicle.getId(), pickupEndTime, dropoffStartTime));
	}

	protected void divertOrAppendDrive(Schedule schedule, VrpPathWithTravelData vrpPath, TaxiTaskType taskType) {
		Task lastTask = Schedules.getLastTask(schedule);
		switch (getBaseTypeOrElseThrow(lastTask)) {
			case EMPTY_DRIVE -> divertDrive((TaxiEmptyDriveTask)lastTask, vrpPath);
			case STAY -> scheduleDrive(schedule, (TaxiStayTask)lastTask, vrpPath, taskType);
			default -> throw new IllegalStateException();
		}
	}

	protected void divertDrive(TaxiEmptyDriveTask lastTask, VrpPathWithTravelData vrpPath) {
		if (!taxiCfg.vehicleDiversion) {
			throw new IllegalStateException();
		}

		((OnlineDriveTaskTracker)lastTask.getTaskTracker()).divertPath(vrpPath);
	}

	protected void scheduleDrive(Schedule schedule, TaxiStayTask lastTask, VrpPathWithTravelData vrpPath,
			TaxiTaskType taskType) {
		switch (lastTask.getStatus()) {
			case PLANNED:
				if (lastTask.getBeginTime() == vrpPath.getDepartureTime()) { // waiting for 0 seconds!!!
					schedule.removeLastTask();// remove WaitTask
				} else {
					// actually this WAIT task will not be performed
					lastTask.setEndTime(vrpPath.getDepartureTime());// shortening the WAIT task
				}
				break;

			case STARTED:
				lastTask.setEndTime(vrpPath.getDepartureTime());// shortening the WAIT task
				break;

			case PERFORMED:
			default:
				throw new IllegalStateException();
		}

		if (vrpPath.getLinkCount() > 1) {
			schedule.addTask(new TaxiEmptyDriveTask(vrpPath, taskType));
		}
	}

	/**
	 * If diversion is enabled, this method must be called after scheduling in order to make sure that no vehicle is
	 * moving aimlessly.
	 * <p>
	 * </p>
	 * The reason: the destination/goal had been removed before scheduling (e.g. by calling the
	 * {@link #removeAwaitingRequestsFromAllSchedules()} method)
	 */
	public void stopAllAimlessDriveTasks() {
		for (DvrpVehicle veh : fleet.getVehicles().values()) {
			if (taxiScheduleInquiry.getImmediateDiversion(veh) != null) {
				stopVehicle(veh);
			}
		}
	}

	public void stopVehicle(DvrpVehicle vehicle) {
		if (!taxiCfg.vehicleDiversion) {
			throw new RuntimeException("Diversion must be on");
		}

		Schedule schedule = vehicle.getSchedule();
		TaxiEmptyDriveTask driveTask = (TaxiEmptyDriveTask)Schedules.getLastTask(schedule);

		OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker)driveTask.getTaskTracker();
		LinkTimePair stopPoint = tracker.getDiversionPoint();
		tracker.divertPath(
				new VrpPathWithTravelDataImpl(stopPoint.time, 0, new Link[] { stopPoint.link }, new double[] { 0 }));

		appendStayTask(vehicle);
	}

	/**
	 * Check and decide if the schedule should be updated due to if vehicle is Update timings (i.e. beginTime and
	 * endTime) of all tasks in the schedule.
	 */
	public void updateBeforeNextTask(DvrpVehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		// Assumption: there is no delay as long as the schedule has not been started (PLANNED)
		if (schedule.getStatus() != ScheduleStatus.STARTED) {
			return;
		}

		if (!taxiCfg.destinationKnown) {
			Task currentTask = schedule.getCurrentTask();
			if (PICKUP.isBaseTypeOf(currentTask)) {
				// use pre-computed path (occupied drive)
				var req = ((TaxiPickupTask)currentTask).getRequest();
				var path = Futures.getUnchecked(pathFutures.remove(req.getId()))
						.withDepartureTime(currentTask.getEndTime());
				appendOccupiedDriveAndDropoff(schedule, req, path);
				appendTasksAfterDropoff(vehicle);
			}
		}
	}

	protected void appendOccupiedDriveAndDropoff(Schedule schedule, DrtRequest req, VrpPathWithTravelData path) {
		schedule.addTask(new TaxiOccupiedDriveTask(path, req));

		double arrivalTime = path.getArrivalTime();
		double departureTime = arrivalTime + taxiCfg.dropoffDuration;
		schedule.addTask(new TaxiDropoffTask(arrivalTime, departureTime, req));
	}

	protected VrpPathWithTravelData calcPath(Link fromLink, Link toLink, double departureTime) {
		return VrpPaths.calcAndCreatePath(fromLink, toLink, departureTime, router, travelTime);
	}

	protected void appendTasksAfterDropoff(DvrpVehicle vehicle) {
		appendStayTask(vehicle);
	}

	protected void appendStayTask(DvrpVehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		double tBegin = schedule.getEndTime();
		double tEnd = Math.max(tBegin, vehicle.getServiceEndTime());// even 0-second WAIT
		Link link = Schedules.getLastLinkInSchedule(vehicle);
		schedule.addTask(new TaxiStayTask(tBegin, tEnd, link));
	}

	// =========================================================================================

	private List<DrtRequest> removedRequests;

	/**
	 * Awaiting == not picked-up (planned, but taxi may not yet be dispatched)
	 */
	public List<DrtRequest> removeAwaitingRequestsFromAllSchedules() {
		removedRequests = new ArrayList<>();
		for (DvrpVehicle veh : fleet.getVehicles().values()) {
			removeAwaitingRequestsImpl(veh);
		}

		return removedRequests;
	}

	private void removeAwaitingRequestsImpl(DvrpVehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		switch (schedule.getStatus()) {
			case STARTED:
				Integer unremovableTasksCount = countUnremovablePlannedTasks(schedule);
				if (unremovableTasksCount == null) {
					return;
				}

				int newLastTaskIdx = schedule.getCurrentTask().getTaskIdx() + unremovableTasksCount;
				removePlannedTasks(vehicle, newLastTaskIdx);
				cleanupScheduleAfterTaskRemoval(vehicle);
				return;

			case PLANNED:
				removePlannedTasks(vehicle, -1);
				cleanupScheduleAfterTaskRemoval(vehicle);
				return;

			case COMPLETED:
				return;

			case UNPLANNED:
				throw new IllegalStateException();
		}
	}

	protected Integer countUnremovablePlannedTasks(Schedule schedule) {
		Task currentTask = schedule.getCurrentTask();
		return switch (getBaseTypeOrElseThrow(currentTask)) {
			case PICKUP -> taxiCfg.destinationKnown ? 2 : null;

			case OCCUPIED_DRIVE -> 1;

			case EMPTY_DRIVE -> {
				if (taxiCfg.vehicleDiversion) {
					yield 0;
				}

				if (PICKUP.isBaseTypeOf(Schedules.getNextTask(schedule))) {
					// if no diversion and driving to pick up sb then serve that request
					yield taxiCfg.destinationKnown ? 3 : null;
				}

				// potentially: driving back to the rank (e.g. to charge batteries)
				throw new RuntimeException("Currently won't happen");
			}

			case DROPOFF, STAY -> 0;
		};
	}

	protected void removePlannedTasks(DvrpVehicle vehicle, int newLastTaskIdx) {
		Schedule schedule = vehicle.getSchedule();
		List<? extends Task> tasks = schedule.getTasks();
		for (int i = schedule.getTaskCount() - 1; i > newLastTaskIdx; i--) {
			Task task = tasks.get(i);
			schedule.removeTask(task);
			taskRemovedFromSchedule(vehicle, task);
		}
	}

	protected void taskRemovedFromSchedule(DvrpVehicle vehicle, Task task) {
		if (PICKUP.isBaseTypeOf(task)) {
			DrtRequest request = ((TaxiPickupTask)task).getRequest();
			removedRequests.add(request);
		}
	}

	// only for planned/started schedule
	private void cleanupScheduleAfterTaskRemoval(DvrpVehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
			schedule.addTask(new TaxiStayTask(vehicle.getServiceBeginTime(), vehicle.getServiceEndTime(),
					vehicle.getStartLink()));
			return;
		}
		// else: PLANNED, STARTED

		Task lastTask = Schedules.getLastTask(schedule);
		double tBegin = schedule.getEndTime();
		double tEnd = Math.max(tBegin, vehicle.getServiceEndTime());

		switch (getBaseTypeOrElseThrow(lastTask)) {
			case STAY -> lastTask.setEndTime(tEnd);

			case DROPOFF -> {
				Link link = Schedules.getLastLinkInSchedule(vehicle);
				schedule.addTask(new TaxiStayTask(tBegin, tEnd, link));
			}

			case EMPTY_DRIVE -> {
				if (!taxiCfg.vehicleDiversion) {
					throw new RuntimeException("Currently won't happen");
				}

				// if diversion -- no STAY afterwards
			}

			default -> throw new RuntimeException();
		}
	}

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		if (executorService != null) {
			executorService.shutdown();
		}
	}
}
