/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.benjamin.scenarios.santiago.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.DefaultPlanStrategiesModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;

import playground.benjamin.scenarios.santiago.SantiagoScenarioConstants;


public class SantiagoScenarioRunner {

//	private static String inputPath = "../../../runs-svn/santiago/run20/input/";
//	private static boolean doModeChoice = false;
	private static String inputPath = "../../../runs-svn/santiago/run30/input/";
	private static boolean doModeChoice = true;
	
	public static void main(String args[]){
		
//		OTFVis.convert(new String[]{
//						"",
//						outputPath + "modeChoice.output_events.xml.gz",	//events
//						outputPath + "modeChoice.output_network.xml.gz",	//network
//						outputPath + "visualisation.mvi", 		//mvi
//						"60" 									//snapshot period
//		});
//		
//		OTFVis.playMVI(outputPath + "visualisation.mvi");
		
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, inputPath + "config_final.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		Controler controler = new Controler(scenario);
		
		// adding ride and taxi as network modes requires some router; here, the same values as for car are used
		setNetworkModeRouting(controler);
		
		// adding pt fare in a simplified way
		controler.getEvents().addHandler(new PTFlatFareHandler(controler));
		
		// adding basic strategies for car and non-car users
		setBasicStrategiesForSubpopulations(controler);
		
		// adding subtour mode choice strategies for car and non-car users
		if(doModeChoice){
			setModeChoiceForSubpopulations(controler);
		}
		controler.run();
	}

	private static void setNetworkModeRouting(Controler controler) {
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());
				addTravelTimeBinding(SantiagoScenarioConstants.Modes.taxi.toString()).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(SantiagoScenarioConstants.Modes.taxi.toString()).to(carTravelDisutilityFactoryKey());
				addTravelTimeBinding(SantiagoScenarioConstants.Modes.colectivo.toString()).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(SantiagoScenarioConstants.Modes.colectivo.toString()).to(carTravelDisutilityFactoryKey());
				addTravelTimeBinding(SantiagoScenarioConstants.Modes.motorcycle.toString()).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(SantiagoScenarioConstants.Modes.motorcycle.toString()).to(carTravelDisutilityFactoryKey());
				addTravelTimeBinding(SantiagoScenarioConstants.Modes.school_bus.toString()).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(SantiagoScenarioConstants.Modes.school_bus.toString()).to(carTravelDisutilityFactoryKey());
			}
		});
	}

	private static void setBasicStrategiesForSubpopulations(Controler controler) {
		setReroute("carAvail", controler);
		setChangeExp("carAvail", controler);
		setReroute(null, controler);
		setChangeExp(null, controler);
	}

	private static void setChangeExp(String subpopName, Controler controler) {
		StrategySettings changeExpSettings = new StrategySettings();
		// TODO: why can the name be identical?
		changeExpSettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		changeExpSettings.setSubpopulation(subpopName);
		changeExpSettings.setWeight(0.7);
		controler.getConfig().strategy().addStrategySettings(changeExpSettings);
	}

	private static void setReroute(String subpopName, Controler controler) {
		StrategySettings reRouteSettings = new StrategySettings();
		// TODO: why can the name be identical?
		reRouteSettings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.toString());
		reRouteSettings.setSubpopulation(subpopName);
		reRouteSettings.setWeight(0.15);
		controler.getConfig().strategy().addStrategySettings(reRouteSettings);
		
	}

	private static void setModeChoiceForSubpopulations(final Controler controler) {
		final String nameMcCarAvail = "SubtourModeChoice_".concat("carAvail");
		StrategySettings modeChoiceCarAvail = new StrategySettings();
		modeChoiceCarAvail.setStrategyName(nameMcCarAvail);
		modeChoiceCarAvail.setSubpopulation("carAvail");
		modeChoiceCarAvail.setWeight(0.15);
		controler.getConfig().strategy().addStrategySettings(modeChoiceCarAvail);
		
		final String nameMcNonCarAvail = "SubtourModeChoice_".concat("nonCarAvail");
		StrategySettings modeChoiceNonCarAvail = new StrategySettings();
		modeChoiceNonCarAvail.setStrategyName(nameMcNonCarAvail);
		modeChoiceNonCarAvail.setSubpopulation(null);
		modeChoiceNonCarAvail.setWeight(0.15);
		controler.getConfig().strategy().addStrategySettings(modeChoiceNonCarAvail);
		
		// adding subtour mode choice strategy module for car and non-car users
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding(nameMcCarAvail).toProvider(new javax.inject.Provider<PlanStrategy>() {
					String[] availableModes = {TransportMode.car, TransportMode.bike, TransportMode.walk, TransportMode.pt};
					String[] chainBasedModes = {TransportMode.car, TransportMode.bike};

					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
						builder.addStrategyModule(new SubtourModeChoice(controler.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false));
						builder.addStrategyModule(new ReRoute(controler.getScenario()));
						return builder.build();
					}
				});
			}
		});
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding(nameMcNonCarAvail).toProvider(new javax.inject.Provider<PlanStrategy>() {
					String[] availableModes = {TransportMode.bike, TransportMode.walk, TransportMode.pt};
					String[] chainBasedModes = {TransportMode.bike};

					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
						builder.addStrategyModule(new SubtourModeChoice(controler.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false));
						builder.addStrategyModule(new ReRoute(controler.getScenario()));
						return builder.build();
					}
				});
			}
		});
	}
}