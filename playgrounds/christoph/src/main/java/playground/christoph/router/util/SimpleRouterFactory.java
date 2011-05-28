/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleRouterFactory.java
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

package playground.christoph.router.util;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

/*
 * A simple LeastCostPathCalculatorFactory for Routing Modules that
 * ignore TravelCosts and TravelTimes as for example a Random Router
 * does.
 * 
 * Each SimpleRouter implements PersonalizableTravelCost. Therefore, the 
 * person is handed over to the SimpleRouter. 
 */
public class SimpleRouterFactory implements LeastCostPathCalculatorFactory {

	public SimpleRouterFactory() {
	}
		
	public LeastCostPathCalculator createPathCalculator(Network network, TravelCost travelCosts, TravelTime travelTimes) {
		if (travelCosts instanceof SimpleRouter) {
			
			SimpleRouter simpleRouter = (SimpleRouter) travelCosts;
			return simpleRouter.createInstance();
		}
		return null;
	}
}
