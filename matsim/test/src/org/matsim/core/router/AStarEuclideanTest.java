/* *********************************************************************** *
 * project: org.matsim.*
 * AStarEuclideanTest.java
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

package org.matsim.core.router;

import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.AStarEuclidean;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PreProcessEuclidean;

public class AStarEuclideanTest extends AbstractLeastCostPathCalculatorTest {

	@Override
	protected LeastCostPathCalculator getLeastCostPathCalculator(NetworkLayer network) {
		FreespeedTravelTimeCost travelTimeCostCalculator = new FreespeedTravelTimeCost();
		PreProcessEuclidean preProcessData = new PreProcessEuclidean(travelTimeCostCalculator);
		preProcessData.run(network);
		return new AStarEuclidean(network, preProcessData, travelTimeCostCalculator);
	}

}
