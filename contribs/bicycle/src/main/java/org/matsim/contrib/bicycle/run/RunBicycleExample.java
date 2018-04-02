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

import java.util.ArrayList;
import java.util.List;

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

/**
 * @author dziemke
 */
public class RunBicycleExample {

	public static void main(String[] args) {
		// This works when the data is stored under "/matsim/contribs/bicycle/src/main/resources/bicycle_example"
		Config config = ConfigUtils.loadConfig("bicycle_example/config.xml", new BicycleConfigGroup());
		config.network().setInputFile("network_cobblestone.xml"); // change test cases
		
//		fillConfigWithBicycleStandardValues(config); // actually not necessary; does not really save anything
		config.controler().setLastIteration(0); // modifiy if motorized interaction is used
		boolean considerMotorizedInteraction = false;
		new RunBicycleExample().run(config, considerMotorizedInteraction);
	}

	public void run(Config config, boolean considerMotorizedInteraction) {
		config.global().setNumberOfThreads(1);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		config.plansCalcRoute().setRoutingRandomness(3.);
				
		Scenario scenario = ScenarioUtils.loadScenario(config);

		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
		scenario.getVehicles().addVehicleType(car);

		VehicleType bicycle = VehicleUtils.getFactory().createVehicleType(Id.create("bicycle", VehicleType.class));
		bicycle.setMaximumVelocity(15.0/3.6);
		bicycle.setPcuEquivalents(0.25);
		scenario.getVehicles().addVehicleType(bicycle);

		scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

		Controler controler = new Controler(scenario);
		BicycleModule bicycleModule = new BicycleModule();
		if (considerMotorizedInteraction) {
			bicycleModule.setConsiderMotorizedInteraction(true);
		}
		controler.addOverridingModule(bicycleModule);
		
		controler.run();
	}
	
	private static void fillConfigWithBicycleStandardValues(Config config) {
		config.controler().setWriteEventsInterval(1);
		
		config.getModules().get("bicycle").getParams().put("marginalUtilityOfInfrastructure_m", "-0.0002");
		config.getModules().get("bicycle").getParams().put("marginalUtilityOfComfort_m", "-0.0002");
		config.getModules().get("bicycle").getParams().put("marginalUtilityOfGradient_m_100m", "-0.02");
		
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