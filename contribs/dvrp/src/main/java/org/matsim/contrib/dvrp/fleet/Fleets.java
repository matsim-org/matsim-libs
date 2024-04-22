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

import java.util.function.Function;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * @author michalm
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class Fleets {
	public static FleetCreator createDefaultFleet(FleetSpecification fleetSpecification,
			Function<Id<Link>, Link> linkProvider) {
		return createCustomFleet(fleetSpecification,
				s -> new DvrpVehicleImpl(s, linkProvider.apply(s.getStartLinkId())));
	}

	public static FleetCreator createCustomFleet(FleetSpecification fleetSpecification,
			Function<DvrpVehicleSpecification, DvrpVehicle> vehicleCreator) {
		return fleet -> {
			fleetSpecification.getVehicleSpecifications() //
			.values().stream() //
			.map(vehicleCreator) //
			.forEach(fleet::addVehicle);
		};
	}
}
