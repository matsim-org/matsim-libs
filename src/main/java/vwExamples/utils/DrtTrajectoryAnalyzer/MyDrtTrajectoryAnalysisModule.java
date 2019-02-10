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

package vwExamples.utils.DrtTrajectoryAnalyzer;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.ev.data.ElectricFleet;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;

/**
 * @author saxer
 */
public class MyDrtTrajectoryAnalysisModule extends AbstractDvrpModeModule {
	private final DrtConfigGroup drtCfg;

	public MyDrtTrajectoryAnalysisModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	public void install() {
		bindModal(MyDynModeTrajectoryStats.class).toProvider(modalProvider(
				getter -> new MyDynModeTrajectoryStats(getter.get(Network.class), getter.get(EventsManager.class),
						drtCfg, getter.getModal(FleetSpecification.class), getter.get(ElectricFleet.class))))
				.asEagerSingleton();

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				// this is a mobsim listener that gets notified whenever a new mobsim starts in
				// order to set Fleet inside MyDynModeTrajectoryStats
				addModalQSimComponentBinding().toProvider(modalProvider(getter -> (MobsimInitializedListener) (e -> {
					getter.getModal(MyDynModeTrajectoryStats.class).setFleetOnMobsimStart(getter.getModal(Fleet.class));
				})));
			}
		});

		addControlerListenerBinding()
				.toProvider(modalProvider(getter -> new DrtTrajectryControlerListener(getter.get(Config.class), drtCfg,
						getter.getModal(MyDynModeTrajectoryStats.class), getter.get(MatsimServices.class),
						getter.get(Network.class))));
	}
}
