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

import org.apache.log4j.Logger;
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
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dziemke
 */
public class RunBicycleExample {
	private static final Logger LOG = Logger.getLogger(RunBicycleExample.class);

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

	public void run(Config config, boolean considerMotorizedInteraction) {
		config.global().setNumberOfThreads(1);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		config.plansCalcRoute().setRoutingRandomness(3.);
		
		if (considerMotorizedInteraction) {
			BicycleConfigGroup bicycleConfigGroup = (BicycleConfigGroup) config.getModules().get(BicycleConfigGroup.GROUP_NAME);
			bicycleConfigGroup.setMotorizedInteraction(considerMotorizedInteraction);
		}
				
		Scenario scenario = ScenarioUtils.loadScenario(config);

		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
		scenario.getVehicles().addVehicleType(car);

		VehicleType bicycle = VehicleUtils.getFactory().createVehicleType(Id.create("bicycle", VehicleType.class));
		bicycle.setMaximumVelocity(20.0/3.6);
		bicycle.setPcuEquivalents(0.25);
		scenario.getVehicles().addVehicleType(bicycle);

		scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

		Controler controler = new Controler(scenario);
		BicycleModule bicycleModule = new BicycleModule(scenario);
		controler.addOverridingModule(bicycleModule);
		
	/*	controler.addOverridingQSimModule(new AbstractQSimModule() {

            @Override
            protected void configureQSim() {
                bind(QNetworkFactory.class).toProvider(new Provider<QNetworkFactory>() {
                    @Inject
                    private EventsManager events;

                    @Override
                    public QNetworkFactory get() {
                        final ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(events, scenario);
                        factory.setLinkSpeedCalculator(new BicycleLinkSpeedCalculator(scenario));
                        return factory;
                    }
                });
            }
        });
	*/
		controler.run();
	}
	
	public static void fillConfigWithBicycleStandardValues(Config config) {
		config.controler().setWriteEventsInterval(1);
		
		BicycleConfigGroup bicycleConfigGroup = (BicycleConfigGroup) config.getModules().get(BicycleConfigGroup.GROUP_NAME);	
		bicycleConfigGroup.setMarginalUtilityOfInfrastructure_m(-0.0002);
		bicycleConfigGroup.setMarginalUtilityOfComfort_m(-0.0002);
		bicycleConfigGroup.setMarginalUtilityOfGradient_m_100m(-0.02);
		bicycleConfigGroup.setMaxBicycleSpeedForRouting(4.16666666);
		
		List<String> mainModeList = new ArrayList<>();
		mainModeList.add("bicycle");
		mainModeList.add(TransportMode.car);
		config.qsim().setMainModes(mainModeList);
		
		config.strategy().setMaxAgentPlanMemorySize(5);
		{
			StrategySettings strategySettings = new StrategySettings();
			strategySettings.setStrategyName("ChangeExpBeta");
			strategySettings.setWeight(0.8);
			config.strategy().addStrategySettings(strategySettings);
		}{
			StrategySettings strategySettings = new StrategySettings();
			strategySettings.setStrategyName("ReRoute");
			strategySettings.setWeight(0.2);
			config.strategy().addStrategySettings(strategySettings);
		}
		
		ActivityParams homeActivity = new ActivityParams("home");
		homeActivity.setTypicalDuration(12*60*60);
		config.planCalcScore().addActivityParams(homeActivity);
		
		ActivityParams workActivity = new ActivityParams("work");
		workActivity.setTypicalDuration(8*60*60);
		config.planCalcScore().addActivityParams(workActivity);
		
		ModeParams bicycle = new ModeParams("bicycle");
		bicycle.setConstant(0.);
		bicycle.setMarginalUtilityOfDistance(-0.0004); // util/m
		bicycle.setMarginalUtilityOfTraveling(-6.0); // util/h
		bicycle.setMonetaryDistanceRate(0.);
		config.planCalcScore().addModeParams(bicycle);
		
		config.plansCalcRoute().setNetworkModes(mainModeList);
	}
}