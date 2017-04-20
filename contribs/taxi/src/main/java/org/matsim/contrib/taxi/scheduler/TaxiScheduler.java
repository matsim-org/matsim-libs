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

import java.util.*;

import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.tracker.*;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.data.TaxiRequest.TaxiRequestStatus;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.*;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.*;
import org.matsim.core.router.util.*;
import org.matsim.core.utils.misc.Time;

public class TaxiScheduler implements TaxiScheduleInquiry {
	private final Fleet fleet;
	protected final TaxiSchedulerParams params;
	private final MobsimTimer timer;

	private final TravelTime travelTime;
	private final LeastCostPathCalculator router;

	public TaxiScheduler(TaxiConfigGroup taxiCfg, Network network, Fleet fleet, MobsimTimer timer,
			TaxiSchedulerParams params, TravelTime travelTime, TravelDisutility travelDisutility) {
		this.fleet = fleet;
		this.params = params;
		this.timer = timer;
		this.travelTime = travelTime;

		PreProcessEuclidean preProcessEuclidean = new PreProcessEuclidean(travelDisutility);
		preProcessEuclidean.run(network);

		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();
		RoutingNetwork routingNetwork = new ArrayRoutingNetworkFactory(preProcessEuclidean)
				.createRoutingNetwork(network);

		router = new FastAStarEuclidean(routingNetwork, preProcessEuclidean, travelDisutility, travelTime,
				params.AStarEuclideanOverdoFactor, fastRouterFactory);

		initFleet(taxiCfg);
	}

	private void initFleet(TaxiConfigGroup taxiCfg) {
		if (taxiCfg.isChangeStartLinkToLastLinkInSchedule()) {
			for (Vehicle veh : fleet.getVehicles().values()) {
				Vehicles.changeStartLinkToLastLinkInSchedule(veh);
			}
		}

		((FleetImpl)fleet).resetSchedules();
		for (Vehicle veh : fleet.getVehicles().values()) {
			veh.getSchedule()
					.addTask(new TaxiStayTask(veh.getServiceBeginTime(), veh.getServiceEndTime(), veh.getStartLink()));
		}
	}

	public TaxiSchedulerParams getParams() {
		return params;
	}

	@Override
	public boolean isIdle(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		if (timer.getTimeOfDay() >= vehicle.getServiceEndTime() || schedule.getStatus() != ScheduleStatus.STARTED) {
			return false;
		}

		TaxiTask currentTask = (TaxiTask)schedule.getCurrentTask();
		return currentTask.getTaskIdx() == schedule.getTaskCount() - 1 // last task
				&& currentTask.getTaxiTaskType() == TaxiTaskType.STAY;
	}

	/**
	 * If the returned LinkTimePair is not null, then time is not smaller than the current time
	 */
	@Override
	public LinkTimePair getImmediateDiversionOrEarliestIdleness(Vehicle veh) {
		if (params.vehicleDiversion) {
			LinkTimePair diversion = getImmediateDiversion(veh);
			if (diversion != null) {
				return diversion;
			}
		}

		return getEarliestIdleness(veh);
	}

	/**
	 * If the returned LinkTimePair is not null, then time is not smaller than the current time
	 */
	@Override
	public LinkTimePair getEarliestIdleness(Vehicle veh) {
		if (timer.getTimeOfDay() >= veh.getServiceEndTime()) {// time window exceeded
			return null;
		}

		Schedule schedule = veh.getSchedule();
		Link link;
		double time;

		switch (schedule.getStatus()) {
			case PLANNED:
			case STARTED:
				TaxiTask lastTask = (TaxiTask)Schedules.getLastTask(schedule);

				switch (lastTask.getTaxiTaskType()) {
					case STAY:
						link = ((StayTask)lastTask).getLink();
						time = Math.max(lastTask.getBeginTime(), timer.getTimeOfDay());// TODO very optimistic!!!
						return createValidLinkTimePair(link, time, veh);

					case PICKUP:
						if (!params.destinationKnown) {
							return null;
						}
						// otherwise: IllegalStateException -- the schedule should end with STAY (or PICKUP if
						// unfinished)

					default:
						throw new IllegalStateException(
								"Type of the last task is wrong: " + lastTask.getTaxiTaskType());
				}

			case COMPLETED:
				return null;

			case UNPLANNED:// there is always at least one STAY task in a schedule
			default:
				throw new IllegalStateException();
		}
	}

	/**
	 * If the returned LinkTimePair is not null, then time is not smaller than the current time
	 */
	@Override
	public LinkTimePair getImmediateDiversion(Vehicle veh) {
		if (!params.vehicleDiversion) {
			throw new RuntimeException("Diversion must be on");
		}

		Schedule schedule = veh.getSchedule();
		if (/* context.getTime() >= veh.getT1() || */schedule.getStatus() != ScheduleStatus.STARTED) {
			return null;
		}

		TaxiTask currentTask = (TaxiTask)schedule.getCurrentTask();
		// no prebooking ==> we can divert vehicle whose current task is an empty drive at the end of the schedule
		if (currentTask.getTaskIdx() != schedule.getTaskCount() - 1 // not last task
				|| currentTask.getTaxiTaskType() != TaxiTaskType.EMPTY_DRIVE) {
			return null;
		}

		OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker)currentTask.getTaskTracker();
		return filterValidLinkTimePair(tracker.getDiversionPoint(), veh);
	}

	private static LinkTimePair filterValidLinkTimePair(LinkTimePair pair, Vehicle veh) {
		return pair.time >= veh.getServiceEndTime() ? null : pair;
	}

	private static LinkTimePair createValidLinkTimePair(Link link, double time, Vehicle veh) {
		return time >= veh.getServiceEndTime() ? null : new LinkTimePair(link, time);
	}

	// =========================================================================================

	public void scheduleRequest(Vehicle vehicle, TaxiRequest request, VrpPathWithTravelData vrpPath) {
		if (request.getStatus() != TaxiRequestStatus.UNPLANNED) {
			throw new IllegalStateException();
		}

		Schedule schedule = vehicle.getSchedule();
		divertOrAppendDrive(schedule, vrpPath);

		double pickupEndTime = Math.max(vrpPath.getArrivalTime(), request.getEarliestStartTime())
				+ params.pickupDuration;
		schedule.addTask(new TaxiPickupTask(vrpPath.getArrivalTime(), pickupEndTime, request));

		if (params.destinationKnown) {
			appendOccupiedDriveAndDropoff(schedule);
			appendTasksAfterDropoff(vehicle);
		}
	}

	protected void divertOrAppendDrive(Schedule schedule, VrpPathWithTravelData vrpPath) {
		TaxiTask lastTask = (TaxiTask)Schedules.getLastTask(schedule);
		switch (lastTask.getTaxiTaskType()) {
			case EMPTY_DRIVE:
				divertDrive((TaxiEmptyDriveTask)lastTask, vrpPath);
				return;

			case STAY:
				scheduleDrive(schedule, (TaxiStayTask)lastTask, vrpPath);
				return;

			default:
				throw new IllegalStateException();
		}
	}

	protected void divertDrive(TaxiEmptyDriveTask lastTask, VrpPathWithTravelData vrpPath) {
		if (!params.vehicleDiversion) {
			throw new IllegalStateException();
		}

		((OnlineDriveTaskTracker)lastTask.getTaskTracker()).divertPath(vrpPath);
	}

	protected void scheduleDrive(Schedule schedule, TaxiStayTask lastTask, VrpPathWithTravelData vrpPath) {
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
			schedule.addTask(new TaxiEmptyDriveTask(vrpPath));
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
		for (Vehicle veh : fleet.getVehicles().values()) {
			if (getImmediateDiversion(veh) != null) {
				stopVehicle(veh);
			}
		}
	}

	public void stopVehicle(Vehicle vehicle) {
		if (!params.vehicleDiversion) {
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
	public void updateBeforeNextTask(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		// Assumption: there is no delay as long as the schedule has not been started (PLANNED)
		if (schedule.getStatus() != ScheduleStatus.STARTED) {
			return;
		}

		updateTimelineImpl(vehicle, timer.getTimeOfDay());

		if (!params.destinationKnown) {
			TaxiTask currentTask = (TaxiTask)schedule.getCurrentTask();
			if (currentTask.getTaxiTaskType() == TaxiTaskType.PICKUP) {
				appendOccupiedDriveAndDropoff(schedule);
				appendTasksAfterDropoff(vehicle);
			}
		}
	}

	protected void appendOccupiedDriveAndDropoff(Schedule schedule) {
		TaxiPickupTask pickupStayTask = (TaxiPickupTask)Schedules.getLastTask(schedule);

		// add DELIVERY after SERVE
		TaxiRequest req = ((TaxiPickupTask)pickupStayTask).getRequest();
		Link reqFromLink = req.getFromLink();
		Link reqToLink = req.getToLink();
		double t3 = pickupStayTask.getEndTime();

		VrpPathWithTravelData path = calcPath(reqFromLink, reqToLink, t3);
		schedule.addTask(new TaxiOccupiedDriveTask(path, req));

		double t4 = path.getArrivalTime();
		double t5 = t4 + params.dropoffDuration;
		schedule.addTask(new TaxiDropoffTask(t4, t5, req));
	}

	protected VrpPathWithTravelData calcPath(Link fromLink, Link toLink, double departureTime) {
		return VrpPaths.calcAndCreatePath(fromLink, toLink, departureTime, router, travelTime);
	}

	protected void appendTasksAfterDropoff(Vehicle vehicle) {
		appendStayTask(vehicle);
	}

	protected void appendStayTask(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		double tBegin = schedule.getEndTime();
		double tEnd = Math.max(tBegin, vehicle.getServiceEndTime());// even 0-second WAIT
		Link link = Schedules.getLastLinkInSchedule(vehicle);
		schedule.addTask(new TaxiStayTask(tBegin, tEnd, link));
	}

	public void updateTimeline(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() != ScheduleStatus.STARTED) {
			return;
		}

		double predictedEndTime = TaskTrackers.predictEndTime(schedule.getCurrentTask(), timer.getTimeOfDay());
		updateTimelineImpl(vehicle, predictedEndTime);
	}

	private void updateTimelineImpl(Vehicle vehicle, double newEndTime) {
		Schedule schedule = vehicle.getSchedule();
		Task currentTask = schedule.getCurrentTask();
		if (currentTask.getEndTime() == newEndTime) {
			return;
		}

		currentTask.setEndTime(newEndTime);

		List<? extends Task> tasks = schedule.getTasks();
		int startIdx = currentTask.getTaskIdx() + 1;
		double newBeginTime = newEndTime;

		for (int i = startIdx; i < tasks.size(); i++) {
			TaxiTask task = (TaxiTask)tasks.get(i);
			double calcEndTime = calcNewEndTime(vehicle, task, newBeginTime);

			if (calcEndTime == Time.UNDEFINED_TIME) {
				schedule.removeTask(task);
				i--;
			} else if (calcEndTime < newBeginTime) {// 0 s is fine (e.g. last 'wait')
				throw new IllegalStateException();
			} else {
				task.setBeginTime(newBeginTime);
				task.setEndTime(calcEndTime);
				newBeginTime = calcEndTime;
			}
		}
	}

	protected double calcNewEndTime(Vehicle vehicle, TaxiTask task, double newBeginTime) {
		switch (task.getTaxiTaskType()) {
			case STAY: {
				if (Schedules.getLastTask(vehicle.getSchedule()).equals(task)) {// last task
					// even if endTime=beginTime, do not remove this task!!! A taxi schedule should end with WAIT
					return Math.max(newBeginTime, vehicle.getServiceEndTime());
				} else {
					// if this is not the last task then some other task (e.g. DRIVE or PICKUP)
					// must have been added at time submissionTime <= t
					double oldEndTime = task.getEndTime();
					if (oldEndTime <= newBeginTime) {// may happen if the previous task is delayed
						return Time.UNDEFINED_TIME;// remove the task
					} else {
						return oldEndTime;
					}
				}
			}

			case EMPTY_DRIVE:
			case OCCUPIED_DRIVE: {
				// cannot be shortened/lengthen, therefore must be moved forward/backward
				VrpPathWithTravelData path = (VrpPathWithTravelData)((DriveTask)task).getPath();
				// TODO one may consider recalculation of SP!!!!
				return newBeginTime + path.getTravelTime();
			}

			case PICKUP: {
				double t0 = ((TaxiPickupTask)task).getRequest().getEarliestStartTime();
				// the actual pickup starts at max(t, t0)
				return Math.max(newBeginTime, t0) + params.pickupDuration;
			}
			case DROPOFF: {
				// cannot be shortened/lengthen, therefore must be moved forward/backward
				return newBeginTime + params.dropoffDuration;
			}

			default:
				throw new IllegalStateException();
		}
	}

	// =========================================================================================

	private List<TaxiRequest> removedRequests;

	/**
	 * Awaiting == unpicked-up, i.e. requests with status PLANNED or TAXI_DISPATCHED See {@link TaxiRequestStatus}
	 */
	public List<TaxiRequest> removeAwaitingRequestsFromAllSchedules() {
		removedRequests = new ArrayList<>();
		for (Vehicle veh : fleet.getVehicles().values()) {
			removeAwaitingRequestsImpl(veh);
		}

		return removedRequests;
	}

	public List<TaxiRequest> removeAwaitingRequests(Vehicle vehicle) {
		removedRequests = new ArrayList<>();
		removeAwaitingRequestsImpl(vehicle);
		return removedRequests;
	}

	private void removeAwaitingRequestsImpl(Vehicle vehicle) {
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
		TaxiTask currentTask = (TaxiTask)schedule.getCurrentTask();
		switch (currentTask.getTaxiTaskType()) {
			case PICKUP:
				return params.destinationKnown ? 2 : null;

			case OCCUPIED_DRIVE:
				return 1;

			case EMPTY_DRIVE:
				if (params.vehicleDiversion) {
					return 0;
				}

				if (((TaxiTask)Schedules.getNextTask(schedule)).getTaxiTaskType() == TaxiTaskType.PICKUP) {
					// if no diversion and driving to pick up sb then serve that request
					return params.destinationKnown ? 3 : null;
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

	protected void removePlannedTasks(Vehicle vehicle, int newLastTaskIdx) {
		Schedule schedule = vehicle.getSchedule();
		List<? extends Task> tasks = schedule.getTasks();
		for (int i = schedule.getTaskCount() - 1; i > newLastTaskIdx; i--) {
			TaxiTask task = (TaxiTask)tasks.get(i);
			schedule.removeTask(task);
			taskRemovedFromSchedule(vehicle, task);
		}
	}

	protected void taskRemovedFromSchedule(Vehicle vehicle, TaxiTask task) {
		if (task instanceof TaxiTaskWithRequest) {
			TaxiRequest request = ((TaxiTaskWithRequest)task).getRequest();

			if (task.getTaxiTaskType() == TaxiTaskType.PICKUP) {
				request.setPickupTask(null);
				removedRequests.add(request);
			} else if (task.getTaxiTaskType() == TaxiTaskType.DROPOFF) {
				request.setDropoffTask(null);
			}
		}
	}

	// only for planned/started schedule
	private void cleanupScheduleAfterTaskRemoval(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
			schedule.addTask(new TaxiStayTask(vehicle.getServiceBeginTime(), vehicle.getServiceEndTime(),
					vehicle.getStartLink()));
			return;
		}
		// else: PLANNED, STARTED

		TaxiTask lastTask = (TaxiTask)Schedules.getLastTask(schedule);
		double tBegin = schedule.getEndTime();
		double tEnd = Math.max(tBegin, vehicle.getServiceEndTime());

		switch (lastTask.getTaxiTaskType()) {
			case STAY:
				lastTask.setEndTime(tEnd);
				return;

			case DROPOFF:
				Link link = Schedules.getLastLinkInSchedule(vehicle);
				schedule.addTask(new TaxiStayTask(tBegin, tEnd, link));
				return;

			case EMPTY_DRIVE:
				if (!params.vehicleDiversion) {
					throw new RuntimeException("Currently won't happen");
				}

				// if diversion -- no STAY afterwards
				return;

			default:
				throw new RuntimeException();
		}
	}
}
