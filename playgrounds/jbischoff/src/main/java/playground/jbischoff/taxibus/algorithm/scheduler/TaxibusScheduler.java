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

package playground.jbischoff.taxibus.algorithm.scheduler;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.tracker.*;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.mobsim.framework.MobsimTimer;

import playground.jbischoff.drt.scheduler.tasks.DrtDriveTask;
import playground.jbischoff.drt.scheduler.tasks.DrtDriveWithPassengerTask;
import playground.jbischoff.drt.scheduler.tasks.DrtDropoffTask;
import playground.jbischoff.drt.scheduler.tasks.DrtPickupTask;
import playground.jbischoff.drt.scheduler.tasks.DrtStayTask;
import playground.jbischoff.drt.scheduler.tasks.DrtTask;
import playground.jbischoff.drt.scheduler.tasks.DrtTaskWithRequests;
import playground.jbischoff.drt.scheduler.tasks.DrtTask.DrtTaskType;
import playground.jbischoff.taxibus.algorithm.passenger.TaxibusRequest;
import playground.jbischoff.taxibus.algorithm.scheduler.vehreqpath.TaxibusDispatch;

public class TaxibusScheduler {

	private static final Logger log = Logger.getLogger(TaxibusScheduler.class);
	private TaxibusSchedulerParams params;
	private final VrpData vrpData;
	private final MobsimTimer timer;

	public TaxibusScheduler(VrpData vrpData, MobsimTimer timer, TaxibusSchedulerParams params) {
		this.vrpData = vrpData;
		this.timer = timer;
		this.params = params;

        vrpData.clearRequestsAndResetSchedules();

		for (Vehicle veh : this.vrpData.getVehicles().values()) {
			Schedule<DrtTask> schedule = (Schedule<DrtTask>) veh.getSchedule();
			schedule.addTask(new DrtStayTask(veh.getT0(), veh.getT1(), veh.getStartLink()));
		}
	}

	public TaxibusSchedulerParams getParams() {
		return params;
	}

	public boolean isIdle(Vehicle vehicle) {
		if (!isStarted(vehicle))
			return false;
		Schedule<DrtTask> schedule = (Schedule<DrtTask>) vehicle.getSchedule();
		DrtTask currentTask = schedule.getCurrentTask();
		return Schedules.isLastTask(currentTask) && currentTask.getDrtTaskType() == DrtTaskType.STAY;
	}

	public boolean isStarted(Vehicle vehicle) {
		Schedule<DrtTask> schedule = (Schedule<DrtTask>) vehicle.getSchedule();
		if (timer.getTimeOfDay() >= vehicle.getT1() || schedule.getStatus() != ScheduleStatus.STARTED) {
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
		if (timer.getTimeOfDay() >= veh.getT1()) {// time window T1 exceeded
			return null;
		}

		Schedule<DrtTask> schedule = (Schedule<DrtTask>) veh.getSchedule();
		Link link;
		double time;

		switch (schedule.getStatus()) {
		case PLANNED:
		case STARTED:
			DrtTask lastTask = Schedules.getLastTask(schedule);

			switch (lastTask.getDrtTaskType()) {
			case STAY:
				link = ((StayTask) lastTask).getLink();
				time = Math.max(lastTask.getBeginTime(), timer.getTimeOfDay());// TODO
																				// very
																				// optimistic!!!
				return createValidLinkTimePair(link, time, veh);

			case PICKUP:
				if (!params.destinationKnown) {
					return null;
				}
				// otherwise: IllegalStateException -- the schedule should end
				// with WAIT

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

		Schedule<DrtTask> schedule = (Schedule<DrtTask>) veh.getSchedule();

		if (/* context.getTime() >= veh.getT1() || */schedule.getStatus() != ScheduleStatus.STARTED) {
			return null;
		}

		DrtTask currentTask = schedule.getCurrentTask();
		if (!Schedules.isLastTask(currentTask) || currentTask.getDrtTaskType() != DrtTaskType.DRIVE_EMPTY) {
			return null;
		}

		OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker) currentTask.getTaskTracker();
		return filterValidLinkTimePair(tracker.getDiversionPoint(), veh);
	}

	private LinkTimePair filterValidLinkTimePair(LinkTimePair pair, Vehicle veh) {
		return pair.time >= veh.getT1() ? null : pair;
	}

	private LinkTimePair createValidLinkTimePair(Link link, double time, Vehicle veh) {
		return time >= veh.getT1() ? null : new LinkTimePair(link, time);
	}

	// =========================================================================================

	public void scheduleRequest(TaxibusDispatch best) {

		Schedule<DrtTask> bestSched = (Schedule<DrtTask>) best.vehicle.getSchedule();

		DrtTask lastTask = Schedules.getLastTask(bestSched);
//		log.info("Bus " + best.vehicle.getId() + " Scheduled Route");
//		for (VrpPathWithTravelData path : best.path) {
//			log.info(path.getFromLink().getId() + " to " + path.getToLink().getId());
//		}
//		log.info("End of route");
//		log.info("scheduled to bus: ");
//		for (TaxibusRequest r : best.requests){
//			log.info(r+" Agent: "+ r.getPassenger().getId());
//		}
		Iterator<VrpPathWithTravelData> iterator = best.path.iterator();
		double lastEndTime;
		VrpPathWithTravelData path;
		Set<TaxibusRequest> onBoard = new LinkedHashSet<>();
		Set<TaxibusRequest> droppedOff = new LinkedHashSet<>();
		Set<TaxibusRequest> pickedUp = new LinkedHashSet<>();
		TreeSet<TaxibusRequest> pickUpsForLink = null;
		TreeSet<TaxibusRequest> dropOffsForLink = null;
		if (lastTask.getDrtTaskType() == DrtTaskType.STAY) {
			best.failIfAnyRequestNotUnplanned();
			path = iterator.next();
			scheduleDriveToFirstRequest((DrtStayTask) lastTask, bestSched, path);
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

		} else if (lastTask.getDrtTaskType() == DrtTaskType.PICKUP) {
//			best.failIfRequestNotUnplannedOrDispatched();
			lastEndTime = lastTask.getEndTime();
			path = iterator.next();

		} else {
			for (DrtTask task : bestSched.getTasks()){
				log.error(task.toString()+" " +task.getType() +" "+task.getStatus() );
			}
			throw new IllegalStateException();
		}

		while (iterator.hasNext()) {
//			log.info("to:" + path.getToLink().getId());
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
			// be the case if last path is an empty ride, i.e. back to the
			// depot:
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

		appendTasksAfterDropoff(bestSched);
		
//		log.info("Done Scheduling");
	}

	private double scheduleDropOffs(Schedule<DrtTask> bestSched, Set<TaxibusRequest> onBoard,
			TreeSet<TaxibusRequest> dropOffsForLink, Set<TaxibusRequest> droppedOff, double beginTime) {
		for (TaxibusRequest req : dropOffsForLink) {
			if (droppedOff.contains(req))
				continue;
			double endTime = beginTime + params.dropoffDuration;
			bestSched.addTask(new DrtDropoffTask(beginTime, endTime, req));
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

	private double schedulePickups(Schedule<DrtTask> bestSched, double beginTime, Set<TaxibusRequest> onBoard,
			Set<TaxibusRequest> pickedUp, TreeSet<TaxibusRequest> pickUpsForLink) {

		for (TaxibusRequest req : pickUpsForLink) {
			if (pickedUp.contains(req))
				continue;
			double t3 = Math.max(beginTime, req.getT0()) + params.pickupDuration;
			bestSched.addTask(new DrtPickupTask(beginTime, t3, req));
//			 log.info("schedule pickup" +
//			 req.getPassenger().getId().toString() + " at link "+req.getFromLink().getId());
			onBoard.add(req);
			beginTime = t3;
			pickedUp.add(req);
		}
		return beginTime;
	}

	private double scheduleDriveAlongPath(Schedule<DrtTask> bestSched, VrpPathWithTravelData path,
			Set<TaxibusRequest> onBoard, double lastEndtime) {

		// VrpPathWithTravelData updatedPath = new
		// VrpPathWithTravelDataImpl(lastEndtime,
		// path.getTravelTime(), path.getTravelCost(), path.getLinks(),
		// path.getLinkTTs());

		DrtDriveWithPassengerTask task = new DrtDriveWithPassengerTask(onBoard, path);
		task.setBeginTime(lastEndtime);
//		for (int i = 0; i < path.getLinkCount(); i++) {
//			log.info(path.getLink(i).getId().toString());
//		}
		double endTime = lastEndtime + path.getTravelTime();
		task.setEndTime(endTime);
		bestSched.addTask(task);

		return endTime;
	}

	private void scheduleDriveToFirstRequest(DrtStayTask lastTask, Schedule<DrtTask> bestSched,
			VrpPathWithTravelData path) {
		switch (lastTask.getStatus()) {
		case STARTED:
			// bus is already idle
			lastTask.setEndTime(path.getDepartureTime());// shortening the WAIT
															// task
			break;

		case PLANNED:

			if (lastTask.getBeginTime() == path.getDepartureTime()) { // waiting
																		// for 0
																		// seconds!!!
				bestSched.removeLastTask();// remove WaitTask
			} else {
				// actually this WAIT task will not be performed
				lastTask.setEndTime(path.getDepartureTime());// shortening the
																// WAIT task
			}
			break;

		case PERFORMED:
		default:
			throw new IllegalStateException();
		}

		if (path.getLinkCount() > 1) {
			bestSched.addTask(new DrtDriveTask(path));
		}
	}

	/**
	 * Check and decide if the schedule should be updated due to if vehicle is
	 * Update timings (i.e. beginTime and endTime) of all tasks in the schedule.
	 */
	public void updateBeforeNextTask(Schedule<DrtTask> schedule) {
		// Assumption: there is no delay as long as the schedule has not been
		// started (PLANNED)
		if (schedule.getStatus() != ScheduleStatus.STARTED) {
			return;
		}

		double endTime = timer.getTimeOfDay();
		DrtTask currentTask = schedule.getCurrentTask();

		updateTimelineImpl(schedule, endTime);

	}

	protected void appendTasksAfterDropoff(Schedule<DrtTask> schedule) {
		appendStayTask(schedule);
	}

	protected void appendStayTask(Schedule<DrtTask> schedule) {
		double tBegin = schedule.getEndTime();
		double tEnd = Math.max(tBegin, schedule.getVehicle().getT1());// even
																		// 0-second
																		// WAIT
		Link link = Schedules.getLastLinkInSchedule(schedule);
		schedule.addTask(new DrtStayTask(tBegin, tEnd, link));
	}

	public void updateTimeline(Schedule<DrtTask> schedule) {
		if (schedule.getStatus() != ScheduleStatus.STARTED) {
			return;
		}

		double predictedEndTime = TaskTrackers.predictEndTime(schedule.getCurrentTask(), timer.getTimeOfDay());
		updateTimelineImpl(schedule, predictedEndTime);
	}

	private void updateTimelineImpl(Schedule<DrtTask> schedule, double newTaskEndTime) {
		Task currentTask = schedule.getCurrentTask();
		if (currentTask.getEndTime() == newTaskEndTime) {
			return;
		}

		currentTask.setEndTime(newTaskEndTime);

		List<DrtTask> tasks = schedule.getTasks();
		int startIdx = currentTask.getTaskIdx() + 1;
		double t = newTaskEndTime;

		for (int i = startIdx; i < tasks.size(); i++) {
			DrtTask task = tasks.get(i);

			switch (task.getDrtTaskType()) {
			case STAY: {
				if (i == tasks.size() - 1) {// last task
					task.setBeginTime(t);

					if (task.getEndTime() < t) {// may happen if the previous
												// task is delayed
						task.setEndTime(t);// do not remove this task!!! A taxi
											// schedule should end with WAIT
					}
				} else {
					// if this is not the last task then some other task (e.g.
					// DRIVE or PICKUP)
					// must have been added at time submissionTime <= t
					double endTime = task.getEndTime();
					if (endTime <= t) {// may happen if the previous task is
										// delayed
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
			case DRIVE_WITH_PASSENGER: {
				// cannot be shortened/lengthen, therefore must be moved
				// forward/backward
				task.setBeginTime(t);
				VrpPathWithTravelData path = (VrpPathWithTravelData) ((DriveTask) task).getPath();
				t += path.getTravelTime(); // TODO one may consider
											// recalculation of SP!!!!
				task.setEndTime(t);

				break;
			}

			case PICKUP: {
				task.setBeginTime(t);// t == taxi's arrival time
				double t0 = ((DrtPickupTask) task).getRequest().getT0();// t0
																			// ==
																			// passenger's
																			// departure
																			// time
				t = Math.max(t, t0) + params.pickupDuration; // the true pickup
																// starts at
																// max(t, t0)
				task.setEndTime(t);

				break;
			}
			case DROPOFF: {
				// cannot be shortened/lengthen, therefore must be moved
				// forward/backward
				task.setBeginTime(t);
				t += params.dropoffDuration;
				task.setEndTime(t);

				break;
			}
			}
		}
	}

	// =========================================================================================

	private List<TaxibusRequest> removedRequests;

	/**
	 * Awaiting == unpicked-up, i.e. requests with status PLANNED or
	 * TAXI_DISPATCHED See {@link TaxiRequestStatus}
	 */

	public Set<TaxibusRequest> getCurrentlyPlannedRequests(Schedule<DrtTask> schedule) {
		Set<TaxibusRequest> plannedRequests = new HashSet<>();
		List<DrtTask> tasks = schedule.getTasks();

		for (int i = schedule.getTaskCount() - 1; i > schedule.getCurrentTask().getTaskIdx(); i--) {
			DrtTask task = tasks.get(i);
			if (task instanceof DrtTaskWithRequests) {
				plannedRequests.addAll(((DrtTaskWithRequests) task).getRequests());
			}

		}

		return plannedRequests;
	}

	public void stopAllAimlessDriveTasks() {
		// TODO Auto-generated method stub

	}
}
