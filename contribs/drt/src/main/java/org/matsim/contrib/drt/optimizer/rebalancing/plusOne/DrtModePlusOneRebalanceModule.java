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
import org.matsim.contrib.drt.analysis.DrtRequestAnalyzer;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalWaitTimesAnalyzer;
import org.matsim.contrib.drt.analysis.zonal.ZonalIdleVehicleXYVisualiser;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.controler.MatsimServices;

/**
 * @author michalm, Chengqi Lu
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
		log.info("Plus one Rebalancing Algorithm is now being installed!");
		RebalancingParams generalParams = drtCfg.getRebalancingParams().orElseThrow();

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(PlusOneRebalancingStrategy.class).toProvider(
						modalProvider(getter -> new PlusOneRebalancingStrategy(getter.getModal(Network.class))))
						.asEagerSingleton();
				bindModal(RebalancingStrategy.class).to(modalKey(PlusOneRebalancingStrategy.class));
				addMobsimScopeEventHandlerBinding().to(modalKey(PlusOneRebalancingStrategy.class));
			}
		});

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
