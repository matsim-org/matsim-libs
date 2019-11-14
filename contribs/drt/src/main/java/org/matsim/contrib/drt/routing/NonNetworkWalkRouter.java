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

package org.matsim.contrib.drt.routing;

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.RoutingModule;
import org.matsim.facilities.Facility;

import one.util.streamex.StreamEx;

/**
 * @author Michal Maciejewski (michalm)
 */
public class NonNetworkWalkRouter implements RoutingModule {
	private final RoutingModule walkRouter;

	public NonNetworkWalkRouter(RoutingModule walkRouter) {
		this.walkRouter = walkRouter;
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime,
			Person person) {
		List<? extends PlanElement> result = walkRouter.calcRoute(fromFacility, toFacility, departureTime, person);
		StreamEx.of(result)
				.select(Leg.class)
				.filterBy(Leg::getMode, TransportMode.walk)
				.forEach(leg -> leg.setMode(TransportMode.non_network_walk));
		return result;
	}
}
