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

import java.lang.reflect.Method;

import org.apache.log4j.Logger;
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
 * Each SimpleRouter implements PersonalizableTravelCost and Cloneable.
 * This Factory should be use in combination with CloneablePlansCalcRoute.
 * There the SimpleRouter instance is cloned therefore we don't clone it
 * here again! Otherwise the CloneablePlansCalcRoute object can't hand 
 * a handled person to the SimpleRouter.
 * 
 */
public class SimpleRouterFactory implements LeastCostPathCalculatorFactory {

	private static final Logger log = Logger.getLogger(SimpleRouterFactory.class);
		
	public SimpleRouterFactory() {
	}
		
	public LeastCostPathCalculator createPathCalculator(Network network, TravelCost travelCosts, TravelTime travelTimes) {
		if (travelCosts instanceof SimpleRouter) {
			
			SimpleRouter simpleRouter = (SimpleRouter) travelCosts;
			if (simpleRouter instanceof Cloneable) {
				try {
					Method method;
					method = simpleRouter.getClass().getMethod("clone", new Class[]{});
					LeastCostPathCalculator clone = simpleRouter.getClass().cast(method.invoke(simpleRouter, new Object[]{}));
					return clone;
				} catch (Exception e) {
					return ((LeastCostPathCalculator) travelCosts);
				} 
			}
			else return ((LeastCostPathCalculator) travelCosts);
		}
		else return null;
	}

}
