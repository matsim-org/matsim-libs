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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.AStarLandmarks;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * @author dgrether
 */
@Singleton
public class AStarLandmarksFactory implements LeastCostPathCalculatorFactory {

	private PreProcessLandmarks preProcessData;

	@Inject
	AStarLandmarksFactory(PlanCalcScoreConfigGroup planCalcScoreConfigGroup, GlobalConfigGroup globalConfigGroup, Network network, Map<String, TravelTime> travelTime, Map<String, TravelDisutilityFactory> travelDisutilityFactory) {
		//TODO: No guarantee that these are the same travel times for which the router is later requested.
		this(network, travelDisutilityFactory.get(TransportMode.car).createTravelDisutility(travelTime.get(TransportMode.car)), globalConfigGroup.getNumberOfThreads());
	}

	public AStarLandmarksFactory(Network network, final TravelDisutility fsttc) {
		processNetwork(network, fsttc, 8);
	}

	public AStarLandmarksFactory(Network network, final TravelDisutility fsttc, final int numberOfThreads){
		processNetwork(network, fsttc, numberOfThreads);
	}

	/**
	 * @deprecated this should be a private method
	 */
	public void processNetwork(Network network, final TravelDisutility fsttc, final int numberOfThreads) {
		synchronized (this) {
				this.preProcessData = new PreProcessLandmarks(fsttc);
				this.preProcessData.setNumberOfThreads(numberOfThreads);
				this.preProcessData.run(network);
		}
	}
	
	@Override
	public LeastCostPathCalculator createPathCalculator(Network network,
			TravelDisutility travelCosts, TravelTime travelTimes) {
		return new AStarLandmarks(network, this.preProcessData, travelCosts, travelTimes);
	}

}
