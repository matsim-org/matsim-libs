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

package org.matsim.contrib.taxi.run.examples;

import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
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

/**
 * @author michalm
 */
public class TaxiDvrpModules {
	public static DvrpModule create() {
		return create(DefaultTaxiOptimizerProvider.class);
	}

	public static DvrpModule create(Class<? extends Provider<? extends TaxiOptimizer>> providerClass) {
		return create(new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(TaxiOptimizer.class).toProvider(providerClass).asEagerSingleton();
			}
		});
	}

	public static DvrpModule create(Module taxiOptimizerModule) {
		return new DvrpModule(createModuleForQSimPlugin(taxiOptimizerModule), TaxiOptimizer.class);
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
			@Singleton
			private MobsimTimer provideTimer(QSim qSim) {
				return qSim.getSimTimer();
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
}
