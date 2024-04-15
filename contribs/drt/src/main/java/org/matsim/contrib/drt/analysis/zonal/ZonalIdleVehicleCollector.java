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

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.vrpagent.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author jbischoff
 * @author Michal Maciejewski
 */
public class ZonalIdleVehicleCollector implements TaskStartedEventHandler, TaskEndedEventHandler {

	private final ZoneSystem zonalSystem;
	private final String dvrpMode;

	private final Map<Zone, Set<Id<DvrpVehicle>>> vehiclesPerZone = new HashMap<>();

	public ZonalIdleVehicleCollector(String dvrpMode, ZoneSystem zonalSystem) {
		this.dvrpMode = dvrpMode;
		this.zonalSystem = zonalSystem;
	}

	@Override
	public void handleEvent(TaskStartedEvent event) {
		handleEvent(event, zone -> {
			vehiclesPerZone.computeIfAbsent(zone, z -> new HashSet<>()).add(event.getDvrpVehicleId());
		});
	}

	@Override
	public void handleEvent(TaskEndedEvent event) {
		handleEvent(event, zone -> {
			vehiclesPerZone.get(zone).remove(event.getDvrpVehicleId());
		});
	}

	private void handleEvent(AbstractTaskEvent event, Consumer<Zone> handler) {
		if (event.getDvrpMode().equals(dvrpMode) && event.getTaskType().equals(DrtStayTask.TYPE)) {
			zonalSystem.getZoneForLinkId(event.getLinkId()).ifPresent(handler);
		}
	}

	public Set<Id<DvrpVehicle>> getIdleVehiclesPerZone(Zone zone) {
		return this.vehiclesPerZone.get(zone);
	}

	@Override
	public void reset(int iteration) {
		vehiclesPerZone.clear();
	}
}
