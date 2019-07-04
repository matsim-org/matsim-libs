
/* *********************************************************************** *
 * project: org.matsim.*
 * BeelineTeleportationRouting.java
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

import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;

import javax.inject.Inject;
import javax.inject.Provider;

class BeelineTeleportationRouting implements Provider<RoutingModule> {

	private final PlansCalcRouteConfigGroup.ModeRoutingParams params;

	public BeelineTeleportationRouting(PlansCalcRouteConfigGroup.ModeRoutingParams params) {
		this.params = params;
	}

	@Inject
	private PopulationFactory populationFactory;

	@Override
	public RoutingModule get() {
		return DefaultRoutingModules.createTeleportationRouter(params.getMode(), populationFactory, params);
	}
}
