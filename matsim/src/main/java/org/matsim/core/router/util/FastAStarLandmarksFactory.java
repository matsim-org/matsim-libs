/* *********************************************************************** *
 * project: org.matsim.*
 * FastAStarLandmarksFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
import org.matsim.core.router.FastAStarLandmarks;

/**
 * @author cdobler
 */
public class FastAStarLandmarksFactory implements LeastCostPathCalculatorFactory {

	private PreProcessLandmarks preProcessData;
	
	public FastAStarLandmarksFactory() {
		// Nothing to do.
	}
	
	public FastAStarLandmarksFactory(Network network, final TravelMinDisutility fsttc){
		processNetwork(network, fsttc);
	}

	public void processNetwork(Network network, final TravelMinDisutility fsttc) {
		synchronized (this) {
				this.preProcessData = new PreProcessLandmarks(fsttc);
				this.preProcessData.run(network);
		}
	}
	
	@Override
	public LeastCostPathCalculator createPathCalculator(Network network,
			TravelDisutility travelCosts, TravelTime travelTimes) {
		return new FastAStarLandmarks(network, this.preProcessData, travelCosts, travelTimes);
	}
}