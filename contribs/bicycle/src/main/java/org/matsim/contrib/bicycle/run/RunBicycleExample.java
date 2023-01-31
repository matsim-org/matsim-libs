/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
import org.matsim.contrib.bicycle.BicycleModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AllowsConfiguration;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dziemke
 */
public class RunBicycleExample {
	private static final Logger LOG = LogManager.getLogger(RunBicycleExample.class);

	public static void main(String[] args) {
		Config config;
		if (args.length == 1) {
			LOG.info("A user-specified config.xml file was provided. Using it...");
			config = ConfigUtils.loadConfig(args[0], new BicycleConfigGroup());
			fillConfigWithBicycleStandardValues(config);
		} else if (args.length == 0) {
			LOG.info("No config.xml file was provided. Using 'standard' example files given in this contrib's resources folder.");
			// Setting the context like this works when the data is stored under "/matsim/contribs/bicycle/src/main/resources/bicycle_example"
			config = ConfigUtils.createConfig("bicycle_example/");
			config.addModule(new BicycleConfigGroup());
			fillConfigWithBicycleStandardValues(config);

			config.network().setInputFile("network_lane.xml"); // Modify this
			config.plans().setInputFile("population_1200.xml");
		} else {
			throw new RuntimeException("More than one argument was provided. There is no procedure for this situation. Thus aborting!"
								     + " Provide either (1) only a suitable config file or (2) no argument at all to run example with given example of resources folder.");
		}
		config.controler().setLastIteration(100); // Modify if motorized interaction is used
		boolean considerMotorizedInteraction = false;

		new RunBicycleExample().run(config, considerMotorizedInteraction);
	}

	static void fillConfigWithBicycleStandardValues(Config config) {
		config.controler().setWriteEventsInterval(1);

		BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule( config, BicycleConfigGroup.class );
		bicycleConfigGroup.setMarginalUtilityOfInfrastructure_m(-0.0002);
		bicycleConfigGroup.setMarginalUtilityOfComfort_m(-0.0002);
		bicycleConfigGroup.setMarginalUtilityOfGradient_m_100m(-0.02);
		bicycleConfigGroup.setMarginalUtilityOfUserDefinedNetworkAttribute_m(-0.0000); // always needs to be negative
		bicycleConfigGroup.setUserDefinedNetworkAttributeName("quietness"); // needs to be defined as a value from 0 to 1, 1 being best, 0 being worst
		bicycleConfigGroup.setUserDefinedNetworkAttributeDefaultValue(0.1); // used for those links that do not have a value for the user-defined attribute

		bicycleConfigGroup.setMaxBicycleSpeedForRouting(4.16666666);

		List<String> mainModeList = new ArrayList<>();
		mainModeList.add("bicycle");
		mainModeList.add(TransportMode.car);

		config.qsim().setMainModes(mainModeList);

		config.strategy().setMaxAgentPlanMemorySize(5);
		config.strategy().addStrategySettings( new StrategySettings().setStrategyName("ChangeExpBeta" ).setWeight(0.8 ) );
		config.strategy().addStrategySettings( new StrategySettings().setStrategyName("ReRoute" ).setWeight(0.2 ) );

		config.planCalcScore().addActivityParams( new ActivityParams("home").setTypicalDuration(12*60*60 ) );
		config.planCalcScore().addActivityParams( new ActivityParams("work").setTypicalDuration(8*60*60 ) );

		config.planCalcScore().addModeParams( new ModeParams("bicycle").setConstant(0. ).setMarginalUtilityOfDistance(-0.0004 ).setMarginalUtilityOfTraveling(-6.0 ).setMonetaryDistanceRate(0. ) );

		config.plansCalcRoute().setNetworkModes(mainModeList);
	}

	public void run(Config config, boolean considerMotorizedInteraction) {
		config.global().setNumberOfThreads(1);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.plansCalcRoute().setRoutingRandomness(3.);

		if (considerMotorizedInteraction) {
			BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule( config, BicycleConfigGroup.class );
			bicycleConfigGroup.setMotorizedInteraction(considerMotorizedInteraction);
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);

		// set config such that the mode vehicles come from vehicles data:
		scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

		// now put hte mode vehicles into the vehicles data:
		final VehiclesFactory vf = VehicleUtils.getFactory();
		scenario.getVehicles().addVehicleType( vf.createVehicleType(Id.create(TransportMode.car, VehicleType.class ) ) );
		scenario.getVehicles().addVehicleType( vf.createVehicleType(Id.create("bicycle", VehicleType.class ) ).setMaximumVelocity(4.16666666 ).setPcuEquivalents(0.25 ) );

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new BicycleModule() );

		controler.run();
	}
}
