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
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.LaneDefinitionsV11ToV20Conversion;
import org.matsim.lanes.data.MatsimLaneDefinitionsWriter;
import org.matsim.lanes.data.v11.LaneDefinitions;
import org.matsim.lanes.data.v11.LaneDefinitionsFactory;
import org.matsim.lanes.data.v11.LanesToLinkAssignment;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.utils.LanesUtils;
import org.matsim.signalsystems.SignalUtils;
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
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsDataFactory;
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

	
	private void createGroupsAndSystem2(Scenario scenario, SignalSystemsData systems, SignalGroupsData groups){
		//signal system 2
		SignalSystemData sys = systems.getFactory().createSignalSystemData(scenario.createId("2"));
		systems.addSignalSystemData(sys);
		SignalSystemsDataFactory factory = systems.getFactory();
		SignalUtils.createAndAddSignal(sys, factory, scenario.createId("1"), 
				scenario.createId("12"), scenario.createId("1"));

		SignalUtils.createAndAddSignal(sys, factory, scenario.createId("2"), 
				scenario.createId("12"), scenario.createId("2"));

		//create the groups TODO reconsider if this would be better done by utils
		SignalGroupData group4signal = groups.getFactory().createSignalGroupData(sys.getId(), scenario.createId("1"));
		groups.addSignalGroupData(group4signal);
		group4signal.addSignalId(scenario.createId("1"));
		
		group4signal = groups.getFactory().createSignalGroupData(sys.getId(), scenario.createId("2"));
		groups.addSignalGroupData(group4signal);
		group4signal.addSignalId(scenario.createId("2"));

		SignalData signal = systems.getFactory().createSignalData(scenario.createId("3"));
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
		SignalSystemsDataFactory factory = systems.getFactory();
		
		SignalUtils.createAndAddSignal(sys, factory, scenario.createId("1"), 
				scenario.createId("65"), scenario.createId("1"));

		SignalUtils.createAndAddSignal(sys, factory, scenario.createId("2"), 
				scenario.createId("65"), scenario.createId("2"));

		//create the groups
		SignalGroupData group4signal = groups.getFactory().createSignalGroupData(sys.getId(), scenario.createId("1"));
		groups.addSignalGroupData(group4signal);
		group4signal.addSignalId(scenario.createId("1"));
		
		group4signal = groups.getFactory().createSignalGroupData(sys.getId(), scenario.createId("2"));
		groups.addSignalGroupData(group4signal);
		group4signal.addSignalId(scenario.createId("2"));
		//signals 3 and 4
		SignalData signal = systems.getFactory().createSignalData(scenario.createId("3"));
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

	private LaneDefinitions20 createLanes(ScenarioImpl scenario) {
		double laneLenght = 150.0;
		LaneDefinitions lanes = scenario.getLaneDefinitions11();
		LaneDefinitionsFactory factory = lanes.getFactory();
		//lanes for link 12
		LanesToLinkAssignment lanesForLink12 = factory.createLanesToLinkAssignment(scenario.createId("12"));
		lanes.addLanesToLinkAssignment(lanesForLink12);
		LanesUtils.createAndAddLane(lanesForLink12, factory, scenario.createId("1"), 
				laneLenght, 1, scenario.createId("23"));

		LanesUtils.createAndAddLane(lanesForLink12, factory, scenario.createId("2"), 
				laneLenght, 1, scenario.createId("27"));

		//lanes for link 65
		LanesToLinkAssignment lanesForLink65 = factory.createLanesToLinkAssignment(scenario.createId("65"));
		lanes.addLanesToLinkAssignment(lanesForLink65);

		LanesUtils.createAndAddLane(lanesForLink65, factory, scenario.createId("1"), 
				laneLenght, 1, scenario.createId("54"));

		LanesUtils.createAndAddLane(lanesForLink65, factory, scenario.createId("2"), 
				laneLenght, 1, scenario.createId("58"));
		
		//convert to 2.0 format and return
		LaneDefinitionsV11ToV20Conversion conversion = new LaneDefinitionsV11ToV20Conversion();
		LaneDefinitions20 l2 = conversion.convertTo20(lanes, scenario.getNetwork());
		scenario.addScenarioElement(l2);
		return l2;
	}
	

	
	private void run() {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("examples/tutorial/unsupported/example90TrafficLights/network.xml.gz");
		config.plans().setInputFile("examples/tutorial/unsupported/example90TrafficLights/population.xml.gz");
		config.scenario().setUseLanes(true);
		config.scenario().setUseSignalSystems(true);
		config.otfVis().setNodeOffset(20.0);
		config.controler().setMobsim("qsim");
		config.addQSimConfigGroup(new QSimConfigGroup());
		config.getQSimConfigGroup().setSnapshotStyle("queue");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
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
		String configFile = "output/example90TrafficLights/config.xml";
		String lanesFile = "output/example90TrafficLights/lane_definitions_v2.0.xml";
		String signalSystemsFile = "output/example90TrafficLights/signal_systems.xml";
		String signalGroupsFile = "output/example90TrafficLights/signal_groups.xml";
		String signalControlFile = "output/example90TrafficLights/signal_control.xml";

		new MatsimLaneDefinitionsWriter().writeFile20(lanesFile, scenario.getScenarioElement(LaneDefinitions20.class));
		
		SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
		signalsWriter.setSignalSystemsOutputFilename(signalSystemsFile);
		signalsWriter.setSignalGroupsOutputFilename(signalGroupsFile);
		signalsWriter.setSignalControlOutputFilename(signalControlFile);
		signalsWriter.writeSignalsData(signalsData);

		config.network().setLaneDefinitionsFile(lanesFile);
		config.signalSystems().setSignalSystemFile(signalSystemsFile);
		config.signalSystems().setSignalGroupsFile(signalGroupsFile);
		config.signalSystems().setSignalControlFile(signalControlFile);
		ConfigWriter configWriter = new ConfigWriter(config);
		configWriter.write(configFile);
		
		//play
		OTFVis.playConfig(configFile);

	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new CreateTrafficSignalScenarioWithLanes().run();
	}


}
