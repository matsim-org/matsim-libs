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
import org.matsim.contrib.util.LinkProvider;

import com.google.common.collect.ImmutableMap;

/**
 * @author michalm
 */
public class Fleets {
	public static Fleet createDefaultFleet(FleetSpecification fleetSpecification, LinkProvider<Id<Link>> linkProvider) {
		return createCustomFleet(fleetSpecification,
				s -> new DvrpVehicleImpl(s, linkProvider.apply(s.getStartLinkId())));
	}

	public static Fleet createCustomFleet(FleetSpecification fleetSpecification,
			Function<DvrpVehicleSpecification, DvrpVehicle> vehicleCreator) {
		return () -> fleetSpecification.getVehicleSpecifications()
				.values()
				.stream()
				.map(vehicleCreator)
				.collect(ImmutableMap.toImmutableMap(DvrpVehicle::getId, v -> v));
	}
}
