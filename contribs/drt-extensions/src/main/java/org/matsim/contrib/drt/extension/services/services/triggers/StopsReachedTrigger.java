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
import org.matsim.contrib.drt.extension.services.services.params.StopsReachedTriggerParam;
import org.matsim.contrib.drt.extension.services.tasks.DrtServiceTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;

/**
 * @author steffenaxer
 */
public class StopsReachedTrigger implements ServiceExecutionTrigger {
	private final StopsReachedTriggerParam stopReachedParam;

	public StopsReachedTrigger(Id<DvrpVehicle> vehicleId, StopsReachedTriggerParam stopReachedParam)
	{
		this.stopReachedParam = stopReachedParam;
	}

	@Override
	public boolean requiresService(DvrpVehicle dvrpVehicle, double timeStep) {
		return this.judgeVehicle(dvrpVehicle, timeStep);
	}

	@Override
	public String getName() {
		return stopReachedParam.name;
	}

	boolean judgeVehicle(DvrpVehicle dvrpVehicle, double timeStep)
	{
		return dvrpVehicle.getSchedule().getStatus() == Schedule.ScheduleStatus.STARTED &&
			TriggerUtils.hasJustStarted(dvrpVehicle.getSchedule(), timeStep) &&
			calcStops(dvrpVehicle) > stopReachedParam.requiredStops;
	}

	private int calcStops(DvrpVehicle dvrpVehicle) {
		double lastService = dvrpVehicle.getSchedule().getTasks().stream()
			.filter(t -> t instanceof DrtServiceTask)
			.mapToDouble(Task::getEndTime)
			.max()
			.orElse(0);

		return (int) dvrpVehicle.getSchedule().getTasks().stream()
			.filter(t -> t instanceof DrtStopTask)
			.filter(t -> t.getBeginTime() > lastService).count();
	}
}
