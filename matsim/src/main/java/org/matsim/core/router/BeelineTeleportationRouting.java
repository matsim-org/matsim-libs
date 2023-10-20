
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.RoutingConfigGroup;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

class BeelineTeleportationRouting implements Provider<RoutingModule> {

	private final RoutingConfigGroup.TeleportedModeParams params;

	public BeelineTeleportationRouting( RoutingConfigGroup.TeleportedModeParams params ) {
		this.params = params;
	}

	@Inject
	private Scenario scenario ;

	@Override
	public RoutingModule get() {
		return DefaultRoutingModules.createTeleportationRouter(params.getMode(), scenario, params);
	}
}
