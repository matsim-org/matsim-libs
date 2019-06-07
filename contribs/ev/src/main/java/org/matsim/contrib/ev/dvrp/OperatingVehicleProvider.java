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

package org.matsim.contrib.ev.dvrp;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleLookup;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.ev.discharging.AuxDischargingHandler;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

import com.google.inject.Inject;

/**
 * @author Michal Maciejewski (michalm)
 */
public class OperatingVehicleProvider implements AuxDischargingHandler.VehicleProvider {
	private final DvrpVehicleLookup dvrpVehicleLookup;

	@Inject
	public OperatingVehicleProvider(DvrpVehicleLookup dvrpVehicleLookup) {
		this.dvrpVehicleLookup = dvrpVehicleLookup;
	}

	@Override
	public ElectricVehicle getVehicle(ActivityStartEvent event) {
		//assumes driverId == vehicleId
		DvrpVehicle vehicle = dvrpVehicleLookup.lookupVehicle((Id<DvrpVehicle>)(Id<?>)event.getPersonId());

		//do not discharge if (1) not a DVRP vehicle or (2) a DVRP vehicle that just completed the schedule
		return vehicle == null || event.getActType().equals(VrpAgentLogic.AFTER_SCHEDULE_ACTIVITY_TYPE) ?
				null :
				((EvDvrpVehicle)vehicle).getElectricVehicle();
	}
}
