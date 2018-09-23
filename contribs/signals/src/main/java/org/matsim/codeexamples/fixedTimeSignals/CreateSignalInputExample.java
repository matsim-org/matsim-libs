/* *********************************************************************** *
 * project: org.matsim.*
 * CreateSignalSystemScenario
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
package org.matsim.codeexamples.fixedTimeSignals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.controller.fixedTime.DefaultPlanbasedSignalSystemController;
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
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Example for how to create signal input files from code.
 * 
 * See also the picture of the scenario in the same package as the input files
 * (examples/tutorial/example90TrafficLights/).
 * 
 * @link VisualizeSignalScenario for how to visualize this scenario.
 * 
 * @author dgrether
 */
public class CreateSignalInputExample {

	private static final Logger log = Logger.getLogger(CreateSignalInputExample.class);
	private static final String INPUT_DIR = "./examples/tutorial/example90TrafficLights/createSignalInput/";
	
	/**
	 * This method creates the locations of signals, i.e. it specifies signalized intersections.
	 * Furthermore groups for the signals are created that specify which signals will always have the same control.
	 * 
	 * @param systems the so far empty object for information about signalized intersections
	 * @param groups the so far empty object for information about signals that are controlled together as groups
	 */
	private void createSignalSystemsAndGroups(SignalSystemsData systems, SignalGroupsData groups){		
		// create signal system 3 (at node 3)
		SignalSystemData sys = systems.getFactory().createSignalSystemData(Id.create("3", SignalSystem.class));
		// add signal system 3 to the overall signal systems container
		systems.addSignalSystemData(sys);
		// create signal 1
		SignalData signal = systems.getFactory().createSignalData(Id.create("1", Signal.class));
		// add signal 1 to signal system 3, such that it belongs to node 3
		sys.addSignalData(signal);
		// specify the link at which signal 1 is located
		signal.setLinkId(Id.create("23", Link.class));
		// create signal 2
		signal = systems.getFactory().createSignalData(Id.create("2", Signal.class));
		// add signal 2 to signal system 3, such that it also belongs to node 3
		sys.addSignalData(signal);
		// specify the link at which signal 2 is located
		signal.setLinkId(Id.create("43", Link.class));
		// create a single signal group for each signal of system 3, i.e. for signal 1 and 2
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
		
		// do the same with signal system 4 (node 4)
		sys = systems.getFactory().createSignalSystemData(Id.create("4", SignalSystem.class));
		systems.addSignalSystemData(sys);
		signal = systems.getFactory().createSignalData(Id.create("1", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(Id.create("34", Link.class));
		signal = systems.getFactory().createSignalData(Id.create("2", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(Id.create("54", Link.class));
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);

		// signal system 7
		sys = systems.getFactory().createSignalSystemData(Id.create("7", SignalSystem.class));
		systems.addSignalSystemData(sys);
		signal = systems.getFactory().createSignalData(Id.create("1", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(Id.create("27", Link.class));
		signal = systems.getFactory().createSignalData(Id.create("2", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(Id.create("87", Link.class));
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
		
		// signal system 8
		sys = systems.getFactory().createSignalSystemData(Id.create("8", SignalSystem.class));
		systems.addSignalSystemData(sys);
		signal = systems.getFactory().createSignalData(Id.create("1", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(Id.create("78", Link.class));
		signal = systems.getFactory().createSignalData(Id.create("2", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(Id.create("58", Link.class));
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
	}
	
	/**
	 * Create a fixed time traffic signal control for all signal groups in the scenario,
	 * i.e. specify when their signals show green or red.
	 * 
	 * Each signal system (signalized intersection) is equipped with a control,
	 * namely each with the same. The control contains the following information.
	 * - A cylce time of 120 seconds.
	 * - An offset (for green waves) of 0 seconds.
	 * - Each direction gets green for second 0 to 55 within the cycle.
	 * 
	 * @param control the so far empty object for information about when to show green and red
	 */
	private void createSignalControl(SignalControlData control) {
		// specify overall cycle time
		int cycle = 120;
		
		// create signal control for systems 3, 4, 7 and 8
		List<Id<SignalSystem>> ids = new LinkedList<Id<SignalSystem>>();
		ids.add(Id.create("3", SignalSystem.class));
		ids.add(Id.create("4", SignalSystem.class));
		ids.add(Id.create("7", SignalSystem.class));
		ids.add(Id.create("8", SignalSystem.class));
		for (Id<SignalSystem> id : ids){
			// create a signal control for the system
			SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(id);
			// add it to the overall signal control container
			control.addSignalSystemControllerData(controller);
			// declare the control as a fixed time control
			controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			
			/* create a first signal plan for the system control (a signal system control (i.e. an intersection) 
			 * can have different (non-overlapping) plans for different times of the day) */
			SignalPlanData plan1 = control.getFactory().createSignalPlanData(Id.create("1", SignalPlan.class));
			// add the (first) plan to the system control
			controller.addSignalPlanData(plan1);
			// fill the plan with information: cycle time, offset, signal settings
			plan1.setStartTime(0.0);
			/* note: use start and end time as 0.0 if you want to define a signal plan that is valid all day. */
			plan1.setEndTime(6.0*3600);
			plan1.setCycleTime(cycle);
			plan1.setOffset(0);
			SignalGroupSettingsData settings1 = control.getFactory().createSignalGroupSettingsData(Id.create("1", SignalGroup.class));
			plan1.addSignalGroupSettings(settings1);
			settings1.setOnset(0);
			settings1.setDropping(55);
			SignalGroupSettingsData settings2 = control.getFactory().createSignalGroupSettingsData(Id.create("2", SignalGroup.class));
			plan1.addSignalGroupSettings(settings2);
			settings2.setOnset(0);
			settings2.setDropping(55);
			
			// create another signal plan for the system control
			SignalPlanData plan2 = control.getFactory().createSignalPlanData(Id.create("2", SignalPlan.class));
			controller.addSignalPlanData(plan2);
			/* note: end and start time of the two signal plans may be equal but not overlapping */
			plan2.setStartTime(6.0*3600);
			/* note: signal plans of a group do not have to cover the hole day.
			 * when no signal plan is defined for a time period, signals are switched off */
			plan2.setEndTime(18.0 * 3600);
			plan2.setCycleTime(cycle/2);
			plan2.setOffset(0);
			SignalGroupSettingsData settings1_2 = control.getFactory().createSignalGroupSettingsData(Id.create("1", SignalGroup.class));
			plan2.addSignalGroupSettings(settings1_2);
			settings1_2.setOnset(0);
			settings1_2.setDropping(25);
			SignalGroupSettingsData settings2_2 = control.getFactory().createSignalGroupSettingsData(Id.create("2", SignalGroup.class));
			plan2.addSignalGroupSettings(settings2_2);
			settings2_2.setOnset(0);
			settings2_2.setDropping(25);
		}
	}
	
	/**
	 * Set up the config and scenario, create signal information 
	 * and write them to file as input for further simulations.
	 * 
	 * @throws IOException
	 */
	public void run(String outputDir) throws IOException {
		// create an empty config
		Config config = ConfigUtils.createConfig();
		
		// set network and population files
		config.network().setInputFile(INPUT_DIR + "network.xml.gz");
		config.plans().setInputFile(INPUT_DIR + "population.xml.gz");
		
		// add the signal config group to the config file
		SignalSystemsConfigGroup signalSystemsConfigGroup = 
				ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		
		/* the following makes the contrib load the signal input files, but not to do anything with them
		 * (this switch will eventually go away) */
		signalSystemsConfigGroup.setUseSignalSystems(true);
		
		// specify some details for the visualization
		config.qsim().setNodeOffset(20.0);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);
		
		// --- create the scenario
		Scenario scenario = ScenarioUtils.loadScenario(config);
		/* create the information about signals data (i.e. create an empty SignalsData object)
		 * and add it to the scenario as scenario element */
		SignalsData signalsData = SignalUtils.createSignalsData(signalSystemsConfigGroup);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, signalsData);
		
		/* fill the SignalsData object with information:
		 * signal systems - specify signalized intersections
		 * signal groups - specify signals that always have the same signal control
		 * signal control - specify cycle time, onset and dropping time, offset... for all signal groups
		 */
		this.createSignalSystemsAndGroups(signalsData.getSignalSystemsData(), signalsData.getSignalGroupsData());
		this.createSignalControl(signalsData.getSignalControlData());
		
		// create the path to the output directory if it does not exist yet
		Files.createDirectories(Paths.get(outputDir));
		
		// set output filenames
		signalSystemsConfigGroup.setSignalSystemFile(outputDir + "signal_systems.xml");
		signalSystemsConfigGroup.setSignalGroupsFile(outputDir + "signal_groups.xml");
		signalSystemsConfigGroup.setSignalControlFile(outputDir + "signal_control.xml");
		
		//write config to file
		String configFile = outputDir  + "config.xml";
		ConfigWriter configWriter = new ConfigWriter(config);
		configWriter.write(configFile);		
		
		// write signal information to file
		SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
		signalsWriter.setSignalSystemsOutputFilename(signalSystemsConfigGroup.getSignalSystemFile());
		signalsWriter.setSignalGroupsOutputFilename(signalSystemsConfigGroup.getSignalGroupsFile());
		signalsWriter.setSignalControlOutputFilename(signalSystemsConfigGroup.getSignalControlFile());
		signalsWriter.writeSignalsData(scenario);
		
		log.info("Config of simple traffic light scenario is written to " + configFile);
		log.info("Visualize scenario by calling VisSimpleTrafficSignalScenario in the same package.");
	}
	
	public static void main(String[] args) throws IOException {
		new CreateSignalInputExample().run("output/example90TrafficLights/");
	}
}
