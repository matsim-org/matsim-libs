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

package org.matsim.contrib.drt.optimizer.rebalancing.plusOne;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;

/**
 * @author Chengqi Lu
 * @author michalm (Michal Maciejewski)
 */
public class DrtModePlusOneRebalanceModule extends AbstractDvrpModeModule {
	private static final Logger log = Logger.getLogger(DrtModePlusOneRebalanceModule.class);
	private final DrtConfigGroup drtCfg;

	public DrtModePlusOneRebalanceModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	public void install() {
		log.info("Plus one rebalancing strategy is now being installed!");
		RebalancingParams generalParams = drtCfg.getRebalancingParams().orElseThrow();
		PlusOneRebalancingStrategyParams specificParams = (PlusOneRebalancingStrategyParams)generalParams.getRebalancingStrategyParams();

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(PlusOneRebalancingStrategy.class).toProvider(modalProvider(
						getter -> new PlusOneRebalancingStrategy(getMode(), getter.getModal(Network.class),
								getter.getModal(LinkBasedRelocationCalculator.class)))).asEagerSingleton();

				// binding zone free relocation calculator
				switch (specificParams.getZoneFreeRelocationCalculatorType()) {
					case FastHeuristic:
						bindModal(LinkBasedRelocationCalculator.class).toProvider(
								modalProvider(getter -> new FastHeuristicLinkBasedRelocationCalculator()))
								.asEagerSingleton();
						break;

					default:
						throw new IllegalArgumentException("Unsupported rebalancingTargetCalculatorType="
								+ specificParams.getZoneFreeRelocationCalculatorType());
				}

				// binding event handler
				bindModal(RebalancingStrategy.class).to(modalKey(PlusOneRebalancingStrategy.class));
				addMobsimScopeEventHandlerBinding().to(modalKey(PlusOneRebalancingStrategy.class));
			}
		});

	}
}
