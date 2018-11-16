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

/**
 *
 */
package org.matsim.contrib.drt.analysis;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.util.stats.DrtVehicleOccupancyProfileWriter;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.MatsimServices;

import com.google.inject.Key;

/**
 * @author michalm (Michal Maciejewski)
 */
public class MultiModeDrtAnalysisModule extends AbstractModule {
	private final DrtConfigGroup drtCfg;

	public MultiModeDrtAnalysisModule(DrtConfigGroup drtCfg) {
		this.drtCfg = drtCfg;
	}

	@Override
	public void install() {
		bind(modalKey(DynModePassengerStats.class)).toProvider(ModalProviders.createProvider(drtCfg.getMode(),
				getter -> new DynModePassengerStats(getter.get(Network.class), getter.get(EventsManager.class), drtCfg,
						getter.getModal(Fleet.class)))).asEagerSingleton();

		bind(modalKey(DrtRequestAnalyzer.class)).toProvider(ModalProviders.createProvider(drtCfg.getMode(),
				getter -> new DrtRequestAnalyzer(getter.get(EventsManager.class), getter.get(Network.class), drtCfg)))
				.asEagerSingleton();

		addControlerListenerBinding().toProvider(ModalProviders.createProvider(drtCfg.getMode(),
				getter -> new DrtAnalysisControlerListener(getter.get(Config.class), drtCfg,
						getter.getModal(Fleet.class), getter.getModal(DynModePassengerStats.class),
						getter.get(MatsimServices.class), getter.get(Network.class),
						getter.getModal(DrtRequestAnalyzer.class)))).asEagerSingleton();

		addMobsimListenerBinding().toProvider(ModalProviders.createProvider(drtCfg.getMode(),
				getter -> new DrtVehicleOccupancyProfileWriter(getter.getModal(Fleet.class),
						getter.get(MatsimServices.class), drtCfg)));
	}

	private <T> Key<T> modalKey(Class<T> type) {
		return DvrpModes.key(type, drtCfg.getMode());
	}
}
