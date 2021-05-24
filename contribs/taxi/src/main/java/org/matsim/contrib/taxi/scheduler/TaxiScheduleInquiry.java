/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.ScheduleInquiry;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class TaxiScheduleInquiry implements ScheduleInquiry {
	private final TaxiConfigGroup taxiCfg;
	private final MobsimTimer timer;

	public TaxiScheduleInquiry(TaxiConfigGroup taxiCfg, MobsimTimer timer) {
		this.taxiCfg = taxiCfg;
		this.timer = timer;
	}

	@Override
	public boolean isIdle(DvrpVehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		if (timer.getTimeOfDay() >= vehicle.getServiceEndTime()
				|| schedule.getStatus() != Schedule.ScheduleStatus.STARTED) {
			return false;
		}

		Task currentTask = schedule.getCurrentTask();
		return currentTask.getTaskIdx() == schedule.getTaskCount() - 1 // last task
				&& STAY.isBaseTypeOf(currentTask);
	}

	/**
	 * If the returned LinkTimePair is not null, then time is not smaller than the current time
	 */
	public LinkTimePair getImmediateDiversionOrEarliestIdleness(DvrpVehicle veh) {
		if (taxiCfg.isVehicleDiversion()) {
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
	public LinkTimePair getEarliestIdleness(DvrpVehicle veh) {
		if (timer.getTimeOfDay() >= veh.getServiceEndTime()) {// time window exceeded
			return null;
		}

		Schedule schedule = veh.getSchedule();

		switch (schedule.getStatus()) {
			case PLANNED:
			case STARTED:
				Task lastTask = Schedules.getLastTask(schedule);

				switch (getBaseTypeOrElseThrow(lastTask)) {
					case STAY:
						Link link = ((StayTask)lastTask).getLink();
						double time = Math.max(lastTask.getBeginTime(), timer.getTimeOfDay());// TODO very optimistic!!!
						return createValidLinkTimePair(link, time, veh);

					case PICKUP:
						if (!taxiCfg.isDestinationKnown()) {
							return null;
						}
						// otherwise: IllegalStateException -- the schedule should end with STAY (or PICKUP if
						// unfinished)

					default:
						throw new IllegalStateException("Type of the last task is wrong: " + lastTask.getTaskType());
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
	public LinkTimePair getImmediateDiversion(DvrpVehicle veh) {
		if (!taxiCfg.isVehicleDiversion()) {
			throw new RuntimeException("Diversion must be on");
		}

		Schedule schedule = veh.getSchedule();
		// timer.getTimeOfDay() >= veh.getServiceEndTime() is ALLOWED because we need to stop/divert delayed vehicles
		// so do not return null
		if (schedule.getStatus() != Schedule.ScheduleStatus.STARTED) {
			return null;
		}

		Task currentTask = schedule.getCurrentTask();
		// no prebooking ==> we can divert vehicle whose current task is an empty drive at the end of the schedule
		if (currentTask.getTaskIdx() != schedule.getTaskCount() - 1 // not last task
				|| !EMPTY_DRIVE.isBaseTypeOf(currentTask)) {
			return null;
		}

		OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker)currentTask.getTaskTracker();
		return filterValidLinkTimePair(tracker.getDiversionPoint(), veh);
	}

	private static LinkTimePair filterValidLinkTimePair(LinkTimePair pair, DvrpVehicle veh) {
		return pair.time >= veh.getServiceEndTime() ? null : pair;
	}

	private static LinkTimePair createValidLinkTimePair(Link link, double time, DvrpVehicle veh) {
		return time >= veh.getServiceEndTime() ? null : new LinkTimePair(link, time);
	}
}
