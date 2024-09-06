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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup.TeleportedModeParams;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.timing.TimeInterpretation;

import javax.annotation.Nullable;

/**
 * @author nagel
 *
 */
public final class DefaultRoutingModules {
	// despite the fact that this class is in "old", I actually think that it is ok, providing a public interface to functionality that could be changed
	// underneath.  Have to keep in in the "old" package in order to package-protect the infrastructure that is used underneath. kai, mar'15


	private DefaultRoutingModules(){} // do not instantiate

	public static RoutingModule createPseudoTransitRouter( String mode, PopulationFactory popFac, Network net, LeastCostPathCalculator routeAlgo,
			RoutingConfigGroup.TeleportedModeParams params ) {
		return new FreespeedFactorRoutingModule(
				mode,
				popFac,
				net,
				routeAlgo,
				params) ;
	}

	public static RoutingModule createTeleportationRouter( String mode, Scenario scenario, TeleportedModeParams params ) {
		return new TeleportationRoutingModule(
				mode,
			  scenario,
				params.getTeleportedModeSpeed(),
                params.getBeelineDistanceFactor() );
	}

	/**
	 * Creates network router without access/egress.
	 */
	@Deprecated // use AccessEgressNetworkRouter instead
	public static RoutingModule createPureNetworkRouter( String mode, PopulationFactory popFact, Network net, final LeastCostPathCalculator routeAlgo ) {
		return new NetworkRoutingModule(
				mode,
				popFact,
				net,
				routeAlgo);
	}

	// TODO: make package private again
	// Please use injection (NetworkRoutingProvider) to get a NetworkRoutingInclAccessEgressModule - kn/gl nov'19
	public static RoutingModule createAccessEgressNetworkRouter( String mode,
											 final LeastCostPathCalculator routeAlgo, Scenario scenario,
											 Network filteredNetwork, RoutingModule accessEgressToNetworkRouter,
											 TimeInterpretation timeInterpretation, MultimodalLinkChooser multimodalLinkChooser) {

		return new NetworkRoutingInclAccessEgressModule(
				mode,
			  routeAlgo,
			  scenario, filteredNetwork, null, accessEgressToNetworkRouter, accessEgressToNetworkRouter, timeInterpretation, multimodalLinkChooser);
	}

	// TODO: make package private again
	// Please use injection (NetworkRoutingProvider) to get a NetworkRoutingInclAccessEgressModule - kn/gl nov'19
	public static RoutingModule createAccessEgressNetworkRouter( String mode,
																 final LeastCostPathCalculator routeAlgo, Scenario scenario,
																 Network filteredNetwork, RoutingModule accessToNetworkRouter, RoutingModule egressFromNetworkRouter,
																 TimeInterpretation timeInterpretation, MultimodalLinkChooser multimodalLinkChooser) {
		return new NetworkRoutingInclAccessEgressModule(
				mode,
				routeAlgo,
				scenario, filteredNetwork,null, accessToNetworkRouter, egressFromNetworkRouter, timeInterpretation, multimodalLinkChooser);
	}

	/**
	 * Creates a new access egress network router.
	 *
	 * @param invertedNetwork       if not null, routing will be on the inverted network, in which case routeAlgo needs to be an {@link InvertedLeastPathCalculator}
	 * @param multimodalLinkChooser
	 */
	static RoutingModule createAccessEgressNetworkRouter(String mode, final LeastCostPathCalculator routeAlgo, Scenario scenario,
														 Network filteredNetwork, @Nullable Network invertedNetwork,
														 RoutingModule accessToNetworkRouter, RoutingModule egressFromNetworkRouter,
														 TimeInterpretation timeInterpretation, MultimodalLinkChooser multimodalLinkChooser) {
		return new NetworkRoutingInclAccessEgressModule(
				mode, routeAlgo, scenario, filteredNetwork, invertedNetwork, accessToNetworkRouter, egressFromNetworkRouter, timeInterpretation,
				multimodalLinkChooser);
	}

}
