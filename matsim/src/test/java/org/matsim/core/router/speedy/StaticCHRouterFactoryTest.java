/* *********************************************************************** *
 * project: org.matsim.*
 * StaticCHRouterFactoryTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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
 * Standard test suite for {@link StaticCHRouterFactory}. Inherits the full
 * {@link AbstractCHLeastCostPathCalculatorTest} suite (basic routing + turn
 * restrictions) and exercises the factory exactly the way external callers
 * would: a single {@code createPathCalculator} call per scenario, using
 * {@link FreespeedTravelTimeAndDisutility} for static travel costs.
 */
public class StaticCHRouterFactoryTest extends AbstractCHLeastCostPathCalculatorTest {
	@Override
	protected LeastCostPathCalculator getLeastCostPathCalculator(final Network network) {
		FreespeedTravelTimeAndDisutility tc = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
		return new StaticCHRouterFactory().createPathCalculator(network, tc, tc);
	}
}
