/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package org.matsim.codeexamples.simpleResponsiveSignalEngine;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.controller.fixedTime.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactoryImpl;
import org.matsim.contrib.signals.data.signalgroups.v20.*;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactoryImpl;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Creates a simple one-crossing-scenario and starts it with the simple responsive signal from this package.
 * 
 * @author tthunig
 *
 */
public class RunSimpleResponsiveSignalExample {
	
	private final Controler controler;

	public static void main(String[] args) {
		new RunSimpleResponsiveSignalExample().run();
	}

	public RunSimpleResponsiveSignalExample() {
		final Config config = defineConfig();
		final Scenario scenario = defineScenario(config);
		controler = new Controler(scenario);

		// add the general signals module
//		controler.addOverridingModule(new SignalsModule());
		Signals.configure( controler );
		
		// add the responsive signal as a controler listener
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(SimpleResponsiveSignal.class).asEagerSingleton();
				addControlerListenerBinding().to(SimpleResponsiveSignal.class);
			}
		});
	}
	
	public void run(){
		controler.run();
	}

	public Controler getControler() {
		return controler;
	}

	private static Scenario defineScenario(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);
		// add missing scenario elements
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

		createNetwork(scenario);
		createPopulation(scenario);
		createSignals(scenario);
		return scenario;
	}

	/** creates a network like this:
	 * 
	 * 					 6
	 * 					 ^
	 * 					 |
	 * 					 v
	 * 					 7
	 * 					 ^
	 * 					 |
	 * 					 v
	 * 1 <----> 2 <----> 3 <----> 4 <----> 5
	 * 					 ^
	 * 					 |
	 * 					 v
	 * 					 8
	 * 					 ^
	 * 					 |
	 * 					 v
	 * 					 9 
	 * 
	 * @param scenario
	 */
	private static void createNetwork(Scenario scenario) {
		Network net = scenario.getNetwork();
		NetworkFactory fac = net.getFactory();
	
		net.addNode(fac.createNode(Id.createNodeId(1), new Coord(-2000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(2), new Coord(-1000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(3), new Coord(0, 0)));
		net.addNode(fac.createNode(Id.createNodeId(4), new Coord(1000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(5), new Coord(2000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(6), new Coord(0, 2000)));
		net.addNode(fac.createNode(Id.createNodeId(7), new Coord(0, 1000)));
		net.addNode(fac.createNode(Id.createNodeId(8), new Coord(0, -1000)));
		net.addNode(fac.createNode(Id.createNodeId(9), new Coord(0, -2000)));
		
		String[] links = {"1_2", "2_1", "2_3", "3_2", "3_4", "4_3", "4_5", "5_4", 
				"6_7", "7_6", "7_3", "3_7", "3_8", "8_3", "8_9", "9_8"};
		
		for (String linkId : links){
			String fromNodeId = linkId.split("_")[0];
			String toNodeId = linkId.split("_")[1];
			Link link = fac.createLink(Id.createLinkId(linkId), 
					net.getNodes().get(Id.createNodeId(fromNodeId)), 
					net.getNodes().get(Id.createNodeId(toNodeId)));
			link.setCapacity(7200);
			link.setLength(1000);
			link.setFreespeed(10);
			net.addLink(link);
		}
	}

	private static void createPopulation(Scenario scenario) {
		Population population = scenario.getPopulation();
		
		String[] odRelations = {"1_2-4_5", "4_5-2_1", "6_7-8_9", "9_8-7_6"};
		
		for (String od : odRelations) {
			String fromLinkId = od.split("-")[0];
			String toLinkId = od.split("-")[1];
	
			for (int i = 0; i < 3600; i++) {
				// create a person
				Person person = population.getFactory().createPerson(Id.createPersonId(od + "-" + i));
				population.addPerson(person);
	
				// create a plan for the person that contains all this
				// information
				Plan plan = population.getFactory().createPlan();
				person.addPlan(plan);
	
				// create a start activity at the from link
				Activity startAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(fromLinkId));
				// distribute agents uniformly during one hour.
				startAct.setEndTime(i);
				plan.addActivity(startAct);
	
				// create a dummy leg
				plan.addLeg(population.getFactory().createLeg(TransportMode.car));
	
				// create a drain activity at the to link
				Activity drainAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(toLinkId));
				plan.addActivity(drainAct);
			}
		}
	}

	private static void createSignals(Scenario scenario) {
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalSystemsDataFactory sysFac = new SignalSystemsDataFactoryImpl();
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalControlData signalControl = signalsData.getSignalControlData();
		SignalControlDataFactory conFac = new SignalControlDataFactoryImpl();
		
		// create signal system
		Id<SignalSystem> signalSystemId = Id.create("SignalSystem1", SignalSystem.class);
		SignalSystemData signalSystem = sysFac.createSignalSystemData(signalSystemId);
		signalSystems.addSignalSystemData(signalSystem);
		
		// create a signal for every inLink
		for (Id<Link> inLinkId : scenario.getNetwork().getNodes().get(Id.createNodeId(3)).getInLinks().keySet()) {
			SignalData signal = sysFac.createSignalData(Id.create("Signal" + inLinkId, Signal.class));
			signalSystem.addSignalData(signal);
			signal.setLinkId(inLinkId);
		}
		
		// group signals with non conflicting streams
		Id<SignalGroup> signalGroupId1 = Id.create("SignalGroup1", SignalGroup.class);
		SignalGroupData signalGroup1 = signalGroups.getFactory()
				.createSignalGroupData(signalSystemId, signalGroupId1);
		signalGroup1.addSignalId(Id.create("Signal2_3", Signal.class));
		signalGroup1.addSignalId(Id.create("Signal4_3", Signal.class));
		signalGroups.addSignalGroupData(signalGroup1);
		
		Id<SignalGroup> signalGroupId2 = Id.create("SignalGroup2", SignalGroup.class);
		SignalGroupData signalGroup2 = signalGroups.getFactory()
				.createSignalGroupData(signalSystemId, signalGroupId2);
		signalGroup2.addSignalId(Id.create("Signal7_3", Signal.class));
		signalGroup2.addSignalId(Id.create("Signal8_3", Signal.class));
		signalGroups.addSignalGroupData(signalGroup2);
		
		// create the signal control
		SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
		signalSystemControl.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		signalControl.addSignalSystemControllerData(signalSystemControl);
		
		// create a plan for the signal system (with defined cycle time and offset 0)
		SignalPlanData signalPlan = SignalUtils.createSignalPlan(conFac, 60, 0, Id.create("SignalPlan1", SignalPlan.class));
		signalSystemControl.addSignalPlanData(signalPlan);
		
		// specify signal group settings for both signal groups
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId1, 0, 5));
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId2, 10, 55));
		signalPlan.setOffset(0);
	}

	private static Config defineConfig() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory("output/simpleResponsiveSignalEngineExample/");

		config.controler().setLastIteration(40);
		config.travelTimeCalculator().setMaxTime(3600 * 5);
		config.qsim().setStartTime(0);
		config.qsim().setEndTime(3600 * 5);
        config.qsim().setUsingFastCapacityUpdate(false);
		
		SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		signalConfigGroup.setUseSignalSystems(true);

		ActivityParams dummyAct = new ActivityParams("dummy");
		dummyAct.setTypicalDuration(12 * 3600);
		config.planCalcScore().addActivityParams(dummyAct);

//		{
//			StrategySettings strat = new StrategySettings();
//			strat.setStrategyName(DefaultStrategy.ReRoute.toString());
//			strat.setWeight(0.1);
//			strat.setDisableAfter(config.controler().getLastIteration() - 95);
//			config.strategy().addStrategySettings(strat);
//		}
//		{
//			StrategySettings strat = new StrategySettings();
//			strat.setStrategyName(DefaultSelector.ChangeExpBeta.toString());
//			strat.setWeight(0.9);
//			strat.setDisableAfter(config.controler().getLastIteration());
//			config.strategy().addStrategySettings(strat);
//		}
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultSelector.KeepLastSelected.toString());
			strat.setWeight(0.0);
			strat.setDisableAfter(config.controler().getLastIteration());
			config.strategy().addStrategySettings(strat);
		}

		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists ) ;
		config.controler().setWriteEventsInterval(config.controler().getLastIteration());
		config.controler().setWritePlansInterval(config.controler().getLastIteration());
		config.vspExperimental().setWritingOutputEvents(true);
		config.planCalcScore().setWriteExperiencedPlans(true);
		config.controler().setCreateGraphs(true);

		return config;
	}

}
