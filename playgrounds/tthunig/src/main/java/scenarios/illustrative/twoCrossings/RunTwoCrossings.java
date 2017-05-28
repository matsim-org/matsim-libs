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
package scenarios.illustrative.twoCrossings;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactoryImpl;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataImpl;
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
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import analysis.signals.TtSignalAnalysisListener;
import analysis.signals.TtSignalAnalysisTool;
import analysis.signals.TtSignalAnalysisWriter;
import signals.CombinedSignalsModule;
import signals.laemmer.model.LaemmerConfig;
import signals.laemmer.model.LaemmerSignalController;
import signals.sylvia.controler.DgSylviaConfig;
import signals.sylvia.data.DgSylviaPreprocessData;

/**
 * @author tthunig
 *
 */
public class RunTwoCrossings {

	private static final Logger log = Logger.getLogger(RunTwoCrossings.class);
	
	private enum SignalType { NONE, PLANBASED, SYLVIA, LAEMMER};
	private static final SignalType SIGNALTYPE = SignalType.LAEMMER;

	public static void main(String[] args) {
		Config config = defineConfig();
		
		OTFVisConfigGroup otfvisConfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.class ) ;
		otfvisConfig.setDrawTime(true);
		// make links visible beyond screen edge
		otfvisConfig.setScaleQuadTreeRect(true);
		config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		createTwoCrossingsNetwork(scenario.getNetwork());
		createSingleStreamPopulation(scenario.getPopulation(), 1, Id.createLinkId("0_1"), Id.createLinkId("4_5"));
		createSingleStreamPopulation(scenario.getPopulation(), 10, Id.createLinkId("10_6"), Id.createLinkId("8_12"));
		createSingleStreamPopulation(scenario.getPopulation(), 10, Id.createLinkId("11_7"), Id.createLinkId("9_13"));
		if (!SIGNALTYPE.equals(SignalType.NONE)){
			createTwoCrossingSignals(scenario);
		}
		
		Controler controler = new Controler(scenario);
		controler.addOverridingModule( new OTFVisWithSignalsLiveModule() ) ;
		if (!SIGNALTYPE.equals(SignalType.NONE)) {
			// add signal module
			CombinedSignalsModule signalsModule = new CombinedSignalsModule();
			DgSylviaConfig sylviaConfig = new DgSylviaConfig();
//			sylviaConfig.setUseFixedTimeCycleAsMaximalExtension(false);
//			sylviaConfig.setSignalGroupMaxGreenScale(2);
			signalsModule.setSylviaConfig(sylviaConfig);
			
			LaemmerConfig laemmerConfig = new LaemmerConfig();
	        laemmerConfig.setDEFAULT_INTERGREEN(5);
	        laemmerConfig.setDESIRED_PERIOD(60);
	        laemmerConfig.setMAX_PERIOD(90);
	        signalsModule.setLaemmerConfig(laemmerConfig);
	        LaemmerSignalController.log.setLevel(Level.OFF);
	        LaemmerSignalController.signalLog.setLevel(Level.OFF);
			
			controler.addOverridingModule(signalsModule);

			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					// bind tool to analyze signals
					this.bind(TtSignalAnalysisTool.class).asEagerSingleton();
					this.addEventHandlerBinding().to(TtSignalAnalysisTool.class);
					this.addControlerListenerBinding().to(TtSignalAnalysisTool.class);
					this.bind(TtSignalAnalysisWriter.class);
					this.addControlerListenerBinding().to(TtSignalAnalysisListener.class);
				}
			});
		}

		controler.run();
	}
	
	private static Config defineConfig() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory("../../../runs-svn/twoCrossings/"+SIGNALTYPE+"/");

		// set number of iterations
		config.controler().setLastIteration(0);

		// able or enable signals
		if (!SIGNALTYPE.equals(SignalType.NONE)) {
			SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
			signalConfigGroup.setUseSignalSystems(true);
		}

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
		
		config.qsim().setUsingFastCapacityUpdate(false);

		config.qsim().setStartTime(0);
		config.qsim().setEndTime(4 * 3600);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.vspExperimental().setWritingOutputEvents(true);
		config.controler().setCreateGraphs(true);

//		config.controler().setWriteEventsInterval(1);
//		config.controler().setWritePlansInterval(1);

		// define activity types
		{
			ActivityParams dummyAct = new ActivityParams("dummy");
			dummyAct.setTypicalDuration(12 * 3600);
			config.planCalcScore().addActivityParams(dummyAct);
		}

		return config;
	}

	/**
	 *             (10)  (11)
	 *              |     |
	 *             (6)   (7)
	 *              |     |
	 * (0)---(1)---[2]---[3]---(4)---(5)
	 *      ^       |     |         ^
	 *             (8)   (9)
	 *              |     |
	 *             (12)  (13)
	 *      
	 * demand travels from ^ to ^
	 */
	private static void createTwoCrossingsNetwork(Network net) {
		NetworkFactory netFac = net.getFactory();
		
		int linkLength = 150;

		net.addNode(netFac.createNode(Id.createNodeId(0), new Coord(-3*linkLength, 0)));
		net.addNode(netFac.createNode(Id.createNodeId(1), new Coord(-2*linkLength, 0)));
		net.addNode(netFac.createNode(Id.createNodeId(2), new Coord(-linkLength, 0)));
		net.addNode(netFac.createNode(Id.createNodeId(3), new Coord(0, 0)));
		net.addNode(netFac.createNode(Id.createNodeId(4), new Coord(linkLength, 0)));
		net.addNode(netFac.createNode(Id.createNodeId(5), new Coord(2*linkLength, 0)));
		net.addNode(netFac.createNode(Id.createNodeId(6), new Coord(-linkLength, linkLength)));
		net.addNode(netFac.createNode(Id.createNodeId(8), new Coord(-linkLength, -linkLength)));
		net.addNode(netFac.createNode(Id.createNodeId(7), new Coord(0, linkLength)));
		net.addNode(netFac.createNode(Id.createNodeId(9), new Coord(0, -linkLength)));
		net.addNode(netFac.createNode(Id.createNodeId(10), new Coord(-linkLength, 2*linkLength)));
		net.addNode(netFac.createNode(Id.createNodeId(11), new Coord(0, 2*linkLength)));
		net.addNode(netFac.createNode(Id.createNodeId(12), new Coord(-linkLength, -2*linkLength)));
		net.addNode(netFac.createNode(Id.createNodeId(13), new Coord(0, -2*linkLength)));

		String[] links = { "0_1", "1_2", "2_3", "3_4", "4_5",
				"6_2", "7_3", "2_8", "3_9", "10_6", "11_7", "8_12", "9_13"};

		for (String linkId : links) {
			String fromNodeId = linkId.split("_")[0];
			String toNodeId = linkId.split("_")[1];
			Link link = netFac.createLink(Id.createLinkId(linkId), net.getNodes().get(Id.createNodeId(fromNodeId)), net.getNodes().get(Id.createNodeId(toNodeId)));
			link.setCapacity(3600);
			link.setLength(linkLength);
			link.setFreespeed(15);
			net.addLink(link);
		}
	}
	
	private static void createSingleStreamPopulation(Population pop, int secPerVeh, Id<Link> fromLinkId, Id<Link> toLinkId) {
		PopulationFactory fac = pop.getFactory();
		
		int simEndTime = 3600;
		int currentActEnd = 0;
		
		for (int i = 0; currentActEnd < simEndTime; i++) {
			// create a person (the i-th person)
			Person person = fac.createPerson(Id.createPersonId(fromLinkId + "-" + toLinkId + "-" + i));
			pop.addPerson(person);

			// create a plan for the person that contains all this information
			Plan plan = fac.createPlan();
			person.addPlan(plan);

			// create a start activity at the from link
			Activity startAct = fac.createActivityFromLinkId("dummy", fromLinkId);
			startAct.setEndTime(currentActEnd);
			plan.addActivity(startAct);
			// one agent every x seconds
			currentActEnd+= secPerVeh;
			
			// create a dummy leg
			Leg leg = fac.createLeg(TransportMode.car);
			List<Id<Link>> path = new ArrayList<>();
			if (fromLinkId.equals(Id.createLinkId("0_1"))) {
				// OD relation equals WO stream
				path.add(Id.createLinkId("1_2"));
				path.add(Id.createLinkId("2_3"));
				path.add(Id.createLinkId("3_4"));
			} else if (fromLinkId.equals(Id.createLinkId("10_6"))){
				// OD relation equals left NS stream
				path.add(Id.createLinkId("6_2"));
				path.add(Id.createLinkId("2_8"));
			} else if (fromLinkId.equals(Id.createLinkId("11_7"))){
				// OD relation equals right NS stream
				path.add(Id.createLinkId("7_3"));
				path.add(Id.createLinkId("3_9"));
			}
			leg.setRoute(new LinkNetworkRouteImpl(fromLinkId, path, toLinkId));
			plan.addLeg(leg);
			
			// create a drain activity at the to link
			Activity drainAct = fac.createActivityFromLinkId("dummy", toLinkId);
			plan.addActivity(drainAct);
		}
	}

	private static void createTwoCrossingSignals(Scenario scenario) {
		// add missing signals scenario element
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(scenario.getConfig()).loadSignalsData());
		
		createSingleCrossingSystem(scenario, 2);
		createSingleCrossingSystem(scenario, 3);
	}

	private static void createSingleCrossingSystem(Scenario scenario, int systemNodeNr) {
		Id<Link> incomingLinkIdWest = Id.createLinkId(systemNodeNr-1 + "_" + systemNodeNr);
		Id<Link> outgoingLinkIdEast = Id.createLinkId(systemNodeNr + "_" + (systemNodeNr+1));
		Id<Link> incomingLinkIdNorth = Id.createLinkId(systemNodeNr+4 + "_" + systemNodeNr);
		Id<Link> outgoingLinkIdSouth = Id.createLinkId(systemNodeNr + "_" + (systemNodeNr+6));
		
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalSystemsDataFactory sysFac = new SignalSystemsDataFactoryImpl();
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalControlData signalControl = signalsData.getSignalControlData();
		SignalControlDataFactory conFac = new SignalControlDataFactoryImpl();

		// create a temporary, empty signal control object needed for sylvia
		SignalControlData tmpSignalControl = new SignalControlDataImpl();
		
		// create signal system
		Id<SignalSystem> signalSystemId = Id.create("SignalSystem" + systemNodeNr, SignalSystem.class);
		SignalSystemData signalSystem = sysFac.createSignalSystemData(signalSystemId);
		signalSystems.addSignalSystemData(signalSystem);

		// create two signals for the system
		SignalData signalWest = sysFac.createSignalData(Id.create("Signal" + incomingLinkIdWest, Signal.class));
		signalSystem.addSignalData(signalWest);
		signalWest.setLinkId(incomingLinkIdWest);
		signalWest.addTurningMoveRestriction(outgoingLinkIdEast);
		SignalData signalNorth = sysFac.createSignalData(Id.create("Signal" + incomingLinkIdNorth + ".s", Signal.class));
		signalSystem.addSignalData(signalNorth);
		signalNorth.setLinkId(incomingLinkIdNorth);
		signalNorth.addTurningMoveRestriction(outgoingLinkIdSouth);

		// create a group for all signals each (one element groups)
		SignalUtils.createAndAddSignalGroups4Signals(signalGroups, signalSystem);

		// create the signal control
		SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
		// create a plan for the signal system (with defined cycle time and offset 0)
		SignalPlanData signalPlan = SignalUtils.createSignalPlan(conFac, 60, 0, Id.create("SignalPlan1", SignalPlan.class));
		signalSystemControl.addSignalPlanData(signalPlan);
		// specify signal group settings for the single element signal groups
		switch (SIGNALTYPE){
		case PLANBASED:
			signalSystemControl.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, Id.create(signalWest.getId(), SignalGroup.class), 0, 25));
			signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, Id.create(signalNorth.getId(), SignalGroup.class), 30, 55));	
			signalControl.addSignalSystemControllerData(signalSystemControl);
			break;
		case SYLVIA:
			signalSystemControl.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, Id.create(signalWest.getId(), SignalGroup.class), 0, 25));
			signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, Id.create(signalNorth.getId(), SignalGroup.class), 30, 55));	
			tmpSignalControl.addSignalSystemControllerData(signalSystemControl);
			// create the sylvia signal control by shorten the temporary signal control
			DgSylviaPreprocessData.convertSignalControlData(tmpSignalControl, signalControl);
			break;
		case LAEMMER:
			signalSystemControl.setControllerIdentifier(LaemmerSignalController.IDENTIFIER);
			signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, Id.create(signalWest.getId(), SignalGroup.class), 0, 25));
			signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, Id.create(signalNorth.getId(), SignalGroup.class), 30, 55));	
			signalControl.addSignalSystemControllerData(signalSystemControl);
			break;
		default:
			break;
		}
	}
	
}
