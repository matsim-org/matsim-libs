
/* *********************************************************************** *
 * project: org.matsim.*
 * Transit.java
 *                                                                         *
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
 * *********************************************************************** */

 package org.matsim.core.router;

import com.google.inject.name.Named;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.pt.router.TransitRouter;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class Transit implements Provider<RoutingModule> {

	private final TransitRouter transitRouter;

	private final Scenario scenario;

	private final RoutingModule transitWalkRouter;

	@Inject
    Transit(TransitRouter transitRouter, Scenario scenario, @Named(TransportMode.walk) RoutingModule transitWalkRouter) {
		this.transitRouter = transitRouter;
		this.scenario = scenario;
		this.transitWalkRouter = transitWalkRouter;
	}

	@Override
	public RoutingModule get() {
		return new TransitRouterWrapper(transitRouter,
					scenario.getTransitSchedule(),
					scenario.getNetwork(),
					transitWalkRouter);
	}
}
