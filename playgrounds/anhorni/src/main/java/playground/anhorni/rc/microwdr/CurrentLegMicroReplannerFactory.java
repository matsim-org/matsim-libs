/* *********************************************************************** *
 * project: org.matsim.*
 * CurrentLegReplannerFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.anhorni.rc.microwdr;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

public class CurrentLegMicroReplannerFactory extends WithinDayDuringLegReplannerFactory {

	private final Scenario scenario;
	private final TripRouterFactory tripRouterFactory;
	private final RoutingContext routingContext;
	private final Controler controler;
	
	public CurrentLegMicroReplannerFactory(Scenario scenario, WithinDayEngine withinDayEngine,
			TripRouterFactory tripRouterFactory, RoutingContext routingContext, Controler controler) {
		super(withinDayEngine);
		this.scenario = scenario;
		this.tripRouterFactory = tripRouterFactory;
		this.routingContext = routingContext;
		this.controler = controler;
	}

	@Override
	public WithinDayDuringLegReplanner createReplanner() {
		WithinDayDuringLegReplanner replanner = new CurrentLegMicroReplanner(super.getId(), scenario,
				this.getWithinDayEngine().getInternalInterface(), 
				this.tripRouterFactory.instantiateAndConfigureTripRouter(routingContext),
				this.controler);
		return replanner;
	}
}