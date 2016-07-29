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

import java.util.LinkedList;
import java.util.List;

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
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.v11.LaneData11;
import org.matsim.lanes.data.v11.LaneDefinitions11;
import org.matsim.lanes.data.v11.LaneDefinitions11Impl;
import org.matsim.lanes.data.v11.LaneDefinitionsFactory11;
import org.matsim.lanes.data.v11.LaneDefinitionsV11ToV20Conversion;
import org.matsim.lanes.data.v11.LanesToLinkAssignment11;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.Lanes;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class DgFigure9ScenarioGenerator {
	
	private static final Logger log = Logger.getLogger(DgFigure9ScenarioGenerator.class);
	
	private String baseDir = DgPaths.STUDIESDG + "koehlerStrehler2010/scenario5/";
	
	private String networkOutfile = baseDir +  "network.xml";

	private String lanesOutfile = baseDir + "lanes.xml";
	
	public DgFigure9ScenarioGenerator(){}
	
	private Id<SignalSystem> id1, id2, id3, id4, id5, id6, id7, id8;
	private Id<Link> id12, id21, id23, id32, id34, id43, id45, id54, id56, id65, id27, id72, id78, id87, id85, id58;
	
	private int onset1 = 0;
	private int dropping1 = 55;
	private int onset2 = 60;
	private int dropping2 = 115;
	private int cycle = 120;
	
	public MutableScenario loadScenario(){
		
		Config config = ConfigUtils.loadConfig(baseDir + "config_signals_coordinated.xml");
		MatsimRandom.reset(config.global().getRandomSeed());
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(sc);
		
		SignalsScenarioLoader signalsLoader = new SignalsScenarioLoader(ConfigUtils.addOrGetModule(sc.getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class));
		SignalsData signals = signalsLoader.loadSignalsData();
		sc.addScenarioElement(SignalsData.ELEMENT_NAME, signals);
		return sc;
	}
	
	private void createScenario() {
		this.initIds();
		Config config = ConfigUtils.createConfig();
		config.qsim().setUseLanes(true);
		ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setUseSignalSystems(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		
		//network
		Network net = this.createNetwork(scenario);
		this.writeMatsimNetwork(net, networkOutfile);
		log.info("network written to " + networkOutfile);
		//lanes
		Lanes lanes = createLanes((MutableScenario)scenario);
		LaneDefinitionsWriter20 laneWriter = new LaneDefinitionsWriter20(lanes);
		laneWriter.write(lanesOutfile);
		log.info("lanes written to " + lanesOutfile);
		//signals
		createSignalSystemsAndGroups(signalsData);
		
		createSignalControl(signalsData);
		
		SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
		signalsWriter.setSignalSystemsOutputFilename(baseDir+ "signal_systems.xml");
		signalsWriter.setSignalGroupsOutputFilename(baseDir + "signal_groups.xml");
		signalsWriter.setAmberTimesOutputFilename(baseDir + "amber_times.xml");
		signalsWriter.setSignalControlOutputFilename(baseDir + "signal_control.xml");
		signalsWriter.writeSignalsData(scenario);
	}
	
	

	private SignalControlData createSignalControl(SignalsData sd) {
		SignalControlData control = sd.getSignalControlData();
		
		this.createSystem2Control(control);
		this.createSystem5Control(control);
		
		//signal system 3, 4, 7, 8 control
		List<Id<SignalSystem>> ids = new LinkedList<>();
		ids.add(id3);
		ids.add(id4);
		for (Id<SignalSystem> id : ids){
			SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(id);
			control.addSignalSystemControllerData(controller);
			controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			SignalPlanData plan = control.getFactory().createSignalPlanData(Id.create(id1, SignalPlan.class));
			controller.addSignalPlanData(plan);
			plan.setCycleTime(this.cycle);
			plan.setOffset(0);
			SignalGroupSettingsData settings1 = control.getFactory().createSignalGroupSettingsData(Id.create(id1, SignalGroup.class));
			plan.addSignalGroupSettings(settings1);
			settings1.setOnset(this.onset1);
			settings1.setDropping(this.dropping1);
			SignalGroupSettingsData settings2 = control.getFactory().createSignalGroupSettingsData(Id.create(id2, SignalGroup.class));
			plan.addSignalGroupSettings(settings2);
			settings2.setOnset(this.onset1);
			settings2.setDropping(this.dropping1);
		}
		ids.clear();
		ids.add(id7);
		ids.add(id8);
		for (Id<SignalSystem> id : ids){
			SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(id);
			control.addSignalSystemControllerData(controller);
			controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			SignalPlanData plan = control.getFactory().createSignalPlanData(Id.create(id1, SignalPlan.class));
			controller.addSignalPlanData(plan);
			plan.setCycleTime(this.cycle);
			plan.setOffset(0);
			SignalGroupSettingsData settings1 = control.getFactory().createSignalGroupSettingsData(Id.create(id1, SignalGroup.class));
			plan.addSignalGroupSettings(settings1);
			settings1.setOnset(this.onset1);
			settings1.setDropping(this.dropping1);
			SignalGroupSettingsData settings2 = control.getFactory().createSignalGroupSettingsData(Id.create(id2, SignalGroup.class));
			plan.addSignalGroupSettings(settings2);
			settings2.setOnset(this.onset1);
			settings2.setDropping(this.dropping1);
		}
		return control;
	}

	
	
	private void createSystem5Control(SignalControlData control) {
		// signal system 5 control
		SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(id5);
		control.addSignalSystemControllerData(controller);
		controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		SignalPlanData plan = control.getFactory().createSignalPlanData(Id.create(id1, SignalPlan.class));
		controller.addSignalPlanData(plan);
		plan.setCycleTime(this.cycle);
		plan.setOffset(0);
		SignalGroupSettingsData settings1 = control.getFactory().createSignalGroupSettingsData(Id.create(id1, SignalGroup.class));
		plan.addSignalGroupSettings(settings1);
		settings1.setOnset(this.onset2);
		settings1.setDropping(this.dropping2);
		SignalGroupSettingsData settings2 = control.getFactory().createSignalGroupSettingsData(Id.create(id2, SignalGroup.class));
		plan.addSignalGroupSettings(settings2);
		settings2.setOnset(this.onset2);
		settings2.setDropping(this.dropping2);
		SignalGroupSettingsData settings3 = control.getFactory().createSignalGroupSettingsData(Id.create(id3, SignalGroup.class));
		plan.addSignalGroupSettings(settings3);
		settings3.setOnset(this.onset1);
		settings3.setDropping(this.dropping1);
		SignalGroupSettingsData settings4 = control.getFactory().createSignalGroupSettingsData(Id.create(id4, SignalGroup.class));
		plan.addSignalGroupSettings(settings4);
		settings4.setOnset(this.onset1);
		settings4.setDropping(this.dropping1);
	}

	private void createSystem2Control(SignalControlData control) {
		// signal system 2 control
		SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(id2);
		control.addSignalSystemControllerData(controller);
		controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		SignalPlanData plan = control.getFactory().createSignalPlanData(Id.create(id1, SignalPlan.class));
		controller.addSignalPlanData(plan);
		plan.setCycleTime(this.cycle);
		plan.setOffset(0);
		SignalGroupSettingsData settings1 =  control.getFactory().createSignalGroupSettingsData(Id.create(id1, SignalGroup.class));
		plan.addSignalGroupSettings(settings1);
		settings1.setOnset(this.onset1);
		settings1.setDropping(this.dropping1);
		SignalGroupSettingsData settings2 = control.getFactory().createSignalGroupSettingsData(Id.create(id2, SignalGroup.class));
		plan.addSignalGroupSettings(settings2);
		settings2.setOnset(this.onset1);
		settings2.setDropping(this.dropping1);
		SignalGroupSettingsData settings3 = control.getFactory().createSignalGroupSettingsData(Id.create(id3, SignalGroup.class));
		plan.addSignalGroupSettings(settings3);
		settings3.setOnset(this.onset2);
		settings3.setDropping(this.dropping2);
		SignalGroupSettingsData settings4 = control.getFactory().createSignalGroupSettingsData(Id.create(id4, SignalGroup.class));
		plan.addSignalGroupSettings(settings4);
		settings4.setOnset(this.onset2);
		settings4.setDropping(this.dropping2);
	}

	private void createAndAddSignalGroups(Id<SignalSystem> signalSystemId, SignalGroupsData groups){
		SignalGroupData group4signal = groups.getFactory().createSignalGroupData(signalSystemId, Id.create(id1, SignalGroup.class));
		groups.addSignalGroupData(group4signal);
		group4signal.addSignalId(Id.create(id1, Signal.class));
		
		group4signal = groups.getFactory().createSignalGroupData(signalSystemId, Id.create(id2, SignalGroup.class));
		groups.addSignalGroupData(group4signal);
		group4signal.addSignalId(Id.create(id2, Signal.class));
	}

	
	private void createGroupsAndSystem2(SignalSystemsData systems, SignalGroupsData groups){
		//signal system 2
		SignalSystemData sys = systems.getFactory().createSignalSystemData(id2);
		systems.addSignalSystemData(sys);
		SignalData signal = systems.getFactory().createSignalData(Id.create(id1, Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(id12);
		signal.addLaneId(Id.create(id1, Lane.class));
		signal = systems.getFactory().createSignalData(Id.create(id2, Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(id12);
		signal.addLaneId(Id.create(id2, Lane.class));
		this.createAndAddSignalGroups(id2, groups);
		signal = systems.getFactory().createSignalData(Id.create(id3, Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(id32);
		SignalGroupData group4signal = groups.getFactory().createSignalGroupData(Id.create(id2, SignalSystem.class), Id.create(id3, SignalGroup.class));
		groups.addSignalGroupData(group4signal);
//		SignalGroupData group4signal  = groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(id1);
		group4signal.addSignalId(Id.create(id3, Signal.class));
		signal = systems.getFactory().createSignalData(Id.create(id4, Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(id72);
		group4signal = groups.getFactory().createSignalGroupData(Id.create(id2, SignalSystem.class), Id.create(id4, SignalGroup.class));
		groups.addSignalGroupData(group4signal);
//		group4signal  = groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(id2);
		group4signal.addSignalId(Id.create(id4, Signal.class));
	}
	
	private void createGroupsAndSystem5(SignalSystemsData systems, SignalGroupsData groups){
		//signal system 5
		SignalSystemData sys = systems.getFactory().createSignalSystemData(id5);
		systems.addSignalSystemData(sys);
		SignalData signal = systems.getFactory().createSignalData(Id.create(id1, Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(id65);
		signal.addLaneId(Id.create(id1, Lane.class));
		signal = systems.getFactory().createSignalData(Id.create(id2, Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(id65);
		signal.addLaneId(Id.create(id2, Lane.class));
		this.createAndAddSignalGroups(id5, groups);
		//signals 3 and 4
		signal = systems.getFactory().createSignalData(Id.create(id3, Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(id45);
		//creates a separate group for signal 3 
		SignalGroupData group4signal = groups.getFactory().createSignalGroupData(Id.create(id5, SignalSystem.class), Id.create(id3, SignalGroup.class));
		groups.addSignalGroupData(group4signal);
		//eventually better: add them to existing group
//		SignalGroupData group4signal = groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(id1);
		group4signal.addSignalId(Id.create(id3, Signal.class));
		signal = systems.getFactory().createSignalData(Id.create(id4, Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(id85);
		//creates a separate group for signal 4
		group4signal = groups.getFactory().createSignalGroupData(Id.create(id5, SignalSystem.class), Id.create(id4, SignalGroup.class));
		groups.addSignalGroupData(group4signal);
		//eventually better: add to existing group
//		group4signal = groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(id2);
		group4signal.addSignalId(Id.create(id4, Signal.class));
	}
	
	/**
	 * Node Id == SignalSystem Id
	 */
	private SignalSystemsData createSignalSystemsAndGroups(SignalsData sd) {
		SignalSystemsData systems = sd.getSignalSystemsData();
		SignalGroupsData groups = sd.getSignalGroupsData();
		this.createGroupsAndSystem2(systems, groups);
		this.createGroupsAndSystem5(systems, groups);
		
		//signal system 3
		SignalSystemData sys = systems.getFactory().createSignalSystemData(id3);
		systems.addSignalSystemData(sys);
		SignalData signal = systems.getFactory().createSignalData(Id.create(id1, Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(id23);
		signal = systems.getFactory().createSignalData(Id.create(id2, Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(id43);
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
		
		//signal system 4
		sys = systems.getFactory().createSignalSystemData(id4);
		systems.addSignalSystemData(sys);
		signal = systems.getFactory().createSignalData(Id.create(id1, Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(id34);
		signal = systems.getFactory().createSignalData(Id.create(id2, Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(id54);
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);

		//signal system 7
		sys = systems.getFactory().createSignalSystemData(id7);
		systems.addSignalSystemData(sys);
		signal = systems.getFactory().createSignalData(Id.create(id1, Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(id27);
		signal = systems.getFactory().createSignalData(Id.create(id2, Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(id87);
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
		
		//signal system 8
		sys = systems.getFactory().createSignalSystemData(id8);
		systems.addSignalSystemData(sys);
		signal = systems.getFactory().createSignalData(Id.create(id1, Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(id78);
		signal = systems.getFactory().createSignalData(Id.create(id2, Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(id58);
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);

		return systems;
	}

	private Lanes createLanes(MutableScenario scenario) {
		double laneLenght = 50.0;
		LaneDefinitions11 lanes = new LaneDefinitions11Impl();
		LaneDefinitionsFactory11 factory = lanes.getFactory();
		//lanes for link 12
		LanesToLinkAssignment11 lanesForLink12 = factory.createLanesToLinkAssignment(id12);
		lanes.addLanesToLinkAssignment(lanesForLink12);
		LaneData11 link12lane1 = factory.createLane(Id.create(id1, Lane.class));
		lanesForLink12.addLane(link12lane1);
		link12lane1.addToLinkId(id23);
		link12lane1.setStartsAtMeterFromLinkEnd(laneLenght);

		LaneData11 link12lane2 = factory.createLane(Id.create(id2, Lane.class));
		lanesForLink12.addLane(link12lane2);
		link12lane2.addToLinkId(id27);
		link12lane2.setStartsAtMeterFromLinkEnd(laneLenght);
		
		//lanes for link 65
		LanesToLinkAssignment11 lanesForLink65 = factory.createLanesToLinkAssignment(id65);
		lanes.addLanesToLinkAssignment(lanesForLink65);
		LaneData11 link65lane1 = factory.createLane(Id.create(id1, Lane.class));
		lanesForLink65.addLane(link65lane1);
		link65lane1.addToLinkId(id54);
		link65lane1.setStartsAtMeterFromLinkEnd(laneLenght);

		LaneData11 link65lane2 = factory.createLane(Id.create(id2, Lane.class));
		lanesForLink65.addLane(link65lane2);
		link65lane2.addToLinkId(id58);
		link65lane2.setStartsAtMeterFromLinkEnd(laneLenght);
		
		//convert to 2.0 format and return
		Lanes lanesv2 = LaneDefinitionsV11ToV20Conversion.convertTo20(lanes, scenario.getNetwork());
		return lanesv2;
	}

	
	
	private Network createNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		double linkLength = 300.0;
		double fs = 15.0;
		double capacity = 1800.0;
		if (net.getCapacityPeriod() != 3600.0){
			throw new IllegalStateException();
		}
		((Network)net).setEffectiveLaneWidth(1.0);
		NetworkFactory fac = net.getFactory();
		double scale = 300.0;
		Node n1, n2, n3, n4, n5, n6, n7, n8;
		net.addNode(n1 = fac.createNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0)));
		net.addNode(n2 = fac.createNode(Id.create(2, Node.class), new Coord(1.0 * scale, (double) 0)));
		net.addNode(n3 = fac.createNode(Id.create(3, Node.class), new Coord(2.0 * scale, 1.0 * scale)));
		net.addNode(n4 = fac.createNode(Id.create(4, Node.class), new Coord(3.0 * scale, 1.0 * scale)));
		net.addNode(n5 = fac.createNode(Id.create(5, Node.class), new Coord(4.0 * scale, (double) 0)));
		net.addNode(n6 = fac.createNode(Id.create(6, Node.class), new Coord(5.0 * scale, (double) 0)));
		double y1 = -1.0*scale;
		net.addNode(n7 = fac.createNode(Id.create(7, Node.class), new Coord(2.0 * scale, y1)));
		double y = -1.0*scale;
		net.addNode(n8 = fac.createNode(Id.create(8, Node.class), new Coord(3.0 * scale, y)));
		Link l = fac.createLink(id12, n1, n2);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id21, n2, n1);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		l.setNumberOfLanes(2.0);
		net.addLink(l);
		l = fac.createLink(id23, n2, n3);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id32, n3, n2);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id34, n3, n4);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id43, n4, n3);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id45, n4, n5);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id54, n5, n4);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id56, n5, n6);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		l.setNumberOfLanes(2.0);
		net.addLink(l);
		l = fac.createLink(id65, n6, n5);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id27, n2, n7);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id72, n7, n2);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id78, n7, n8);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id87, n8, n7);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id85, n8, n5);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id58, n5, n8);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		return net;
	}

	private void writeMatsimNetwork(Network net, String networkOutfile) {
		new NetworkWriter(net).write(networkOutfile);
	}

	private void initIds() {
		this.id1 = Id.create("1", SignalSystem.class);
		this.id2 = Id.create("2", SignalSystem.class);
		this.id3 = Id.create("3", SignalSystem.class);
		this.id4 = Id.create("4", SignalSystem.class);
		this.id5 = Id.create("5", SignalSystem.class);
		this.id6 = Id.create("6", SignalSystem.class);
		this.id7 = Id.create("7", SignalSystem.class);
		this.id8 = Id.create("8", SignalSystem.class);
		this.id12 = Id.create("12", Link.class);
		this.id21 = Id.create("21", Link.class);
		this.id23 = Id.create("23", Link.class);
		this.id32 = Id.create("32", Link.class);
		this.id34 = Id.create("34", Link.class);
		this.id43 = Id.create("43", Link.class);
		this.id45 = Id.create("45", Link.class);
		this.id54 = Id.create("54", Link.class);
		this.id56 = Id.create("56", Link.class);
		this.id65 = Id.create("65", Link.class);
		this.id27 = Id.create("27", Link.class);
		this.id72 = Id.create("72", Link.class);
		this.id78 = Id.create("78", Link.class);
		this.id87 = Id.create("87", Link.class);
		this.id58 = Id.create("58", Link.class);
		this.id85 = Id.create("85", Link.class);
	}
	
	public static void main(String[] args) {
		new DgFigure9ScenarioGenerator().createScenario();
	}
}
