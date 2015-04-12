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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.LaneDefinitionsV11ToV20Conversion;
import org.matsim.lanes.data.v11.LaneData11;
import org.matsim.lanes.data.v11.LaneDefinitions11;
import org.matsim.lanes.data.v11.LaneDefinitions11Impl;
import org.matsim.lanes.data.v11.LaneDefinitionsFactory11;
import org.matsim.lanes.data.v11.LanesToLinkAssignment11;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;
import org.matsim.signals.data.SignalsData;
import org.matsim.signals.data.signalcontrol.v20.SignalControlData;
import org.matsim.signals.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signals.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signals.data.signalsystems.v20.SignalData;
import org.matsim.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signals.model.Signal;
import org.matsim.signals.model.SignalGroup;
import org.matsim.signals.model.SignalPlan;
import org.matsim.signals.model.SignalSystem;
import org.matsim.contrib.signals.SignalUtils;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;

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
	
	public ScenarioImpl loadScenario(){
		ScenarioLoaderImpl scl = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(baseDir + "config_signals_coordinated.xml");
		ScenarioImpl sc = (ScenarioImpl) scl.loadScenario();
		SignalsScenarioLoader signalsLoader = new SignalsScenarioLoader(sc.getConfig().signalSystems());
		SignalsData signals = signalsLoader.loadSignalsData();
		sc.addScenarioElement(SignalsData.ELEMENT_NAME, signals);
		return sc;
	}
	
	private void createScenario() {
		this.initIds();
		Config config = ConfigUtils.createConfig();
		config.scenario().setUseLanes(true);
		config.scenario().setUseSignalSystems(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		
		//network
		Network net = this.createNetwork(scenario);
		this.writeMatsimNetwork(net, networkOutfile);
		log.info("network written to " + networkOutfile);
		//lanes
		LaneDefinitions20 lanes = createLanes((ScenarioImpl)scenario);
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
		signalsWriter.writeSignalsData(signalsData);
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

	private LaneDefinitions20 createLanes(ScenarioImpl scenario) {
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
		LaneDefinitionsV11ToV20Conversion conversion = new LaneDefinitionsV11ToV20Conversion();
		LaneDefinitions20 lanesv2 = conversion.convertTo20(lanes, scenario.getNetwork());
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
		((NetworkImpl)net).setEffectiveLaneWidth(1.0);
		NetworkFactory fac = net.getFactory();
		double scale = 300.0;
		net.addNode(fac.createNode(Id.create(1, Node.class), sc.createCoord(0, 0)));
		net.addNode(fac.createNode(Id.create(2, Node.class), sc.createCoord(1.0*scale, 0)));
		net.addNode(fac.createNode(Id.create(3, Node.class), sc.createCoord(2.0*scale, 1.0*scale)));
		net.addNode(fac.createNode(Id.create(4, Node.class), sc.createCoord(3.0*scale, 1.0*scale)));
		net.addNode(fac.createNode(Id.create(5, Node.class), sc.createCoord(4.0*scale, 0)));
		net.addNode(fac.createNode(Id.create(6, Node.class), sc.createCoord(5.0*scale, 0)));
		net.addNode(fac.createNode(Id.create(7, Node.class), sc.createCoord(2.0*scale, -1.0*scale)));
		net.addNode(fac.createNode(Id.create(8, Node.class), sc.createCoord(3.0*scale, -1.0*scale)));
		Link l = fac.createLink(id12, Id.create(id1, Node.class), Id.create(id2, Node.class));
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id21, Id.create(id2, Node.class), Id.create(id1, Node.class));
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		l.setNumberOfLanes(2.0);
		net.addLink(l);
		l = fac.createLink(id23, Id.create(id2, Node.class), Id.create(id3, Node.class));
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id32, Id.create(id3, Node.class), Id.create(id2, Node.class));
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id34, Id.create(id3, Node.class), Id.create(id4, Node.class));
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id43, Id.create(id4, Node.class), Id.create(id3, Node.class));
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id45, Id.create(id4, Node.class), Id.create(id5, Node.class));
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id54, Id.create(id5, Node.class), Id.create(id4, Node.class));
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id56, Id.create(id5, Node.class), Id.create(id6, Node.class));
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		l.setNumberOfLanes(2.0);
		net.addLink(l);
		l = fac.createLink(id65, Id.create(id6, Node.class), Id.create(id5, Node.class));
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id27, Id.create(id2, Node.class), Id.create(id7, Node.class));
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id72, Id.create(id7, Node.class), Id.create(id2, Node.class));
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id78, Id.create(id7, Node.class), Id.create(id8, Node.class));
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id87, Id.create(id8, Node.class), Id.create(id7, Node.class));
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id85, Id.create(id8, Node.class), Id.create(id5, Node.class));
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id58, Id.create(id5, Node.class), Id.create(id8, Node.class));
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
