
/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkRoutingProvider.java
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

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.timing.TimeInterpretation;

import com.google.inject.name.Named;

import java.util.Map;
import java.util.Set;

public class NetworkRoutingProvider implements Provider<RoutingModule>{
	private static final Logger log = LogManager.getLogger( NetworkRoutingProvider.class ) ;

	private final String routingMode;
	private boolean alreadyCheckedConsistency = false;

	@Inject Map<String, TravelTime> travelTimes;
	@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;
	@Inject SingleModeNetworksCache singleModeNetworksCache;
	@Inject
	RoutingConfigGroup routingConfigGroup;
	@Inject PopulationFactory populationFactory;
	@Inject LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
	@Inject Scenario scenario ;
	@Inject TimeInterpretation timeInterpretation;
	@Inject MultimodalLinkChooser multimodalLinkChooser;
	@Inject
	@Named(TransportMode.walk)
	private RoutingModule walkRouter;

	/**
	 * This is the older (and still more standard) constructor, where the routingMode and the resulting mode were the
	 * same.
	 *
	 * @param mode
	 */
	public NetworkRoutingProvider(String mode) {
		this( mode, mode ) ;
	}

	/**
	 * The effect of this constructor is a router configured for "routingMode" will be used for routing, but the route
	 * will then have the mode "mode".   So one can, for example, have an uncongested and a congested within-day router,
	 * for travellers who first might be unaware, but then switch on some help, and the both produce a route of type "car".
	 *
	 * @param mode
	 * @param routingMode
	 */
	public NetworkRoutingProvider(String mode, String routingMode ) {
//		log.setLevel(Level.DEBUG);

		this.mode = mode;
		this.routingMode = routingMode ;
	}

	private final String mode;

	@Override
	public RoutingModule get() {
		log.debug( "requesting network routing module with routingMode="
						   + routingMode + ";\tmode=" + mode) ;

		// the network refers to the (transport)mode:
		Network filteredNetwork = singleModeNetworksCache.getOrCreateSingleModeNetwork(mode);

		checkNetwork(filteredNetwork);

		// the travel time & disutility refer to the routing mode:
		TravelDisutilityFactory travelDisutilityFactory = this.travelDisutilityFactories.get(routingMode);
		if (travelDisutilityFactory == null) {
			throw new RuntimeException("No TravelDisutilityFactory bound for mode "+routingMode+".");
		}
		TravelTime travelTime = travelTimes.get(routingMode);
		if (travelTime == null) {
			throw new RuntimeException("No TravelTime bound for mode "+routingMode+".");
		}
		LeastCostPathCalculator routeAlgo =
				leastCostPathCalculatorFactory.createPathCalculator(
						filteredNetwork,
						travelDisutilityFactory.createTravelDisutility(travelTime),
						travelTime);

		// the following again refers to the (transport)mode, since it will determine the mode of the leg on the network:
		if ( !routingConfigGroup.getAccessEgressType().equals(RoutingConfigGroup.AccessEgressType.none) ) {
			/*
			 * All network modes should fall back to the TransportMode.walk RoutingModule for access/egress to the Network.
			 * However, TransportMode.walk cannot fallback on itself for access/egress to the Network, so don't pass a standard
			 * accessEgressToNetworkRouter RoutingModule for walk..
			 */

			//null only works because walk is hardcoded and treated uniquely in the routing module. tschlenther june '20

			// more precisely: If the transport mode is walk, then code is used that does not need the accessEgressToNetwork router.  kai, jun'22

			if (mode.equals(TransportMode.walk)) {
				return DefaultRoutingModules.createAccessEgressNetworkRouter(mode, routeAlgo, scenario, filteredNetwork, null, timeInterpretation, multimodalLinkChooser);
			} else {
				return DefaultRoutingModules.createAccessEgressNetworkRouter(mode, routeAlgo, scenario, filteredNetwork, walkRouter, timeInterpretation, multimodalLinkChooser) ;
			}

		} else {
			log.warn("[mode: {}; routingMode: {}] Using deprecated routing module without access/egress. Consider using AccessEgressNetworkRouter instead.", mode, routingMode);
			log.warn( "This can be achieved by something like" );
			log.warn("\t\tconfig.routing().setAccessEgressType( AccessEgressType.accessEgressModeToLink );");
			log.warn( "in code or" );
			log.warn( "\t<module name=\"routing\" >" );
			log.warn( "\t\t<param name=\"accessEgressType\" value=\"accessEgressModeToLink\" />" );
			log.warn( "in the config xml.");
			return DefaultRoutingModules.createPureNetworkRouter(mode, populationFactory, filteredNetwork, routeAlgo);
		}
	}

	private void checkNetwork(Network filteredNetwork) {
		if(routingConfigGroup.getNetworkRouteConsistencyCheck() == RoutingConfigGroup.NetworkRouteConsistencyCheck.disable) {
			return;
		}

		if(alreadyCheckedConsistency){
			return;
		}

		log.info("Checking network for mode '{}' for consistency...", mode);

		int nLinks = filteredNetwork.getLinks().size();
		int nNodes = filteredNetwork.getNodes().size();
		NetworkUtils.cleanNetwork(filteredNetwork, Set.of(this.routingMode));
		boolean changed = nLinks != filteredNetwork.getLinks().size() || nNodes != filteredNetwork.getNodes().size();

		if(changed) {
			String errorMessage = "Network for mode '" + mode + "' has unreachable links and nodes. This may be caused by mode restrictions on certain links. Aborting.";
			log.error(errorMessage + "\n If you restricted modes on some links, consider doing that with NetworkUtils.restrictModesAndCleanNetwork(). This makes sure, that the network is consistent for each mode." +
				"\n If this network topology is intended, set the routing config parameter 'networkRouteConsistencyCheck' to 'disable'.");
			throw new RuntimeException(errorMessage);
		}

		alreadyCheckedConsistency = true;
	}
}
