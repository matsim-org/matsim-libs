/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxi.optimizer.rank;

import java.util.ArrayList;
import java.util.List;

import org.jfree.util.Log;
import org.matsim.core.basic.v01.IdImpl;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.ScheduleStatus;
import pl.poznan.put.vrp.dynamic.data.schedule.Task.TaskType;
import pl.poznan.put.vrp.dynamic.data.schedule.impl.*;
import playground.jbischoff.energy.charging.DepotArrivalDepartureCharger;
import playground.jbischoff.taxi.rank.BackToRankTask;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.schedule.TaxiDriveTask;

/**
 * The main assumption: Requests are scheduled at the last request in the
 * schedule (of a given vehicle)
 * <p>
 * However, different strategies/policies may be used for:
 * <li>vehicle selection set (only idle, idle+delivering, all)
 * <li>rescheduling on/off (a reaction t changes in a schedule after updating
 * it)
 * 
 * @author michalm, jbischoff
 */
public abstract class RankTaxiOptimizer extends AbstractTaxiOptimizer {
    private static final int PICKUP_DURATION = 120;
    
    
	protected static class VehicleDrive {
		public static final VehicleDrive NO_VEHICLE_DRIVE_FOUND = new VehicleDrive(
				null, null, Integer.MAX_VALUE, Integer.MAX_VALUE);

		public final Vehicle vehicle;
		public final Arc arc;
		public final int t1;
		public final int t2;

		public VehicleDrive(Vehicle vehicle, Arc arc, int t1, int t2) {
			this.vehicle = vehicle;
			this.arc = arc;
			this.t1 = t1;
			this.t2 = t2;
		}
	}

	protected static class VertexTimePair {
		public static final VertexTimePair NO_VERTEX_TIME_PAIR_FOUND = new VertexTimePair(
				null, Integer.MAX_VALUE);

		public final Vertex vertex;
		public final int time;

		public VertexTimePair(Vertex vertex, int time) {
			this.vertex = vertex;
			this.time = time;
		}
	}

	private TaxiDelaySpeedupStats delaySpeedupStats;
	private final boolean destinationKnown;
	private List<Integer> shortTimeIdlers;
	private boolean idleRankMode;
	protected DepotArrivalDepartureCharger depotArrivalDepartureCharger;

	public RankTaxiOptimizer(VrpData data, boolean destinationKnown) {
		super(data);
		this.destinationKnown = destinationKnown;
		this.shortTimeIdlers = new ArrayList<Integer>();
	}

	public void setIdleRankMode(boolean b) {
		this.idleRankMode = b;
	}

	public void addDepotArrivalCharger(
			DepotArrivalDepartureCharger depotArrivalDepartureCharger) {
		this.depotArrivalDepartureCharger = depotArrivalDepartureCharger;
	}

	public void setDelaySpeedupStats(TaxiDelaySpeedupStats delaySpeedupStats) {
		this.delaySpeedupStats = delaySpeedupStats;
	}

	protected void scheduleRequest(Request request) {
		VehicleDrive bestVehicle = findBestVehicle(request, data.getVehicles());

		if (bestVehicle != VehicleDrive.NO_VEHICLE_DRIVE_FOUND) {
			scheduleRequestImpl(bestVehicle, request);
		}
	}

	protected VehicleDrive findBestVehicle(Request req, List<Vehicle> vehicles) {
		int currentTime = data.getTime();
		VehicleDrive best = VehicleDrive.NO_VEHICLE_DRIVE_FOUND;

		for (Vehicle veh : vehicles) {
			Schedule schedule = veh.getSchedule();

			// COMPLETED or STARTED but delayed (time window T1 exceeded)
			if (schedule.getStatus() == ScheduleStatus.COMPLETED
					|| currentTime >= Schedules.getActualT1(schedule)) {
				// skip this vehicle
				continue;
			}

			// status = UNPLANNED/PLANNED/STARTED
			VertexTimePair departure = calculateDeparture(veh, currentTime);

			if (departure == VertexTimePair.NO_VERTEX_TIME_PAIR_FOUND) {
				continue;
			}

			Arc arc = data.getVrpGraph().getArc(departure.vertex,
					req.getFromVertex());
			int t2 = departure.time + arc.getTimeOnDeparture(departure.time);

			if (t2 < best.t2) {
				// TODO: in the future: add a check if the taxi time windows are
				// satisfied
				best = new VehicleDrive(veh, arc, departure.time, t2);
			}
		}

		return best;
	}

	protected VertexTimePair calculateDeparture(Vehicle vehicle, int currentTime) {
		Schedule schedule = vehicle.getSchedule();
		Vertex vertex;
		int time;

		switch (schedule.getStatus()) {
		case UNPLANNED:
			vertex = vehicle.getDepot().getVertex();
			time = Math.max(vehicle.getT0(), currentTime);
			return new VertexTimePair(vertex, time);

		case PLANNED:
		case STARTED:
			Task lastTask = Schedules.getLastTask(vehicle.getSchedule());

			switch (lastTask.getType()) {
			case WAIT:
				vertex = ((WaitTask) lastTask).getAtVertex();
				time = Math.max(lastTask.getBeginTime(), currentTime);
				return new VertexTimePair(vertex, time);

			case SERVE:
				if (!destinationKnown) {
					return VertexTimePair.NO_VERTEX_TIME_PAIR_FOUND;
				}
			}
		}

		throw new IllegalStateException();
	}

	public void doSimStep(double time) {

		if (time % 60. == 0.) {
			checkWaitingVehiclesBatteryState();
			if (this.idleRankMode)
				updateIdlers();
		}
		if (this.idleRankMode)
			if (time % 60. == 5.) {
				sendIdlingTaxisToRank();

			}

	}

	private void checkWaitingVehiclesBatteryState() {
		for (Vehicle veh : data.getVehicles()) {
			Task last = Schedules.getLastTask(veh.getSchedule());
			if (last instanceof WaitTask) {
				WaitTask lastw = (WaitTask) last;
				if (!lastw.getAtVertex().equals(veh.getDepot().getVertex())) {
					if (this.depotArrivalDepartureCharger
							.needsToReturnToRank(new IdImpl(veh.getName()))) {
						scheduleRankReturn(veh);
					}

				}
			}

		}
	}

	protected void scheduleRequestImpl(VehicleDrive best, Request req) {
		Schedule bestSched = best.vehicle.getSchedule();

		if (bestSched.getStatus() != ScheduleStatus.UNPLANNED) {// PLANNED or
																// STARTED
			WaitTask lastTask = (WaitTask) Schedules.getLastTask(bestSched);// only
																			// WAIT

			switch (lastTask.getStatus()) {
			case PLANNED:
				if (lastTask.getBeginTime() == best.t1) { // waiting for 0
															// seconds!!!
					bestSched.removeLastPlannedTask();// remove WaitTask
				} else {
					// TODO actually this WAIT task will not be performed
					// so maybe we can remove it right now?

					lastTask.setEndTime(best.t1);// shortening the WAIT task
				}
				break;

			case STARTED:
				lastTask.setEndTime(best.t1);// shortening the WAIT task
				break;

			case PERFORMED:
			default:
				throw new IllegalStateException();
			}

		}

		Vertex reqFromVertex = req.getFromVertex();

		if (best.arc.getFromVertex() != reqFromVertex) {// not a loop
			bestSched
					.addTask(new TaxiDriveTask(best.t1, best.t2, best.arc, req));
		}

		int t3 = best.t2 + PICKUP_DURATION;
		bestSched.addTask(new ServeTaskImpl(best.t2, t3, req.getFromVertex(),
				req));

		if (destinationKnown) {
			appendDeliveryAndWaitTasksAfterServeTask(bestSched);
		}
	}

	protected void scheduleRankReturn(Vehicle veh) {

		Schedule sched = veh.getSchedule();
		int currentTime = data.getTime();
		int oldendtime;
		WaitTask lastTask = (WaitTask) Schedules.getLastTask(sched);// only WAIT

		switch (lastTask.getStatus()) {
		case PLANNED:
			return;

		case STARTED:
			oldendtime = lastTask.getEndTime();
			lastTask.setEndTime(currentTime);// shortening the WAIT task

			break;

		case PERFORMED:
			throw new IllegalStateException();
		default:
			throw new IllegalStateException();
		}

		Vertex lastVertex = lastTask.getAtVertex();

		if (veh.getDepot().getVertex() != lastVertex) {// not a loop
			Arc darc = data.getVrpGraph().getArc(lastVertex,
					veh.getDepot().getVertex());
			int arrivalTime = darc.getTimeOnDeparture(currentTime)
					+ currentTime;

			sched.addTask(new BackToRankTask(currentTime, arrivalTime, darc));
			sched.addTask(new WaitTaskImpl(arrivalTime, oldendtime, veh
					.getDepot().getVertex()));
			// System.out.println("T :"+data.getTime()+" V: "+veh.getName()+" OET:"
			// +oldendtime);
		}

	}

	public void updateIdlers() {
		this.shortTimeIdlers.clear();
		for (Vehicle veh : data.getVehicles()) {
			Task last = Schedules.getLastTask(veh.getSchedule());
			if (last instanceof WaitTask) {
				WaitTask lastw = (WaitTask) last;
				if (!lastw.getAtVertex().equals(veh.getDepot().getVertex())) {
					this.shortTimeIdlers.add(veh.getId());
				}
			}

		}

	}

	public void sendIdlingTaxisToRank() {

		for (Vehicle veh : data.getVehicles()) {
			if (shortTimeIdlers.contains(veh.getId())) {
				Task last = Schedules.getLastTask(veh.getSchedule());
				if (last instanceof WaitTask) {
					scheduleRankReturn(veh);
				}
			}
		}

	}

	@Override
	protected boolean updateBeforeNextTask(Vehicle vehicle) {
		int time = data.getTime();
		Schedule schedule = vehicle.getSchedule();
		Task currentTask = schedule.getCurrentTask();

		int delay = time - currentTask.getEndTime();

		if (delay != 0) {
			if (delaySpeedupStats != null) {// optionally, one may record delays
				delaySpeedupStats.updateStats(currentTask, delay);
			}

			currentTask.setEndTime(time);
			updatePlannedTasks(vehicle);
		}

		if (!destinationKnown) {
			if (currentTask.getType() == TaskType.SERVE) {
				appendDeliveryAndWaitTasksAfterServeTask(schedule);
				return true;
			}
		}

		return delay != 0;
	}

	abstract protected void appendDeliveryAndWaitTasksAfterServeTask(
			Schedule schedule);

	/**
	 * @param vehicle
	 * @return {@code true} if something has been changed; otherwise
	 *         {@code false}
	 */
	protected void updatePlannedTasks(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		List<Task> tasks = schedule.getTasks();
		int startIdx = schedule.getCurrentTask().getTaskIdx() + 1;
		int t = data.getTime();

		for (int i = startIdx; i < tasks.size(); i++) {
			Task task = tasks.get(i);

			switch (task.getType()) {
			case WAIT: {
				// wait can be at the end of the schedule
				//
				// BUT:
				//
				// t1 - beginTime for WAIT
				// t2 - arrival of new task
				// t3 - updateSchedule() called -- end of the current task
				// (STARTED)
				// t4 - endTime for WAIT (according to some earlier plan)
				//
				// t1 <= t2 <= t3 < t4
				//
				// it may also be kept in situation where we have WAIT for
				// [t1;t4) and at t2
				// (t1 < t2 < t4) arrives new tasks are to be added but WAIT is
				// still PLANNED
				// (not STARTED) since there has been a delay in performing some
				// previous tasks
				// so we shorten WAIT to [t1;t2) and plan the newly arrived task
				// at [t2; t2+x)
				// but of course there will probably be a delay in performing
				// this task, as
				// we do not know when exactly the current task (one that is
				// STARTED) will end
				// (t3).
				//
				// Of course, the WAIT task will never be performed as we have
				// now time t2
				// and it is planned for [t1;t2); it will be removed on the
				// nearest
				// updateSchedule(), just like here:

				if (i == tasks.size() - 1) {// last task
					task.setBeginTime(t);

					if (task.getEndTime() < t) {// may happen if a previous task
												// was delayed
						// I used to remove this WAIT_TASK, but now I keep it in
						// the schedule:
						// schedule.removePlannedTask(task.getTaskIdx());
						task.setEndTime(t);
					}
				} else {
					// if this is not the last task then some other task must
					// have been added
					// at time <= t
					// THEREFORE: task.endTime() <= t, and so it can be removed
					schedule.removePlannedTask(task.getTaskIdx());
					i--;
				}

				break;
			}
			case DRIVE: {
				// cannot be shortened/lengthen, therefore must be moved
				// forward/backward
				task.setBeginTime(t);
				t += ((DriveTask) task).getArc().getTimeOnDeparture(t);
				task.setEndTime(t);

				break;
			}
			case SERVE: {
				// cannot be shortened/lengthen, therefore must be moved
				// forward/backward
				task.setBeginTime(t);
				t += PICKUP_DURATION;
				task.setEndTime(t);

				break;
			}
			default:
				throw new IllegalStateException();
			}
		}
	}
}
