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
package playground.dgrether.koehlerstrehlersignal;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LaneDefinitionsFactory;
import org.matsim.lanes.LaneDefinitionsV11ToV20Conversion;
import org.matsim.lanes.LaneDefinitionsWriter20;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.signalsystems.SignalUtils;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsDataImpl;
import org.matsim.signalsystems.data.SignalsScenarioLoader;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.model.DefaultPlanbasedSignalSystemController;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class DgKoehlerStrehler2010ScenarioGenerator {
	
	private static final Logger log = Logger.getLogger(DgKoehlerStrehler2010ScenarioGenerator.class);
	
	private String baseDir = DgPaths.STUDIESDG + "koehlerStrehler2010/scenario5/";
	
	private String networkOutfile = baseDir +  "network.xml";

	private String lanesOutfile = baseDir + "lanes.xml";
	
	public DgKoehlerStrehler2010ScenarioGenerator(){}
	
	private Id id1, id2, id3, id4, id5, id6, id7, id8;
	private Id id12, id21, id23, id32, id34, id43, id45, id54, id56, id65, id27, id72, id78, id87, id85, id58;
	
	private int onset1 = 0;
	private int dropping1 = 55;
	private int onset2 = 60;
	private int dropping2 = 115;
	private int cycle = 120;
	
	public ScenarioImpl loadScenario(){
		ScenarioLoaderImpl scl = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(baseDir + "config_signals.xml");
		ScenarioImpl sc = (ScenarioImpl) scl.loadScenario();
		SignalsScenarioLoader signalsLoader = new SignalsScenarioLoader(sc.getConfig().signalSystems());
		SignalsData signals = signalsLoader.loadSignalsData();
		sc.addScenarioElement(signals);
		return sc;
	}
	
	private void createScenario() {
		this.initIds();
		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().scenario().setUseLanes(true);
		//network
		Network net = this.createNetwork(sc);
		this.writeMatsimNetwork(net, networkOutfile);
		log.info("network written to " + networkOutfile);
		//lanes
		LaneDefinitions lanes = createLanes((ScenarioImpl)sc);
		LaneDefinitionsWriter20 laneWriter = new LaneDefinitionsWriter20(lanes);
		laneWriter.write(lanesOutfile);
		log.info("lanes written to " + lanesOutfile);
		//signals
		SignalsData signalsData = new SignalsDataImpl();
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
		List<Id> ids = new LinkedList<Id>();
		ids.add(id3);
		ids.add(id4);
		for (Id id : ids){
			SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(id);
			control.addSignalSystemControllerData(controller);
			controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			SignalPlanData plan = control.getFactory().createSignalPlanData(id1);
			controller.addSignalPlanData(plan);
			plan.setCycleTime(this.cycle);
			plan.setOffset(0);
			SignalGroupSettingsData settings1 = control.getFactory().createSignalGroupSettingsData(id1);
			plan.addSignalGroupSettings(settings1);
			settings1.setOnset(this.onset1);
			settings1.setDropping(this.dropping1);
			SignalGroupSettingsData settings2 = control.getFactory().createSignalGroupSettingsData(id2);
			plan.addSignalGroupSettings(settings2);
			settings2.setOnset(this.onset1);
			settings2.setDropping(this.dropping1);
		}
		ids.clear();
		ids.add(id7);
		ids.add(id8);
		for (Id id : ids){
			SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(id);
			control.addSignalSystemControllerData(controller);
			controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			SignalPlanData plan = control.getFactory().createSignalPlanData(id1);
			controller.addSignalPlanData(plan);
			plan.setCycleTime(this.cycle);
			plan.setOffset(0);
			SignalGroupSettingsData settings1 = control.getFactory().createSignalGroupSettingsData(id1);
			plan.addSignalGroupSettings(settings1);
			settings1.setOnset(this.onset1);
			settings1.setDropping(this.dropping1);
			SignalGroupSettingsData settings2 = control.getFactory().createSignalGroupSettingsData(id2);
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
		SignalPlanData plan = control.getFactory().createSignalPlanData(id1);
		controller.addSignalPlanData(plan);
		plan.setCycleTime(this.cycle);
		plan.setOffset(0);
		SignalGroupSettingsData settings1 = control.getFactory().createSignalGroupSettingsData(id1);
		plan.addSignalGroupSettings(settings1);
		settings1.setOnset(this.onset2);
		settings1.setDropping(this.dropping2);
		SignalGroupSettingsData settings2 = control.getFactory().createSignalGroupSettingsData(id2);
		plan.addSignalGroupSettings(settings2);
		settings2.setOnset(this.onset2);
		settings2.setDropping(this.dropping2);
		SignalGroupSettingsData settings3 = control.getFactory().createSignalGroupSettingsData(id3);
		plan.addSignalGroupSettings(settings3);
		settings3.setOnset(this.onset1);
		settings3.setDropping(this.dropping1);
		SignalGroupSettingsData settings4 = control.getFactory().createSignalGroupSettingsData(id4);
		plan.addSignalGroupSettings(settings4);
		settings4.setOnset(this.onset1);
		settings4.setDropping(this.dropping1);
	}

	private void createSystem2Control(SignalControlData control) {
		// signal system 2 control
		SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(id2);
		control.addSignalSystemControllerData(controller);
		controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		SignalPlanData plan = control.getFactory().createSignalPlanData(id1);
		controller.addSignalPlanData(plan);
		plan.setCycleTime(this.cycle);
		plan.setOffset(0);
		SignalGroupSettingsData settings1 =  control.getFactory().createSignalGroupSettingsData(id1);
		plan.addSignalGroupSettings(settings1);
		settings1.setOnset(this.onset1);
		settings1.setDropping(this.dropping1);
		SignalGroupSettingsData settings2 = control.getFactory().createSignalGroupSettingsData(id2);
		plan.addSignalGroupSettings(settings2);
		settings2.setOnset(this.onset1);
		settings2.setDropping(this.dropping1);
		SignalGroupSettingsData settings3 = control.getFactory().createSignalGroupSettingsData(id3);
		plan.addSignalGroupSettings(settings3);
		settings3.setOnset(this.onset2);
		settings3.setDropping(this.dropping2);
		SignalGroupSettingsData settings4 = control.getFactory().createSignalGroupSettingsData(id4);
		plan.addSignalGroupSettings(settings4);
		settings4.setOnset(this.onset2);
		settings4.setDropping(this.dropping2);
	}

	private void createAndAddSignalGroups(Id signalSystemId, SignalGroupsData groups){
		SignalGroupData group4signal = groups.getFactory().createSignalGroupData(signalSystemId, id1);
		groups.addSignalGroupData(group4signal);
		group4signal.addSignalId(id1);
		
		group4signal = groups.getFactory().createSignalGroupData(signalSystemId, id2);
		groups.addSignalGroupData(group4signal);
		group4signal.addSignalId(id2);
	}

	
	private void createGroupsAndSystem2(SignalSystemsData systems, SignalGroupsData groups){
		//signal system 2
		SignalSystemData sys = systems.getFactory().createSignalSystemData(id2);
		systems.addSignalSystemData(sys);
		SignalData signal = systems.getFactory().createSignalData(id1);
		sys.addSignalData(signal);
		signal.setLinkId(id12);
		signal.addLaneId(id1);
		signal = systems.getFactory().createSignalData(id2);
		sys.addSignalData(signal);
		signal.setLinkId(id12);
		signal.addLaneId(id2);
		this.createAndAddSignalGroups(id2, groups);
		signal = systems.getFactory().createSignalData(id3);
		sys.addSignalData(signal);
		signal.setLinkId(id32);
		SignalGroupData group4signal = groups.getFactory().createSignalGroupData(id2, id3);
		groups.addSignalGroupData(group4signal);
//		SignalGroupData group4signal  = groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(id1);
		group4signal.addSignalId(id3);
		signal = systems.getFactory().createSignalData(id4);
		sys.addSignalData(signal);
		signal.setLinkId(id72);
		group4signal = groups.getFactory().createSignalGroupData(id2, id4);
		groups.addSignalGroupData(group4signal);
//		group4signal  = groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(id2);
		group4signal.addSignalId(id4);
	}
	
	private void createGroupsAndSystem5(SignalSystemsData systems, SignalGroupsData groups){
		//signal system 5
		SignalSystemData sys = systems.getFactory().createSignalSystemData(id5);
		systems.addSignalSystemData(sys);
		SignalData signal = systems.getFactory().createSignalData(id1);
		sys.addSignalData(signal);
		signal.setLinkId(id65);
		signal.addLaneId(id1);
		signal = systems.getFactory().createSignalData(id2);
		sys.addSignalData(signal);
		signal.setLinkId(id65);
		signal.addLaneId(id2);
		this.createAndAddSignalGroups(id5, groups);
		//signals 3 and 4
		signal = systems.getFactory().createSignalData(id3);
		sys.addSignalData(signal);
		signal.setLinkId(id45);
		//creates a separate group for signal 3 
		SignalGroupData group4signal = groups.getFactory().createSignalGroupData(id5, id3);
		groups.addSignalGroupData(group4signal);
		//eventually better: add them to existing group
//		SignalGroupData group4signal = groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(id1);
		group4signal.addSignalId(id3);
		signal = systems.getFactory().createSignalData(id4);
		sys.addSignalData(signal);
		signal.setLinkId(id85);
		//creates a separate group for signal 4
		group4signal = groups.getFactory().createSignalGroupData(id5, id4);
		groups.addSignalGroupData(group4signal);
		//eventually better: add to existing group
//		group4signal = groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(id2);
		group4signal.addSignalId(id4);
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
		SignalData signal = systems.getFactory().createSignalData(id1);
		sys.addSignalData(signal);
		signal.setLinkId(id23);
		signal = systems.getFactory().createSignalData(id2);
		sys.addSignalData(signal);
		signal.setLinkId(id43);
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
		
		//signal system 4
		sys = systems.getFactory().createSignalSystemData(id4);
		systems.addSignalSystemData(sys);
		signal = systems.getFactory().createSignalData(id1);
		sys.addSignalData(signal);
		signal.setLinkId(id34);
		signal = systems.getFactory().createSignalData(id2);
		sys.addSignalData(signal);
		signal.setLinkId(id54);
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);

		//signal system 7
		sys = systems.getFactory().createSignalSystemData(id7);
		systems.addSignalSystemData(sys);
		signal = systems.getFactory().createSignalData(id1);
		sys.addSignalData(signal);
		signal.setLinkId(id27);
		signal = systems.getFactory().createSignalData(id2);
		sys.addSignalData(signal);
		signal.setLinkId(id87);
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
		
		//signal system 8
		sys = systems.getFactory().createSignalSystemData(id8);
		systems.addSignalSystemData(sys);
		signal = systems.getFactory().createSignalData(id1);
		sys.addSignalData(signal);
		signal.setLinkId(id78);
		signal = systems.getFactory().createSignalData(id2);
		sys.addSignalData(signal);
		signal.setLinkId(id58);
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);

		return systems;
	}

	private LaneDefinitions createLanes(ScenarioImpl scenario) {
		double laneLenght = 50.0;
		LaneDefinitions lanes = scenario.getLaneDefinitions();
		LaneDefinitionsFactory factory = lanes.getFactory();
		//lanes for link 12
		LanesToLinkAssignment lanesForLink12 = factory.createLanesToLinkAssignment(id12);
		lanes.addLanesToLinkAssignment(lanesForLink12);
		Lane link12lane1 = factory.createLane(id1);
		lanesForLink12.addLane(link12lane1);
		link12lane1.addToLinkId(id23);
		link12lane1.setStartsAtMeterFromLinkEnd(laneLenght);

		Lane link12lane2 = factory.createLane(id2);
		lanesForLink12.addLane(link12lane2);
		link12lane2.addToLinkId(id27);
		link12lane2.setStartsAtMeterFromLinkEnd(laneLenght);
		
		//lanes for link 65
		LanesToLinkAssignment lanesForLink65 = factory.createLanesToLinkAssignment(id65);
		lanes.addLanesToLinkAssignment(lanesForLink65);
		Lane link65lane1 = factory.createLane(id1);
		lanesForLink65.addLane(link65lane1);
		link65lane1.addToLinkId(id54);
		link65lane1.setStartsAtMeterFromLinkEnd(laneLenght);

		Lane link65lane2 = factory.createLane(id2);
		lanesForLink65.addLane(link65lane2);
		link65lane2.addToLinkId(id58);
		link65lane2.setStartsAtMeterFromLinkEnd(laneLenght);
		
		//convert to 2.0 format and return
		LaneDefinitionsV11ToV20Conversion conversion = new LaneDefinitionsV11ToV20Conversion();
		lanes = conversion.convertTo20(lanes, scenario.getNetwork());
		return lanes;
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
		net.addNode(fac.createNode(id1, sc.createCoord(0, 0)));
		net.addNode(fac.createNode(id2, sc.createCoord(1.0*scale, 0)));
		net.addNode(fac.createNode(id3, sc.createCoord(2.0*scale, 1.0*scale)));
		net.addNode(fac.createNode(id4, sc.createCoord(3.0*scale, 1.0*scale)));
		net.addNode(fac.createNode(id5, sc.createCoord(4.0*scale, 0)));
		net.addNode(fac.createNode(id6, sc.createCoord(5.0*scale, 0)));
		net.addNode(fac.createNode(id7, sc.createCoord(2.0*scale, -1.0*scale)));
		net.addNode(fac.createNode(id8, sc.createCoord(3.0*scale, -1.0*scale)));
		Link l = fac.createLink(id12, id1, id2);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id21, id2, id1);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		l.setNumberOfLanes(2.0);
		net.addLink(l);
		l = fac.createLink(id23, id2, id3);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id32, id3, id2);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id34, id3, id4);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id43, id4, id3);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id45, id4, id5);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id54, id5, id4);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id56, id5, id6);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		l.setNumberOfLanes(2.0);
		net.addLink(l);
		l = fac.createLink(id65, id6, id5);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id27, id2, id7);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id72, id7, id2);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id78, id7, id8);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id87, id8, id7);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id85, id8, id5);
		l.setCapacity(capacity);
		l.setFreespeed(fs);
		l.setLength(linkLength);
		net.addLink(l);
		l = fac.createLink(id58, id5, id8);
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
		this.id1 = new IdImpl("1");
		this.id2 = new IdImpl("2");
		this.id3 = new IdImpl("3");
		this.id4 = new IdImpl("4");
		this.id5 = new IdImpl("5");
		this.id6 = new IdImpl("6");
		this.id7 = new IdImpl("7");
		this.id8 = new IdImpl("8");
		this.id12 = new IdImpl("12");
		this.id21 = new IdImpl("21");
		this.id23 = new IdImpl("23");
		this.id32 = new IdImpl("32");
		this.id34 = new IdImpl("34");
		this.id43 = new IdImpl("43");
		this.id45 = new IdImpl("45");
		this.id54 = new IdImpl("54");
		this.id56 = new IdImpl("56");
		this.id65 = new IdImpl("65");
		this.id27 = new IdImpl("27");
		this.id72 = new IdImpl("72");
		this.id78 = new IdImpl("78");
		this.id87 = new IdImpl("87");
		this.id58 = new IdImpl("58");
		this.id85 = new IdImpl("85");
	}
	
	public static void main(String[] args) {
		new DgKoehlerStrehler2010ScenarioGenerator().createScenario();
	}
}
