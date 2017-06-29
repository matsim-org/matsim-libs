/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.run;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.*;
import com.google.inject.name.Named;

/**
 * @author michalm
 */
public final class TaxiModule extends AbstractModule {
	public static final String TAXI_MODE = "taxi";

	private final DvrpModule dvrpModule;

	public TaxiModule() {
		this(DefaultTaxiOptimizerProvider.class);
	}

	public TaxiModule(Class<? extends Provider<? extends TaxiOptimizer>> providerClass) {
		dvrpModule = new DvrpModule(createModuleForQSimPlugin(providerClass), TaxiOptimizer.class);
	}

	private static com.google.inject.AbstractModule createModuleForQSimPlugin(
			final Class<? extends Provider<? extends TaxiOptimizer>> providerClass) {
		return new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(TaxiOptimizer.class).toProvider(providerClass).asEagerSingleton();
				bind(VrpOptimizer.class).to(TaxiOptimizer.class);
				bind(DynActionCreator.class).to(TaxiActionCreator.class).asEagerSingleton();
				bind(PassengerRequestCreator.class).to(TaxiRequestCreator.class).asEagerSingleton();
			}
		};
	}

	@Override
	public void install() {
		bind(Fleet.class).toProvider(DefaultTaxiFleetProvider.class).asEagerSingleton();
		install(dvrpModule);
	}

	@Singleton
	public static final class DefaultTaxiFleetProvider implements Provider<Fleet> {
		@Inject
		@Named(DvrpModule.DVRP_ROUTING)
		Network network;
		@Inject
		Config config;
		@Inject
		TaxiConfigGroup taxiCfg;

		@Override
		public Fleet get() {
			FleetImpl fleet = new FleetImpl();
			new VehicleReader(network, fleet).parse(taxiCfg.getTaxisFileUrl(config.getContext()));
			return fleet;
		}
	}
}
