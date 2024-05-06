/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.optimizer.rebalancing;

import org.matsim.contrib.drt.analysis.zonal.DrtModeZonalSystemModule;
import org.matsim.contrib.drt.optimizer.rebalancing.Feedforward.DrtModeFeedforwardRebalanceModule;
import org.matsim.contrib.drt.optimizer.rebalancing.Feedforward.FeedforwardRebalancingStrategyParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.DrtModeMinCostFlowRebalancingModule;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.optimizer.rebalancing.plusOne.DrtModePlusOneRebalanceModule;
import org.matsim.contrib.drt.optimizer.rebalancing.plusOne.PlusOneRebalancingStrategyParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;

/**
 * @author Michal Maciejewski (michalm)
 */
public class RebalancingModule extends AbstractDvrpModeModule {

	private final DrtConfigGroup drtCfg;

	public RebalancingModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	public void install() {
		if (drtCfg.getRebalancingParams().isPresent()) {
			RebalancingParams rebalancingParams = drtCfg.getRebalancingParams().get();

			if (rebalancingParams.getRebalancingStrategyParams() instanceof MinCostFlowRebalancingStrategyParams) {
				install(new DrtModeMinCostFlowRebalancingModule(drtCfg));
			} else if (rebalancingParams.getRebalancingStrategyParams() instanceof PlusOneRebalancingStrategyParams) {
				install(new DrtModePlusOneRebalanceModule(drtCfg));
			} else if (rebalancingParams.getRebalancingStrategyParams() instanceof FeedforwardRebalancingStrategyParams) {
				install(new DrtModeFeedforwardRebalanceModule(drtCfg));
			} else if (rebalancingParams.getRebalancingStrategyParams() instanceof CustomRebalancingStrategyParams) {
				// User is responsible for installing custom module
			} else {
				throw new RuntimeException(
						"Unsupported rebalancingStrategyParams: " + rebalancingParams.getRebalancingStrategyParams());
			}
		} else {
			bindModal(RebalancingStrategy.class).to(NoRebalancingStrategy.class).asEagerSingleton();
		}
	}
}
