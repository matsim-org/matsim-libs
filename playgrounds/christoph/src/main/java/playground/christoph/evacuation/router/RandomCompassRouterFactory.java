/* *********************************************************************** *
 * project: org.matsim.*
 * RandomCompassRouter.java
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
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class RandomCompassRouterFactory implements LeastCostPathCalculatorFactory {

	private final boolean tabuSearch;
	private final double compassProbability;
	private final AcosProvider acosProvider;
	
	public RandomCompassRouterFactory(boolean tabuSearch, double compassProbability) {
		this.tabuSearch = tabuSearch;
		this.compassProbability = compassProbability;
		this.acosProvider = new AcosProvider();
	}
	
	@Override
	public LeastCostPathCalculator createPathCalculator(Network network,
			TravelDisutility travelCosts, TravelTime travelTimes) {
		return new RandomCompassRouter(network, tabuSearch, compassProbability, acosProvider);
	}
}