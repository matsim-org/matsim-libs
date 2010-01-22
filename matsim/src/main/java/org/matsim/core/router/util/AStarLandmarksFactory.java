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
package org.matsim.core.router.util;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.AStarLandmarks;


/**
 * @author dgrether
 *
 */
public class AStarLandmarksFactory implements LeastCostPathCalculatorFactory {

	private PreProcessLandmarks preProcessData;
	
	public AStarLandmarksFactory(Network network, final TravelMinCost fsttc){
		synchronized (this) {
				this.preProcessData = new PreProcessLandmarks(fsttc);
				this.preProcessData.run(network);
		}
	}
	
	public LeastCostPathCalculator createPathCalculator(Network network,
			TravelCost travelCosts, TravelTime travelTimes) {
		return new AStarLandmarks(network, this.preProcessData, travelCosts, travelTimes);
	}

}
