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

import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.contrib.drt.analysis.zonal.ZonalDemandAggregator;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategy.RebalancingTargetCalculator;

/**
 * @author michalm
 */
public class LinearRebalancingTargetCalculator implements RebalancingTargetCalculator {
	private final ZonalDemandAggregator demandAggregator;
	private final MinCostFlowRebalancingParams params;

	public LinearRebalancingTargetCalculator(ZonalDemandAggregator demandAggregator,
			MinCostFlowRebalancingParams params) {
		this.demandAggregator = demandAggregator;
		this.params = params;
	}

	// FIXME targets should be calculated more intelligently
	@Override
	public int estimate(String zone, double time) {
		// XXX this "time+60" (taken from old code) means probably "in the next time bin"
		MutableInt expectedDemand = demandAggregator.getExpectedDemandForTimeBin(time + 60).get(zone);
		if (expectedDemand == null || expectedDemand.intValue() == 0) {
			return 0;// for larger zones we may assume that target is at least 1 (or in some cases) ??????
		}
		return (int)Math.round(params.getTargetAlpha() * expectedDemand.intValue() + params.getTargetBeta());
	}
}
