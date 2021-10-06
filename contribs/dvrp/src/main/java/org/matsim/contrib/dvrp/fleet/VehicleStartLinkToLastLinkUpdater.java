/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.fleet;

import org.matsim.contrib.dvrp.analysis.ExecutedScheduleCollector;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

/**
 * Updates vehicle fleet specifications on IterationEndsEvent.
 *
 * @author Michal Maciejewski (michalm)
 */
public class VehicleStartLinkToLastLinkUpdater implements IterationEndsListener {
	private final FleetSpecification fleetSpecification;
	private final ExecutedScheduleCollector executedScheduleCollector;

	public VehicleStartLinkToLastLinkUpdater(FleetSpecification fleetSpecification,
			ExecutedScheduleCollector executedScheduleCollector) {
		this.fleetSpecification = fleetSpecification;
		this.executedScheduleCollector = executedScheduleCollector;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		executedScheduleCollector.getExecutedSchedules().forEach(schedule -> {
			var currentSpecification = fleetSpecification.getVehicleSpecifications().get(schedule.vehicleId);
			var tasks = schedule.getExecutedTasks();
			var updatedSpecification = ImmutableDvrpVehicleSpecification.newBuilder(currentSpecification)
					.startLinkId(tasks.get(tasks.size() - 1).endLinkId) // update start link to last link
					.build();
			fleetSpecification.replaceVehicleSpecification(updatedSpecification);
		});
	}
}
