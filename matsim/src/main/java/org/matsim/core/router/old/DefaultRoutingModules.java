/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.core.router.old;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.NetworkRoutingModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TeleportationRoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LinkToLinkTravelTime;

/**
 * @author nagel
 *
 */
public final class DefaultRoutingModules {
	// despite the fact that this class is in "old", I actually think that it is ok, providing a public interface to functionality that could be changed
	// underneath.  Have to keep in in the "old" package in order to package-protect the infrastructure that is used underneath. kai, mar'15
	
	
	private DefaultRoutingModules(){} // do not instantiate

	public static RoutingModule createPseudoTransitRouter( String mode, PopulationFactory popFac, Network net, LeastCostPathCalculator routeAlgo,
			ModeRoutingParams params ) {
		LegRouter toWrap = new PseudoTransitLegRouter( net, routeAlgo, params.getTeleportedModeFreespeedFactor(), params.getBeelineDistanceFactor(), 
				((PopulationFactoryImpl) popFac).getModeRouteFactory() ) ;
		return new LegRouterWrapper( mode, popFac, toWrap ) ;
	}

	public static RoutingModule createTeleportationRouter( String mode, PopulationFactory popFac, ModeRoutingParams params ) {
		return new TeleportationRoutingModule(
				mode,
				popFac,
				((PopulationFactoryImpl) popFac).getModeRouteFactory(),
				params.getTeleportedModeSpeed(),
                params.getBeelineDistanceFactor() );
	}

	public static RoutingModule createNetworkRouter( String mode, PopulationFactory popFact, Network net, final LeastCostPathCalculator routeAlgo ) {
		return new NetworkRoutingModule(
				mode,
				popFact,
				net,
				routeAlgo,
				((PopulationFactoryImpl) popFact).getModeRouteFactory() );
	}

}
