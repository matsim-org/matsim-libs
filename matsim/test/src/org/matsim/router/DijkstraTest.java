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

package org.matsim.router;

import org.matsim.network.NetworkLayer;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.LeastCostPathCalculator;

/**
 * @author mrieser
 */
public class DijkstraTest extends AbstractLeastCostPathCalculatorTest {
	
	protected LeastCostPathCalculator getLeastCostPathCalculator(final NetworkLayer network) {
		FreespeedTravelTimeCost travelTimeCostCalculator = new FreespeedTravelTimeCost();
		return new Dijkstra(network, travelTimeCostCalculator, travelTimeCostCalculator);
	}
	
}
