/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationTripRouterFactory.java
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

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.multimodal.router.DefaultDelegateFactory;
import org.matsim.contrib.multimodal.router.MultimodalTripRouterFactory;
import org.matsim.contrib.multimodal.router.TransitTripRouterFactory;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.TransitRouter;

import java.util.Map;

public class EvacuationTripRouterFactory implements TripRouterFactory {

	private final TripRouterFactory delegateFactory;
	
	public EvacuationTripRouterFactory(Scenario scenario, Map<String, TravelTime> multiModalTravelTimes,
			TravelDisutilityFactory travelDisutilityFactory, LeastCostPathCalculatorFactory leastCostPathCalculatorFactory,
									   Provider<TransitRouter> transitRouterFactory) {
		
		TripRouterFactory defaultDelegateFactory = new DefaultDelegateFactory(scenario, leastCostPathCalculatorFactory);
		TripRouterFactory multiModalTripRouterFactory = new MultimodalTripRouterFactory(scenario, multiModalTravelTimes, 
				travelDisutilityFactory, defaultDelegateFactory, new FastDijkstraFactory());
		
		TripRouterFactory transitTripRouterFactory = new TransitTripRouterFactory(scenario, multiModalTripRouterFactory, 
				transitRouterFactory);
		
		this.delegateFactory = transitTripRouterFactory;
	}
	
	@Override
	public TripRouter instantiateAndConfigureTripRouter(RoutingContext routingContext) {

		TripRouter instance = this.delegateFactory.instantiateAndConfigureTripRouter(routingContext);
		return instance;
	}

}
