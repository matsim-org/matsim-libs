/* *********************************************************************** *
 * project: org.matsim.*
 * CreateTrafficSignalScenarioWithLanes
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package tutorial.unsupported.example90TrafficLights;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LaneDefinitionsFactory;
import org.matsim.lanes.LaneDefinitionsV11ToV20Conversion;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.signalsystems.data.SignalsData;
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


/**
 * @author dgrether
 *
 */
public class CreateTrafficSignalScenarioWithLanes {

	private int onset1 = 0;
	private int dropping1 = 55;
	private int onset2 = 60;
	private int dropping2 = 115;
	private int cycle = 120;

	
	public Scenario loadScenario(){
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("examples/tutorial/unsupported/example90TrafficLights/network.xml.gz");
		config.plans().setInputFile("examples/tutorial/unsupported/example90TrafficLights/population.xml.gz");
		config.scenario().setUseLanes(true);
		config.scenario().setUseSignalSystems(true);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	private void createGroupsAndSystem2(Scenario scenario, SignalSystemsData systems, SignalGroupsData groups){
		//signal system 2
		SignalSystemData sys = systems.getFactory().createSignalSystemData(scenario.createId("2"));
		systems.addSignalSystemData(sys);
		SignalData signal = systems.getFactory().createSignalData(scenario.createId("1"));
		sys.addSignalData(signal);
		signal.setLinkId(scenario.createId("12"));
		signal.addLaneId(scenario.createId("1"));
		signal = systems.getFactory().createSignalData(scenario.createId("2"));
		sys.addSignalData(signal);
		signal.setLinkId(scenario.createId("12"));
		signal.addLaneId(scenario.createId("2"));
		//create the groups TODO reconsider if this would be better done by utils
		SignalGroupData group4signal = groups.getFactory().createSignalGroupData(sys.getId(), scenario.createId("1"));
		groups.addSignalGroupData(group4signal);
		group4signal.addSignalId(scenario.createId("1"));
		
		group4signal = groups.getFactory().createSignalGroupData(sys.getId(), scenario.createId("2"));
		groups.addSignalGroupData(group4signal);
		group4signal.addSignalId(scenario.createId("2"));

		signal = systems.getFactory().createSignalData(scenario.createId("3"));
		sys.addSignalData(signal);
		signal.setLinkId(scenario.createId("32"));
		group4signal = groups.getFactory().createSignalGroupData(scenario.createId("2"), scenario.createId("3"));
		groups.addSignalGroupData(group4signal);
//		SignalGroupData group4signal  = groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(scenario.createId("1"));
		group4signal.addSignalId(scenario.createId("3"));
		signal = systems.getFactory().createSignalData(scenario.createId("4"));
		sys.addSignalData(signal);
		signal.setLinkId(scenario.createId("72"));
		group4signal = groups.getFactory().createSignalGroupData(scenario.createId("2"), scenario.createId("4"));
		groups.addSignalGroupData(group4signal);
//		group4signal  = groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(scenario.createId("2"));
		group4signal.addSignalId(scenario.createId("4"));
	}
	
	private void createGroupsAndSystem5(Scenario scenario, SignalSystemsData systems, SignalGroupsData groups){
		//signal system 5
		SignalSystemData sys = systems.getFactory().createSignalSystemData(scenario.createId("5"));
		systems.addSignalSystemData(sys);
		SignalData signal = systems.getFactory().createSignalData(scenario.createId("1"));
		sys.addSignalData(signal);
		signal.setLinkId(scenario.createId("65"));
		signal.addLaneId(scenario.createId("1"));
		signal = systems.getFactory().createSignalData(scenario.createId("2"));
		sys.addSignalData(signal);
		signal.setLinkId(scenario.createId("65"));
		signal.addLaneId(scenario.createId("2"));
		//create the groups
		SignalGroupData group4signal = groups.getFactory().createSignalGroupData(sys.getId(), scenario.createId("1"));
		groups.addSignalGroupData(group4signal);
		group4signal.addSignalId(scenario.createId("1"));
		
		group4signal = groups.getFactory().createSignalGroupData(sys.getId(), scenario.createId("2"));
		groups.addSignalGroupData(group4signal);
		group4signal.addSignalId(scenario.createId("2"));
		//signals 3 and 4
		signal = systems.getFactory().createSignalData(scenario.createId("3"));
		sys.addSignalData(signal);
		signal.setLinkId(scenario.createId("45"));
		//creates a separate group for signal 3 
		group4signal = groups.getFactory().createSignalGroupData(scenario.createId("5"), scenario.createId("3"));
		groups.addSignalGroupData(group4signal);
		//eventually better: add them to existing group
//		SignalGroupData group4signal = groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(scenario.createId("1"));
		group4signal.addSignalId(scenario.createId("3"));
		signal = systems.getFactory().createSignalData(scenario.createId("4"));
		sys.addSignalData(signal);
		signal.setLinkId(scenario.createId("85"));
		//creates a separate group for signal 4
		group4signal = groups.getFactory().createSignalGroupData(scenario.createId("5"), scenario.createId("4"));
		groups.addSignalGroupData(group4signal);
		//eventually better: add to existing group
//		group4signal = groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(scenario.createId("2"));
		group4signal.addSignalId(scenario.createId("4"));
	}

	
	
	private void createSystem5Control(Scenario scenario, SignalControlData control) {
		// signal system 5 control
		SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(scenario.createId("5"));
		control.addSignalSystemControllerData(controller);
		controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		SignalPlanData plan = control.getFactory().createSignalPlanData(scenario.createId("1"));
		controller.addSignalPlanData(plan);
		plan.setCycleTime(this.cycle);
		plan.setOffset(0);
		SignalGroupSettingsData settings1 = control.getFactory().createSignalGroupSettingsData(scenario.createId("1"));
		plan.addSignalGroupSettings(settings1);
		settings1.setOnset(this.onset2);
		settings1.setDropping(this.dropping2);
		SignalGroupSettingsData settings2 = control.getFactory().createSignalGroupSettingsData(scenario.createId("2"));
		plan.addSignalGroupSettings(settings2);
		settings2.setOnset(this.onset2);
		settings2.setDropping(this.dropping2);
		SignalGroupSettingsData settings3 = control.getFactory().createSignalGroupSettingsData(scenario.createId("3"));
		plan.addSignalGroupSettings(settings3);
		settings3.setOnset(this.onset1);
		settings3.setDropping(this.dropping1);
		SignalGroupSettingsData settings4 = control.getFactory().createSignalGroupSettingsData(scenario.createId("4"));
		plan.addSignalGroupSettings(settings4);
		settings4.setOnset(this.onset1);
		settings4.setDropping(this.dropping1);
	}

	private void createSystem2Control(Scenario scenario, SignalControlData control) {
		// signal system 2 control
		SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(scenario.createId("2"));
		control.addSignalSystemControllerData(controller);
		controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		SignalPlanData plan = control.getFactory().createSignalPlanData(scenario.createId("1"));
		controller.addSignalPlanData(plan);
		plan.setCycleTime(this.cycle);
		plan.setOffset(0);
		SignalGroupSettingsData settings1 =  control.getFactory().createSignalGroupSettingsData(scenario.createId("1"));
		plan.addSignalGroupSettings(settings1);
		settings1.setOnset(this.onset1);
		settings1.setDropping(this.dropping1);
		SignalGroupSettingsData settings2 = control.getFactory().createSignalGroupSettingsData(scenario.createId("2"));
		plan.addSignalGroupSettings(settings2);
		settings2.setOnset(this.onset1);
		settings2.setDropping(this.dropping1);
		SignalGroupSettingsData settings3 = control.getFactory().createSignalGroupSettingsData(scenario.createId("3"));
		plan.addSignalGroupSettings(settings3);
		settings3.setOnset(this.onset2);
		settings3.setDropping(this.dropping2);
		SignalGroupSettingsData settings4 = control.getFactory().createSignalGroupSettingsData(scenario.createId("4"));
		plan.addSignalGroupSettings(settings4);
		settings4.setOnset(this.onset2);
		settings4.setDropping(this.dropping2);
	}

	private LaneDefinitions createLanes(ScenarioImpl scenario) {
		double laneLenght = 50.0;
		LaneDefinitions lanes = scenario.getLaneDefinitions();
		LaneDefinitionsFactory factory = lanes.getFactory();
		//lanes for link 12
		LanesToLinkAssignment lanesForLink12 = factory.createLanesToLinkAssignment(scenario.createId("12"));
		lanes.addLanesToLinkAssignment(lanesForLink12);
		Lane link12lane1 = factory.createLane(scenario.createId("1"));
		lanesForLink12.addLane(link12lane1);
		link12lane1.addToLinkId(scenario.createId("23"));
		link12lane1.setStartsAtMeterFromLinkEnd(laneLenght);

		Lane link12lane2 = factory.createLane(scenario.createId("2"));
		lanesForLink12.addLane(link12lane2);
		link12lane2.addToLinkId(scenario.createId("27"));
		link12lane2.setStartsAtMeterFromLinkEnd(laneLenght);
		
		//lanes for link 65
		LanesToLinkAssignment lanesForLink65 = factory.createLanesToLinkAssignment(scenario.createId("65"));
		lanes.addLanesToLinkAssignment(lanesForLink65);
		Lane link65lane1 = factory.createLane(scenario.createId("1"));
		lanesForLink65.addLane(link65lane1);
		link65lane1.addToLinkId(scenario.createId("54"));
		link65lane1.setStartsAtMeterFromLinkEnd(laneLenght);

		Lane link65lane2 = factory.createLane(scenario.createId("2"));
		lanesForLink65.addLane(link65lane2);
		link65lane2.addToLinkId(scenario.createId("58"));
		link65lane2.setStartsAtMeterFromLinkEnd(laneLenght);
		
		//convert to 2.0 format and return
		LaneDefinitionsV11ToV20Conversion conversion = new LaneDefinitionsV11ToV20Conversion();
		lanes = conversion.convertTo20(lanes, scenario.getNetwork());
		return lanes;
	}
	
	private void run() {
		Scenario scenario = this.loadScenario();
		
		this.createLanes((ScenarioImpl) scenario);
		
		SignalsData signalsData = scenario.getScenarioElement(SignalsData.class);
		
		this.createGroupsAndSystem2(scenario, signalsData.getSignalSystemsData(), signalsData.getSignalGroupsData());
		this.createGroupsAndSystem5(scenario, signalsData.getSignalSystemsData(), signalsData.getSignalGroupsData());
		this.createSystem2Control(scenario, signalsData.getSignalControlData());
		this.createSystem5Control(scenario, signalsData.getSignalControlData());
		
		File outputDirectory = new File("output/example90TrafficLights/");
		if (! outputDirectory.exists()) {
			outputDirectory.mkdir();
		}
		
		//write to file
		SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
		signalsWriter.setSignalSystemsOutputFilename("output/example90TrafficLights/signal_systems.xml");
		signalsWriter.setSignalGroupsOutputFilename("output/example90TrafficLights/signal_groups.xml");
		signalsWriter.setSignalControlOutputFilename("output/example90TrafficLights/signal_control.xml");
		signalsWriter.writeSignalsData(signalsData);

	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new CreateTrafficSignalScenarioWithLanes().run();
	}


}
