/* *********************************************************************** *
 * project: org.matsim.*
 * LeastCostPathCalculatorSelectorFactory.java
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

package playground.christoph.evacuation.router;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;

public class LeastCostPathCalculatorSelectorFactory implements LeastCostPathCalculatorFactory {
	
	private final DecisionDataProvider decisionDataProvider;
	private final LeastCostPathCalculatorFactory panicRouterFactory;
	private final LeastCostPathCalculatorFactory nonPanicRouterFactory;
	
	public LeastCostPathCalculatorSelectorFactory(LeastCostPathCalculatorFactory nonPanicRouterFactory,
			 LeastCostPathCalculatorFactory panicRouterFactory, DecisionDataProvider decisionDataProvider) {
		this.nonPanicRouterFactory = nonPanicRouterFactory;
		this.panicRouterFactory = panicRouterFactory;
		this.decisionDataProvider = decisionDataProvider;
	}
	
	@Override
	public IntermodalLeastCostPathCalculator createPathCalculator(Network network,
			TravelDisutility travelCosts, TravelTime travelTimes) {
		IntermodalLeastCostPathCalculator panicRouter = (IntermodalLeastCostPathCalculator) 
				panicRouterFactory.createPathCalculator(network, travelCosts, travelTimes);
		IntermodalLeastCostPathCalculator nonPanicRouter = (IntermodalLeastCostPathCalculator) 
				nonPanicRouterFactory.createPathCalculator(network, travelCosts, travelTimes);
		return new LeastCostPathCalculatorSelector(nonPanicRouter, panicRouter, decisionDataProvider);
	}
}