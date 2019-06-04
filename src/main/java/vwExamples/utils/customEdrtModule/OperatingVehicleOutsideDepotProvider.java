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

package vwExamples.utils.customEdrtModule;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleLookup;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.ev.discharging.AuxDischargingHandler;
import org.matsim.contrib.ev.dvrp.EvDvrpVehicle;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

/**
 * @author Michal Maciejewski (michalm)
 */
public class OperatingVehicleOutsideDepotProvider implements AuxDischargingHandler.VehicleProvider {
	private final DvrpVehicleLookup dvrpVehicleLookup;
	private final ImmutableSet<Id<Link>> depotLinkIds;

	@Inject
	public OperatingVehicleOutsideDepotProvider(DvrpVehicleLookup dvrpVehicleLookup,
			ChargingInfrastructure chargingInfrastructure) {
		this.dvrpVehicleLookup = dvrpVehicleLookup;
		depotLinkIds = chargingInfrastructure.getChargers()
				.values()
				.stream()
				.map(Charger::getLink)
				.map(Identifiable::getId)
				.collect(ImmutableSet.toImmutableSet());
	}

	@Override
	public ElectricVehicle getVehicle(ActivityStartEvent event) {
		//assumes driverId == vehicleId
		DvrpVehicle vehicle = dvrpVehicleLookup.lookupVehicle((Id<DvrpVehicle>)(Id<?>)event.getPersonId());

		//do not discharge if (1) not a DVRP vehicle or (2) a DVRP vehicle that just completed the schedule
		// or (3) a DVRP vehicle in a depot
		return vehicle == null
				|| event.getActType().equals(VrpAgentLogic.AFTER_SCHEDULE_ACTIVITY_TYPE)
				|| depotLinkIds.contains(event.getLinkId()) ?
				null :
				((EvDvrpVehicle)vehicle).getElectricVehicle(); // discharge only if staying outside depot
	}
}
