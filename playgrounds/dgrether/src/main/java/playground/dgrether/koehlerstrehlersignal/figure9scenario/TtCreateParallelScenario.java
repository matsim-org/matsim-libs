/* *********************************************************************** *
 * project: org.matsim.*
 * DgStrehler2010DemandGenerator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.figure9scenario;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.v11.LaneData11;
import org.matsim.lanes.data.v11.LaneDefinitions11;
import org.matsim.lanes.data.v11.LaneDefinitions11Impl;
import org.matsim.lanes.data.v11.LaneDefinitionsFactory11;
import org.matsim.lanes.data.v11.LaneDefinitionsV11ToV20Conversion;
import org.matsim.lanes.data.v11.LanesToLinkAssignment11;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;

import playground.dgrether.DgPaths;

/**
 * Class to create the parallel scenario (former figure9 scenario) with one direction.
 * This class doesn't create a population.
 * It creates a base case signal control plan described at the specific methods.
 * The user is able to adapt the capacity of the network before creating it.
 * 
 * @author tthunig 
 */
public class TtCreateParallelScenario {

	private static final Logger log = Logger
			.getLogger(TtCreateParallelScenario.class);

	private int capacity;

	private String baseDir;
	private String networkOutfile;
	private String lanesOutfile;
	private String signalSystemsOutfile;
	private String signalGroupsOutfile;
	private String signalControlOutfileBC;

	private Id<Node> idN1, idN2, idN3, idN4, idN5, idN6, idN7, idN8;
	private Id<Link> idL12, idL21, idL23, idL32, idL24, idL42, idL35, idL53,
			idL46, idL64, idL57, idL75, idL76, idL67, idL87, idL78;
	private Id<Lane> idL12L1, idL12L2, idL87L1, idL87L2;
	private Id<SignalSystem> idS2, idS3, idS4, idS5, idS6, idS7;

	private int onset1 = 0;
	private int dropping1 = 25;
	private int onset2 = 30;
	private int dropping2 = 55;
	private int cycle = 60;

	private double linkLength = 200.0;
	private double fs = 10.0;

	public TtCreateParallelScenario(int capacity) {
		this.capacity = capacity;

		// define output filenames
		this.baseDir = DgPaths.SHAREDSVN
				+ "projects/cottbus/data/scenarios/parallel_scenario/AB/";
		this.networkOutfile = baseDir + "network" + capacity + ".xml";
		this.lanesOutfile = baseDir + "lanes.xml";

		this.signalSystemsOutfile = baseDir + "signalSystems.xml";
		this.signalGroupsOutfile = baseDir + "signalGroups.xml";
		this.signalControlOutfileBC = baseDir + "signalControlBC.xml";
	}

	private void createScenario() {
		this.initIds();
		Config config = ConfigUtils.createConfig();
		config.qsim().setUseLanes(true);
		ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME,
				SignalSystemsConfigGroup.class).setUseSignalSystems(true);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// network
		Network net = this.createNetwork(scenario);
		this.writeMatsimNetwork(net, networkOutfile);
		log.info("network written to " + networkOutfile);
		// lanes
		createLanes((ScenarioImpl) scenario);
		LaneDefinitionsWriter20 laneWriter = new LaneDefinitionsWriter20(scenario.getLanes());
		laneWriter.write(lanesOutfile);
		log.info("lanes written to " + lanesOutfile);
		// signals
		SignalsData signalsData = (SignalsData) scenario
				.getScenarioElement(SignalsData.ELEMENT_NAME);
		createSignalSystemsAndGroups(signalsData);
		createSignalControl(signalsData);

		SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
		signalsWriter.setSignalSystemsOutputFilename(signalSystemsOutfile);
		signalsWriter.setSignalGroupsOutputFilename(signalGroupsOutfile);
		signalsWriter.setSignalControlOutputFilename(signalControlOutfileBC);
		signalsWriter.writeSignalsData(signalsData);
	}

	private SignalControlData createSignalControl(SignalsData sd) {
		SignalControlData control = sd.getSignalControlData();

		createSignalControlFor2LightCrossing(idS3, control);
		createSignalControlFor2LightCrossing(idS5, control);
		createSignalControlFor2LightCrossing(idS4, control);
		createSignalControlFor2LightCrossing(idS6, control);
		
		createSignalControlFor4LightCrossing(idS2, control);
		
//		this.createSystem2Control(control);
//		this.createSystem5Control(control);
//
//		// signal system 3, 4, 7, 8 control
//		List<Id<SignalSystem>> ids = new LinkedList<>();
//		ids.add(id3);
//		ids.add(id4);
//		for (Id<SignalSystem> id : ids) {
//			SignalSystemControllerData controller = control.getFactory()
//					.createSignalSystemControllerData(id);
//			control.addSignalSystemControllerData(controller);
//			controller
//					.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
//			SignalPlanData plan = control.getFactory().createSignalPlanData(
//					Id.create(id1, SignalPlan.class));
//			controller.addSignalPlanData(plan);
//			plan.setCycleTime(this.cycle);
//			plan.setOffset(0);
//			SignalGroupSettingsData settings1 = control.getFactory()
//					.createSignalGroupSettingsData(
//							Id.create(id1, SignalGroup.class));
//			plan.addSignalGroupSettings(settings1);
//			settings1.setOnset(this.onset1);
//			settings1.setDropping(this.dropping1);
//			SignalGroupSettingsData settings2 = control.getFactory()
//					.createSignalGroupSettingsData(
//							Id.create(id2, SignalGroup.class));
//			plan.addSignalGroupSettings(settings2);
//			settings2.setOnset(this.onset1);
//			settings2.setDropping(this.dropping1);
//		}
//		ids.clear();
//		ids.add(id7);
//		ids.add(id8);
//		for (Id<SignalSystem> id : ids) {
//			SignalSystemControllerData controller = control.getFactory()
//					.createSignalSystemControllerData(id);
//			control.addSignalSystemControllerData(controller);
//			controller
//					.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
//			SignalPlanData plan = control.getFactory().createSignalPlanData(
//					Id.create(id1, SignalPlan.class));
//			controller.addSignalPlanData(plan);
//			plan.setCycleTime(this.cycle);
//			plan.setOffset(0);
//			SignalGroupSettingsData settings1 = control.getFactory()
//					.createSignalGroupSettingsData(
//							Id.create(id1, SignalGroup.class));
//			plan.addSignalGroupSettings(settings1);
//			settings1.setOnset(this.onset1);
//			settings1.setDropping(this.dropping1);
//			SignalGroupSettingsData settings2 = control.getFactory()
//					.createSignalGroupSettingsData(
//							Id.create(id2, SignalGroup.class));
//			plan.addSignalGroupSettings(settings2);
//			settings2.setOnset(this.onset1);
//			settings2.setDropping(this.dropping1);
//		}
		return control;
	}

	/**
	 * creates signal control for a crossing like node 2 or 7, which contains
	 * 4 signal groups with single signals. this method switches signal 1 and 3 
	 * together in the first onset-dropping rhythm and signal 2 and 4 together 
	 * in the second onset-dropping rhythm.
	 * 
	 * @param signalSystemId
	 * @param control
	 */
	private void createSignalControlFor4LightCrossing(Id<SignalSystem> signalSystemId,
			SignalControlData control) {
		
		SignalSystemControllerData controller = control.getFactory()
				.createSignalSystemControllerData(signalSystemId);
		control.addSignalSystemControllerData(controller);
		controller
				.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		SignalPlanData plan = control.getFactory().createSignalPlanData(
				Id.create(signalSystemId.toString() + "P", SignalPlan.class));
		controller.addSignalPlanData(plan);
		plan.setCycleTime(this.cycle);
		plan.setOffset(0);
		
		SignalGroupSettingsData settings1 = control.getFactory()
				.createSignalGroupSettingsData(
						Id.create(signalSystemId.toString() + "S1", SignalGroup.class));
		plan.addSignalGroupSettings(settings1);
		settings1.setOnset(this.onset1);
		settings1.setDropping(this.dropping1);
		
		SignalGroupSettingsData settings2 = control.getFactory()
				.createSignalGroupSettingsData(
						Id.create(signalSystemId.toString() + "S3", SignalGroup.class));
		plan.addSignalGroupSettings(settings2);
		settings2.setOnset(this.onset1);
		settings2.setDropping(this.dropping1);
		
		SignalGroupSettingsData settings3 = control.getFactory()
				.createSignalGroupSettingsData(
						Id.create(signalSystemId.toString() + "S2", SignalGroup.class));
		plan.addSignalGroupSettings(settings3);
		settings3.setOnset(this.onset2);
		settings3.setDropping(this.dropping2);
		
		SignalGroupSettingsData settings4 = control.getFactory()
				.createSignalGroupSettingsData(
						Id.create(signalSystemId.toString() + "S4", SignalGroup.class));
		plan.addSignalGroupSettings(settings4);
		settings4.setOnset(this.onset2);
		settings4.setDropping(this.dropping2);		
	}

	/**
	 * creates signal control for a crossing like node 3,4,5 or 6, which contains
	 * 2 signal groups with single signals. this method switches both signals together
	 * in the first onset-dropping rhythm.
	 * 
	 * @param signalSystemId
	 * @param control
	 */
	private void createSignalControlFor2LightCrossing(Id<SignalSystem> signalSystemId,
			SignalControlData control) {
		
		SignalSystemControllerData controller = control.getFactory()
				.createSignalSystemControllerData(signalSystemId);
		control.addSignalSystemControllerData(controller);
		controller.setControllerIdentifier(
				DefaultPlanbasedSignalSystemController.IDENTIFIER);
		SignalPlanData plan = control.getFactory().createSignalPlanData(
				Id.create(signalSystemId.toString() + "P", SignalPlan.class));
		controller.addSignalPlanData(plan);
		plan.setCycleTime(this.cycle);
		plan.setOffset(0);
		
		SignalGroupSettingsData settings1 = control.getFactory()
				.createSignalGroupSettingsData(
						Id.create(signalSystemId.toString()+"S1", SignalGroup.class));
		plan.addSignalGroupSettings(settings1);
		settings1.setOnset(this.onset1);
		settings1.setDropping(this.dropping1);
		
		SignalGroupSettingsData settings2 = control.getFactory()
				.createSignalGroupSettingsData(
						Id.create(signalSystemId.toString() + "S2", SignalGroup.class));
		plan.addSignalGroupSettings(settings2);
		settings2.setOnset(this.onset1);
		settings2.setDropping(this.dropping1);		
	}

//	private void createSystem5Control(SignalControlData control) {
//		// signal system 5 control
//		SignalSystemControllerData controller = control.getFactory()
//				.createSignalSystemControllerData(id5);
//		control.addSignalSystemControllerData(controller);
//		controller
//				.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
//		SignalPlanData plan = control.getFactory().createSignalPlanData(
//				Id.create(id1, SignalPlan.class));
//		controller.addSignalPlanData(plan);
//		plan.setCycleTime(this.cycle);
//		plan.setOffset(0);
//		SignalGroupSettingsData settings1 = control.getFactory()
//				.createSignalGroupSettingsData(
//						Id.create(id1, SignalGroup.class));
//		plan.addSignalGroupSettings(settings1);
//		settings1.setOnset(this.onset2);
//		settings1.setDropping(this.dropping2);
//		SignalGroupSettingsData settings2 = control.getFactory()
//				.createSignalGroupSettingsData(
//						Id.create(id2, SignalGroup.class));
//		plan.addSignalGroupSettings(settings2);
//		settings2.setOnset(this.onset2);
//		settings2.setDropping(this.dropping2);
//		SignalGroupSettingsData settings3 = control.getFactory()
//				.createSignalGroupSettingsData(
//						Id.create(id3, SignalGroup.class));
//		plan.addSignalGroupSettings(settings3);
//		settings3.setOnset(this.onset1);
//		settings3.setDropping(this.dropping1);
//		SignalGroupSettingsData settings4 = control.getFactory()
//				.createSignalGroupSettingsData(
//						Id.create(id4, SignalGroup.class));
//		plan.addSignalGroupSettings(settings4);
//		settings4.setOnset(this.onset1);
//		settings4.setDropping(this.dropping1);
//	}
//
//	private void createSystem2Control(SignalControlData control) {
//		// signal system 2 control
//		SignalSystemControllerData controller = control.getFactory()
//				.createSignalSystemControllerData(id2);
//		control.addSignalSystemControllerData(controller);
//		controller
//				.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
//		SignalPlanData plan = control.getFactory().createSignalPlanData(
//				Id.create(id1, SignalPlan.class));
//		controller.addSignalPlanData(plan);
//		plan.setCycleTime(this.cycle);
//		plan.setOffset(0);
//		SignalGroupSettingsData settings1 = control.getFactory()
//				.createSignalGroupSettingsData(
//						Id.create(id1, SignalGroup.class));
//		plan.addSignalGroupSettings(settings1);
//		settings1.setOnset(this.onset1);
//		settings1.setDropping(this.dropping1);
//		SignalGroupSettingsData settings2 = control.getFactory()
//				.createSignalGroupSettingsData(
//						Id.create(id2, SignalGroup.class));
//		plan.addSignalGroupSettings(settings2);
//		settings2.setOnset(this.onset1);
//		settings2.setDropping(this.dropping1);
//		SignalGroupSettingsData settings3 = control.getFactory()
//				.createSignalGroupSettingsData(
//						Id.create(id3, SignalGroup.class));
//		plan.addSignalGroupSettings(settings3);
//		settings3.setOnset(this.onset2);
//		settings3.setDropping(this.dropping2);
//		SignalGroupSettingsData settings4 = control.getFactory()
//				.createSignalGroupSettingsData(
//						Id.create(id4, SignalGroup.class));
//		plan.addSignalGroupSettings(settings4);
//		settings4.setOnset(this.onset2);
//		settings4.setDropping(this.dropping2);
//	}

//	private void createAndAddSignalGroup(Id<SignalSystem> signalSystemId,
//			Id<Signal> signalId, SignalGroupsData groups) {
//		
//		SignalGroupData group4signal = groups.getFactory()
//				.createSignalGroupData(signalSystemId,
//						Id.create(signalId.toString() + "G", SignalGroup.class));
//		groups.addSignalGroupData(group4signal);
//		group4signal.addSignalId(signalId);
//	}
	
//	private void createAndAddSignalGroups(Id<SignalSystem> signalSystemId,
//			SignalGroupsData groups) {
//		SignalGroupData group4signal = groups.getFactory()
//				.createSignalGroupData(signalSystemId,
//						Id.create(id1, SignalGroup.class));
//		groups.addSignalGroupData(group4signal);
//		group4signal.addSignalId(Id.create(id1, Signal.class));
//
//		group4signal = groups.getFactory().createSignalGroupData(
//				signalSystemId, Id.create(id2, SignalGroup.class));
//		groups.addSignalGroupData(group4signal);
//		group4signal.addSignalId(Id.create(id2, Signal.class));
//	}
//
//	private void createGroupsAndSystem2(SignalSystemsData systems,
//			SignalGroupsData groups) {
//		// signal system 2
//		SignalSystemData sys = systems.getFactory().createSignalSystemData(id2);
//		systems.addSignalSystemData(sys);
//		SignalData signal = systems.getFactory().createSignalData(
//				Id.create(id1, Signal.class));
//		sys.addSignalData(signal);
//		signal.setLinkId(id12);
//		signal.addLaneId(Id.create(id1, Lane.class));
//		signal = systems.getFactory().createSignalData(
//				Id.create(id2, Signal.class));
//		sys.addSignalData(signal);
//		signal.setLinkId(id12);
//		signal.addLaneId(Id.create(id2, Lane.class));
//		this.createAndAddSignalGroups(id2, groups);
//		signal = systems.getFactory().createSignalData(
//				Id.create(id3, Signal.class));
//		sys.addSignalData(signal);
//		signal.setLinkId(id32);
//		SignalGroupData group4signal = groups.getFactory()
//				.createSignalGroupData(Id.create(id2, SignalSystem.class),
//						Id.create(id3, SignalGroup.class));
//		groups.addSignalGroupData(group4signal);
//		// SignalGroupData group4signal =
//		// groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(id1);
//		group4signal.addSignalId(Id.create(id3, Signal.class));
//		signal = systems.getFactory().createSignalData(
//				Id.create(id4, Signal.class));
//		sys.addSignalData(signal);
//		signal.setLinkId(id72);
//		group4signal = groups.getFactory().createSignalGroupData(
//				Id.create(id2, SignalSystem.class),
//				Id.create(id4, SignalGroup.class));
//		groups.addSignalGroupData(group4signal);
//		// group4signal =
//		// groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(id2);
//		group4signal.addSignalId(Id.create(id4, Signal.class));
//	}
//
//	private void createGroupsAndSystem5(SignalSystemsData systems,
//			SignalGroupsData groups) {
//		// signal system 5
//		SignalSystemData sys = systems.getFactory().createSignalSystemData(id5);
//		systems.addSignalSystemData(sys);
//		SignalData signal = systems.getFactory().createSignalData(
//				Id.create(id1, Signal.class));
//		sys.addSignalData(signal);
//		signal.setLinkId(id65);
//		signal.addLaneId(Id.create(id1, Lane.class));
//		signal = systems.getFactory().createSignalData(
//				Id.create(id2, Signal.class));
//		sys.addSignalData(signal);
//		signal.setLinkId(id65);
//		signal.addLaneId(Id.create(id2, Lane.class));
//		this.createAndAddSignalGroups(id5, groups);
//		// signals 3 and 4
//		signal = systems.getFactory().createSignalData(
//				Id.create(id3, Signal.class));
//		sys.addSignalData(signal);
//		signal.setLinkId(id45);
//		// creates a separate group for signal 3
//		SignalGroupData group4signal = groups.getFactory()
//				.createSignalGroupData(Id.create(id5, SignalSystem.class),
//						Id.create(id3, SignalGroup.class));
//		groups.addSignalGroupData(group4signal);
//		// eventually better: add them to existing group
//		// SignalGroupData group4signal =
//		// groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(id1);
//		group4signal.addSignalId(Id.create(id3, Signal.class));
//		signal = systems.getFactory().createSignalData(
//				Id.create(id4, Signal.class));
//		sys.addSignalData(signal);
//		signal.setLinkId(id85);
//		// creates a separate group for signal 4
//		group4signal = groups.getFactory().createSignalGroupData(
//				Id.create(id5, SignalSystem.class),
//				Id.create(id4, SignalGroup.class));
//		groups.addSignalGroupData(group4signal);
//		// eventually better: add to existing group
//		// group4signal =
//		// groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(id2);
//		group4signal.addSignalId(Id.create(id4, Signal.class));
//	}

	private SignalSystemsData createSignalSystemsAndGroups(SignalsData sd) {
		SignalSystemsData systems = sd.getSignalSystemsData();
		SignalGroupsData groups = sd.getSignalGroupsData();

		createSignalSystem2Lights(idS3, idL23, idL53, systems, groups);
		createSignalSystem2Lights(idS5, idL35, idL75, systems, groups);
		createSignalSystem2Lights(idS4, idL24, idL64, systems, groups);
		createSignalSystem2Lights(idS6, idL46, idL76, systems, groups);

		createSignalSystem4Lights(idS2, idL12, idL12L1, idL12L2, idL32, 
				idL42, systems, groups);
		createSignalSystem4Lights(idS7, idL87, idL87L1, idL87L2, idL67, 
				idL57, systems, groups);
		
		return systems;
	}

	private void createSignalSystem4Lights(Id<SignalSystem> signalSystemId,
			Id<Link> linkWithLanesId, Id<Lane> laneId1, Id<Lane> laneId2, 
			Id<Link> linkWoLanesId1, Id<Link> linkWoLanesId2,
			SignalSystemsData systems, SignalGroupsData groups) {
		
		SignalSystemData sys = systems.getFactory().createSignalSystemData(signalSystemId);
		systems.addSignalSystemData(sys);
		
		// create signals at the link with lanes
		SignalData signal = systems.getFactory().createSignalData(
				Id.create(signalSystemId.toString()+"S1", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(linkWithLanesId);
		signal.addLaneId(laneId1);
		
		signal = systems.getFactory().createSignalData(
				Id.create(signalSystemId.toString()+"S2", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(linkWithLanesId);
		signal.addLaneId(laneId2);
		
		// create signals at links without lanes
		signal = systems.getFactory().createSignalData(
				Id.create(signalSystemId.toString()+"S3", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(linkWoLanesId1);
		
		signal = systems.getFactory().createSignalData(
				Id.create(signalSystemId.toString()+"S4", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(linkWoLanesId2);
		
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
	}

	private void createSignalSystem2Lights(Id<SignalSystem> signalSystemId, Id<Link> linkId1, 
			Id<Link> linkId2, SignalSystemsData systems, SignalGroupsData groups) {
		
		SignalSystemData sys = systems.getFactory().createSignalSystemData(signalSystemId);
		systems.addSignalSystemData(sys);
		SignalData signal = systems.getFactory().createSignalData(
				Id.create(signalSystemId.toString()+"S1", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(linkId1);
		
		signal = systems.getFactory().createSignalData(
				Id.create(signalSystemId.toString()+"S2", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(linkId2);
		
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
	}
	
	

	private void createLanes(ScenarioImpl scenario) {
		double laneLenght = 50.0;
		LaneDefinitions11 lanes11 = new LaneDefinitions11Impl();
		LaneDefinitionsFactory11 factory = lanes11.getFactory();
		// lanes for link 12
		LanesToLinkAssignment11 lanesForLink12 = factory
				.createLanesToLinkAssignment(idL12);
		lanes11.addLanesToLinkAssignment(lanesForLink12);
		LaneData11 link12lane1 = factory.createLane(idL12L1);
		lanesForLink12.addLane(link12lane1);
		link12lane1.addToLinkId(idL23);
		link12lane1.setStartsAtMeterFromLinkEnd(laneLenght);

		LaneData11 link12lane2 = factory.createLane(idL12L2);
		lanesForLink12.addLane(link12lane2);
		link12lane2.addToLinkId(idL24);
		link12lane2.setStartsAtMeterFromLinkEnd(laneLenght);

		// lanes for link 87
		LanesToLinkAssignment11 lanesForLink87 = factory
				.createLanesToLinkAssignment(idL87);
		lanes11.addLanesToLinkAssignment(lanesForLink87);
		LaneData11 link87lane1 = factory.createLane(idL87L1);
		lanesForLink87.addLane(link87lane1);
		link87lane1.addToLinkId(idL76);
		link87lane1.setStartsAtMeterFromLinkEnd(laneLenght);

		LaneData11 link87lane2 = factory.createLane(idL87L2);
		lanesForLink87.addLane(link87lane2);
		link87lane2.addToLinkId(idL75);
		link87lane2.setStartsAtMeterFromLinkEnd(laneLenght);

		// convert to 2.0 format and save in scenario
		LaneDefinitionsV11ToV20Conversion.convertTo20(lanes11,
				scenario.getLanes(), scenario.getNetwork());
	}

	private Network createNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		if (net.getCapacityPeriod() != 3600.0) {
			throw new IllegalStateException();
		}
		
		((NetworkImpl) net).setEffectiveLaneWidth(1.0);
		NetworkFactory fac = net.getFactory();
		net.addNode(fac.createNode(idN1, new Coord(-500, 0)));
		net.addNode(fac.createNode(idN2, new Coord(-300, 0)));
		net.addNode(fac.createNode(idN3, new Coord(-100, 100)));
		net.addNode(fac.createNode(idN4, new Coord(-100, -100)));
		net.addNode(fac.createNode(idN5, new Coord(100, 100)));
		net.addNode(fac.createNode(idN6, new Coord(100, -100)));
		net.addNode(fac.createNode(idN7, new Coord(300, 0)));
		net.addNode(fac.createNode(idN8, new Coord(500, 0)));
		
		this.createAndAddLinks(idL12, idL21, idN1, idN2, net);
		this.createAndAddLinks(idL23, idL32, idN2, idN3, net);
		this.createAndAddLinks(idL24, idL42, idN2, idN4, net);
		this.createAndAddLinks(idL35, idL53, idN3, idN5, net);
		this.createAndAddLinks(idL46, idL64, idN4, idN6, net);
		this.createAndAddLinks(idL57, idL75, idN5, idN7, net);
		this.createAndAddLinks(idL67, idL76, idN6, idN7, net);
		this.createAndAddLinks(idL78, idL87, idN7, idN8, net);
		
		return net;
	}

	private void createAndAddLinks(Id<Link> linkId, Id<Link> backLinkId,
			Id<Node> fromNodeId, Id<Node> toNodeId, Network net) {

		NetworkFactory fac = net.getFactory();

		Link l = fac.createLink(linkId, net.getNodes().get(fromNodeId), net
				.getNodes().get(toNodeId));
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);

		l = fac.createLink(backLinkId, net.getNodes().get(toNodeId), net
				.getNodes().get(fromNodeId));
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
	}

	private void writeMatsimNetwork(Network net, String networkOutfile) {
		new NetworkWriter(net).write(networkOutfile);
	}

	private void initIds() {
		this.idN1 = Id.create("N1", Node.class);
		this.idN2 = Id.create("N2", Node.class);
		this.idN3 = Id.create("N3", Node.class);
		this.idN4 = Id.create("N4", Node.class);
		this.idN5 = Id.create("N5", Node.class);
		this.idN6 = Id.create("N6", Node.class);
		this.idN7 = Id.create("N7", Node.class);
		this.idN8 = Id.create("N8", Node.class);

		this.idS2 = Id.create("S2", SignalSystem.class);
		this.idS3 = Id.create("S3", SignalSystem.class);
		this.idS4 = Id.create("S4", SignalSystem.class);
		this.idS5 = Id.create("S5", SignalSystem.class);
		this.idS6 = Id.create("S6", SignalSystem.class);
		this.idS7 = Id.create("S7", SignalSystem.class);

		this.idL12 = Id.create("L12", Link.class);
		this.idL21 = Id.create("L21", Link.class);
		this.idL23 = Id.create("L23", Link.class);
		this.idL32 = Id.create("L32", Link.class);
		this.idL24 = Id.create("L24", Link.class);
		this.idL42 = Id.create("L42", Link.class);
		this.idL35 = Id.create("L35", Link.class);
		this.idL53 = Id.create("L53", Link.class);
		this.idL46 = Id.create("L46", Link.class);
		this.idL64 = Id.create("L64", Link.class);
		this.idL57 = Id.create("L57", Link.class);
		this.idL75 = Id.create("L75", Link.class);
		this.idL76 = Id.create("L76", Link.class);
		this.idL67 = Id.create("L67", Link.class);
		this.idL78 = Id.create("L78", Link.class);
		this.idL87 = Id.create("L87", Link.class);
		
		this.idL12L1 = Id.create("L12L1", Lane.class);
		this.idL12L2 = Id.create("L12L2", Lane.class);
		this.idL87L1 = Id.create("L87L1", Lane.class);
		this.idL87L2 = Id.create("L87L2", Lane.class);
	}

	public static void main(String[] args) {

		int capacity = 1800; // TODO check parameter when tool is rerun

		new TtCreateParallelScenario(capacity).createScenario();
	}
}
