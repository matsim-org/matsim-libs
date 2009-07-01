/* *********************************************************************** *
 * project: org.matsim.*
 * LeastCostPathCalculatorInvertedNetProxyFactory
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.core.router.util;

import org.matsim.core.network.NetworkLayer;



/**
 * Factory that wraps some Proxies around the instances needed for LeastCostPath calculations
 * that consider LinkToLink travel times. This class requires the TravelTime instance to be an
 * instance of LinkToLinkTravelTime. 
 * Furthermore, as the correct function of the AStarLandmarksrouter is not guaranteed while 
 * working on an inverted network this class won't work together with the AStarLandmarksFactory.
 * 
 * @author dgrether
 *
 */
public class LeastCostPathCalculatorInvertedNetProxyFactory implements
		LeastCostPathCalculatorFactory {

	private LeastCostPathCalculatorFactory originalFactory;
	private NetworkLayer invertedNetwork;
	private TravelTimesInvertedNetProxy travelTimesProxy;
	private TravelCostsInvertedNetProxy travelCostsProxy;


	public LeastCostPathCalculatorInvertedNetProxyFactory(LeastCostPathCalculatorFactory originalFactory){
		if (originalFactory instanceof AStarLandmarksFactory){
			throw new IllegalStateException("Link to link routing is not available for AStarLandmarks routing," +
					" use the Dijkstra router instead. ");
		}
		this.originalFactory = originalFactory;
	}
	
	/**
	 * @see org.matsim.core.router.util.LeastCostPathCalculatorFactory#createPathCalculator(org.matsim.core.network.NetworkLayer, org.matsim.core.router.util.TravelCost, org.matsim.core.router.util.TravelTime)
	 */
	public LeastCostPathCalculator createPathCalculator(NetworkLayer network,
			TravelCost travelCosts, TravelTime travelTimes) {
		if (!(travelTimes instanceof LinkToLinkTravelTime)){
			throw new IllegalStateException("The TravelTimeCalculator must be an instance of LinkToLinkTravelTime" +
					" if link to link travel times should be used for routing. Check the appropriate config option in the" +
					" controler config module!");
		}
		NetworkInverter networkInverter = new NetworkInverter(network);
		
		this.invertedNetwork = networkInverter.getInvertedNetwork();
		
		this.travelTimesProxy = new TravelTimesInvertedNetProxy(network, (LinkToLinkTravelTime) travelTimes);
		this.travelCostsProxy = new TravelCostsInvertedNetProxy(network, travelCosts);
		
		LeastCostPathCalculator calculator = this.originalFactory.createPathCalculator(this.invertedNetwork, this.travelCostsProxy, this.travelTimesProxy);
		
		return new LeastCostPathCalculatorInvertedNetProxy(networkInverter, calculator);
	}

}
