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
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author dgrether
 * @author sebhoerl, IRT SystemX
 */
@Singleton
public class AStarLandmarksFactory implements LeastCostPathCalculatorFactory {

	private final Map<Network, PreProcessLandmarks> cache = new ConcurrentHashMap<>();

	private final int nThreads;
	private final int landmarks;

	@Inject
	public AStarLandmarksFactory(final GlobalConfigGroup globalConfigGroup, RoutingConfigGroup routingConfig) {
		this(globalConfigGroup.getNumberOfThreads(), routingConfig.getNetworkRoutingLandmarks());
	}

	public AStarLandmarksFactory(int numberOfThreads, int landmarks) {
		this.nThreads = Math.max(1, numberOfThreads);
		this.landmarks = landmarks;
	}

	@Override
	public synchronized LeastCostPathCalculator createPathCalculator(final Network network, final TravelDisutility travelCosts, final TravelTime travelTimes) {
		PreProcessLandmarks preProcessLandmarks = cache.computeIfAbsent(network, n -> {
			PreProcessLandmarks preprocess = new PreProcessLandmarks(travelCosts, landmarks);
			preprocess.setNumberOfThreads(nThreads);
			preprocess.run(n);
			return preprocess;
		});

		final double overdoFactor = 1.0;
		return new AStarLandmarks(network, preProcessLandmarks, travelCosts, travelTimes, overdoFactor);
	}
}
