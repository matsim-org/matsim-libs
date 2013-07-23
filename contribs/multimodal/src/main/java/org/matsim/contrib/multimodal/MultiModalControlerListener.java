/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalControlerListener.java
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

package org.matsim.contrib.multimodal;

import java.util.Collections;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.router.DefaultDelegateFactory;
import org.matsim.contrib.multimodal.router.MultimodalTripRouterFactory;
import org.matsim.contrib.multimodal.router.TransitTripRouterFactory;
import org.matsim.contrib.multimodal.router.util.LinkSlopesReader;
import org.matsim.contrib.multimodal.router.util.MultiModalTravelTimeFactory;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.pt.router.TransitRouterFactory;

public class MultiModalControlerListener implements StartupListener {

	private Map<String, TravelTime> multiModalTravelTimes;
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		Controler controler = event.getControler();
		MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) controler.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
		
		Map<Id, Double> linkSlopes = new LinkSlopesReader().getLinkSlopes(multiModalConfigGroup, controler.getNetwork());
		MultiModalTravelTimeFactory multiModalTravelTimeFactory = new MultiModalTravelTimeFactory(controler.getConfig(), linkSlopes);
		this.multiModalTravelTimes = multiModalTravelTimeFactory.createTravelTimes();	
	
		/*
		 * Cannot get the factory from the controler, therefore create a new one as the controler does. 
		 */
		TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults();
		LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = builder.createDefaultLeastCostPathCalculatorFactory(controler.getScenario());
		TransitRouterFactory transitRouterFactory = null;
		if (controler.getConfig().scenario().isUseTransit()) transitRouterFactory = builder.createDefaultTransitRouter(controler.getScenario());
		
		/*
		 * Use a ...
		 * - defaultDelegateFactory for the QNetsim modes
		 * - multiModalTripRouterFactory for the multi-modal modes
		 * - transitTripRouterFactory for transit trips
		 * 
		 * Note that a FastDijkstraFactory is used for the multiModalTripRouterFactory
		 * since ...
		 * - only "fast" router implementations handle sub-networks correct
		 * - AStarLandmarks uses link speed information instead of agent speeds
		 */
		TripRouterFactory defaultDelegateFactory = new DefaultDelegateFactory(controler.getScenario(), leastCostPathCalculatorFactory);
		TripRouterFactory multiModalTripRouterFactory = new MultimodalTripRouterFactory(controler.getScenario(), multiModalTravelTimes, 
				controler.getTravelDisutilityFactory(), defaultDelegateFactory, new FastDijkstraFactory());
		TripRouterFactory transitTripRouterFactory = new TransitTripRouterFactory(controler.getScenario(), multiModalTripRouterFactory, 
				transitRouterFactory);
		controler.setTripRouterFactory(transitTripRouterFactory);

		MultimodalQSimFactory qSimFactory = new MultimodalQSimFactory(multiModalTravelTimes);
		controler.setMobsimFactory(qSimFactory);
		
		// ensure that NetworkRoutes are created for legs using one of the simulated modes
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) controler.getScenario().getPopulation().getFactory()).getModeRouteFactory();
		for (String mode : CollectionUtils.stringToSet(multiModalConfigGroup.getSimulatedModes())) {
			routeFactory.setRouteFactory(mode, new LinkNetworkRouteFactory());
		}
	}
	
	public Map<String, TravelTime> getMultiModalTravelTimes() {
		return Collections.unmodifiableMap(this.multiModalTravelTimes);
	}
}