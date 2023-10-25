
/* *********************************************************************** *
 * project: org.matsim.*
 * FreespeedFactorRouting.java
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

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

class FreespeedFactorRouting implements Provider<RoutingModule> {

	private final RoutingConfigGroup.TeleportedModeParams params;

	public FreespeedFactorRouting( RoutingConfigGroup.TeleportedModeParams params ) {
		this.params = params;
	}

	@Inject
	private Network network;

	@Inject
	private PopulationFactory populationFactory;

	@Inject
	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

	@Override
	public RoutingModule get() {

//		FreespeedTravelTimeAndDisutility ptTimeCostCalc = new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);
		// I wanted to introduce the freespeed limit.  Decided to locally re-implement rather than making the FreespeedTravelTimeAndDisutility
		// class longer. kai, nov'16

		// yyyy the following might be improved by including additional disutility parameters.  But the original one I found was also
		// just doing fastest time (see commented out version above).  kai, nov'16
		final TravelTime travelTime = new TravelTime(){
			@Override public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				return link.getLength() / Math.min( link.getFreespeed(time) , params.getTeleportedModeFreespeedLimit() ) ;
			}
		} ;
		TravelDisutility travelDisutility = new TravelDisutility(){
			@Override public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				return travelTime.getLinkTravelTime(link, time, person, vehicle) ;
			}
			@Override public double getLinkMinimumTravelDisutility(Link link) {
				return link.getLength() / Math.min( link.getFreespeed() , params.getTeleportedModeFreespeedLimit() ) ;
			}
		} ;
		Gbl.assertNotNull(leastCostPathCalculatorFactory);
		LeastCostPathCalculator routeAlgoPtFreeFlow = leastCostPathCalculatorFactory.createPathCalculator(
						network, travelDisutility, travelTime);
		return DefaultRoutingModules.createPseudoTransitRouter(params.getMode(), populationFactory,
				network, routeAlgoPtFreeFlow, params);
	}
}
