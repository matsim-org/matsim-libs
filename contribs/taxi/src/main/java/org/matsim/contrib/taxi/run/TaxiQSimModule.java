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

package org.matsim.contrib.taxi.run;

import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.MobsimTimerProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelDisutilityProvider;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.Key;

/**
 * @author michalm
 */
public class TaxiQSimModule extends AbstractQSimModule {

	TaxiQSimModule() {
	}

	@Override
	protected void configureQSim() {
		bind(MobsimTimer.class).toProvider(MobsimTimerProvider.class).asEagerSingleton();
		DvrpTravelDisutilityProvider.bindTravelDisutilityForOptimizer(binder(), Taxi.class);

		bind(TaxiOptimizer.class).toProvider(DefaultTaxiOptimizerProvider.class).asEagerSingleton();
		bind(TaxiScheduler.class).asEagerSingleton();

		DvrpMode dvrpMode = DvrpModes.mode(TaxiConfigGroup.get(getConfig()).getMode());
		bind(VrpOptimizer.class).annotatedWith(dvrpMode).to(TaxiOptimizer.class);
		addQSimComponentBinding(dvrpMode).to(TaxiOptimizer.class);
		bind(DynActionCreator.class).annotatedWith(dvrpMode).to(TaxiActionCreator.class).asEagerSingleton();
		bind(PassengerRequestCreator.class).annotatedWith(dvrpMode).to(TaxiRequestCreator.class).asEagerSingleton();
		bind(PassengerEngine.class).annotatedWith(Taxi.class).to(Key.get(PassengerEngine.class, dvrpMode));
	}
}
