
/* *********************************************************************** *
 * project: org.matsim.*
 * ModeAndRouteConsistencyChecker.java
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

 package org.matsim.core.replanning.modules;

import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.TripStructureUtils;

public class ModeAndRouteConsistencyChecker implements PlanStrategyModule {
	@Override public void prepareReplanning(ReplanningContext replanningContext) { }

	@Override public void handlePlan(Plan plan) {
		for (Leg leg : TripStructureUtils.getLegs(plan)) {
			if (leg.getRoute() instanceof NetworkRoute) {
				switch ( leg.getMode() ) {
					case TransportMode.car:
					case TransportMode.bike:
					case TransportMode.walk:
						break;
					default:
						LogManager.getLogger(this.getClass()).warn( "route is of type=" + leg.getRoute().getClass() ) ;
						LogManager.getLogger(this.getClass()).warn( "mode=" + leg.getMode() ) ;
						throw new RuntimeException("inconsistent");
				}
			}
		}
	}

	@Override public void finishReplanning() { }
}
