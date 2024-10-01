/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2024 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */
package org.matsim.contrib.drt.extension.services.services.triggers;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.services.services.params.TimeOfDayReachedTriggerParam;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;

/**
 * @author steffenaxer
 */
public class TimeOfDayReachedTrigger implements ServiceExecutionTrigger {
	private final TimeOfDayReachedTriggerParam timeOfDayReachedTriggerParam;

	public TimeOfDayReachedTrigger(Id<DvrpVehicle> vehicleId, TimeOfDayReachedTriggerParam timeOfDayBasedConditionParam)
	{
		this.timeOfDayReachedTriggerParam = timeOfDayBasedConditionParam;
	}

	@Override
	public boolean requiresService(DvrpVehicle dvrpVehicle, double timeStep) {
		return this.judgeVehicle(dvrpVehicle, timeStep);
	}

	@Override
	public String getName() {
		return timeOfDayReachedTriggerParam.name;
	}

	boolean judgeVehicle(DvrpVehicle dvrpVehicle, double timeStep)
	{
		return dvrpVehicle.getSchedule().getStatus() == Schedule.ScheduleStatus.STARTED
			&& timeStep==this.timeOfDayReachedTriggerParam.executionTime;
	}

}
