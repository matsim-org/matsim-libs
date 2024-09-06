/* *********************************************************************** *
 * project: org.matsim.*
 * DijkstraTest.java
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

package org.matsim.core.router.speedy;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.router.AbstractLeastCostPathCalculatorTest;
import org.matsim.core.router.AbstractLeastCostPathCalculatorTestWithTurnRestrictions;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;

/**
 * @author mrieser
 */
public class SpeedyDijkstraTest extends AbstractLeastCostPathCalculatorTestWithTurnRestrictions {

	@Override
	protected LeastCostPathCalculator getLeastCostPathCalculator(final Network network) {
		FreespeedTravelTimeAndDisutility travelTimeCostCalculator = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
		SpeedyGraph g = SpeedyGraphBuilder.build(network);
		return new SpeedyDijkstra(g, travelTimeCostCalculator, travelTimeCostCalculator);
	}

}
