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

import org.matsim.contrib.drt.schedule.DrtTask;
import org.matsim.contrib.drt.schedule.DrtTask.DrtTaskType;
import org.matsim.contrib.dvrp.data.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.ScheduleInquiry;
import org.matsim.core.mobsim.framework.MobsimTimer;

import com.google.inject.Inject;

/**
 * @author michalm
 */
public class DrtScheduleInquiry implements ScheduleInquiry {
	private final MobsimTimer timer;

	@Inject
	public DrtScheduleInquiry(MobsimTimer timer) {
		this.timer = timer;
	}

	@Override
	public boolean isIdle(DvrpVehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		if (timer.getTimeOfDay() >= vehicle.getServiceEndTime() || schedule.getStatus() != ScheduleStatus.STARTED) {
			return false;
		}

		DrtTask currentTask = (DrtTask)schedule.getCurrentTask();
		return currentTask.getTaskIdx() == schedule.getTaskCount() - 1 // last task (because no prebooking)
				&& currentTask.getDrtTaskType() == DrtTaskType.STAY;
	}
}
