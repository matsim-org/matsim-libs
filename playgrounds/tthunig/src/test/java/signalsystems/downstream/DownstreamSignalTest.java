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
package signalsystems.downstream;

import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
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
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactoryImpl;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
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
import org.matsim.testcases.MatsimTestUtils;

import analysis.signals.TtSignalAnalysisTool;
import signals.CombinedSignalsModule;
import signals.downstreamSensor.DownstreamPlanbasedSignalController;

/**
 * @author tthunig
 *
 */
public class DownstreamSignalTest {

	private static final Logger log = Logger.getLogger(DownstreamSignalTest.class);

	private enum ScenarioType {
		SingleStream, OneDirTwoStreams, TwoDir
	}

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * test downstream signal algo with one single traffic stream: a signal and a bottleneck downstream
	 */
	@Test
	public void testSingleStream() {
		TtSignalAnalysisTool analyzerWoBottleneck = runScenario(ScenarioType.SingleStream, 3600);
		TtSignalAnalysisTool analyzerWBottleneck = runScenario(ScenarioType.SingleStream, 900);

		Map<Id<SignalGroup>, Double> signalGreenTimeRatiosWoBottleneck = analyzerWoBottleneck.calculateSignalGreenTimeRatios();
		Map<Id<SignalGroup>, Double> signalGreenTimeRatiosWBottleneck = analyzerWBottleneck.calculateSignalGreenTimeRatios();

		Id<SignalGroup> signalGroup = Id.create("Signal2_3", SignalGroup.class);
		log.info("signal green time ratio: without bottleneck " + signalGreenTimeRatiosWoBottleneck.get(signalGroup) + ", with bottleneck " + signalGreenTimeRatiosWBottleneck.get(signalGroup));
		Assert.assertTrue("Signal green time ratio without bottleneck should be higher", signalGreenTimeRatiosWoBottleneck.get(signalGroup) - signalGreenTimeRatiosWBottleneck.get(signalGroup) > 0);
		Assert.assertEquals("Signal green time ratio without bottleneck should be 59s in 60s", 59 / 60., signalGreenTimeRatiosWoBottleneck.get(signalGroup), 0.01); // may differ by one percent
		Assert.assertEquals("Signal green time ratio without bottleneck should be around 15s in 60s", 15 / 60., signalGreenTimeRatiosWBottleneck.get(signalGroup), 0.05); // may differ by five percent
	}

	@Test
	public void testOneDirTwoStreams() {
		TtSignalAnalysisTool analyzerWoBottleneck = runScenario(ScenarioType.OneDirTwoStreams, 3600);
		TtSignalAnalysisTool analyzerWBottleneck = runScenario(ScenarioType.OneDirTwoStreams, 900);

		Map<Id<SignalGroup>, Double> signalGreenTimeRatiosWoBottleneck = analyzerWoBottleneck.calculateSignalGreenTimeRatios();
		Map<Id<SignalGroup>, Double> signalGreenTimeRatiosWBottleneck = analyzerWBottleneck.calculateSignalGreenTimeRatios();

		Id<SignalGroup> signalGroupUp = Id.create("Signal2_5", SignalGroup.class);
		Id<SignalGroup> signalGroupDown = Id.create("Signal4_5", SignalGroup.class);
		log.info("signal green time ratios: without bottleneck: up " + signalGreenTimeRatiosWoBottleneck.get(signalGroupUp) + ", down " + signalGreenTimeRatiosWoBottleneck.get(signalGroupDown)
				+ "; with bottleneck: up " + signalGreenTimeRatiosWBottleneck.get(signalGroupUp) + ", down " + signalGreenTimeRatiosWBottleneck.get(signalGroupDown));
		Assert.assertTrue("Signal green time ratio without bottleneck should be higher than with bottleneck",
				signalGreenTimeRatiosWoBottleneck.get(signalGroupUp) - signalGreenTimeRatiosWBottleneck.get(signalGroupUp) > 0);
		Assert.assertTrue("Signal green time ratio without bottleneck should be higher than with bottleneck",
				signalGreenTimeRatiosWoBottleneck.get(signalGroupDown) - signalGreenTimeRatiosWBottleneck.get(signalGroupDown) > 0);
		Assert.assertEquals("Signal green time ratio without bottleneck should be 29s in 60s", 29 / 60., signalGreenTimeRatiosWoBottleneck.get(signalGroupUp), 0.01); // may differ by one percent
		Assert.assertEquals("Signal green time ratio without bottleneck should be 29s in 60s", 29 / 60., signalGreenTimeRatiosWoBottleneck.get(signalGroupDown), 0.01); // may differ by one percent
		Assert.assertEquals("Sum of signal green time ratios of both signals with bottleneck should be around 15s in 60s", 15 / 60.,
				signalGreenTimeRatiosWBottleneck.get(signalGroupUp) + signalGreenTimeRatiosWBottleneck.get(signalGroupDown), 0.05); // may differ by five percent
		Assert.assertEquals("Signal green time ratios of both signals should be more or less equal with bottleneck", signalGreenTimeRatiosWBottleneck.get(signalGroupUp),
				signalGreenTimeRatiosWBottleneck.get(signalGroupDown), 0.05); // may differ by five percent
	}

	@Test
	public void testTwoDir() {
		TtSignalAnalysisTool analyzerWoBottleneck = runScenario(ScenarioType.TwoDir, 3600);
		TtSignalAnalysisTool analyzerWBottleneck = runScenario(ScenarioType.TwoDir, 900);

		Map<Id<SignalGroup>, Double> signalGreenTimeRatiosWoBottleneck = analyzerWoBottleneck.calculateSignalGreenTimeRatios();
		Map<Id<SignalGroup>, Double> signalGreenTimeRatiosWBottleneck = analyzerWBottleneck.calculateSignalGreenTimeRatios();

		Id<SignalGroup> signalGroupWE = Id.create("Signal2_5", SignalGroup.class);
		Id<SignalGroup> signalGroupSN = Id.create("Signal4_5", SignalGroup.class);
		log.info("signal green time ratios: without bottleneck: W-E " + signalGreenTimeRatiosWoBottleneck.get(signalGroupWE) + ", S-N " + signalGreenTimeRatiosWoBottleneck.get(signalGroupSN)
				+ "; with bottleneck: W-E " + signalGreenTimeRatiosWBottleneck.get(signalGroupWE) + ", S-N " + signalGreenTimeRatiosWBottleneck.get(signalGroupSN));
		Assert.assertTrue("Signal green time ratio without bottleneck should be higher than with bottleneck for W-E direction",
				signalGreenTimeRatiosWoBottleneck.get(signalGroupWE) - signalGreenTimeRatiosWBottleneck.get(signalGroupWE) > 0);
		Assert.assertEquals("Signal green time ratios with and without bottleneck should be equal for S-N direction", signalGreenTimeRatiosWoBottleneck.get(signalGroupSN),
				signalGreenTimeRatiosWBottleneck.get(signalGroupSN), 0.01); // may differ by one percent
		// may differ by one percent
		Assert.assertEquals("Signal green time ratio without bottleneck should be 29s in 60s for W-E direction", 29 / 60., signalGreenTimeRatiosWoBottleneck.get(signalGroupWE), 0.01);
		Assert.assertEquals("Signal green time ratio with bottleneck should be around 15s in 60s (one quarter as the bottleneck capacity) for W-E direction", 15 / 60.,
				signalGreenTimeRatiosWBottleneck.get(signalGroupWE), 0.05); // may differ by five percent
		// may differ by one percent
		Assert.assertEquals("Signal green time ratio with and without bottleneck should be 29s in 60s for S-N direction", 29 / 60., signalGreenTimeRatiosWoBottleneck.get(signalGroupSN), 0.01);
	}

	private TtSignalAnalysisTool runScenario(ScenarioType scenarioType, double bottleneckCap) {
		Config config = defineConfig();

		Scenario scenario = ScenarioUtils.loadScenario(config);
		// add missing scenario elements
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

		switch (scenarioType) {
		case SingleStream:
			createSingleStreamNetwork(scenario.getNetwork(), bottleneckCap);
			createSingleStreamPopulation(scenario.getPopulation(), Id.createLinkId("1_2"), Id.createLinkId("4_5"));
			createAlmostAllDayGreenSignal(scenario, Id.createLinkId("2_3"));
			break;
		case OneDirTwoStreams:
			createOneDirTwoStreamsNetwork(scenario.getNetwork(), bottleneckCap);
			createTwoStreamPopulation(scenario.getPopulation(), Id.createLinkId("1_2"), Id.createLinkId("6_7"), Id.createLinkId("3_4"), Id.createLinkId("6_7"));
			createAlterntingSignals(scenario, Id.createLinkId("2_5"), Id.createLinkId("4_5"), Id.createLinkId("5_6"), Id.createLinkId("5_6"));
			break;
		case TwoDir:
			createTwoDirNetwork(scenario.getNetwork(), bottleneckCap);
			createTwoStreamPopulation(scenario.getPopulation(), Id.createLinkId("1_2"), Id.createLinkId("6_7"), Id.createLinkId("3_4"), Id.createLinkId("8_9"));
			createAlterntingSignals(scenario, Id.createLinkId("2_5"), Id.createLinkId("4_5"), Id.createLinkId("5_6"), Id.createLinkId("5_8"));
			break;
		default:
			break;
		}

		Controler controler = new Controler(scenario);
		// add signal module
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

	private Config defineConfig() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory(testUtils.getOutputDirectory());

		// set number of iterations
		config.controler().setLastIteration(0);

		// able or enable signals and lanes
		SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		signalConfigGroup.setUseSignalSystems(true);
		
		config.qsim().setUsingFastCapacityUpdate(false);

		// // set brain exp beta
		// config.planCalcScore().setBrainExpBeta( 2 );

		// // set travelTimeBinSize (only has effect if reRoute is used)
		// config.travelTimeCalculator().setTraveltimeBinSize( 10 );

		// config.travelTimeCalculator().setTravelTimeCalculatorType(TravelTimeCalculatorType.TravelTimeCalculatorHashMap.toString());
		// // hash map and array produce same results. only difference: memory and time.
		// // for small time bins and sparse values hash map is better. theresa, may'15

		// define strategies:
		// {
		// StrategySettings strat = new StrategySettings();
		// strat.setStrategyName(DefaultStrategy.ReRoute.toString());
		// strat.setWeight(0.1);
		// strat.setDisableAfter(config.controler().getLastIteration() - 50);
		// config.strategy().addStrategySettings(strat);
		// }
		// {
		// StrategySettings strat = new StrategySettings();
		// strat.setStrategyName(DefaultSelector.ChangeExpBeta.toString());
		// strat.setWeight(0.9);
		// strat.setDisableAfter(config.controler().getLastIteration());
		// config.strategy().addStrategySettings(strat);
		// }
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultSelector.KeepLastSelected.toString());
			strat.setWeight(0.9);
			strat.setDisableAfter(config.controler().getLastIteration());
			config.strategy().addStrategySettings(strat);
		}

		// choose maximal number of plans per agent. 0 means unlimited
		config.strategy().setMaxAgentPlanMemorySize(1);

		config.qsim().setStuckTime(3600);
		config.qsim().setRemoveStuckVehicles(false);

		config.qsim().setStartTime(0);
		config.qsim().setEndTime(6 * 3600);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.vspExperimental().setWritingOutputEvents(true);
		config.planCalcScore().setWriteExperiencedPlans(false);
		config.controler().setCreateGraphs(false);

		config.controler().setWriteEventsInterval(1);
		config.controler().setWritePlansInterval(config.controler().getLastIteration());

		// define activity types
		{
			ActivityParams dummyAct = new ActivityParams("dummy");
			dummyAct.setTypicalDuration(12 * 3600);
			config.planCalcScore().addActivityParams(dummyAct);
		}

		return config;
	}

	private void createSingleStreamNetwork(Network net, double bottleneckCap) {
		NetworkFactory fac = net.getFactory();

		net.addNode(fac.createNode(Id.createNodeId(1), new Coord(-2000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(2), new Coord(-1000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(3), new Coord(0, 0)));
		net.addNode(fac.createNode(Id.createNodeId(4), new Coord(1000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(5), new Coord(2000, 0)));

		String[] links = { "1_2", "2_3", "3_4", "4_5" };

		for (String linkId : links) {
			String fromNodeId = linkId.split("_")[0];
			String toNodeId = linkId.split("_")[1];
			Link link = fac.createLink(Id.createLinkId(linkId), net.getNodes().get(Id.createNodeId(fromNodeId)), net.getNodes().get(Id.createNodeId(toNodeId)));
			link.setCapacity(3600);
			link.setLength(1000);
			link.setFreespeed(10);
			net.addLink(link);
		}
		// create bottleneck at link 3_4
		net.getLinks().get(Id.createLinkId("3_4")).setCapacity(bottleneckCap);
	}

	private void createOneDirTwoStreamsNetwork(Network net, double bottleneckCap) {
		NetworkFactory fac = net.getFactory();

		net.addNode(fac.createNode(Id.createNodeId(1), new Coord(-2000, 2000)));
		net.addNode(fac.createNode(Id.createNodeId(2), new Coord(-1000, 1000)));
		net.addNode(fac.createNode(Id.createNodeId(3), new Coord(-2000, -2000)));
		net.addNode(fac.createNode(Id.createNodeId(4), new Coord(-1000, -1000)));
		net.addNode(fac.createNode(Id.createNodeId(5), new Coord(0, 0)));
		net.addNode(fac.createNode(Id.createNodeId(6), new Coord(1000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(7), new Coord(2000, 0)));

		String[] links = { "1_2", "2_5", "3_4", "4_5", "5_6", "6_7" };

		for (String linkId : links) {
			String fromNodeId = linkId.split("_")[0];
			String toNodeId = linkId.split("_")[1];
			Link link = fac.createLink(Id.createLinkId(linkId), net.getNodes().get(Id.createNodeId(fromNodeId)), net.getNodes().get(Id.createNodeId(toNodeId)));
			link.setCapacity(3600);
			link.setLength(1000);
			link.setFreespeed(10);
			net.addLink(link);
		}
		// create bottleneck at link 5_6
		net.getLinks().get(Id.createLinkId("5_6")).setCapacity(bottleneckCap);
	}

	private void createTwoDirNetwork(Network net, double bottleneckCap) {
		NetworkFactory fac = net.getFactory();

		net.addNode(fac.createNode(Id.createNodeId(1), new Coord(-2000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(2), new Coord(-1000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(3), new Coord(0, -2000)));
		net.addNode(fac.createNode(Id.createNodeId(4), new Coord(0, -1000)));
		net.addNode(fac.createNode(Id.createNodeId(5), new Coord(0, 0)));
		net.addNode(fac.createNode(Id.createNodeId(6), new Coord(1000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(7), new Coord(2000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(8), new Coord(0, 1000)));
		net.addNode(fac.createNode(Id.createNodeId(9), new Coord(0, 2000)));

		String[] links = { "1_2", "2_5", "3_4", "4_5", "5_6", "6_7", "5_8", "8_9" };

		for (String linkId : links) {
			String fromNodeId = linkId.split("_")[0];
			String toNodeId = linkId.split("_")[1];
			Link link = fac.createLink(Id.createLinkId(linkId), net.getNodes().get(Id.createNodeId(fromNodeId)), net.getNodes().get(Id.createNodeId(toNodeId)));
			link.setCapacity(3600);
			link.setLength(1000);
			link.setFreespeed(10);
			net.addLink(link);
		}
		// create bottleneck at link 5_6
		net.getLinks().get(Id.createLinkId("5_6")).setCapacity(bottleneckCap);
	}

	private void createSingleStreamPopulation(Population pop, Id<Link> fromLinkId, Id<Link> toLinkId) {
		PopulationFactory fac = pop.getFactory();

		for (int i = 0; i < 3600; i++) {
			// create a person
			Person person = fac.createPerson(Id.createPersonId(fromLinkId + "-" + toLinkId + "-" + i));
			pop.addPerson(person);

			// create a plan for the person that contains all this information
			Plan plan = fac.createPlan();
			person.addPlan(plan);

			// create a start activity at the from link
			Activity startAct = fac.createActivityFromLinkId("dummy", fromLinkId);
			// distribute agents uniformly during one hour.
			startAct.setEndTime(i);
			plan.addActivity(startAct);

			// create a dummy leg
			plan.addLeg(fac.createLeg(TransportMode.car));

			// create a drain activity at the to link
			Activity drainAct = fac.createActivityFromLinkId("dummy", toLinkId);
			plan.addActivity(drainAct);
		}
	}

	private void createTwoStreamPopulation(Population pop, Id<Link> fromLinkId1, Id<Link> toLinkId1, Id<Link> fromLinkId2, Id<Link> toLinkId2) {
		createSingleStreamPopulation(pop, fromLinkId1, toLinkId1);
		createSingleStreamPopulation(pop, fromLinkId2, toLinkId2);
	}

	private void createAlmostAllDayGreenSignal(Scenario scenario, Id<Link> signalLinkId) {
		String toNodeId = signalLinkId.toString().split("_")[1];

		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalSystemsDataFactory sysFac = signalSystems.getFactory();
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalControlData signalControl = signalsData.getSignalControlData();
		SignalControlDataFactory conFac = signalControl.getFactory();

		// create signal system
		Id<SignalSystem> signalSystemId = Id.create("SignalSystem" + toNodeId, SignalSystem.class);
		SignalSystemData signalSystem = sysFac.createSignalSystemData(signalSystemId);
		signalSystems.addSignalSystemData(signalSystem);

		// create a signal for the given link
		SignalData signal = sysFac.createSignalData(Id.create("Signal" + signalLinkId, Signal.class));
		signalSystem.addSignalData(signal);
		signal.setLinkId(signalLinkId);

		// create a group for the signal (one element group)
		SignalUtils.createAndAddSignalGroups4Signals(signalGroups, signalSystem);

		// create the signal control
		SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
		signalSystemControl.setControllerIdentifier(DownstreamPlanbasedSignalController.IDENTIFIER);

		// create a plan for the signal system (with defined cycle time and offset 0)
		SignalPlanData signalPlan = SignalUtils.createSignalPlan(conFac, 60, 0, Id.create("SignalPlan1", SignalPlan.class));
		signalSystemControl.addSignalPlanData(signalPlan);
		// specify signal group setting for the single signal group
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, Id.create(signal.getId(), SignalGroup.class), 0, 59));
		signalControl.addSignalSystemControllerData(signalSystemControl);
	}

	private void createAlterntingSignals(Scenario scenario, Id<Link> signalLinkId1, Id<Link> signalLinkId2, Id<Link> signalToLinkId1, Id<Link> signalToLinkId2) {
		if (!signalLinkId1.toString().split("_")[1].equals(signalLinkId2.toString().split("_")[1])) {
			throw new IllegalArgumentException("the two signal links should lead to the same node");
		}
		String toNodeId = signalLinkId1.toString().split("_")[1];

		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalSystemsDataFactory sysFac = new SignalSystemsDataFactoryImpl();
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalControlData signalControl = signalsData.getSignalControlData();
		SignalControlDataFactory conFac = new SignalControlDataFactoryImpl();

		// create signal system
		Id<SignalSystem> signalSystemId = Id.create("SignalSystem" + toNodeId, SignalSystem.class);
		SignalSystemData signalSystem = sysFac.createSignalSystemData(signalSystemId);
		signalSystems.addSignalSystemData(signalSystem);

		// create a signal for the given links
		SignalData signal1 = sysFac.createSignalData(Id.create("Signal" + signalLinkId1, Signal.class));
		signalSystem.addSignalData(signal1);
		signal1.setLinkId(signalLinkId1);
		signal1.addTurningMoveRestriction(signalToLinkId1);
		SignalData signal2 = sysFac.createSignalData(Id.create("Signal" + signalLinkId2, Signal.class));
		signalSystem.addSignalData(signal2);
		signal2.setLinkId(signalLinkId2);
		signal2.addTurningMoveRestriction(signalToLinkId2);

		// create a group for both signals each (one element groups)
		SignalUtils.createAndAddSignalGroups4Signals(signalGroups, signalSystem);

		// create the signal control
		SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
		signalSystemControl.setControllerIdentifier(DownstreamPlanbasedSignalController.IDENTIFIER);

		// create a plan for the signal system (with defined cycle time and offset 0)
		SignalPlanData signalPlan = SignalUtils.createSignalPlan(conFac, 60, 0, Id.create("SignalPlan1", SignalPlan.class));
		signalSystemControl.addSignalPlanData(signalPlan);
		// specify signal group settings for the single element signal groups
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, Id.create(signal1.getId(), SignalGroup.class), 0, 29));
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, Id.create(signal2.getId(), SignalGroup.class), 30, 59));
		signalControl.addSignalSystemControllerData(signalSystemControl);
	}

}
