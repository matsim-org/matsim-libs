/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.examples.onetruck;

import java.util.Arrays;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.file.FleetProvider;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.DvrpConfigConsistencyChecker;
import org.matsim.contrib.dvrp.run.DvrpQSimModuleBuilder;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSource;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * @author michalm
 */
public final class RunOneTruckExample {
	private static final String CONFIG_FILE = "./src/main/resources/one_truck/one_truck_config.xml";
	private static final String TRUCK_FILE = "one_truck_vehicles.xml";

	public static void run(boolean otfvis, int lastIteration) {
		// load config
		Config config = ConfigUtils.loadConfig(CONFIG_FILE, new OTFVisConfigGroup());
		config.qsim().setMainModes(Arrays.asList(TransportMode.car, TransportMode.truck));
		config.travelTimeCalculator().setAnalyzedModes(TransportMode.car + "," + TransportMode.truck);
		config.controler().setLastIteration(lastIteration);
		config.addConfigConsistencyChecker(new DvrpConfigConsistencyChecker());
		config.checkConsistency();

		// load scenario
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// set up DVRP
		DvrpQSimModuleBuilder builder = createQSimModuleBuilder();

		// setup controler
		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new AbstractModule() {
			public void install() {
				bind(VehicleType.class).annotatedWith(Names.named(VrpAgentSource.DVRP_VEHICLE_TYPE))
						.toInstance(createTruckType());
				addTravelTimeBinding(DvrpTravelTimeModule.DVRP_ESTIMATED).to(
						Key.get(TravelTime.class, Names.named(TransportMode.truck)));
				bind(Network.class).annotatedWith(Names.named(DvrpRoutingNetworkProvider.DVRP_ROUTING))
						.to(Network.class);
			}
		});

		controler.addQSimModule(builder.build(config));
		controler.configureQSimComponents(builder::configureComponents);

		controler.addOverridingModule(
				FleetProvider.createModule(ConfigGroup.getInputFileURL(config.getContext(), TRUCK_FILE)));

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule()); // OTFVis visualisation
		}

		// run simulation
		controler.run();
	}

	private static VehicleType createTruckType() {
		VehicleType truckType = VehicleUtils.getFactory().createVehicleType(Id.create("truck", VehicleType.class));
		truckType.setLength(15.);
		VehicleCapacity vehicleCapacity = new VehicleCapacityImpl();
		vehicleCapacity.setSeats(1);
		truckType.setCapacity(vehicleCapacity);
		return truckType;
	}

	private static DvrpQSimModuleBuilder createQSimModuleBuilder() {
		return new DvrpQSimModuleBuilder(cfg -> {
			return new AbstractQSimModule() {
				@Override
				protected void configureQSim() {
					bind(OneTruckRequestCreator.class).asEagerSingleton();
					bind(VrpOptimizer.class).to(OneTruckOptimizer.class).asEagerSingleton();
					bind(DynActionCreator.class).to(OneTruckActionCreator.class).asEagerSingleton();
				}
			};
		}).addListener(OneTruckRequestCreator.class)//
				.setAddPassengerEnginePlugin(false);
	}

	public static void main(String... args) {
		run(true, 0); // switch to 'false' to turn off visualisation
	}
}
