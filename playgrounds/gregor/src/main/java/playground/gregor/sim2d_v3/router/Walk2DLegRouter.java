/* *********************************************************************** *
 * project: org.matsim.*
 * Walk2DLegRouter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v3.router;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.old.LegRouter;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.util.TravelTime;

public class Walk2DLegRouter implements LegRouter {

	private Set<String> walk2DModeRestrictions;
	
	private final IntermodalLeastCostPathCalculator routeAlgo;
	private final ModeRouteFactory modeRouteFactory;
	private final RouteFactory routeFactory;
	
	private final TravelTime travelTime;

	private final LegRouter legRouter;
	
	public Walk2DLegRouter(final Network network, final TravelTime travelTime, 
			final IntermodalLeastCostPathCalculator routeAlgo) {
		
		this.travelTime = travelTime;
		this.routeAlgo = routeAlgo;
		
		this.routeFactory = new LinkNetworkRouteFactory();
		this.modeRouteFactory = new ModeRouteFactory();
		modeRouteFactory.setRouteFactory("walk2d", routeFactory);
		
		this.legRouter = new NetworkLegRouter(network, this.routeAlgo, this.modeRouteFactory);
		
		initModeRestrictions();
	}
	
	@Override
	public double routeLeg(Person person, Leg leg, Activity fromAct, Activity toAct, double depTime) {
		String legMode = leg.getMode();
		
		if (legMode.equals("walk2d")) {
			routeAlgo.setModeRestriction(walk2DModeRestrictions);
		} else {
			throw new RuntimeException("cannot handle legmode '" + legMode + "'.");
		}
		
		return legRouter.routeLeg(person, leg, fromAct, toAct, depTime);
	}
	
	private void initModeRestrictions() {
		/*
		 * Walk2D
		 */	
		walk2DModeRestrictions = new HashSet<String>();
		walk2DModeRestrictions.add("walk2d");
	}
}
