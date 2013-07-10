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

package contrib.multimodal.router;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.LegRouterWrapper;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.old.LegRouter;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.CollectionUtils;

/**
 * @author cdobler
 */
public class MultimodalTripRouterFactory implements TripRouterFactory {
	
	protected static final Logger log = Logger.getLogger(MultimodalTripRouterFactory.class);
	
	private final Controler controler;
	private final Map<String, TravelTime> multimodalTravelTimes;
	
	private TripRouterFactory delegateFactory;
	
	private final Map<String, Network> multimodalSubNetworks = new HashMap<String, Network>();
	
	public MultimodalTripRouterFactory(Controler controler, Map<String, TravelTime> multimodalTravelTimes) {
		this.controler = controler;
		this.multimodalTravelTimes = multimodalTravelTimes;
	}
	
	public MultimodalTripRouterFactory(Controler controler, Map<String, TravelTime> multimodalTravelTimes, 
			TripRouterFactory delegate) {
		this.controler = controler;
		this.multimodalTravelTimes = multimodalTravelTimes;
		this.delegateFactory = delegate;
	}
	
	@Override
	public TripRouter instantiateAndConfigureTripRouter() {	

		if (this.delegateFactory == null) {
			this.delegateFactory = new TripRouterFactoryImpl(
					controler.getScenario(),
					controler.getTravelDisutilityFactory(),
					controler.getLinkTravelTimes(),
					controler.getLeastCostPathCalculatorFactory(),
					controler.getTransitRouterFactory() );
		}
		TripRouter instance = this.delegateFactory.instantiateAndConfigureTripRouter();
		
		Network network = this.controler.getNetwork();
		LeastCostPathCalculatorFactory leastCostAlgoFactory = this.controler.getLeastCostPathCalculatorFactory();
		TravelDisutilityFactory travelDisutilityFactory = this.controler.getTravelDisutilityFactory();
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = this.controler.getConfig().planCalcScore();
		PopulationFactory populationFactory = this.controler.getPopulation().getFactory();
		ModeRouteFactory modeRouteFactory = ((PopulationFactoryImpl) populationFactory).getModeRouteFactory();
		
		Set<String> simulatedModes = CollectionUtils.stringToSet(this.controler.getConfig().multiModal().getSimulatedModes());
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
			
			TravelDisutility travelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime, planCalcScoreConfigGroup);
			LeastCostPathCalculator routeAlgo = leastCostAlgoFactory.createPathCalculator(subNetwork, travelDisutility, travelTime);
			LegRouter networkLegRouter = new NetworkLegRouter(subNetwork, routeAlgo, modeRouteFactory);
			RoutingModule legRouterWrapper = new LegRouterWrapper(mode, populationFactory, networkLegRouter); 
			instance.setRoutingModule(mode, legRouterWrapper);
		}

		return instance;
	}
}
