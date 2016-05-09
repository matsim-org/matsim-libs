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
package org.matsim.core.router;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.util.LeastCostPathCalculator;

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
		return new PseudoTransitRoutingModule(
				mode,
				popFac,
				net,
				routeAlgo,
				params.getTeleportedModeFreespeedFactor(),
				params.getBeelineDistanceFactor(),
				((PopulationFactoryImpl) popFac).getRouteFactory() ) ;
	}

	public static RoutingModule createTeleportationRouter( String mode, PopulationFactory popFac, ModeRoutingParams params ) {
		return new TeleportationRoutingModule(
				mode,
				popFac,
				((PopulationFactoryImpl) popFac).getRouteFactory(),
				params.getTeleportedModeSpeed(),
                params.getBeelineDistanceFactor() );
	}

	/**
	 * Creates network router without access/egress.
	 */
	public static RoutingModule createPureNetworkRouter( String mode, PopulationFactory popFact, Network net, final LeastCostPathCalculator routeAlgo ) {
		return new NetworkRoutingModule(	
				mode,
				popFact,
				net,
				routeAlgo,
				((PopulationFactoryImpl) popFact).getRouteFactory() );
	}
	
	public static RoutingModule createAccessEgressNetworkRouter( String mode, PopulationFactory popFact, Network net, 
			final LeastCostPathCalculator routeAlgo, PlansCalcRouteConfigGroup calcRouteConfig ) {
		return new NetworkRoutingInclAccessEgressModule(
				mode,
				popFact,
				net,
				routeAlgo,
				((PopulationFactoryImpl) popFact).getRouteFactory(), calcRouteConfig );
	}

}
