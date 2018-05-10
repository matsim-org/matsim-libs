/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.consistency.ConfigConsistencyChecker;

public class MinCostFlowRebalancingParamsConsistencyChecker implements ConfigConsistencyChecker {
	@Override
	public void checkConsistency(Config config) {
		MinCostFlowRebalancingParams params = DrtConfigGroup.get(config).getMinCostFlowRebalancing();
		if (params == null) {
			return;// no rebalancing
		}
		if (params.getMinServiceTime() <= params.getMaxTimeBeforeIdle()) {
			throw new RuntimeException(MinCostFlowRebalancingParams.MIN_SERVICE_TIME + " must be greater than "
					+ MinCostFlowRebalancingParams.MAX_TIME_BEFORE_IDLE);
		}
	}
}
