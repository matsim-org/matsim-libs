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

import static org.matsim.contrib.taxi.schedule.TaxiTaskBaseType.*;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelDataImpl;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.taxi.passenger.TaxiRequest;
import org.matsim.contrib.taxi.passenger.TaxiRequest.TaxiRequestStatus;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiDropoffTask;
import org.matsim.contrib.taxi.schedule.TaxiEmptyDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiOccupiedDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiPickupTask;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.schedule.TaxiTaskBaseType;
import org.matsim.contrib.taxi.schedule.TaxiTaskType;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.name.Named;

public class TaxiScheduler {
	protected final TaxiConfigGroup taxiCfg;
	private final Fleet fleet;
	private final TravelTime travelTime;
	private final LeastCostPathCalculator router;
	private final TaxiScheduleInquiry taxiScheduleInquiry;

	public TaxiScheduler(TaxiConfigGroup taxiCfg, Fleet fleet, TaxiScheduleInquiry taxiScheduleInquiry,
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime, LeastCostPathCalculator router) {
		this.taxiCfg = taxiCfg;
		this.fleet = fleet;
		this.taxiScheduleInquiry = taxiScheduleInquiry;
		this.travelTime = travelTime;
		this.router = router;

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

	public void scheduleRequest(DvrpVehicle vehicle, TaxiRequest request, VrpPathWithTravelData vrpPath) {
		if (request.getStatus() != TaxiRequestStatus.UNPLANNED) {
			throw new IllegalStateException();
		}

		Schedule schedule = vehicle.getSchedule();
		divertOrAppendDrive(schedule, vrpPath, TaxiEmptyDriveTask.TYPE);

		double pickupEndTime = Math.max(vrpPath.getArrivalTime(), request.getEarliestStartTime())
				+ taxiCfg.getPickupDuration();
		schedule.addTask(new TaxiPickupTask(vrpPath.getArrivalTime(), pickupEndTime, request));

		if (taxiCfg.isDestinationKnown()) {
			appendOccupiedDriveAndDropoff(schedule);
			appendTasksAfterDropoff(vehicle);
		}
	}

	protected void divertOrAppendDrive(Schedule schedule, VrpPathWithTravelData vrpPath, TaxiTaskType taskType) {
		Task lastTask = Schedules.getLastTask(schedule);
		switch (getBaseTypeOrElseThrow(lastTask)) {
			case EMPTY_DRIVE:
				divertDrive((TaxiEmptyDriveTask)lastTask, vrpPath);
				return;

			case STAY:
				scheduleDrive(schedule, (TaxiStayTask)lastTask, vrpPath, taskType);
				return;

			default:
				throw new IllegalStateException();
		}
	}

	protected void divertDrive(TaxiEmptyDriveTask lastTask, VrpPathWithTravelData vrpPath) {
		if (!taxiCfg.isVehicleDiversion()) {
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
		if (!taxiCfg.isVehicleDiversion()) {
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

		if (!taxiCfg.isDestinationKnown()) {
			Task currentTask = schedule.getCurrentTask();
			if (PICKUP.isBaseTypeOf(currentTask)) {
				appendOccupiedDriveAndDropoff(schedule);
				appendTasksAfterDropoff(vehicle);
			}
		}
	}

	protected void appendOccupiedDriveAndDropoff(Schedule schedule) {
		TaxiPickupTask pickupStayTask = (TaxiPickupTask)Schedules.getLastTask(schedule);

		// add DELIVERY after SERVE
		TaxiRequest req = pickupStayTask.getRequest();
		Link reqFromLink = req.getFromLink();
		Link reqToLink = req.getToLink();
		double t3 = pickupStayTask.getEndTime();

		VrpPathWithTravelData path = calcPath(reqFromLink, reqToLink, t3);
		schedule.addTask(new TaxiOccupiedDriveTask(path, req));

		double t4 = path.getArrivalTime();
		double t5 = t4 + taxiCfg.getDropoffDuration();
		schedule.addTask(new TaxiDropoffTask(t4, t5, req));
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

	private List<TaxiRequest> removedRequests;

	/**
	 * Awaiting == unpicked-up, i.e. requests with status PLANNED or TAXI_DISPATCHED See {@link TaxiRequestStatus}
	 */
	public List<TaxiRequest> removeAwaitingRequestsFromAllSchedules() {
		removedRequests = new ArrayList<>();
		for (DvrpVehicle veh : fleet.getVehicles().values()) {
			removeAwaitingRequestsImpl(veh);
		}

		return removedRequests;
	}

	public List<TaxiRequest> removeAwaitingRequests(DvrpVehicle vehicle) {
		removedRequests = new ArrayList<>();
		removeAwaitingRequestsImpl(vehicle);
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
		switch (getBaseTypeOrElseThrow(currentTask)) {
			case PICKUP:
				return taxiCfg.isDestinationKnown() ? 2 : null;

			case OCCUPIED_DRIVE:
				return 1;

			case EMPTY_DRIVE:
				if (taxiCfg.isVehicleDiversion()) {
					return 0;
				}

				if (PICKUP.isBaseTypeOf(Schedules.getNextTask(schedule))) {
					// if no diversion and driving to pick up sb then serve that request
					return taxiCfg.isDestinationKnown() ? 3 : null;
				}

				// potentially: driving back to the rank (e.g. to charge batteries)
				throw new RuntimeException("Currently won't happen");

			case DROPOFF:
			case STAY:
				return 0;

			default:
				throw new RuntimeException();
		}
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
		TaxiTaskBaseType baseType = getBaseTypeOrElseThrow(task);
		if (baseType == PICKUP) {
			TaxiRequest request = ((TaxiPickupTask)task).getRequest();
			request.setPickupTask(null);
			removedRequests.add(request);
		} else if (baseType == DROPOFF) {
			TaxiRequest request = ((TaxiDropoffTask)task).getRequest();
			request.setDropoffTask(null);
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
			case STAY:
				lastTask.setEndTime(tEnd);
				return;

			case DROPOFF:
				Link link = Schedules.getLastLinkInSchedule(vehicle);
				schedule.addTask(new TaxiStayTask(tBegin, tEnd, link));
				return;

			case EMPTY_DRIVE:
				if (!taxiCfg.isVehicleDiversion()) {
					throw new RuntimeException("Currently won't happen");
				}

				// if diversion -- no STAY afterwards
				return;

			default:
				throw new RuntimeException();
		}
	}
}
