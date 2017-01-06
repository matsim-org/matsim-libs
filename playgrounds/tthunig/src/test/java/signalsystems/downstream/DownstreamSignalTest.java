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
import org.jcodec.common.Assert;
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
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
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
import signals.downstreamSensor.DownstreamSignalController;
import signals.downstreamSensor.DownstreamSignalsModule;

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

//		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = analyzerWoBottleneck.getTotalSignalGreenTime();
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleWoBottleneck = analyzerWoBottleneck.calculateAvgSignalGreenTimePerCycle();
//		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = analyzerWoBottleneck.calculateAvgCycleTimePerSignalSystem();
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleWBottleneck = analyzerWBottleneck.calculateAvgSignalGreenTimePerCycle();
		// TODO test these values
		// TODO test value like signalGreenTimePerCycle Map<Id<SignalGruoup>, List<Double>>
		Id<SignalGroup> signalGroup = Id.create("SignalGroup2_3", SignalGroup.class);
		log.info("avg signal green time per cycle: without bottleneck " + avgSignalGreenTimePerCycleWoBottleneck.get(signalGroup)
				+ ", with bottleneck " + avgSignalGreenTimePerCycleWBottleneck.get(signalGroup));
		Assert.assertTrue("Signal green time without bottleneck should be higher", 
				avgSignalGreenTimePerCycleWoBottleneck.get(signalGroup) - avgSignalGreenTimePerCycleWBottleneck.get(signalGroup) > 0);
	}

	private TtSignalAnalysisTool runScenario(ScenarioType scenarioType, double bottleneckCap) {
		Config config = defineConfig();

		Scenario scenario = ScenarioUtils.loadScenario(config);
		// add missing scenario elements
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

		switch (scenarioType) {
		case SingleStream:
			createSingleStreamNetwork(scenario.getNetwork(), bottleneckCap);
			createSingleStreamPopulation(scenario.getPopulation(), Id.createLinkId("1_2"), Id.createLinkId("4_5"));
			createAllDayGreenSignal(scenario, Id.createLinkId("2_3"));
			break;
		case OneDirTwoStreams:
			createOneDirTwoStreamsNetwork(scenario.getNetwork(), bottleneckCap);
			createTwoStreamsPopulation(scenario.getPopulation(), Id.createLinkId("1_2"), Id.createLinkId("6_7"), Id.createLinkId("3_4"), Id.createLinkId("6_7"));
			createAlterntingSignals(scenario, Id.createLinkId("2_5"), Id.createLinkId("4_5"));
			break;
		case TwoDir:
			createTwoDirNetwork(scenario.getNetwork(), bottleneckCap);
			createTwoStreamsPopulation(scenario.getPopulation(), Id.createLinkId("1_2"), Id.createLinkId("6_7"), Id.createLinkId("3_4"), Id.createLinkId("8_9"));
			createAlterntingSignals(scenario, Id.createLinkId("2_5"), Id.createLinkId("4_5"));
			break;
		default:
			break;
		}

		Controler controler = new Controler(scenario);
		// add signal module
		DownstreamSignalsModule signalsModule = new DownstreamSignalsModule();
		controler.addOverridingModule(signalsModule);

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

		config.vspExperimental().setWritingOutputEvents(false);
		config.planCalcScore().setWriteExperiencedPlans(false);
		config.controler().setCreateGraphs(false);

		config.controler().setWriteEventsInterval(config.controler().getLastIteration());
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
		
		String[] links = {"1_2", "2_3", "3_4", "4_5"};
		
		for (String linkId : links){
			String fromNodeId = linkId.split("_")[0];
			String toNodeId = linkId.split("_")[1];
			Link link = fac.createLink(Id.createLinkId(linkId), 
					net.getNodes().get(Id.createNodeId(fromNodeId)), 
					net.getNodes().get(Id.createNodeId(toNodeId)));
			link.setCapacity(3600);
			link.setLength(1000);
			link.setFreespeed(10);
			net.addLink(link);
		}
		// create bottleneck at link 3_4
		net.getLinks().get(Id.createLinkId("3_4")).setCapacity(bottleneckCap);
	}

	private void createOneDirTwoStreamsNetwork(Network net, double bottleneckCap) {
		// TODO Auto-generated method stub

	}

	private void createTwoDirNetwork(Network net, double bottleneckCap) {
		// TODO Auto-generated method stub

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

	private void createTwoStreamsPopulation(Population pop, Id<Link> fromLinkId1, Id<Link> toLinkId1, Id<Link> fromLinkId2, Id<Link> toLinkId2) {
		createSingleStreamPopulation(pop, fromLinkId1, toLinkId1);
		createSingleStreamPopulation(pop, fromLinkId2, toLinkId2);
	}

	private void createAllDayGreenSignal(Scenario scenario, Id<Link> signalLinkId) {
		String toNodeId = signalLinkId.toString().split("_")[1];
		
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalSystemsDataFactory sysFac = signalSystems.getFactory();
//		SignalSystemsDataFactory sysFac = new SignalSystemsDataFactoryImpl();
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalControlData signalControl = signalsData.getSignalControlData();
		SignalControlDataFactory conFac = signalControl.getFactory();
//		SignalControlDataFactory conFac = new SignalControlDataFactoryImpl();
				
		// create signal system
		Id<SignalSystem> signalSystemId = Id.create("SignalSystem" + toNodeId, SignalSystem.class);
		SignalSystemData signalSystem = sysFac.createSignalSystemData(signalSystemId);
		signalSystems.addSignalSystemData(signalSystem);
		
		// create a signal for the given link
		SignalData signal = sysFac.createSignalData(Id.create("Signal" + signalLinkId, Signal.class));
		signalSystem.addSignalData(signal);
		signal.setLinkId(signalLinkId);
		
		// create a group for the signal (one element group)
		Id<SignalGroup> signalGroupId = Id.create("SignalGroup" + signalLinkId, SignalGroup.class);
		SignalGroupData signalGroup = signalGroups.getFactory().createSignalGroupData(signalSystemId, signalGroupId);
		signalGroup.addSignalId(signal.getId());
		signalGroups.addSignalGroupData(signalGroup);
		
		// create the signal control
		SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
		signalSystemControl.setControllerIdentifier(DownstreamSignalController.CONTROLLER_IDENTIFIER);
		
		// create a plan for the signal system (with defined cycle time and offset 0)
		SignalPlanData signalPlan = SignalUtils.createSignalPlan(conFac, 60, 0, Id.create("SignalPlan1", SignalPlan.class));
		signalSystemControl.addSignalPlanData(signalPlan);
		// specify signal group setting for the single signal group
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId, 0, 60));
		signalControl.addSignalSystemControllerData(signalSystemControl);		
	}

	private void createAlterntingSignals(Scenario scenario, Id<Link> signalLinkId1, Id<Link> signalLinkId2) {
		if (signalLinkId1.toString().split("_")[1] != signalLinkId2.toString().split("_")[1]){
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
		SignalData signal2 = sysFac.createSignalData(Id.create("Signal" + signalLinkId2, Signal.class));
		signalSystem.addSignalData(signal2);
		signal2.setLinkId(signalLinkId2);
		
		// create a group for both signals each (one element groups)
		Id<SignalGroup> signalGroupId1 = Id.create("SignalGroup" + signalLinkId1, SignalGroup.class);
		SignalGroupData signalGroup1 = signalGroups.getFactory().createSignalGroupData(signalSystemId, signalGroupId1);
		signalGroup1.addSignalId(signal1.getId());
		signalGroups.addSignalGroupData(signalGroup1);
		Id<SignalGroup> signalGroupId2 = Id.create("SignalGroup" + signalLinkId2, SignalGroup.class);
		SignalGroupData signalGroup2 = signalGroups.getFactory().createSignalGroupData(signalSystemId, signalGroupId2);
		signalGroup2.addSignalId(signal2.getId());
		signalGroups.addSignalGroupData(signalGroup2);
		
		// create the signal control
		SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
		signalSystemControl.setControllerIdentifier(DownstreamSignalController.CONTROLLER_IDENTIFIER);
		
		// create a plan for the signal system (with defined cycle time and offset 0)
		SignalPlanData signalPlan = SignalUtils.createSignalPlan(conFac, 60, 0, Id.create("SignalPlan1", SignalPlan.class));
		signalSystemControl.addSignalPlanData(signalPlan);
		// specify signal group settings for the single element signal groups
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId1, 0, 30));
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId2, 30, 60));
		signalControl.addSignalSystemControllerData(signalSystemControl);
	}

}
