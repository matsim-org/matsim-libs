/*
 * *********************************************************************** *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.optimizer.rebalancing.mincostflow;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.ZonalDemandAggregator;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategy.RebalancingTargetCalculator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Key;

/**
 * @author michalm
 */
public class MultiModalMinCostFlowRebalancingModule extends AbstractModule {
	private final DrtConfigGroup drtCfg;

	public MultiModalMinCostFlowRebalancingModule(DrtConfigGroup drtCfg) {
		this.drtCfg = drtCfg;
	}

	@Override
	public void install() {
		MinCostFlowRebalancingParams params = drtCfg.getMinCostFlowRebalancing();
		bind(modalKey(DrtZonalSystem.class)).toProvider(
				new DrtZonalSystem.DrtZonalSystemProvider(params.getCellSize()));

		bind(modalKey(RebalancingStrategy.class)).toProvider(ModalProviders.createProvider(drtCfg.getMode(),
				getter -> new MinCostFlowRebalancingStrategy(getter.getModal(RebalancingTargetCalculator.class),
						getter.getModal(DrtZonalSystem.class), getter.getModal(Fleet.class),
						getter.getModal(MinCostRelocationCalculator.class), drtCfg))).asEagerSingleton();

		bind(modalKey(RebalancingTargetCalculator.class)).toProvider(ModalProviders.createProvider(drtCfg.getMode(),
				getter -> new LinearRebalancingTargetCalculator(getter.getModal(ZonalDemandAggregator.class), drtCfg)))
				.asEagerSingleton();

		bind(modalKey(MinCostRelocationCalculator.class)).toProvider(ModalProviders.createProvider(drtCfg.getMode(),
				getter -> new AggregatedMinCostRelocationCalculator(getter.getModal(DrtZonalSystem.class),
						getter.getNamed(Network.class, DvrpRoutingNetworkProvider.DVRP_ROUTING)))).asEagerSingleton();

		bind(modalKey(ZonalDemandAggregator.class)).toProvider(ModalProviders.createProvider(drtCfg.getMode(),
				getter -> new ZonalDemandAggregator(getter.get(EventsManager.class),
						getter.getModal(DrtZonalSystem.class), drtCfg))).asEagerSingleton();
	}

	private <T> Key<T> modalKey(Class<T> type) {
		return DvrpModes.key(type, drtCfg.getMode());
	}
}
