/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.scheduler;

import com.google.inject.Inject;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.ScheduleInquiry;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 * @author nkuehnel / MOIA added idle criteria
 */
public class DrtScheduleInquiry implements ScheduleInquiry {

	private final MobsimTimer timer;

	@Inject
	public DrtScheduleInquiry(MobsimTimer timer) {
		this.timer = timer;
	}

	@Override
	public boolean isIdle(DvrpVehicle vehicle) {
		return isIdle(vehicle, IdleCriteria.none());
	}

	@Override
	public boolean isIdle(DvrpVehicle vehicle, IdleCriteria criteria) {
		final Schedule schedule = vehicle.getSchedule();
		final double now = timer.getTimeOfDay();

		// Schedule must be started
		if (schedule.getStatus() != Schedule.ScheduleStatus.STARTED) {
			return false;
		}

		// Must be in a STAY task to be idle
		final Task current = schedule.getCurrentTask();
		if (!(current instanceof DrtStayTask)) {
			return false;
		}

		// Service must not have ended
		if (now >= vehicle.getServiceEndTime()) {
			return false;
		}

		// Compute elapsed/remaining within the current STAY task
		final double elapsed = Math.max(0.0, now - current.getBeginTime());
		final double remaining = Math.max(0.0, current.getEndTime() - now);

		// Require minimum elapsed idle time
		if (elapsed < criteria.minIdleElapsed()) {
			return false;
		}

		// If this is the last task, we consider it idle (no remaining check)
		final boolean isLastTask = current.getTaskIdx() == schedule.getTaskCount() - 1;
		if (isLastTask) {
			return true;
		}

		// Require minimum remaining idle time
		if (remaining < criteria.minIdleGapRemaining()) {
			return false;
		}

		return true;
	}
}
