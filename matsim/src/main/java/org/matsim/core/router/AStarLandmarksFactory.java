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

package org.matsim.core.router;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author dgrether
 */
@Singleton
public class AStarLandmarksFactory implements LeastCostPathCalculatorFactory {

	private final Map<Network, PreProcessLandmarks> preProcessData = new HashMap<>();

	private final int nThreads;

	@Inject
	public AStarLandmarksFactory(final GlobalConfigGroup globalConfigGroup) {
		this(globalConfigGroup.getNumberOfThreads());
	}

	public AStarLandmarksFactory(int numberOfThreads) {
		this.nThreads = numberOfThreads;
	}

	@Override
	public synchronized LeastCostPathCalculator createPathCalculator(final Network network, final TravelDisutility travelCosts, final TravelTime travelTimes) {
		PreProcessLandmarks preProcessLandmarks = this.preProcessData.get(network);
		if (preProcessLandmarks == null) {
			preProcessLandmarks = new PreProcessLandmarks(travelCosts);
			preProcessLandmarks.setNumberOfThreads(nThreads);
			preProcessLandmarks.run(network);
			this.preProcessData.put(network, preProcessLandmarks);
		}

		final double overdoFactor = 1.0;
		return new AStarLandmarks(network, preProcessLandmarks, travelCosts, travelTimes, overdoFactor);
	}
}
