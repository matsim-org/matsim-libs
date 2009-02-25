/* *********************************************************************** *
 * project: org.matsim.*
 * AStarLandmarksFactory
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
package org.matsim.router.util;

import org.matsim.network.NetworkLayer;
import org.matsim.router.AStarLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;


/**
 * @author dgrether
 *
 */
public class AStarLandmarksFactory implements LeastCostPathCalculatorFactory {

	private PreProcessLandmarks preProcessData;
	
	public AStarLandmarksFactory(NetworkLayer network, final FreespeedTravelTimeCost fsttc){
		synchronized (this) {
				this.preProcessData = new PreProcessLandmarks(fsttc);
				this.preProcessData.run(network);
		}
	}
	/**
	 * @deprecated Use other constructor of this class
	 * @param preProcessData
	 */
	@Deprecated
	public AStarLandmarksFactory(final PreProcessLandmarks preProcessData){
		this.preProcessData = preProcessData;
	}
	
	/**
	 * @see org.matsim.router.util.LeastCostPathCalculatorFactory#createPathCalculator(org.matsim.network.NetworkLayer, org.matsim.router.util.TravelCost, org.matsim.router.util.TravelTime)
	 */
	public LeastCostPathCalculator createPathCalculator(NetworkLayer network,
			TravelCost travelCosts, TravelTime travelTimes) {
		return new AStarLandmarks(network, this.preProcessData, travelCosts, travelTimes);
	}

}
