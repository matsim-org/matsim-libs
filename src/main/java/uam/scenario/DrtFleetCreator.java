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

package uam.scenario;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtFleetCreator {
	private FleetSpecification generateFleet(int vehiclesPerHub) {
		FleetSpecification fleet = new FleetSpecificationImpl();
		for (int i = 0; i < UamNetworkCreator.SELECTED_LINK_IDS.size(); i++) {
			Id<Link> hub = UamNetworkCreator.SELECTED_LINK_IDS.get(i);

			int vehicles = vehiclesPerHub;
			int capacity = 4;
			for (int j = 0; j < vehicles; j++) {
				Id<DvrpVehicle> vehicleId = Id.create("DRT_" + i + "_" + j, DvrpVehicle.class);
				DvrpVehicleSpecification vehicle = ImmutableDvrpVehicleSpecification.newBuilder()
						.id(vehicleId)
						.capacity(capacity)
						.serviceBeginTime(0)
						.serviceEndTime(60 * 3600)
						.startLinkId(hub)
						.build();

				fleet.addVehicleSpecification(vehicle);
			}
		}
		return fleet;
	}

	public static void main(String[] args) {
		int vehiclesPerHub = 2;
		new FleetWriter(
				new DrtFleetCreator().generateFleet(vehiclesPerHub).getVehicleSpecifications().values().stream()).write(
				"output/uam/drt_fleet_6x" + vehiclesPerHub + "_60h.xml");
	}
}
