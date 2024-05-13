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

package org.matsim.contrib.drt.optimizer.rebalancing.Feedforward;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZoneTargetLinkSelector;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.demandestimator.NetDepartureReplenishDemandEstimator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;

/**
 * @author Chengqi Lu
 * @author michalm (Michal Maciejewski)
 */
public class DrtModeFeedforwardRebalanceModule extends AbstractDvrpModeModule {
	private static final Logger log = LogManager.getLogger(DrtModeFeedforwardRebalanceModule.class);
	private final DrtConfigGroup drtCfg;

	public DrtModeFeedforwardRebalanceModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	public void install() {
		log.info("Feedforward Rebalancing Strategy is now being installed!");
		RebalancingParams generalParams = drtCfg.getRebalancingParams().orElseThrow();
		FeedforwardRebalancingStrategyParams strategySpecificParams = (FeedforwardRebalancingStrategyParams) generalParams
				.getRebalancingStrategyParams();

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(RebalancingStrategy.class).toProvider(modalProvider(
						getter -> new FeedforwardRebalancingStrategy(getter.getModal(ZoneSystem.class),
								getter.getModal(Fleet.class), generalParams, strategySpecificParams,
								getter.getModal(FeedforwardSignalHandler.class),
								getter.getModal(DrtZoneTargetLinkSelector.class),
								getter.getModal(FastHeuristicZonalRelocationCalculator.class))))
						.asEagerSingleton();
			}
		});

		// Create PreviousIterationDepartureRecoder (this will be created only once)
		bindModal(FeedforwardSignalHandler.class).toProvider(modalProvider(
				getter -> new FeedforwardSignalHandler(getter.getModal(ZoneSystem.class), strategySpecificParams,
						getter.getModal(NetDepartureReplenishDemandEstimator.class)))).asEagerSingleton();

		bindModal(NetDepartureReplenishDemandEstimator.class).toProvider(modalProvider(
				getter -> new NetDepartureReplenishDemandEstimator(getter.getModal(ZoneSystem.class), drtCfg,
						strategySpecificParams))).asEagerSingleton();

		bindModal(FastHeuristicZonalRelocationCalculator.class).toProvider(modalProvider(
				getter -> new FastHeuristicZonalRelocationCalculator(
						getter.getModal(DrtZoneTargetLinkSelector.class))));

		addEventHandlerBinding().to(modalKey(NetDepartureReplenishDemandEstimator.class));
		addControlerListenerBinding().to(modalKey(FeedforwardSignalHandler.class));
	}
}
