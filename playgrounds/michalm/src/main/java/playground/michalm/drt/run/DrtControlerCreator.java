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

/**
 * 
 */
package playground.michalm.drt.run;

import java.util.Collections;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.*;
import org.matsim.core.controler.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;

import com.google.inject.Provider;
import com.google.inject.name.Names;

import playground.michalm.drt.analysis.DRTAnalysisModule;
import playground.michalm.drt.optimizer.*;
import playground.michalm.drt.passenger.NDrtRequestCreator;
import playground.michalm.drt.routing.*;
import playground.michalm.drt.vrpagent.NDrtActionCreator;

/**
 * @author  jbischoff
 *
 */
public class DrtControlerCreator {

	public static Controler createControler(Config config, boolean otfvis) {
		DrtConfigGroup drtCfg = DrtConfigGroup.get(config);
		config.addConfigConsistencyChecker(new DvrpConfigConsistencyChecker());
		config.checkConsistency();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network drtNet = prepareDrtNetwork(scenario.getNetwork(), drtCfg.getDrtNetworkMode());
		FleetImpl fleet = new FleetImpl();
		new VehicleReader(drtNet, fleet).parse(drtCfg.getVehiclesFileUrl(config.getContext()));

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new DvrpModule(fleet,
				createModuleForQSimPlugin(DefaultDrtOptimizerProvider.class), DrtOptimizer.class));
		controler.addOverridingModule(new DRTAnalysisModule());
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind(Network.class).annotatedWith(Names.named(DrtConfigGroup.GROUP_NAME)).toInstance(drtNet);
			}
		});

		switch (drtCfg.getOperationalScheme()) {
			case door2door: {
				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						addRoutingModuleBinding(DrtConfigGroup.DRT_MODE).to(DrtRoutingModule.class).asEagerSingleton();
					}
				});
				break;
			}
			case stationbased: {
				final Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
				new TransitScheduleReader(scenario2)
						.readFile(drtCfg.getTransitStopsFileUrl(config.getContext()).getFile());
				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						bind(TransitSchedule.class).annotatedWith(Names.named(DrtConfigGroup.DRT_MODE))
								.toInstance(scenario2.getTransitSchedule());;
						addRoutingModuleBinding(DrtConfigGroup.DRT_MODE).to(StopBasedDrtRoutingModule.class)
								.asEagerSingleton();

					}
				});
				break;

			}
			default:
				throw new IllegalStateException();
		}
		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		return controler;
	}

	private static com.google.inject.AbstractModule createModuleForQSimPlugin(
			final Class<? extends Provider<? extends DrtOptimizer>> providerClass) {
		return new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(DrtOptimizer.class).toProvider(providerClass).asEagerSingleton();
				bind(VrpOptimizer.class).to(DrtOptimizer.class);
				bind(DynActionCreator.class).to(NDrtActionCreator.class).asEagerSingleton();
				bind(PassengerRequestCreator.class).to(NDrtRequestCreator.class).asEagerSingleton();
			}
		};
	}

	private static Network prepareDrtNetwork(Network network, String drtNetworkMode) {
		Network drtNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(drtNetwork, Collections.singleton(drtNetworkMode));
		return drtNetwork;
	}
}
