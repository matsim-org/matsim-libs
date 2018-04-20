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

import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.ZonalDemandAggregator;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategy.RebalancingTargetCalculator;
import org.matsim.core.controler.AbstractModule;

/**
 * @author michalm
 */
public class MinCostFlowRebalancingModule extends AbstractModule {
	private final double cellSize;

	public MinCostFlowRebalancingModule(double cellSize) {
		this.cellSize = cellSize;
	}

	@Override
	public void install() {
		bind(DrtZonalSystem.class).toProvider(new DrtZonalSystem.DrtZonalSystemProvider(cellSize));
		bind(RebalancingStrategy.class).to(MinCostFlowRebalancingStrategy.class).asEagerSingleton();
		bind(RebalancingTargetCalculator.class).to(LinearRebalancingTargetCalculator.class).asEagerSingleton();
		bind(MinCostRelocationCalculator.class).to(AggregatedMinCostRelocationCalculator.class).asEagerSingleton();
		bind(ZonalDemandAggregator.class).asEagerSingleton();
	}
}
