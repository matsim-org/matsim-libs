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
package signalsystems.gershenson;

import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
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
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import analysis.signals.TtSignalAnalysisTool;
import signals.CombinedSignalsModule;
import signals.gershenson.DgRoederGershensonSignalController;

/**
 * Test gershenson logic at an intersection with four incoming links and one signal each. No lanes are used.
 * 
 * Copied from SylviaTest.
 * 
 * @author tthunig
 *
 */
public class GershensonIT {

	private static final Logger log = Logger.getLogger(GershensonIT.class);

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * conflicting streams
	 */
	@Test
	public void testDemandAB() {
		double[] noPersons = { 3600, 3600 };
		TtSignalAnalysisTool signalAnalyzer = runScenario(noPersons);

		// check signal results
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); // should be more or less equal (OW direction is always favored as the first phase)
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle(); // should be more or less equal and around 25
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem(); // should be 60
		Id<SignalGroup> signalGroupId1 = Id.create("SignalGroup1", SignalGroup.class);
		Id<SignalGroup> signalGroupId2 = Id.create("SignalGroup2", SignalGroup.class);
		Id<SignalSystem> signalSystemId = Id.create("SignalSystem1", SignalSystem.class);

		log.info("total signal green times: " + totalSignalGreenTimes.get(signalGroupId1) + ", " + totalSignalGreenTimes.get(signalGroupId2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(signalGroupId1) + ", " + avgSignalGreenTimePerCycle.get(signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(signalSystemId));
//		Assert.assertEquals("total signal green times of both groups are not similiar enough", 0.0, totalSignalGreenTimes.get(signalGroupId1) - totalSignalGreenTimes.get(signalGroupId2),
//				totalSignalGreenTimes.get(signalGroupId1) / 3); // may differ by 1/3
//		Assert.assertEquals("avg green time per cycle of signal group 1 is wrong", 25, avgSignalGreenTimePerCycle.get(signalGroupId1), 5);
//		Assert.assertEquals("avg green time per cycle of signal group 2 is wrong", 25, avgSignalGreenTimePerCycle.get(signalGroupId2), 5);
//		Assert.assertEquals("avg cycle time of the system is wrong", 60, avgCycleTimePerSystem.get(signalSystemId), MatsimTestUtils.EPSILON);
	}

	/**
	 * demand crossing only in east-west direction
	 */
	@Test
	public void testDemandA() {
		double[] noPersons = { 3600, 0 };
		TtSignalAnalysisTool signalAnalyzer = runScenario(noPersons);

		// check signal results
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); // group 1 should have more total green time than group 2
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem();
		Id<SignalGroup> signalGroupId1 = Id.create("SignalGroup1", SignalGroup.class);
		Id<SignalGroup> signalGroupId2 = Id.create("SignalGroup2", SignalGroup.class);
		Id<SignalSystem> signalSystemId = Id.create("SignalSystem1", SignalSystem.class);

		log.info("total signal green times: " + totalSignalGreenTimes.get(signalGroupId1) + ", " + totalSignalGreenTimes.get(signalGroupId2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(signalGroupId1) + ", " + avgSignalGreenTimePerCycle.get(signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(signalSystemId));
		
		// TODO does not seem to be plausible. but what is plausible for gershenson?
//		Assert.assertTrue("total signal green time of group 1 is not bigger than of group 2", totalSignalGreenTimes.get(signalGroupId1) > totalSignalGreenTimes.get(signalGroupId2));
//		Assert.assertEquals("avg green time per cycle of signal group 1 is wrong", 45, avgSignalGreenTimePerCycle.get(signalGroupId1), 5);
//		Assert.assertEquals("avg green time per cycle of signal group 2 is wrong", 5, avgSignalGreenTimePerCycle.get(signalGroupId2), 5);
//		Assert.assertEquals("avg cycle time of the system is wrong", 60, avgCycleTimePerSystem.get(signalSystemId), MatsimTestUtils.EPSILON);
	}

	private TtSignalAnalysisTool runScenario(double[] noPersons) {
		Config config = defineConfig();

		Scenario scenario = ScenarioUtils.loadScenario(config);
		// add missing scenario elements
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

		createScenarioElements(scenario, noPersons);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new CombinedSignalsModule());

		// add signal analysis tool
		TtSignalAnalysisTool signalAnalyzer = new TtSignalAnalysisTool();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(signalAnalyzer);
				this.addControlerListenerBinding().toInstance(signalAnalyzer);
			}
		});

		controler.run();

		return signalAnalyzer;
	}

	private void createScenarioElements(Scenario scenario, double[] noPersons) {
		createNetwork(scenario.getNetwork());
		createPopulation(scenario.getPopulation(), noPersons);
		createSignals(scenario);
	}

	/**
	 * creates a network like this:
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
	 * @param net
	 *            the object where the network should be stored
	 */
	private static void createNetwork(Network net) {
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

		String[] links = { "1_2", "2_1", "2_3", "3_2", "3_4", "4_3", "4_5", "5_4", "6_7", "7_6", "7_3", "3_7", "3_8", "8_3", "8_9", "9_8" };

		for (String linkId : links) {
			String fromNodeId = linkId.split("_")[0];
			String toNodeId = linkId.split("_")[1];
			Link link = fac.createLink(Id.createLinkId(linkId), net.getNodes().get(Id.createNodeId(fromNodeId)), net.getNodes().get(Id.createNodeId(toNodeId)));
			link.setCapacity(7200);
			link.setLength(1000);
			link.setFreespeed(10);
			net.addLink(link);
		}
	}

	private static void createPopulation(Population population, double[] noPersons) {
		String[] odRelations = { "1_2-4_5", "6_7-8_9" };
		int odIndex = 0;

		for (String od : odRelations) {
			String fromLinkId = od.split("-")[0];
			String toLinkId = od.split("-")[1];

			for (int i = 0; i < noPersons[odIndex]; i++) {
				// create a person
				Person person = population.getFactory().createPerson(Id.createPersonId(od + "-" + i));
				population.addPerson(person);

				// create a plan for the person that contains all this information
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
			odIndex++;
		}
	}

	private void createSignals(Scenario scenario) {
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalSystemsDataFactory sysFac = signalSystems.getFactory();
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalControlData signalControl = signalsData.getSignalControlData();
		SignalControlDataFactory conFac = signalControl.getFactory();

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
		SignalGroupData signalGroup1 = signalGroups.getFactory().createSignalGroupData(signalSystemId, signalGroupId1);
		signalGroup1.addSignalId(Id.create("Signal2_3", Signal.class));
		signalGroup1.addSignalId(Id.create("Signal4_3", Signal.class));
		signalGroups.addSignalGroupData(signalGroup1);

		Id<SignalGroup> signalGroupId2 = Id.create("SignalGroup2", SignalGroup.class);
		SignalGroupData signalGroup2 = signalGroups.getFactory().createSignalGroupData(signalSystemId, signalGroupId2);
		signalGroup2.addSignalId(Id.create("Signal7_3", Signal.class));
		signalGroup2.addSignalId(Id.create("Signal8_3", Signal.class));
		signalGroups.addSignalGroupData(signalGroup2);

		// create the signal control
		SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
		signalSystemControl.setControllerIdentifier(DgRoederGershensonSignalController.IDENTIFIER);
		signalControl.addSignalSystemControllerData(signalSystemControl);

		// no plan is needed for GershensonController
//		// create a plan for the signal system (with defined cycle time and offset 0)
//		SignalPlanData signalPlan = SignalUtils.createSignalPlan(conFac, 60, 0, Id.create("SignalPlan1", SignalPlan.class));
//		signalSystemControl.addSignalPlanData(signalPlan);
//
//		// specify signal group settings for both signal groups
//		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId1, 0, 5));
//		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId2, 10, 55));
//		signalPlan.setOffset(0);
	}

	private Config defineConfig() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory(testUtils.getOutputDirectory());

		// set number of iterations
		config.controler().setLastIteration(0);

		// able or enable signals and lanes
		SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		signalConfigGroup.setUseSignalSystems(true);
		
		config.qsim().setUsingFastCapacityUpdate(false);

		// define strategies:
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultSelector.KeepLastSelected.toString());
			strat.setWeight(0.9);
			strat.setDisableAfter(config.controler().getLastIteration());
			config.strategy().addStrategySettings(strat);
		}

		config.qsim().setStuckTime(3600);
		config.qsim().setRemoveStuckVehicles(false);

		config.qsim().setStartTime(0);
		config.qsim().setEndTime(3 * 3600);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.vspExperimental().setWritingOutputEvents(false);
		config.planCalcScore().setWriteExperiencedPlans(false);
		config.controler().setCreateGraphs(false);

		// define activity types
		{
			ActivityParams dummyAct = new ActivityParams("dummy");
			dummyAct.setTypicalDuration(12 * 3600);
			config.planCalcScore().addActivityParams(dummyAct);
		}

		return config;
	}

}
