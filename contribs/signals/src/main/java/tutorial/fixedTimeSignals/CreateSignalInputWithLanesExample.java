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
package tutorial.fixedTimeSignals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.controller.fixedTime.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.LanesUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesFactory;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.lanes.LanesWriter;

/**
 * Example for how to create signal input files for a scenario with lanes from code.
 * 
 * @link VisualizeSignalScenarioWithLanes for how to visualize this scenario.
 * 
 * @author dgrether
 * @author tthunig
 */
public class CreateSignalInputWithLanesExample {

	private static final Logger log = Logger.getLogger(CreateSignalInputWithLanesExample.class);
	private static final String INPUT_DIR = "./examples/tutorial/example90TrafficLights/createSignalInput/";
	
	private static final int ONSET1 = 0;
	private static final int DROPPING1 = 55;
	private static final int ONSET2 = 60;
	private static final int DROPPING2 = 115;
	private static final int CYCLE = 120;
	private static final double LANE_LENGTH = 150.0;
	private static final int LANE_CAPACITY = 1800;
	private static final int NO_LANES = 1; // number of represented lanes per lane
	private static final double LINK_LENGTH = 300;

	private void createGroupsAndSystem2(Scenario scenario, SignalSystemsData systems, SignalGroupsData groups) {
		SignalSystemData sys = systems.getFactory().createSignalSystemData(Id.create("2", SignalSystem.class));
		systems.addSignalSystemData(sys);
		SignalSystemsDataFactory factory = systems.getFactory();
		
		SignalUtils.createAndAddSignal(sys, factory, Id.create("1", Signal.class), Id.createLinkId("12"),
				Arrays.asList(Id.create("12.l", Lane.class)));
		SignalUtils.createAndAddSignal(sys, factory, Id.create("2", Signal.class), Id.createLinkId("12"),
				Arrays.asList(Id.create("12.r", Lane.class)));
		SignalUtils.createAndAddSignal(sys, factory, Id.create("3", Signal.class), Id.createLinkId("32"), null);
		SignalUtils.createAndAddSignal(sys, factory, Id.create("4", Signal.class), Id.createLinkId("72"), null);
		
		// create a signal group for every signal
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
	}

	private void createGroupsAndSystem5(Scenario scenario, SignalSystemsData systems, SignalGroupsData groups) {
		SignalSystemData sys = systems.getFactory().createSignalSystemData(Id.create("5", SignalSystem.class));
		systems.addSignalSystemData(sys);
		SignalSystemsDataFactory factory = systems.getFactory();

		SignalUtils.createAndAddSignal(sys, factory, Id.create("1", Signal.class), Id.createLinkId("65"),
				Arrays.asList(Id.create("65.r", Lane.class)));
		SignalUtils.createAndAddSignal(sys, factory, Id.create("2", Signal.class), Id.createLinkId("65"),
				Arrays.asList(Id.create("65.l", Lane.class)));
		SignalUtils.createAndAddSignal(sys, factory, Id.create("3", Signal.class), Id.createLinkId("45"), null);
		SignalUtils.createAndAddSignal(sys, factory, Id.create("4", Signal.class), Id.createLinkId("85"), null);

		// create a signal group for every signal
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
	}
	
	private void createSystemControl(SignalControlData control, Id<SignalSystem> signalSystemId, 
			int onset1, int dropping1, int onset2, int dropping2) {
		SignalControlDataFactory fac = control.getFactory();
		
		// create and add signal control for the given system id
		SignalSystemControllerData controller = 
				fac.createSignalSystemControllerData(signalSystemId);
		control.addSignalSystemControllerData(controller);
		controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		
		// create and add signal plan with defined cycle time and offset 0		
		SignalPlanData plan = SignalUtils.createSignalPlan(fac, CYCLE, 0);
		controller.addSignalPlanData(plan);
		
		// create and add control settings for signal groups
		plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
				Id.create("1", SignalGroup.class), onset1, dropping1));
		plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
				Id.create("2", SignalGroup.class), onset1, dropping1));
		plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
				Id.create("3", SignalGroup.class), onset2, dropping2));
		plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
				Id.create("4", SignalGroup.class), onset2, dropping2));
	}

	private void createLanes(Lanes lanes) {
		LanesFactory factory = lanes.getFactory();
		
		// create lanes for link 12
		LanesToLinkAssignment lanesForLink12 = factory
				.createLanesToLinkAssignment(Id.createLinkId("12"));
		lanes.addLanesToLinkAssignment(lanesForLink12);
		
		// original lane, i.e. lane that starts at the link from node and leads to all other lanes of the link
		LanesUtils.createAndAddLane(lanesForLink12, factory, 
				Id.create("12.ol", Lane.class), LANE_CAPACITY, LINK_LENGTH, 0, NO_LANES, 
				null, Arrays.asList(Id.create("12.l", Lane.class), Id.create("12.r", Lane.class)));
		
		// left turning lane (alignment 1)
		LanesUtils.createAndAddLane(lanesForLink12, factory,
				Id.create("12.l", Lane.class), LANE_CAPACITY, LANE_LENGTH, 1, NO_LANES,
				Collections.singletonList(Id.create("23", Link.class)), null);

		// right turning lane (alignment -1)
		LanesUtils.createAndAddLane(lanesForLink12, factory,
				Id.create("12.r", Lane.class), LANE_CAPACITY, LANE_LENGTH, -1, NO_LANES,
				Collections.singletonList(Id.create("27", Link.class)), null);

		// create lanes for link 65
		LanesToLinkAssignment lanesForLink65 = factory
				.createLanesToLinkAssignment(Id.create("65", Link.class));
		lanes.addLanesToLinkAssignment(lanesForLink65);

		// original lane, i.e. lane that starts at the link from node and leads to all other lanes of the link
		LanesUtils.createAndAddLane(lanesForLink65, factory, 
				Id.create("65.ol", Lane.class), LANE_CAPACITY, LINK_LENGTH, 0, NO_LANES, null,
				Arrays.asList(Id.create("65.l", Lane.class), Id.create("65.r", Lane.class)));		
		
		// right turning lane (alignment -1)
		LanesUtils.createAndAddLane(lanesForLink65, factory,
				Id.create("65.r", Lane.class), LANE_CAPACITY, LANE_LENGTH, -1, NO_LANES,
				Collections.singletonList(Id.create("54", Link.class)), null);

		// left turning lane (alignment 1)
		LanesUtils.createAndAddLane(lanesForLink65, factory,
				Id.create("65.l", Lane.class), LANE_CAPACITY, LANE_LENGTH, 1, NO_LANES,
				Collections.singletonList(Id.create("58", Link.class)), null);
	}

	public void run(String outputDir) throws IOException {		
		// create an empty config
		Config config = ConfigUtils.createConfig();

		// set network and population files
		config.network().setInputFile(INPUT_DIR + "network.xml.gz");
		config.plans().setInputFile(INPUT_DIR + "population.xml.gz");
		
		// enable lanes
		config.qsim().setUseLanes(true);
		
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
		
		// create lanes for the scenario
		this.createLanes(scenario.getLanes());

		/* fill the SignalsData object with information:
		 * signal systems - specify signalized intersections
		 * signal groups - specify signals that always have the same signal control
		 * signal control - specify cycle time, onset and dropping time, offset... for all signal groups */
		this.createGroupsAndSystem2(scenario, signalsData.getSignalSystemsData(), signalsData.getSignalGroupsData());
		this.createGroupsAndSystem5(scenario, signalsData.getSignalSystemsData(), signalsData.getSignalGroupsData());
		this.createSystemControl(signalsData.getSignalControlData(), Id.create("2", SignalSystem.class), 
				ONSET1, DROPPING1, ONSET2, DROPPING2);
		this.createSystemControl(signalsData.getSignalControlData(), Id.create("5", SignalSystem.class), 
				ONSET2, DROPPING2, ONSET1, DROPPING1);

		// create the path to the output directory if it does not exist yet
		Files.createDirectories(Paths.get(outputDir));
		
		// set output filenames
		config.network().setLaneDefinitionsFile(outputDir + "lane_definitions_v2.0.xml");
		signalSystemsConfigGroup.setSignalSystemFile(outputDir + "signal_systems.xml");
		signalSystemsConfigGroup.setSignalGroupsFile(outputDir + "signal_groups.xml");
		signalSystemsConfigGroup.setSignalControlFile(outputDir + "signal_control.xml");

		//write config to file
		String configFile = outputDir + "config.xml";
		ConfigWriter configWriter = new ConfigWriter(config);
		configWriter.write(configFile);
		
		// write lanes to file
		LanesWriter writerDelegate = new LanesWriter(scenario.getLanes());
		writerDelegate.write(config.network().getLaneDefinitionsFile());

		// write signal information to file
		SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
		signalsWriter.setSignalSystemsOutputFilename(signalSystemsConfigGroup.getSignalSystemFile());
		signalsWriter.setSignalGroupsOutputFilename(signalSystemsConfigGroup.getSignalGroupsFile());
		signalsWriter.setSignalControlOutputFilename(signalSystemsConfigGroup.getSignalControlFile());
		signalsWriter.writeSignalsData(scenario);

		log.info("Config of traffic light scenario with lanes is written to " + configFile);
		log.info("Visualize scenario by calling VisTrafficSignalScenarioWithLanes in the same package.");
	}

	public static void main(String[] args) throws IOException {
		new CreateSignalInputWithLanesExample().run("output/example90TrafficLights/");
	}
}
