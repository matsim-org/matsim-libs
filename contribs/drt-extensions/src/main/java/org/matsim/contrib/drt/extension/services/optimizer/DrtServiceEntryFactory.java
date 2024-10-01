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
package org.matsim.contrib.drt.extension.services.optimizer;

import org.matsim.contrib.drt.extension.operations.shifts.schedule.OperationalStop;
import org.matsim.contrib.drt.extension.services.tasks.DrtServiceTask;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;

/**
 * @author steffenaxer
 */
public class DrtServiceEntryFactory implements VehicleEntry.EntryFactory {
	private final VehicleEntry.EntryFactory delegate;

	public DrtServiceEntryFactory(VehicleEntry.EntryFactory delegate) {
		this.delegate = delegate;
	}

	@Override
	public VehicleEntry create(DvrpVehicle vehicle, double currentTime) {
		Schedule schedule = vehicle.getSchedule();

		// Do not insert into service tasks
		if(vehicle.getSchedule().getCurrentTask() instanceof OperationalStop)
		{
			return null;
		}

		int taskCount = schedule.getTaskCount();
		if (taskCount > 1) {
			Task oneBeforeLast = schedule.getTasks().get(taskCount - 2);
			if (oneBeforeLast.getStatus() != Task.TaskStatus.PERFORMED && oneBeforeLast.getTaskType()
				.equals(DrtServiceTask.TYPE)) {
				return null;
			}
		}

		return delegate.create(vehicle, currentTime);
	}
}
