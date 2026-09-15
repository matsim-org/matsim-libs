/* *********************************************************************** *
 * project: org.matsim.*
 * CHRouterStaticTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.router.AbstractCHLeastCostPathCalculatorTest;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;

/**
 * Standard test suite for the static (non-time-dependent) {@link CHRouter}.
 *
 * <p>Uses {@link SpeedyGraphBuilder#buildWithSpatialOrdering(Network)} (Z-order
 * Morton curve), matching the production path in {@link CHRouterFactory}.
 * {@link FreespeedTravelTimeAndDisutility} provides static travel costs.
 *
 * <p>Inherits the full standard test suite (3 basic routing tests + 4 turn-restriction
 * tests) from {@link AbstractCHLeastCostPathCalculatorTest}.
 *
 * @author Steffen Axer
 */
public class CHRouterStaticTest extends AbstractCHLeastCostPathCalculatorTest {
	@Override
	protected LeastCostPathCalculator getLeastCostPathCalculator(final Network network) {
		FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
		SpeedyGraph g = SpeedyGraphBuilder.buildWithSpatialOrdering(network);
		CHGraph chGraph = new CHBuilder(g, tc).build();
		new CHCustomizer().customize(chGraph, tc);
		return new CHRouter(chGraph, tc, tc);
	}
}

