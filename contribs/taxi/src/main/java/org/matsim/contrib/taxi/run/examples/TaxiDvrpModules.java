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

import java.util.Collections;

import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.MobsimTimerProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelDisutilityProvider;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.mobsim.framework.MobsimTimer;

import com.google.inject.Module;
import com.google.inject.Provider;

/**
 * @author michalm
 */
public class TaxiDvrpModules {
	public static DvrpModule create() {
		return create(DefaultTaxiOptimizerProvider.class);
	}

	public static DvrpModule create(Class<? extends Provider<? extends TaxiOptimizer>> providerClass) {
		return new DvrpModule(cfg -> createModuleForQSimPlugin(providerClass),
				Collections.singleton(TaxiOptimizer.class));
	}

	public static Module createModuleForQSimPlugin(Class<? extends Provider<? extends TaxiOptimizer>> providerClass) {
		return new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(MobsimTimer.class).toProvider(MobsimTimerProvider.class).asEagerSingleton();
				DvrpTravelDisutilityProvider.bindTravelDisutilityForOptimizer(binder(),
						DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER);
				bind(VrpOptimizer.class).to(TaxiOptimizer.class);
				bind(TaxiScheduler.class).asEagerSingleton();
				bind(DynActionCreator.class).to(TaxiActionCreator.class).asEagerSingleton();
				bind(PassengerRequestCreator.class).to(TaxiRequestCreator.class).asEagerSingleton();
				install(new com.google.inject.AbstractModule() {
					@Override
					protected void configure() {
						bind(TaxiOptimizer.class).toProvider(providerClass).asEagerSingleton();
					}
				});
			}
		};
	}
}
