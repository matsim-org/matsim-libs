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

package org.matsim.contrib.taxibus.algorithm.scheduler;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.taxibus.TaxibusRequest;
import org.matsim.contrib.taxibus.algorithm.scheduler.vehreqpath.TaxibusDispatch;
import org.matsim.contrib.taxibus.tasks.*;
import org.matsim.contrib.taxibus.tasks.TaxibusTask.TaxibusTaskType;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.tracker.*;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.taxi.data.TaxiRequest.TaxiRequestStatus;
import org.matsim.core.mobsim.framework.MobsimTimer;

public class TaxibusScheduler {

	private static final Logger log = Logger.getLogger(TaxibusScheduler.class);
	private TaxibusSchedulerParams params;
	private final Fleet vrpData;
	private final MobsimTimer timer;

	public TaxibusScheduler(Fleet vrpData, MobsimTimer timer, TaxibusSchedulerParams params) {
		this.vrpData = vrpData;
		this.timer = timer;
		this.params = params;

		((FleetImpl)vrpData).resetSchedules();

		for (Vehicle veh : this.vrpData.getVehicles().values()) {
			Schedule schedule = veh.getSchedule();
			schedule.addTask(new TaxibusStayTask(veh.getServiceBeginTime(), veh.getServiceEndTime(), veh.getStartLink()));
		}
	}

	public TaxibusSchedulerParams getParams() {
		return params;
	}

	public boolean isIdle(Vehicle vehicle) {
		if (!isStarted(vehicle)) {
			return false;
		}

		Schedule schedule = vehicle.getSchedule();
		TaxibusTask currentTask = (TaxibusTask)schedule.getCurrentTask();
		return currentTask.getTaskIdx() == schedule.getTaskCount() - 1 // last task
				&& currentTask.getTaxibusTaskType() == TaxibusTaskType.STAY;
	}

	public boolean isStarted(Vehicle vehicle) {
		if (timer.getTimeOfDay() >= vehicle.getServiceEndTime()
				|| vehicle.getSchedule().getStatus() != ScheduleStatus.STARTED) {
			return false;
		} else
			return true;
	}

	public LinkTimePair getImmediateDiversionOrEarliestIdleness(Vehicle veh) {
		if (params.vehicleDiversion) {
			LinkTimePair diversion = getImmediateDiversion(veh);
			if (diversion != null) {
				return diversion;
			}
		}

		return getEarliestIdleness(veh);
	}

	public LinkTimePair getEarliestIdleness(Vehicle veh) {
		if (timer.getTimeOfDay() >= veh.getServiceEndTime()) {// time window T1 exceeded
			return null;
		}

		Schedule schedule = veh.getSchedule();
		Link link;
		double time;

		switch (schedule.getStatus()) {
			case PLANNED:
			case STARTED:
				TaxibusTask lastTask = (TaxibusTask)Schedules.getLastTask(schedule);

				switch (lastTask.getTaxibusTaskType()) {
					case STAY:
						link = ((StayTask)lastTask).getLink();
						time = Math.max(lastTask.getBeginTime(), timer.getTimeOfDay());// TODO very optimistic!!!
						return createValidLinkTimePair(link, time, veh);

					case PICKUP:
						if (!params.destinationKnown) {
							return null;
						}
						// otherwise: IllegalStateException -- the schedule should end with WAIT

					default:
						throw new IllegalStateException();
				}

			case COMPLETED:
				return null;

			case UNPLANNED:// there is always at least one WAIT task in a schedule
			default:
				throw new IllegalStateException();
		}
	}

	public LinkTimePair getImmediateDiversion(Vehicle veh) {
		if (!params.vehicleDiversion) {
			throw new RuntimeException("Diversion must be on");
		}

		Schedule schedule = veh.getSchedule();

		if (/* context.getTime() >= veh.getT1() || */schedule.getStatus() != ScheduleStatus.STARTED) {
			return null;
		}

		TaxibusTask currentTask = (TaxibusTask)schedule.getCurrentTask();
		if (currentTask.getTaskIdx() != schedule.getTaskCount() - 1 // not last task
				|| currentTask.getTaxibusTaskType() != TaxibusTaskType.DRIVE_EMPTY) {
			return null;
		}

		OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker)currentTask.getTaskTracker();
		return filterValidLinkTimePair(tracker.getDiversionPoint(), veh);
	}

	private LinkTimePair filterValidLinkTimePair(LinkTimePair pair, Vehicle veh) {
		return pair.time >= veh.getServiceEndTime() ? null : pair;
	}

	private LinkTimePair createValidLinkTimePair(Link link, double time, Vehicle veh) {
		return time >= veh.getServiceEndTime() ? null : new LinkTimePair(link, time);
	}

	// =========================================================================================

	public void scheduleRequest(TaxibusDispatch best) {

		Schedule bestSched = best.vehicle.getSchedule();

		TaxibusTask lastTask = (TaxibusTask)Schedules.getLastTask(bestSched);
		// log.info("Bus " + best.vehicle.getId() + " Scheduled Route");
		// for (VrpPathWithTravelData path : best.path) {
		// log.info(path.getFromLink().getId() + " to " + path.getToLink().getId());
		// }
		// log.info("End of route");
		// log.info("scheduled to bus: ");
		// for (TaxibusRequest r : best.requests){
		// log.info(r+" Agent: "+ r.getPassenger().getId());
		// }
		Iterator<VrpPathWithTravelData> iterator = best.path.iterator();
		double lastEndTime;
		VrpPathWithTravelData path;
		Set<TaxibusRequest> onBoard = new LinkedHashSet<>();
		Set<TaxibusRequest> droppedOff = new LinkedHashSet<>();
		Set<TaxibusRequest> pickedUp = new LinkedHashSet<>();
		TreeSet<TaxibusRequest> pickUpsForLink = null;
		TreeSet<TaxibusRequest> dropOffsForLink = null;
		if (lastTask.getTaxibusTaskType() == TaxibusTaskType.STAY) {
			best.failIfAnyRequestNotUnplanned();
			path = iterator.next();
			scheduleDriveToFirstRequest((TaxibusStayTask)lastTask, bestSched, path);
			lastEndTime = path.getArrivalTime();
			path = iterator.next();
			pickUpsForLink = best.getPickUpsForLink(path.getFromLink());
			dropOffsForLink = best.getDropOffsForLink(path.getFromLink());
			// double lastEndTime = path.getDepartureTime();
			if (pickUpsForLink != null) {
				lastEndTime = schedulePickups(bestSched, lastEndTime, onBoard, pickedUp, pickUpsForLink);

			} else {
				// it shouldnt be null for the first pickup
				throw new IllegalStateException();
			}
			if (dropOffsForLink != null) {
				// this is the very first pickup, anyone who would be dropped
				// off here would hence not really ride on the bus...
				lastEndTime = scheduleDropOffs(bestSched, onBoard, dropOffsForLink, droppedOff, lastEndTime);
			}

		} else if (lastTask.getTaxibusTaskType() == TaxibusTaskType.PICKUP) {
			// best.failIfRequestNotUnplannedOrDispatched();
			lastEndTime = lastTask.getEndTime();
			path = iterator.next();

		} else {
			for (Task task : bestSched.getTasks()) {
				log.error(task.toString() /* +" " +task.getType() */ + " " + task.getStatus());
			}
			throw new IllegalStateException();
		}

		while (iterator.hasNext()) {
			// log.info("to:" + path.getToLink().getId());
			if (path.getFromLink() != path.getToLink()) {

				lastEndTime = scheduleDriveAlongPath(bestSched, path, onBoard, lastEndTime);

			} else {
				path = iterator.next();
				continue;
			}
			dropOffsForLink = best.getDropOffsForLink(path.getToLink());
			if (dropOffsForLink != null) {

				lastEndTime = scheduleDropOffs(bestSched, onBoard, dropOffsForLink, droppedOff, lastEndTime);
			}
			pickUpsForLink = best.getPickUpsForLink(path.getToLink());
			if (pickUpsForLink != null) {
				lastEndTime = schedulePickups(bestSched, lastEndTime, onBoard, pickedUp, pickUpsForLink);

			}
			path = iterator.next();

		}
		if (path.getFromLink() != path.getToLink()) {

			lastEndTime = scheduleDriveAlongPath(bestSched, path, onBoard, lastEndTime);

		}
		// last path, we might have some people to still drop off. Would not
		// be the case if last path is an empty ride, i.e. back to the depot:
		dropOffsForLink = best.getDropOffsForLink(path.getToLink());

		if (dropOffsForLink != null) {

			lastEndTime = scheduleDropOffs(bestSched, onBoard, dropOffsForLink, droppedOff, lastEndTime);

		}
		if (!onBoard.isEmpty()) {
			log.error("we forgot someone, expected route: ");
			for (TaxibusRequest r : onBoard) {
				log.error("pax:" + r.getPassenger().getId() + " from: " + r.getFromLink().getId() + " to "
						+ r.getToLink().getId());
				for (VrpPathWithTravelData desiredPath : best.path) {
					log.info(desiredPath.getFromLink().getId() + " to " + desiredPath.getToLink().getId());
				}
				log.info("End of route");
			}
			throw new IllegalStateException();
			// we forgot a customer?
		}

		appendTasksAfterDropoff(best.vehicle);

		// log.info("Done Scheduling");
	}

	private double scheduleDropOffs(Schedule bestSched, Set<TaxibusRequest> onBoard, TreeSet<TaxibusRequest> dropOffsForLink,
			Set<TaxibusRequest> droppedOff, double beginTime) {
		for (TaxibusRequest req : dropOffsForLink) {
			if (!onBoard.contains(req)) {
				continue;
			}
			if (droppedOff.contains(req)) {
				continue;
			}
			double endTime = beginTime + params.dropoffDuration;
			bestSched.addTask(new TaxibusDropoffTask(beginTime, endTime, Collections.singleton(req)));
			// log.info("schedule dropoff" +
			// req.getPassenger().getId().toString()+ " at link
			// "+req.getToLink().getId());
			beginTime = endTime;
			if (!onBoard.remove(req)) {
				// throw new IllegalStateException("Dropoff without pickup.");
			}
			droppedOff.add(req);

		}
		return beginTime;
	}

	private double schedulePickups(Schedule bestSched, double beginTime, Set<TaxibusRequest> onBoard,
			Set<TaxibusRequest> pickedUp, TreeSet<TaxibusRequest> pickUpsForLink) {

		for (TaxibusRequest req : pickUpsForLink) {
			if (pickedUp.contains(req))
				continue;
			double t3 = Math.max(beginTime, req.getEarliestStartTime()) + params.pickupDuration;
			bestSched.addTask(new TaxibusPickupTask(beginTime, t3, Collections.singleton(req)));
			// log.info("schedule pickup" +
			// req.getPassenger().getId().toString() + " at link "+req.getFromLink().getId());
			onBoard.add(req);
			beginTime = t3;
			pickedUp.add(req);
		}
		return beginTime;
	}

	private double scheduleDriveAlongPath(Schedule bestSched, VrpPathWithTravelData path, Set<TaxibusRequest> onBoard,
			double lastEndtime) {

		// VrpPathWithTravelData updatedPath = new
		// VrpPathWithTravelDataImpl(lastEndtime,
		// path.getTravelTime(), path.getTravelCost(), path.getLinks(),
		// path.getLinkTTs());

		TaxibusDriveWithPassengersTask task = new TaxibusDriveWithPassengersTask(path, onBoard);
		task.setBeginTime(lastEndtime);
		// for (int i = 0; i < path.getLinkCount(); i++) {
		// log.info(path.getLink(i).getId().toString());
		// }
		double endTime = lastEndtime + path.getTravelTime();
		task.setEndTime(endTime);
		bestSched.addTask(task);

		return endTime;
	}

	private void scheduleDriveToFirstRequest(TaxibusStayTask lastTask, Schedule bestSched, VrpPathWithTravelData path) {
		switch (lastTask.getStatus()) {
			case STARTED:
				// bus is already idle
				lastTask.setEndTime(path.getDepartureTime());// shortening the WAIT task
				break;

			case PLANNED:

				if (lastTask.getBeginTime() == path.getDepartureTime()) { // waiting for 0 seconds!!!
					bestSched.removeLastTask();// remove WaitTask
				} else {
					// actually this WAIT task will not be performed
					lastTask.setEndTime(path.getDepartureTime());// shortening the WAIT task
				}
				break;

			case PERFORMED:
			default:
				throw new IllegalStateException();
		}

		if (path.getLinkCount() > 1) {
			bestSched.addTask(new TaxibusEmptyDriveTask(path));
		}
	}

	/**
	 * Check and decide if the schedule should be updated due to if vehicle is Update timings (i.e. beginTime and
	 * endTime) of all tasks in the schedule.
	 */
	public void updateBeforeNextTask(Schedule schedule) {
		// Assumption: there is no delay as long as the schedule has not been
		// started (PLANNED)
		if (schedule.getStatus() != ScheduleStatus.STARTED) {
			return;
		}

		double endTime = timer.getTimeOfDay();

		updateTimelineImpl(schedule, endTime);

	}

	protected void appendTasksAfterDropoff(Vehicle vehicle) {
		appendStayTask(vehicle);
	}

	protected void appendStayTask(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		double tBegin = schedule.getEndTime();
		double tEnd = Math.max(tBegin, vehicle.getServiceEndTime());// even 0-second WAIT
		Link link = Schedules.getLastLinkInSchedule(vehicle);
		schedule.addTask(new TaxibusStayTask(tBegin, tEnd, link));
	}

	public void updateTimeline(Schedule schedule) {
		if (schedule.getStatus() != ScheduleStatus.STARTED) {
			return;
		}

		double predictedEndTime = TaskTrackers.predictEndTime(schedule.getCurrentTask(), timer.getTimeOfDay());
		updateTimelineImpl(schedule, predictedEndTime);
	}

	private void updateTimelineImpl(Schedule schedule, double newTaskEndTime) {
		Task currentTask = schedule.getCurrentTask();
		if (currentTask.getEndTime() == newTaskEndTime) {
			return;
		}

		currentTask.setEndTime(newTaskEndTime);

		List<? extends Task> tasks = schedule.getTasks();
		int startIdx = currentTask.getTaskIdx() + 1;
		double t = newTaskEndTime;

		for (int i = startIdx; i < tasks.size(); i++) {
			TaxibusTask task = (TaxibusTask)tasks.get(i);

			switch (task.getTaxibusTaskType()) {
				case STAY: {
					if (i == tasks.size() - 1) {// last task
						task.setBeginTime(t);

						if (task.getEndTime() < t) {// may happen if the previous task is delayed
							task.setEndTime(t);// do not remove this task!!! A taxi schedule should end with WAIT
						}
					} else {
						// if this is not the last task then some other task (e.g. DRIVE or PICKUP)
						// must have been added at time submissionTime <= t
						double endTime = task.getEndTime();
						if (endTime <= t) {// may happen if the previous task is delayed
							schedule.removeTask(task);
							i--;
						} else {
							task.setBeginTime(t);
							t = endTime;
						}
					}

					break;
				}

				case DRIVE_EMPTY:
				case DRIVE_WITH_PASSENGERS: {
					// cannot be shortened/lengthen, therefore must be moved forward/backward
					task.setBeginTime(t);
					VrpPathWithTravelData path = (VrpPathWithTravelData)((DriveTask)task).getPath();
					t += path.getTravelTime(); // TODO one may consider recalculation of SP!!!!
					task.setEndTime(t);

					break;
				}

				case PICKUP: {
					task.setBeginTime(t);// t == taxi's arrival time
					double t0 = ((TaxibusPickupTask)task).getRequests().iterator().next().getEarliestStartTime();
					// t0 == passenger's departure time
					t = Math.max(t, t0) + params.pickupDuration; // the true pickup starts at max(t, t0)
					task.setEndTime(t);

					break;
				}
				case DROPOFF: {
					// cannot be shortened/lengthen, therefore must be moved forward/backward
					task.setBeginTime(t);
					t += params.dropoffDuration;
					task.setEndTime(t);

					break;
				}
			}
		}
	}

	// =========================================================================================

	/**
	 * Awaiting == unpicked-up, i.e. requests with status PLANNED or TAXI_DISPATCHED See {@link TaxiRequestStatus}
	 */

	public Set<TaxibusRequest> getCurrentlyPlannedRequests(Schedule schedule) {
		Set<TaxibusRequest> plannedRequests = new HashSet<>();
		List<? extends Task> tasks = schedule.getTasks();

		for (int i = schedule.getTaskCount() - 1; i > schedule.getCurrentTask().getTaskIdx(); i--) {
			Task task = tasks.get(i);
			if (task instanceof TaxibusTaskWithRequests) {
				plannedRequests.addAll(((TaxibusTaskWithRequests)task).getRequests());
			}

		}

		return plannedRequests;
	}

	public void stopAllAimlessDriveTasks() {
		// TODO Auto-generated method stub

	}
}
