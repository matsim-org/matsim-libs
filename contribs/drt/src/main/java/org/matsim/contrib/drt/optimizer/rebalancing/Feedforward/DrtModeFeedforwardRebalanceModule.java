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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.DrtRequestAnalyzer;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalWaitTimesAnalyzer;
import org.matsim.contrib.drt.analysis.zonal.ZonalIdleVehicleXYVisualiser;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;

/**
 * @author michalm, Chengqi Lu
 */
public class DrtModeFeedforwardRebalanceModule extends AbstractDvrpModeModule {
	private static final Logger log = Logger.getLogger(DrtModeFeedforwardRebalanceModule.class);
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
				bindModal(RebalancingStrategy.class)
						.toProvider(modalProvider(
								getter -> new FeedforwardRebalancingStrategy(getter.getModal(DrtZonalSystem.class),
										getter.getModal(Fleet.class), getter.getModal(Network.class), generalParams,
										strategySpecificParams, getter.getModal(FeedforwardSignalHandler.class))))
						.asEagerSingleton();
			}
		});

		// Create PreviousIterationDepartureRecoder (this will be created only once)
		bindModal(FeedforwardSignalHandler.class)
				.toProvider(modalProvider(getter -> new FeedforwardSignalHandler(getter.getModal(DrtZonalSystem.class),
						strategySpecificParams, getter.get(EventsManager.class))))
				.asEagerSingleton();

		addEventHandlerBinding().to(modalKey(FeedforwardSignalHandler.class));

		{
			// this is rather analysis - but depends on DrtZonalSystem so it can not be
			// moved into DrtModeAnalysisModule until DrtZonalSystem at the moment...
			bindModal(ZonalIdleVehicleXYVisualiser.class).toProvider(
					modalProvider(getter -> new ZonalIdleVehicleXYVisualiser(getter.get(MatsimServices.class),
							drtCfg.getMode(), getter.getModal(DrtZonalSystem.class))))
					.asEagerSingleton();
			addControlerListenerBinding().to(modalKey(ZonalIdleVehicleXYVisualiser.class));
			addEventHandlerBinding().to(modalKey(ZonalIdleVehicleXYVisualiser.class));

			bindModal(DrtZonalWaitTimesAnalyzer.class)
					.toProvider(modalProvider(getter -> new DrtZonalWaitTimesAnalyzer(drtCfg,
							getter.getModal(DrtRequestAnalyzer.class), getter.getModal(DrtZonalSystem.class))))
					.asEagerSingleton();
			addControlerListenerBinding().to(modalKey(DrtZonalWaitTimesAnalyzer.class));
			addEventHandlerBinding().to(modalKey(DrtZonalWaitTimesAnalyzer.class));
		}

	}
}
