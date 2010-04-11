/* *********************************************************************** *
 * project: org.matsim.*
 * CloningDijkstraFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.router.MyDijkstra;

/*
 * Basically we could also extend LeastCostPathCalculatorFactory -
 * currently we don't use methods from DijkstraFactory but maybe
 * somewhere is checked if our Class is instanceof DijkstraFactory...
 * 
 * Instead of a Dijkstra we return a MyDijkstra which is able
 * to use personalized (Sub)-Networks for each Agent.
 */
public class CloningDijkstraFactory extends DijkstraFactory {

	private static final Logger log = Logger.getLogger(CloningDijkstraFactory.class);
	
	@Override
	public LeastCostPathCalculator createPathCalculator(final Network network, final TravelCost travelCosts, final TravelTime travelTimes)
	{
		/*
		 *  Return only a clone (if possible)
		 *  Otherwise we could get problems when doing the
		 *  Replanning multi-threaded.
		 */
		TravelCost travelCostClone = null;
		if (travelCosts instanceof Cloneable)
		{
			try
			{
				Method method;
				method = travelCosts.getClass().getMethod("clone", new Class[]{});
				travelCostClone = travelCosts.getClass().cast(method.invoke(travelCosts, new Object[]{}));
			}
			catch (Exception e)
			{
				Gbl.errorMsg(e);
			} 
		}
		// not cloneable or an Exception occured
		if (travelCostClone == null)
		{
			travelCostClone = travelCosts;
			log.warn("Could not clone the Travel Cost Calculator - use reference to the existing Calculator and hope the best...");
		}
		
		TravelTime travelTimeClone = null;
		if (travelTimes instanceof Cloneable)
		{
			try
			{
				Method method;
				method = travelTimes.getClass().getMethod("clone", new Class[]{});
				travelTimeClone = travelTimes.getClass().cast(method.invoke(travelTimes, new Object[]{}));
			}
			catch (Exception e)
			{
				Gbl.errorMsg(e);
			} 
		}
		// not cloneable or an Exception occured
		if (travelTimeClone == null)
		{
			travelTimeClone = travelTimes;
			log.warn("Could not clone the Travel Time Calculator - use reference to the existing Calculator and hope the best...");
		}
		
		Dijkstra dijkstra = new MyDijkstra(network, travelCostClone, travelTimeClone);
		return dijkstra;
	}

}
