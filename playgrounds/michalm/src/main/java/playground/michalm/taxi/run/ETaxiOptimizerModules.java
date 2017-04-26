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

package playground.michalm.taxi.run;

import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentQueryHelper;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.core.controler.AbstractModule;
import org.matsim.vis.otfvis.OnTheFlyServer.NonPlanAgentQueryHelper;

import com.google.inject.Inject;

import playground.michalm.taxi.optimizer.ETaxiOptimizerProvider;
import playground.michalm.taxi.vrpagent.ETaxiActionCreator;

public class ETaxiOptimizerModules {
	public static AbstractModule createDefaultModule() {
		return new DvrpModule(createModuleForQSimPlugin(), TaxiOptimizer.class);
	}

	public static AbstractModule createBenchmarkModule() {
		return new DvrpModule(createModuleForQSimPlugin(), TaxiOptimizer.class) {
			@Inject
			private DvrpConfigGroup dvrpCfg;

			/**
			 * Overrides the {@link org.matsim.contrib.dvrp.run.DvrpModule#install() in order to install
			 * freeSpeedTravelTimeForBenchmarkingModule instead of travelTimeEstimatorModule}
			 */
			@Override
			public void install() {
				String mode = dvrpCfg.getMode();
				addRoutingModuleBinding(mode).toInstance(new DynRoutingModule(mode));

				// Visualisation of schedules for DVRP DynAgents
				bind(NonPlanAgentQueryHelper.class).to(VrpAgentQueryHelper.class);

				// Fixed free-speed TT
				install(VrpTravelTimeModules.createFreeSpeedTravelTimeForBenchmarkingModule());
			}
		};
	}

	private static com.google.inject.AbstractModule createModuleForQSimPlugin() {
		return new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(TaxiOptimizer.class).toProvider(ETaxiOptimizerProvider.class).asEagerSingleton();
				bind(VrpOptimizer.class).to(TaxiOptimizer.class);
				bind(DynActionCreator.class).to(ETaxiActionCreator.class).asEagerSingleton();
				bind(PassengerRequestCreator.class).to(TaxiRequestCreator.class).asEagerSingleton();
			}
		};
	}
}
