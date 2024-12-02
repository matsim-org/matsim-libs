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

import com.google.common.collect.Streams;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.services.services.params.MileageReachedTriggerParam;
import org.matsim.contrib.drt.extension.services.tasks.DrtServiceTask;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPath;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;

/**
 * @author steffenaxer
 */
public class MileageReachedTrigger implements ServiceExecutionTrigger {
	private final MileageReachedTriggerParam mileageReachedTriggerParam;

	public MileageReachedTrigger(Id<DvrpVehicle> vehicleId, MileageReachedTriggerParam mileageReachedTriggerParam)
	{
		this.mileageReachedTriggerParam = mileageReachedTriggerParam;
	}

	@Override
	public boolean requiresService(DvrpVehicle dvrpVehicle, double timeStep) {
		return this.judgeVehicle(dvrpVehicle, timeStep);
	}

	@Override
	public String getName() {
		return mileageReachedTriggerParam.name;
	}

	boolean judgeVehicle(DvrpVehicle dvrpVehicle, double timeStep)
	{
		if(dvrpVehicle.getSchedule().getStatus() == Schedule.ScheduleStatus.STARTED)
		{
			double lastMaintenanceTime = dvrpVehicle.getSchedule().getTasks().stream()
				.filter(t -> t instanceof DrtServiceTask)
				.mapToDouble(Task::getEndTime)
				.max()
				.orElse(0);

			double drivenDistance = dvrpVehicle.getSchedule().getTasks().stream()
				.filter(t -> t instanceof DrtDriveTask)
				.filter(t -> t.getEndTime() > lastMaintenanceTime)
				.filter(t -> t.getEndTime() < timeStep)
				.mapToDouble(t -> getDistance(((DrtDriveTask) t).getPath()))
				.sum();

			return drivenDistance > mileageReachedTriggerParam.requiredMileage;
		}

		return false;


	}

	double getDistance(VrpPath path)
	{
		return Streams.stream(path.iterator()).mapToDouble(Link::getLength).sum();
	}

}
