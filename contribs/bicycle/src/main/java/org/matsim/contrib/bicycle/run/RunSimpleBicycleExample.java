/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.BicycleConfigGroup.MotorizedInteraction;
import org.matsim.contrib.bicycle.BicycleModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Minimal run example for the MATSim bicycle contrib.
 * <p>
 * Shows the smallest reasonable setup to run a mixed bicycle + car simulation
 * using the default example data (network_lane.xml, population_1200.xml).
 * The example data lives in the top-level matsim-libs
 * {@code examples/scenarios/bicycle_example/} directory and is resolved via
 * {@link ExamplesUtils}; the {@code matsim-examples} Maven dependency must be
 * on the classpath (declared in this contrib's pom.xml).
 * <p>
 */
public final class RunSimpleBicycleExample {
	private static final Logger LOG = LogManager.getLogger(RunSimpleBicycleExample.class);

	private static final String BICYCLE = "bicycle";

	// 6.944 m/s ~ 25 km/h; closer to typical urban cycling speeds.
	// Previous default was 4.1667 m/s (~15 km/h).
	private static final double BICYCLE_SPEED = 6.944;

	public static void main(String[] args) {
		Config config;
		if (args.length == 1) {
			LOG.info("A user-specified config.xml file was provided. Using it...");
			config = ConfigUtils.loadConfig(args[0], new BicycleConfigGroup());
		} else if (args.length == 0) {
			LOG.info("No config.xml provided. Using the default bicycle_example shipped with matsim-libs.");
			// Resolve the scenario directory via ExamplesUtils so that the example
			// works both when run from a built matsim-examples.jar and when run
			// directly from the matsim-libs source tree.
			URL scenarioUrl = ExamplesUtils.getTestScenarioURL("bicycle_example");
			config = ConfigUtils.createConfig();
			config.addModule(new BicycleConfigGroup());
			config.network().setInputFile(IOUtils.extendUrl(scenarioUrl, "network_lane.xml").toString());
			config.plans().setInputFile(IOUtils.extendUrl(scenarioUrl, "population_1200.xml").toString());
			fillConfigWithDefaults(config);
		} else {
			throw new RuntimeException("Expected 0 or 1 argument (optional path to a config.xml), got " + args.length + ".");
		}

		new RunSimpleBicycleExample().run(config);
	}

	private static void fillConfigWithDefaults(Config config) {
		config.controller().setLastIteration(10);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setWriteEventsInterval(1);
		config.global().setNumberOfThreads(1);

		// --- bicycle-specific scoring weights ---
		BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);
		bicycleConfigGroup.setBicycleMode(BICYCLE);
		bicycleConfigGroup.setMarginalUtilityOfInfrastructure_m(-0.002);
		bicycleConfigGroup.setMarginalUtilityOfComfort_m(-0.002);
		bicycleConfigGroup.setMarginalUtilityOfGradient_pct_m(-0.002);

		// Motorized interaction between cars and bicycles is disabled in this
		// minimal example. For the available modes and how to enable them, see
		// RunBicycleExampleWithVariants#runWithMotorizedInteraction.
		bicycleConfigGroup.setMotorizedInteractionType(MotorizedInteraction.NONE);

		// --- main/network modes: bicycle + car share the same network ---
		List<String> mainModes = Arrays.asList(BICYCLE, TransportMode.car);
		config.qsim().setMainModes(mainModes);
		// PassingQ lets bicycles be overtaken by cars on the same link; required
		// for meaningful mixed-traffic dynamics and for motorized interaction.
		config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
		config.routing().setNetworkModes(mainModes);
		// Make bicycle a network-routed mode only; otherwise it would be defined
		// as both teleportation and network mode, which triggers a consistency error.
		config.routing().removeTeleportedModeParams(BICYCLE);

		// routingRandomness:
		//   0.0 -> deterministic routing; recommended for small test scenarios
		//          and for the unit tests that rely on this example.
		//   ~3.0 -> randomized routing; more realistic for real-world scenarios
		//          where identical-cost routes should not always be picked.
		config.routing().setRoutingRandomness(0.);

		// --- replanning: keep a small plan memory; mix ChangeExpBeta + ReRoute ---
		config.replanning().setMaxAgentPlanMemorySize(5);
		config.replanning().addStrategySettings(new StrategySettings().setStrategyName("ChangeExpBeta").setWeight(0.8));
		config.replanning().addStrategySettings(new StrategySettings().setStrategyName("ReRoute").setWeight(0.2));

		// --- activities + mode scoring ---
		config.scoring().addActivityParams(new ActivityParams("home").setTypicalDuration(12 * 60 * 60));
		config.scoring().addActivityParams(new ActivityParams("work").setTypicalDuration(8 * 60 * 60));
		config.scoring().addModeParams(new ModeParams(BICYCLE)
			.setConstant(0.)
			.setMarginalUtilityOfDistance(-0.0004)
			.setMarginalUtilityOfTraveling(-6.0)
			.setMonetaryDistanceRate(0.));
	}

	public void run(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// Use mode-specific vehicle types defined in the scenario's vehicles
		// container (not inferred from network defaults).
		scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

		final VehiclesFactory vf = VehicleUtils.getFactory();
		scenario.getVehicles().addVehicleType(
			vf.createVehicleType(Id.createVehicleTypeId(TransportMode.car))
				.setNetworkMode(TransportMode.car));
		scenario.getVehicles().addVehicleType(
			vf.createVehicleType(Id.createVehicleTypeId(BICYCLE))
				.setNetworkMode(BICYCLE)
				.setMaximumVelocity(BICYCLE_SPEED)
				.setPcuEquivalents(0.25)
				.setLength(2.0));

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new BicycleModule());
		controler.run();
	}
}
