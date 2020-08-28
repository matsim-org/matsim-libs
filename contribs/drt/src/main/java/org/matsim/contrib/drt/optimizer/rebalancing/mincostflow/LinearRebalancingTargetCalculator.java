/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.rebalancing.mincostflow;

import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.optimizer.rebalancing.demandestimator.ZonalDemandEstimator;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategy.RebalancingTargetCalculator;

/**
 * @author michalm
 */
public class LinearRebalancingTargetCalculator implements RebalancingTargetCalculator {
	private final ZonalDemandEstimator demandEstimator;
	private final MinCostFlowRebalancingStrategyParams params;

	public LinearRebalancingTargetCalculator(ZonalDemandEstimator demandEstimator,
											 MinCostFlowRebalancingStrategyParams params) {
		this.demandEstimator = demandEstimator;
		this.params = params;
	}

	// FIXME targets should be calculated more intelligently
	@Override
	public int estimate(DrtZone zone, double time) {
		// XXX this "time+60" (taken from old code) means probably "in the next time bin"
		int expectedDemand = demandEstimator.getExpectedDemandForTimeBin(time + 60).applyAsInt(zone);
		if (expectedDemand == 0) {
			return 0;// for larger zones we may assume that target is at least 1 (or in some cases) ??????
		}
		return (int)Math.round(params.getTargetAlpha() * expectedDemand + params.getTargetBeta());
	}
}
