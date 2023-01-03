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

package org.matsim.contrib.drt.analysis.zonal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.vrpagent.AbstractTaskEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEventHandler;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEventHandler;

/**
 * @author jbischoff
 * @author Michal Maciejewski
 */
public class ZonalIdleVehicleCollector implements TaskStartedEventHandler, TaskEndedEventHandler {

	private final DrtZonalSystem zonalSystem;
	private final String dvrpMode;

	private final Map<DrtZone, Set<Id<DvrpVehicle>>> vehiclesPerZone = new HashMap<>();
	private final Map<Id<DvrpVehicle>, DrtZone> zonePerVehicle = new HashMap<>();

	public ZonalIdleVehicleCollector(String dvrpMode, DrtZonalSystem zonalSystem) {
		this.dvrpMode = dvrpMode;
		this.zonalSystem = zonalSystem;
	}

	@Override
	public void handleEvent(TaskStartedEvent event) {
		handleEvent(event, zone -> {
			vehiclesPerZone.computeIfAbsent(zone, z -> new HashSet<>()).add(event.getDvrpVehicleId());
			zonePerVehicle.put(event.getDvrpVehicleId(), zone);
		});
	}

	@Override
	public void handleEvent(TaskEndedEvent event) {
		handleEvent(event, zone -> {
			zonePerVehicle.remove(event.getDvrpVehicleId());
			vehiclesPerZone.get(zone).remove(event.getDvrpVehicleId());
		});
	}

	private void handleEvent(AbstractTaskEvent event, Consumer<DrtZone> handler) {
		if (event.getDvrpMode().equals(dvrpMode) && event.getTaskType().equals(DrtStayTask.TYPE)) {
			DrtZone zone = zonalSystem.getZoneForLinkId(event.getLinkId());
			if (zone != null) {
				handler.accept(zone);
			}
		}
	}

	public Set<Id<DvrpVehicle>> getIdleVehiclesPerZone(DrtZone zone) {
		return this.vehiclesPerZone.get(zone);
	}

	@Override
	public void reset(int iteration) {
		zonePerVehicle.clear();
		vehiclesPerZone.clear();
	}
}
