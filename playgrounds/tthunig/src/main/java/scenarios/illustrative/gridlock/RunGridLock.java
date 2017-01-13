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
package scenarios.illustrative.gridlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.analysis.SignalEvents2ViaCSVWriter;
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
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
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
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.LanesUtils;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;
import org.matsim.lanes.data.LanesFactory;
import org.matsim.lanes.data.LanesToLinkAssignment;

import analysis.signals.TtSignalAnalysisListener;
import analysis.signals.TtSignalAnalysisTool;
import analysis.signals.TtSignalAnalysisWriter;
import signals.downstreamSensor.DownstreamSignalController;
import signals.downstreamSensor.DownstreamSignalsModule;

/**
 * @author tthunig
 *
 */
public class RunGridLock {
	
	// TODO add SYLVIA
	private enum SignalType { PLANBASED, DOWNSTREAM};
	private static final SignalType SIGNALTYPE = SignalType.PLANBASED;
	
	// TODO try around which effect number of lanes has (1800 or 3600 needed here?)
	private static final double middleLinkCap = 1800;

	public static void main(String[] args) {
		Config config = defineConfig();

		Scenario scenario = ScenarioUtils.loadScenario(config);
		// add missing scenario elements
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		
		createGridLockNetworkAndLanes(scenario.getNetwork(), scenario.getLanes(), middleLinkCap);
		createTwoStreamPopulation(scenario.getPopulation(), Id.createLinkId("0_1"), Id.createLinkId("2_1"), Id.createLinkId("5_4"), Id.createLinkId("3_4"), true);
		createGridlockSignals(scenario);
		
		Controler controler = new Controler(scenario);
		// add signal module (also works for planbased signals)
		controler.addOverridingModule(new DownstreamSignalsModule());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				// TODO add agent analysis: number of cars on link per time, departed agents, arrived agents, 
				
				// bind tool to analyze signals
				this.bind(TtSignalAnalysisTool.class).asEagerSingleton();
				this.addEventHandlerBinding().to(TtSignalAnalysisTool.class);
				this.addControlerListenerBinding().to(TtSignalAnalysisTool.class);
				this.bind(TtSignalAnalysisWriter.class);
				this.addControlerListenerBinding().to(TtSignalAnalysisListener.class);
			}
		});

		controler.run();
	}
	
	private static Config defineConfig() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory("../../../runs-svn/gridlock/twoStream/"+SIGNALTYPE+middleLinkCap+"/");

		// set number of iterations
		config.controler().setLastIteration(0);

		// able or enable signals and lanes
		SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		signalConfigGroup.setUseSignalSystems(true);
		config.qsim().setUseLanes(true);

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
		config.planCalcScore().setWriteExperiencedPlans(true);
		config.controler().setCreateGraphs(true);

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

	private static void createGridLockNetworkAndLanes(Network net, Lanes lanes, double bottleneckCap) {
		NetworkFactory netFac = net.getFactory();

		net.addNode(netFac.createNode(Id.createNodeId(0), new Coord(-3000, 0)));
		net.addNode(netFac.createNode(Id.createNodeId(1), new Coord(-2000, 0)));
		net.addNode(netFac.createNode(Id.createNodeId(2), new Coord(-1000, 0)));
		net.addNode(netFac.createNode(Id.createNodeId(3), new Coord(0, 0)));
		net.addNode(netFac.createNode(Id.createNodeId(4), new Coord(1000, 0)));
		net.addNode(netFac.createNode(Id.createNodeId(5), new Coord(2000, 0)));

		String[] links = { "0_1", "1_2", "2_1", "2_3", "3_2", "3_4", "4_3", "5_4" };

		for (String linkId : links) {
			String fromNodeId = linkId.split("_")[0];
			String toNodeId = linkId.split("_")[1];
			Link link = netFac.createLink(Id.createLinkId(linkId), net.getNodes().get(Id.createNodeId(fromNodeId)), net.getNodes().get(Id.createNodeId(toNodeId)));
			link.setCapacity(3600);
			link.setLength(1000);
			link.setFreespeed(10);
			net.addLink(link);
		}
		// create bottleneck at link 2_3 and 3_2
		net.getLinks().get(Id.createLinkId("2_3")).setCapacity(bottleneckCap);
		net.getLinks().get(Id.createLinkId("3_2")).setCapacity(bottleneckCap);

		LanesFactory lanesFac = lanes.getFactory();

		// create link assignment of link 2_3
		LanesToLinkAssignment linkAssignment2_3 = lanesFac.createLanesToLinkAssignment(Id.createLinkId("2_3"));
		LanesUtils.createAndAddLane(linkAssignment2_3, lanesFac, Id.create("2_3.ol", Lane.class), bottleneckCap, 1000, 0, 2, null,
				Arrays.asList(Id.create("2_3.l", Lane.class), Id.create("2_3.s", Lane.class)));
		LanesUtils.createAndAddLane(linkAssignment2_3, lanesFac, Id.create("2_3.l", Lane.class), bottleneckCap, 500, 1, 1, Collections.singletonList(Id.createLinkId("3_2")), null);
		LanesUtils.createAndAddLane(linkAssignment2_3, lanesFac, Id.create("2_3.s", Lane.class), bottleneckCap, 500, 0, 1, Collections.singletonList(Id.createLinkId("3_4")), null);
		lanes.addLanesToLinkAssignment(linkAssignment2_3);

		// create link assignment of link 3_2
		LanesToLinkAssignment linkAssignment3_2 = lanesFac.createLanesToLinkAssignment(Id.createLinkId("3_2"));
		LanesUtils.createAndAddLane(linkAssignment3_2, lanesFac, Id.create("3_2.ol", Lane.class), bottleneckCap, 1000, 0, 2, null,
				Arrays.asList(Id.create("3_2.l", Lane.class), Id.create("3_2.s", Lane.class)));
		LanesUtils.createAndAddLane(linkAssignment3_2, lanesFac, Id.create("3_2.l", Lane.class), bottleneckCap, 500, 1, 1, Collections.singletonList(Id.createLinkId("2_3")), null);
		LanesUtils.createAndAddLane(linkAssignment3_2, lanesFac, Id.create("3_2.s", Lane.class), bottleneckCap, 500, 0, 1, Collections.singletonList(Id.createLinkId("2_1")), null);
		lanes.addLanesToLinkAssignment(linkAssignment3_2);
	}
	
	private static void createSingleStreamPopulation(Population pop, Id<Link> fromLinkId, Id<Link> toLinkId, boolean initRoutes) {
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
			Leg leg = fac.createLeg(TransportMode.car);
			if (initRoutes) {
				// create routes for the agents. otherwise the router will not directly find the right one's because it is not sensitive for turning restrictions of signals
				List<Id<Link>> path = new ArrayList<>();
				if (fromLinkId.equals(Id.createLinkId("0_1"))) {
					path.add(Id.createLinkId("1_2"));
					path.add(Id.createLinkId("2_3"));
					path.add(Id.createLinkId("3_2"));
				} else { // fromLinkId equals 5_4
					path.add(Id.createLinkId("4_3"));
					path.add(Id.createLinkId("3_2"));
					path.add(Id.createLinkId("2_3"));
				}
				leg.setRoute(new LinkNetworkRouteImpl(fromLinkId, path, toLinkId));
			}
			plan.addLeg(leg);

			// create a drain activity at the to link
			Activity drainAct = fac.createActivityFromLinkId("dummy", toLinkId);
			plan.addActivity(drainAct);
		}
	}

	private static void createTwoStreamPopulation(Population pop, Id<Link> fromLinkId1, Id<Link> toLinkId1, Id<Link> fromLinkId2, Id<Link> toLinkId2, boolean initRoutes) {
		createSingleStreamPopulation(pop, fromLinkId1, toLinkId1, initRoutes);
		createSingleStreamPopulation(pop, fromLinkId2, toLinkId2, initRoutes);
	}
	
	private static void createGridlockSignals(Scenario scenario) {
		createUTurnNoTurnSystem(scenario, Id.createNodeId(2), Id.createLinkId("1_2"), Id.createLinkId("2_3"), Id.createLinkId("3_2"), Id.createLinkId("2_1"));
		createUTurnNoTurnSystem(scenario, Id.createNodeId(3), Id.createLinkId("4_3"), Id.createLinkId("3_2"), Id.createLinkId("2_3"), Id.createLinkId("3_4"));
	}

	private static void createUTurnNoTurnSystem(Scenario scenario, Id<Node> systemNodeId, Id<Link> incomminigLinkId, Id<Link> incommingToLinkId, Id<Link> TwoLaneLinkId, Id<Link> outgoingLinkId) {
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalSystemsDataFactory sysFac = new SignalSystemsDataFactoryImpl();
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalControlData signalControl = signalsData.getSignalControlData();
		SignalControlDataFactory conFac = new SignalControlDataFactoryImpl();

		// create signal system
		Id<SignalSystem> signalSystemId = Id.create("SignalSystem" + systemNodeId, SignalSystem.class);
		SignalSystemData signalSystem = sysFac.createSignalSystemData(signalSystemId);
		signalSystems.addSignalSystemData(signalSystem);

		// create three signals for the system
		SignalData signalIncomming = sysFac.createSignalData(Id.create("Signal" + incomminigLinkId, Signal.class));
		signalSystem.addSignalData(signalIncomming);
		signalIncomming.setLinkId(incomminigLinkId);
		signalIncomming.addTurningMoveRestriction(incommingToLinkId);
		SignalData signalOutgoing = sysFac.createSignalData(Id.create("Signal" + TwoLaneLinkId + ".s", Signal.class));
		signalSystem.addSignalData(signalOutgoing);
		signalOutgoing.setLinkId(TwoLaneLinkId);
		signalOutgoing.addLaneId(Id.create(TwoLaneLinkId + ".s", Lane.class));
		signalOutgoing.addTurningMoveRestriction(outgoingLinkId);
		SignalData signalUTurn = sysFac.createSignalData(Id.create("Signal" + TwoLaneLinkId + ".l", Signal.class));
		signalSystem.addSignalData(signalUTurn);
		signalUTurn.setLinkId(TwoLaneLinkId);
		signalUTurn.addLaneId(Id.create(TwoLaneLinkId + ".l", Lane.class));
		signalUTurn.addTurningMoveRestriction(incommingToLinkId);

		// create a group for all signals each (one element groups)
		SignalUtils.createAndAddSignalGroups4Signals(signalGroups, signalSystem);

		// create the signal control - almost all day green
		SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
		switch (SIGNALTYPE){
		case DOWNSTREAM:
			signalSystemControl.setControllerIdentifier(DownstreamSignalController.IDENTIFIER);
			break;
		case PLANBASED:
			signalSystemControl.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			break;
		default:
			break;
		}

		// create a plan for the signal system (with defined cycle time and offset 0)
		SignalPlanData signalPlan = SignalUtils.createSignalPlan(conFac, 60, 0, Id.create("SignalPlan1", SignalPlan.class));
		signalSystemControl.addSignalPlanData(signalPlan);
		// specify signal group settings for the single element signal groups
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, Id.create(signalIncomming.getId(), SignalGroup.class), 0, 59));
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, Id.create(signalOutgoing.getId(), SignalGroup.class), 0, 59));
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, Id.create(signalUTurn.getId(), SignalGroup.class), 0, 59));
		signalControl.addSignalSystemControllerData(signalSystemControl);
	}
	
}
