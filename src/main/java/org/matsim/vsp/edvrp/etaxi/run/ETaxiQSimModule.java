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

package org.matsim.vsp.edvrp.etaxi.run;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.MobsimTimerProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelDisutilityProvider;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.contrib.taxi.run.Taxi;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.vsp.edvrp.etaxi.ETaxiActionCreator;
import org.matsim.vsp.edvrp.etaxi.ETaxiScheduler;
import org.matsim.vsp.edvrp.etaxi.optimizer.ETaxiOptimizerProvider;

import com.google.inject.Key;

public class ETaxiQSimModule extends AbstractQSimModule {
	@Override
	protected void configureQSim() {
		bind(MobsimTimer.class).toProvider(MobsimTimerProvider.class).asEagerSingleton();
		DvrpTravelDisutilityProvider.bindTravelDisutilityForOptimizer(binder(), Taxi.class);

		bind(TaxiOptimizer.class).toProvider(ETaxiOptimizerProvider.class).asEagerSingleton();
		bind(ETaxiScheduler.class).asEagerSingleton();
		bind(TaxiActionCreator.class).asEagerSingleton();

		DvrpMode dvrpMode = DvrpModes.mode(DrtConfigGroup.get(getConfig()).getMode());
		bind(VrpOptimizer.class).annotatedWith(dvrpMode).to(TaxiOptimizer.class);
		bind(DynActionCreator.class).annotatedWith(dvrpMode).to(ETaxiActionCreator.class).asEagerSingleton();
		bind(PassengerRequestCreator.class).annotatedWith(dvrpMode).to(TaxiRequestCreator.class).asEagerSingleton();
		bind(PassengerEngine.class).annotatedWith(Taxi.class).to(Key.get(PassengerEngine.class, dvrpMode));
	}
}
