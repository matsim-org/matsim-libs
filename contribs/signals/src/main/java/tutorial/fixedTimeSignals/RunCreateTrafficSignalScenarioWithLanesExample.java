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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.data.signalgroups.v20.*;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.model.*;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.v11.*;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;

import java.io.File;
import java.util.Arrays;

/**
 * This class contains some examples how to set up a scenario with lanes and
 * signalized intersections.
 * 
 * @author dgrether
 * @author tthunig
 * 
 * @see org.matsim.signalsystems
 *
 */
public class RunCreateTrafficSignalScenarioWithLanesExample {

	private static final Logger log = Logger
			.getLogger(RunCreateTrafficSignalScenarioWithLanesExample.class);
	
	private static final String INPUT_DIR = "./examples/tutorial/example90TrafficLights/";

	private static final String OUTPUT_DIR = "output/example90TrafficLights/";

	private int onset1 = 0;
	private int dropping1 = 55;
	private int onset2 = 60;
	private int dropping2 = 115;
	private int cycle = 120;

	private void createGroupsAndSystem2(Scenario scenario,
			SignalSystemsData systems, SignalGroupsData groups) {
		
		// create signal system 2
		SignalSystemData sys = systems.getFactory().createSignalSystemData(
				Id.create("2", SignalSystem.class));
		systems.addSignalSystemData(sys);
		SignalSystemsDataFactory factory = systems.getFactory();
		
		SignalUtils.createAndAddSignal(sys, factory,
				Id.create("1", Signal.class), Id.createLinkId("12"),
				Arrays.asList(Id.create("1", Lane.class)));

		SignalUtils.createAndAddSignal(sys, factory,
				Id.create("2", Signal.class), Id.createLinkId("12"),
				Arrays.asList(Id.create("2", Lane.class)));

		SignalUtils.createAndAddSignal(sys, factory,
				Id.create("3", Signal.class), Id.createLinkId("32"), null);

		SignalUtils.createAndAddSignal(sys, factory,
				Id.create("4", Signal.class), Id.createLinkId("72"), null);
		
		// create a signal group for every signal
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
	}

	private void createGroupsAndSystem5(Scenario scenario,
			SignalSystemsData systems, SignalGroupsData groups) {
		
		// create signal system 5
		SignalSystemData sys = systems.getFactory().createSignalSystemData(
				Id.create("5", SignalSystem.class));
		systems.addSignalSystemData(sys);
		SignalSystemsDataFactory factory = systems.getFactory();

		SignalUtils.createAndAddSignal(sys, factory,
				Id.create("1", Signal.class), Id.createLinkId("65"),
				Arrays.asList(Id.create("1", Lane.class)));

		SignalUtils.createAndAddSignal(sys, factory,
				Id.create("2", Signal.class), Id.createLinkId("65"),
				Arrays.asList(Id.create("2", Lane.class)));

		SignalUtils.createAndAddSignal(sys, factory,
				Id.create("3", Signal.class), Id.createLinkId("45"), null);

		SignalUtils.createAndAddSignal(sys, factory,
				Id.create("4", Signal.class), Id.createLinkId("85"), null);

		// create a signal group for every signal
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
	}

	private void createSystem5Control(Scenario scenario,
			SignalControlData control) {
		
		SignalControlDataFactory fac = control.getFactory();
		
		// create and add signal control for system 5
		SignalSystemControllerData controller = fac.
				createSignalSystemControllerData(
						Id.create("5", SignalSystem.class));
		control.addSignalSystemControllerData(controller);
		controller.setControllerIdentifier(
				DefaultPlanbasedSignalSystemController.IDENTIFIER);
		
		// create and add signal plan with defined cycle time and offset 0		
		SignalPlanData plan = SignalUtils.createSignalPlan(fac, this.cycle, 0);
		controller.addSignalPlanData(plan);
		
		// create and add control settings for signal groups
		plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(
				fac, Id.create("1", SignalGroup.class), this.onset2, this.dropping2));
		plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(
				fac, Id.create("2", SignalGroup.class), this.onset2, this.dropping2));
		plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(
				fac, Id.create("3", SignalGroup.class), this.onset1, this.dropping1));
		plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(
				fac, Id.create("4", SignalGroup.class), this.onset1, this.dropping1));
	}

	private void createSystem2Control(Scenario scenario,
			SignalControlData control) {
		
		SignalControlDataFactory fac = control.getFactory();
		
		// create and add signal control for system 2
		SignalSystemControllerData controller = control.getFactory()
				.createSignalSystemControllerData(
						Id.create("2", SignalSystem.class));
		control.addSignalSystemControllerData(controller);
		controller.setControllerIdentifier(
				DefaultPlanbasedSignalSystemController.IDENTIFIER);
		
		// create and add signal plan with defined cycle time and offset 0		
		SignalPlanData plan = SignalUtils.createSignalPlan(fac, this.cycle, 0);
		controller.addSignalPlanData(plan);
		
		// create and add control settings for signal groups
		plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
				Id.create("1", SignalGroup.class), this.onset1, this.dropping1));
		plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
				Id.create("2", SignalGroup.class), this.onset1, this.dropping1));
		plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
				Id.create("3", SignalGroup.class), this.onset2, this.dropping2));
		plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
				Id.create("4", SignalGroup.class), this.onset2, this.dropping2));
	}

	private void createLanes(MutableScenario scenario) {
		double laneLenght = 150.0;
		LaneDefinitions11 lanes = new LaneDefinitions11Impl();
		LaneDefinitionsFactory11 factory = lanes.getFactory();
		//lanes for link 12
		LanesToLinkAssignment11 lanesForLink12 = factory
				.createLanesToLinkAssignment(Id.create("12", Link.class));
		lanes.addLanesToLinkAssignment(lanesForLink12);
		LanesUtils11.createAndAddLane11(lanesForLink12, factory,
				Id.create("1", Lane.class), laneLenght, 1,
				Id.create("23", Link.class));

		LanesUtils11.createAndAddLane11(lanesForLink12, factory,
				Id.create("2", Lane.class), laneLenght, 1,
				Id.create("27", Link.class));

		// lanes for link 65
		LanesToLinkAssignment11 lanesForLink65 = factory
				.createLanesToLinkAssignment(Id.create("65", Link.class));
		lanes.addLanesToLinkAssignment(lanesForLink65);

		LanesUtils11.createAndAddLane11(lanesForLink65, factory,
				Id.create("1", Lane.class), laneLenght, 1,
				Id.create("54", Link.class));

		LanesUtils11.createAndAddLane11(lanesForLink65, factory,
				Id.create("2", Lane.class), laneLenght, 1,
				Id.create("58", Link.class));

		LaneDefinitionsV11ToV20Conversion.convertTo20(lanes,
				scenario.getLanes(), scenario.getNetwork());
	}

	public String run() {
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(INPUT_DIR + "network.xml.gz");
		config.plans().setInputFile(INPUT_DIR + "population.xml.gz");
		config.qsim().setUseLanes(true);
		SignalSystemsConfigGroup signalSystemsConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		signalSystemsConfigGroup.setUseSignalSystems(true);
		config.controler().setMobsim("qsim");
		config.qsim().setNodeOffset(20.0);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);

		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsScenarioLoader(signalSystemsConfigGroup).loadSignalsData());

		this.createLanes((MutableScenario) scenario);

		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);

		this.createGroupsAndSystem2(scenario, signalsData.getSignalSystemsData(), signalsData.getSignalGroupsData());
		this.createGroupsAndSystem5(scenario, signalsData.getSignalSystemsData(), signalsData.getSignalGroupsData());
		this.createSystem2Control(scenario, signalsData.getSignalControlData());
		this.createSystem5Control(scenario, signalsData.getSignalControlData());

		File outputDirectory = new File(OUTPUT_DIR);
		if (!outputDirectory.exists()) {
			outputDirectory.mkdir();
		}

		config.network().setLaneDefinitionsFile(OUTPUT_DIR + "lane_definitions_v2.0.xml");
		signalSystemsConfigGroup.setSignalSystemFile(OUTPUT_DIR + "signal_systems.xml");
		signalSystemsConfigGroup.setSignalGroupsFile(OUTPUT_DIR + "signal_groups.xml");
		signalSystemsConfigGroup.setSignalControlFile(OUTPUT_DIR + "signal_control.xml");

		// write to file
		String configFile = OUTPUT_DIR + "config.xml";
		ConfigWriter configWriter = new ConfigWriter(config);
		configWriter.write(configFile);
		
		LaneDefinitionsWriter20 writerDelegate = new LaneDefinitionsWriter20(scenario.getLanes());
		writerDelegate.write(config.network().getLaneDefinitionsFile());

		SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
		signalsWriter.setSignalSystemsOutputFilename(signalSystemsConfigGroup.getSignalSystemFile());
		signalsWriter.setSignalGroupsOutputFilename(signalSystemsConfigGroup.getSignalGroupsFile());
		signalsWriter.setSignalControlOutputFilename(signalSystemsConfigGroup.getSignalControlFile());
		signalsWriter.writeSignalsData(scenario);

		log.info("Config of traffic light scenario with lanes is written to " + configFile);
		log.info("Visualize scenario by calling VisTrafficSignalScenarioWithLanes in the same package.");
		return configFile;

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new RunCreateTrafficSignalScenarioWithLanesExample().run();
	}

}
