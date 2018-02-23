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

import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.file.FleetProvider;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

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
		this(new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(TaxiOptimizer.class).toProvider(providerClass).asEagerSingleton();
			}
		});
	}

	public TaxiModule(Module taxiOptimizerModule) {
		dvrpModule = new DvrpModule(createModuleForQSimPlugin(taxiOptimizerModule), TaxiOptimizer.class);
	}

	public static Module createModuleForQSimPlugin(Module taxiOptimizerModule) {
		return new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(VrpOptimizer.class).to(TaxiOptimizer.class);
				bind(TaxiScheduler.class).asEagerSingleton();
				bind(DynActionCreator.class).to(TaxiActionCreator.class).asEagerSingleton();
				bind(PassengerRequestCreator.class).to(TaxiRequestCreator.class).asEagerSingleton();
				install(taxiOptimizerModule);
			}

			@Provides
			@Named(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER)
			private TravelDisutility provideTravelDisutility(
					@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime,
					@Named(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER) TravelDisutilityFactory travelDisutilityFactory) {
				return travelDisutilityFactory.createTravelDisutility(travelTime);
			}
		};
	}

	@Override
	public void install() {
		TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(getConfig());
		bind(Fleet.class).toProvider(new FleetProvider(taxiCfg.getTaxisFileUrl(getConfig().getContext())))
				.asEagerSingleton();
		bind(TravelDisutilityFactory.class).annotatedWith(Names.named(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER))
				.toInstance(travelTime -> new TimeAsTravelDisutility(travelTime));

		install(dvrpModule);
	}
}
