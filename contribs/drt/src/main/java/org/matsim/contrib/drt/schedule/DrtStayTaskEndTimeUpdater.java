/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.schedule;

import com.google.inject.Inject;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.*;

import static org.matsim.contrib.dvrp.schedule.ScheduleUpdater.REMOVE_STAY_TASK;

public class DrtStayTaskEndTimeUpdater implements StayTaskEndTimeUpdater {

	final double stopDuration;

	public DrtStayTaskEndTimeUpdater(DrtConfigGroup drtConfigGroup) {
		this.stopDuration = drtConfigGroup.getStopDuration();
	}

	@Override
	public double calcNewEndTime(DvrpVehicle vehicle, Task task, double newBeginTime) {
		if(! (task instanceof StayTask) ){
			throw new IllegalArgumentException();
		}
		switch (((DrtTaskType)task.getTaskType())) {
			case STAY: {
				if (Schedules.getLastTask(vehicle.getSchedule()).equals(task)) {// last task
					// even if endTime=beginTime, do not remove this task!!! A DRT schedule should end with WAIT
					return Math.max(newBeginTime, vehicle.getServiceEndTime());
				} else {
					// if this is not the last task then some other task (e.g. DRIVE or PICKUP)
					// must have been added at time submissionTime <= t
					double oldEndTime = task.getEndTime();
					if (oldEndTime <= newBeginTime) {// may happen if the previous task is delayed
						return REMOVE_STAY_TASK;// remove the task
					} else {
						return oldEndTime;
					}
				}
			}
			case STOP: {
				double maxEarliestPickupTime = ((DrtStopTask)task).getPickupRequests()
						.values()
						.stream()
						.mapToDouble(DrtRequest::getEarliestStartTime)
						.max()
						.orElse(Double.NEGATIVE_INFINITY); //TODO REMOVE_STAY_TASK ?? @michal
				double duration = stopDuration;
				return Math.max(newBeginTime + duration, maxEarliestPickupTime);
			}

			default:
				throw new IllegalStateException();
		}
	}
}
