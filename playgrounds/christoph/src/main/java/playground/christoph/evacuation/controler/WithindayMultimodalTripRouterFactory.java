/* *********************************************************************** *
 * project: org.matsim.*
 * MultimodalTripRouterFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.controler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.LegRouterWrapper;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryInternal;
import org.matsim.core.router.old.LegRouter;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.CollectionUtils;

import contrib.multimodal.config.MultiModalConfigGroup;

/**
 * @author cdobler
 */
public class WithindayMultimodalTripRouterFactory implements TripRouterFactoryInternal {
	
	protected static final Logger log = Logger.getLogger(WithindayMultimodalTripRouterFactory.class);
	
	private final TripRouterObjectProvider objectProvider;
	private final Map<String, TravelTime> multimodalTravelTimes;
	
	private final TripRouterFactoryInternal delegateFactory;
	
	private final Map<String, Network> multimodalSubNetworks = new HashMap<String, Network>();
	

	
	public WithindayMultimodalTripRouterFactory(Controler controler, Map<String, TravelTime> multimodalTravelTimes, 
			TripRouterFactoryInternal delegate) {
		this.objectProvider = new TripRouterObjectProvider(controler);
		this.multimodalTravelTimes = multimodalTravelTimes;
		this.delegateFactory = delegate;
	}

	public WithindayMultimodalTripRouterFactory(TripRouterObjectProvider objectProvider, Map<String, TravelTime> multimodalTravelTimes, 
			TripRouterFactoryInternal delegate) {
		this.objectProvider = objectProvider;
		this.multimodalTravelTimes = multimodalTravelTimes;
		this.delegateFactory = delegate;
	}
	
	@Override
	public TripRouter instantiateAndConfigureTripRouter() {	

		TripRouter instance = this.delegateFactory.instantiateAndConfigureTripRouter();
		
		Network network = this.objectProvider.getScenario().getNetwork();
		LeastCostPathCalculatorFactory leastCostAlgoFactory = this.objectProvider.getLeastCostPathCalculatorFactory();
		PopulationFactory populationFactory = this.objectProvider.getScenario().getPopulation().getFactory();
		ModeRouteFactory modeRouteFactory = ((PopulationFactoryImpl) populationFactory).getModeRouteFactory();

        MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) this.objectProvider.getScenario().getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
        Set<String> simulatedModes = CollectionUtils.stringToSet(multiModalConfigGroup.getSimulatedModes());
		for (String mode : simulatedModes) {

			if (instance.getRegisteredModes().contains(mode)) {
				log.warn("A routing algorithm for " + mode + " is already registered. It is replaced!");
			}
			
			TravelTime travelTime = this.multimodalTravelTimes.get(mode);
			if (travelTime == null) {
				throw new RuntimeException("No travel time object was found for mode " + mode + "! Aborting.");
			}
			
			Network subNetwork = multimodalSubNetworks.get(mode);
			if (subNetwork == null) {
				subNetwork = NetworkImpl.createNetwork();
				Set<String> restrictions = new HashSet<String>();
				restrictions.add(mode);
				TransportModeNetworkFilter networkFilter = new TransportModeNetworkFilter(network);
				networkFilter.filter(subNetwork, restrictions);
				this.multimodalSubNetworks.put(mode, subNetwork);
			}
			
			TravelDisutility travelDisutility = objectProvider.getTravelDisutilityFactory().createTravelDisutility(travelTime, objectProvider.getScenario().getConfig().planCalcScore());
			LeastCostPathCalculator routeAlgo = leastCostAlgoFactory.createPathCalculator(subNetwork, travelDisutility, travelTime);
			LegRouter networkLegRouter = new NetworkLegRouter(subNetwork, routeAlgo, modeRouteFactory);
			RoutingModule legRouterWrapper = new LegRouterWrapper(mode, populationFactory, networkLegRouter); 
			instance.setRoutingModule(mode, legRouterWrapper);
		}

		return instance;
	}
}
