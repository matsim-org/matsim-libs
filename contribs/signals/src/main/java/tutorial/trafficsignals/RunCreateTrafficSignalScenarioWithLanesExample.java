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
package tutorial.trafficsignals;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.data.SignalsData;
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
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.LaneDefinitionsV11ToV20Conversion;
import org.matsim.lanes.data.MatsimLaneDefinitionsWriter;
import org.matsim.lanes.data.v11.LaneDefinitions11;
import org.matsim.lanes.data.v11.LaneDefinitions11Impl;
import org.matsim.lanes.data.v11.LaneDefinitionsFactory11;
import org.matsim.lanes.data.v11.LanesToLinkAssignment11;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.utils.LanesUtils;


/**
 * This class contains some examples how to set up a scenario
 * with lanes and signalized intersections.
 * 
 * @author dgrether
 *
 * @see org.matsim.signalsystems
 * @see http://matsim.org/node/384
 *
 */
public class RunCreateTrafficSignalScenarioWithLanesExample {

	
	private static final Logger log = Logger.getLogger(RunCreateTrafficSignalScenarioWithLanesExample.class);
	
	private int onset1 = 0;
	private int dropping1 = 55;
	private int onset2 = 60;
	private int dropping2 = 115;
	private int cycle = 120;

	
	private void createGroupsAndSystem2(Scenario scenario, SignalSystemsData systems, SignalGroupsData groups){
		//signal system 2
		SignalSystemData sys = systems.getFactory().createSignalSystemData(Id.create("2", SignalSystem.class));
		systems.addSignalSystemData(sys);
		SignalSystemsDataFactory factory = systems.getFactory();
		SignalUtils.createAndAddSignal(sys, factory, Id.create("1", Signal.class), 
				Id.create("12", Link.class), Id.create("1", Lane.class));

		SignalUtils.createAndAddSignal(sys, factory, Id.create("2", Signal.class), 
				Id.create("12", Link.class), Id.create("2", Lane.class));

		//create the groups TODO reconsider if this would be better done by utils
		SignalGroupData group4signal = groups.getFactory().createSignalGroupData(sys.getId(), Id.create("1", SignalGroup.class));
		groups.addSignalGroupData(group4signal);
		group4signal.addSignalId(Id.create("1", Signal.class));
		
		group4signal = groups.getFactory().createSignalGroupData(sys.getId(), Id.create("2", SignalGroup.class));
		groups.addSignalGroupData(group4signal);
		group4signal.addSignalId(Id.create("2", Signal.class));

		SignalData signal = systems.getFactory().createSignalData(Id.create("3", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(Id.create("32", Link.class));
		group4signal = groups.getFactory().createSignalGroupData(Id.create("2", SignalSystem.class), Id.create("3", SignalGroup.class));
		groups.addSignalGroupData(group4signal);
//		SignalGroupData group4signal  = groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(Id.create("1"));
		group4signal.addSignalId(Id.create("3", Signal.class));
		signal = systems.getFactory().createSignalData(Id.create("4", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(Id.create("72", Link.class));
		group4signal = groups.getFactory().createSignalGroupData(Id.create("2", SignalSystem.class), Id.create("4", SignalGroup.class));
		groups.addSignalGroupData(group4signal);
//		group4signal  = groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(Id.create("2"));
		group4signal.addSignalId(Id.create("4", Signal.class));
	}
	
	private void createGroupsAndSystem5(Scenario scenario, SignalSystemsData systems, SignalGroupsData groups){
		//signal system 5
		SignalSystemData sys = systems.getFactory().createSignalSystemData(Id.create("5", SignalSystem.class));
		systems.addSignalSystemData(sys);
		SignalSystemsDataFactory factory = systems.getFactory();
		
		SignalUtils.createAndAddSignal(sys, factory, Id.create("1", Signal.class), 
				Id.create("65", Link.class), Id.create("1", Lane.class));

		SignalUtils.createAndAddSignal(sys, factory, Id.create("2", Signal.class), 
				Id.create("65", Link.class), Id.create("2", Lane.class));

		//create the groups
		SignalGroupData group4signal = groups.getFactory().createSignalGroupData(sys.getId(), Id.create("1", SignalGroup.class));
		groups.addSignalGroupData(group4signal);
		group4signal.addSignalId(Id.create("1", Signal.class));
		
		group4signal = groups.getFactory().createSignalGroupData(sys.getId(), Id.create("2", SignalGroup.class));
		groups.addSignalGroupData(group4signal);
		group4signal.addSignalId(Id.create("2", Signal.class));
		//signals 3 and 4
		SignalData signal = systems.getFactory().createSignalData(Id.create("3", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(Id.create("45", Link.class));
		//creates a separate group for signal 3 
		group4signal = groups.getFactory().createSignalGroupData(Id.create("5", SignalSystem.class), Id.create("3", SignalGroup.class));
		groups.addSignalGroupData(group4signal);
		//eventually better: add them to existing group
//		SignalGroupData group4signal = groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(Id.create("1"));
		group4signal.addSignalId(Id.create("3", Signal.class));
		signal = systems.getFactory().createSignalData(Id.create("4", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(Id.create("85", Link.class));
		//creates a separate group for signal 4
		group4signal = groups.getFactory().createSignalGroupData(Id.create("5", SignalSystem.class), Id.create("4", SignalGroup.class));
		groups.addSignalGroupData(group4signal);
		//eventually better: add to existing group
//		group4signal = groups.getSignalGroupDataBySignalSystemId().get(sys.getId()).get(Id.create("2"));
		group4signal.addSignalId(Id.create("4", Signal.class));
	}

	
	
	private void createSystem5Control(Scenario scenario, SignalControlData control) {
		// signal system 5 control
		SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(Id.create("5", SignalSystem.class));
		control.addSignalSystemControllerData(controller);
		controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		SignalPlanData plan = control.getFactory().createSignalPlanData(Id.create("1", SignalPlan.class));
		controller.addSignalPlanData(plan);
		plan.setCycleTime(this.cycle);
		plan.setOffset(0);
		SignalGroupSettingsData settings1 = control.getFactory().createSignalGroupSettingsData(Id.create("1", SignalGroup.class));
		plan.addSignalGroupSettings(settings1);
		settings1.setOnset(this.onset2);
		settings1.setDropping(this.dropping2);
		SignalGroupSettingsData settings2 = control.getFactory().createSignalGroupSettingsData(Id.create("2", SignalGroup.class));
		plan.addSignalGroupSettings(settings2);
		settings2.setOnset(this.onset2);
		settings2.setDropping(this.dropping2);
		SignalGroupSettingsData settings3 = control.getFactory().createSignalGroupSettingsData(Id.create("3", SignalGroup.class));
		plan.addSignalGroupSettings(settings3);
		settings3.setOnset(this.onset1);
		settings3.setDropping(this.dropping1);
		SignalGroupSettingsData settings4 = control.getFactory().createSignalGroupSettingsData(Id.create("4", SignalGroup.class));
		plan.addSignalGroupSettings(settings4);
		settings4.setOnset(this.onset1);
		settings4.setDropping(this.dropping1);
	}

	private void createSystem2Control(Scenario scenario, SignalControlData control) {
		// signal system 2 control
		SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(Id.create("2", SignalSystem.class));
		control.addSignalSystemControllerData(controller);
		controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		SignalPlanData plan = control.getFactory().createSignalPlanData(Id.create("1", SignalPlan.class));
		controller.addSignalPlanData(plan);
		plan.setCycleTime(this.cycle);
		plan.setOffset(0);
		SignalGroupSettingsData settings1 =  control.getFactory().createSignalGroupSettingsData(Id.create("1", SignalGroup.class));
		plan.addSignalGroupSettings(settings1);
		settings1.setOnset(this.onset1);
		settings1.setDropping(this.dropping1);
		SignalGroupSettingsData settings2 = control.getFactory().createSignalGroupSettingsData(Id.create("2", SignalGroup.class));
		plan.addSignalGroupSettings(settings2);
		settings2.setOnset(this.onset1);
		settings2.setDropping(this.dropping1);
		SignalGroupSettingsData settings3 = control.getFactory().createSignalGroupSettingsData(Id.create("3", SignalGroup.class));
		plan.addSignalGroupSettings(settings3);
		settings3.setOnset(this.onset2);
		settings3.setDropping(this.dropping2);
		SignalGroupSettingsData settings4 = control.getFactory().createSignalGroupSettingsData(Id.create("4", SignalGroup.class));
		plan.addSignalGroupSettings(settings4);
		settings4.setOnset(this.onset2);
		settings4.setDropping(this.dropping2);
	}

	private LaneDefinitions20 createLanes(ScenarioImpl scenario) {
		double laneLenght = 150.0;
		LaneDefinitions11 lanes = new LaneDefinitions11Impl();
		LaneDefinitionsFactory11 factory = lanes.getFactory();
		//lanes for link 12
		LanesToLinkAssignment11 lanesForLink12 = factory.createLanesToLinkAssignment(Id.create("12", Link.class));
		lanes.addLanesToLinkAssignment(lanesForLink12);
		LanesUtils.createAndAddLane(lanesForLink12, factory, Id.create("1", Lane.class), 
				laneLenght, 1, Id.create("23", Link.class));

		LanesUtils.createAndAddLane(lanesForLink12, factory, Id.create("2", Lane.class), 
				laneLenght, 1, Id.create("27", Link.class));

		//lanes for link 65
		LanesToLinkAssignment11 lanesForLink65 = factory.createLanesToLinkAssignment(Id.create("65", Link.class));
		lanes.addLanesToLinkAssignment(lanesForLink65);

		LanesUtils.createAndAddLane(lanesForLink65, factory, Id.create("1", Lane.class), 
				laneLenght, 1, Id.create("54", Link.class));

		LanesUtils.createAndAddLane(lanesForLink65, factory, Id.create("2", Lane.class), 
				laneLenght, 1, Id.create("58", Link.class));
		
		//convert to 2.0 format and return
		LaneDefinitionsV11ToV20Conversion conversion = new LaneDefinitionsV11ToV20Conversion();
		LaneDefinitions20 l2 = conversion.convertTo20(lanes, scenario.getNetwork());
		scenario.addScenarioElement( LaneDefinitions20.ELEMENT_NAME , l2);
		return l2;
	}
	

	
	public String run() {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("examples/tutorial/unsupported/example90TrafficLights/network.xml.gz");
		config.plans().setInputFile("examples/tutorial/unsupported/example90TrafficLights/population.xml.gz");
		config.scenario().setUseLanes(true);
		config.scenario().setUseSignalSystems(true);
		config.controler().setMobsim("qsim");
		config.qsim().setNodeOffset(20.0);
		config.qsim().setSnapshotStyle( SnapshotStyle.queue ) ;;
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		this.createLanes((ScenarioImpl) scenario);
		
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		
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

		new MatsimLaneDefinitionsWriter().writeFile20(lanesFile, (LaneDefinitions20) scenario.getScenarioElement(LaneDefinitions20.ELEMENT_NAME));
		
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
		
		log.info("Config of traffic light scenario with lanes is written to " + configFile);
		log.info("Visualize scenario by calling VisTrafficSignalScenarioWithLanes.main() of contrib.otfvis project.");
		return configFile;

	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new RunCreateTrafficSignalScenarioWithLanesExample().run();
	}


}
