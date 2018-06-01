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

import javax.inject.Inject;

import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.ZonalDemandAggregator;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategy.RebalancingTargetCalculator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.controler.AbstractModule;

/**
 * @author michalm
 */
public class MinCostFlowRebalancingModule extends AbstractModule {
	@Inject
	private DrtConfigGroup drtCfg;

	@Override
	public void install() {
		MinCostFlowRebalancingParams params = drtCfg.getMinCostFlowRebalancing();
		bind(DrtZonalSystem.class).toProvider(new DrtZonalSystem.DrtZonalSystemProvider(params.getCellSize()));
		bind(RebalancingStrategy.class).to(MinCostFlowRebalancingStrategy.class).asEagerSingleton();
		bind(RebalancingTargetCalculator.class).to(LinearRebalancingTargetCalculator.class).asEagerSingleton();
		bind(MinCostRelocationCalculator.class).to(AggregatedMinCostRelocationCalculator.class).asEagerSingleton();
		bind(ZonalDemandAggregator.class).asEagerSingleton();
	}
}
