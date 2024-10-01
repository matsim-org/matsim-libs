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
package org.matsim.contrib.drt.extension.services.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.services.schedule.DrtService;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * @author steffenaxer
 */
public class DrtServiceStartedEvent extends AbstractServiceEvent {
	public static final String EVENT_TYPE = "DRT service started";

	public DrtServiceStartedEvent(Id<DrtService> drtServiceId, double time, String mode, String serviceType, Id<DvrpVehicle> vehicleId, Id<Link> linkId, Id<OperationFacility> id) {
		super(drtServiceId, time, mode, serviceType, vehicleId, linkId, id);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
}
