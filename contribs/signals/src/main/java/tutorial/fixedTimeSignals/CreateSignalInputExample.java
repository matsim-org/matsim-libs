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
package tutorial.fixedTimeSignals;

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
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Example for how to create signal input files from code.
 * 
 * @link VisualizeSignalScenario for how to visualize this scenario.
 * 
 * @author dgrether
 */
public class CreateSignalInputExample {

	private static final Logger log = Logger.getLogger(CreateSignalInputExample.class);
	private static final String INPUT_DIR = "./examples/tutorial/example90TrafficLights/createSignalInput/";
	private static final String OUTPUT_DIR = "output/example90TrafficLights/";
	
	private void createSignalSystemsAndGroups(Scenario scenario, SignalSystemsData systems, SignalGroupsData groups){		
		//signal system 3
		SignalSystemData sys = systems.getFactory().createSignalSystemData(Id.create("3", SignalSystem.class));
		systems.addSignalSystemData(sys);
		SignalData signal = systems.getFactory().createSignalData(Id.create("1", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(Id.create("23", Link.class));
		signal = systems.getFactory().createSignalData(Id.create("2", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(Id.create("43", Link.class));
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
		
		//signal system 4
		sys = systems.getFactory().createSignalSystemData(Id.create("4", SignalSystem.class));
		systems.addSignalSystemData(sys);
		signal = systems.getFactory().createSignalData(Id.create("1", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(Id.create("34", Link.class));
		signal = systems.getFactory().createSignalData(Id.create("2", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(Id.create("54", Link.class));
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);

		//signal system 7
		sys = systems.getFactory().createSignalSystemData(Id.create("7", SignalSystem.class));
		systems.addSignalSystemData(sys);
		signal = systems.getFactory().createSignalData(Id.create("1", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(Id.create("27", Link.class));
		signal = systems.getFactory().createSignalData(Id.create("2", Signal.class));
		sys.addSignalData(signal);
		signal.setLinkId(Id.create("87", Link.class));
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
		
		//signal system 8
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
	
	private SignalControlData createSignalControl(Scenario scenario, SignalControlData control) {
		int cycle = 120;
		
		//signal system 3, 4 control
		List<Id<SignalSystem>> ids = new LinkedList<Id<SignalSystem>>();
		ids.add(Id.create("3", SignalSystem.class));
		ids.add(Id.create("4", SignalSystem.class));
		for (Id<SignalSystem> id : ids){
			SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(id);
			control.addSignalSystemControllerData(controller);
			controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			SignalPlanData plan = control.getFactory().createSignalPlanData(Id.create("1", SignalPlan.class));
			controller.addSignalPlanData(plan);
			plan.setCycleTime(cycle);
			plan.setOffset(0);
			SignalGroupSettingsData settings1 = control.getFactory().createSignalGroupSettingsData(Id.create("1", SignalGroup.class));
			plan.addSignalGroupSettings(settings1);
			settings1.setOnset(0);
			settings1.setDropping(55);
			SignalGroupSettingsData settings2 = control.getFactory().createSignalGroupSettingsData(Id.create("2", SignalGroup.class));
			plan.addSignalGroupSettings(settings2);
			settings2.setOnset(0);
			settings2.setDropping(55);
		}
		// signal system 7, 8 control
		ids.clear();
		ids.add(Id.create("7", SignalSystem.class));
		ids.add(Id.create("8", SignalSystem.class));
		for (Id<SignalSystem> id : ids) {
			SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(id);
			control.addSignalSystemControllerData(controller);
			controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			SignalPlanData plan = control.getFactory().createSignalPlanData(Id.create("1", SignalPlan.class));
			controller.addSignalPlanData(plan);
			plan.setCycleTime(cycle);
			plan.setOffset(0);
			SignalGroupSettingsData settings1 = control.getFactory().createSignalGroupSettingsData(Id.create("1", SignalGroup.class));
			plan.addSignalGroupSettings(settings1);
			settings1.setOnset(0);
			settings1.setDropping(55);
			SignalGroupSettingsData settings2 = control.getFactory().createSignalGroupSettingsData(Id.create("2", SignalGroup.class));
			plan.addSignalGroupSettings(settings2);
			settings2.setOnset(0);
			settings2.setDropping(55);
		}
		return control;
	}
	
	public String run() throws IOException {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(INPUT_DIR + "network.xml.gz");
		config.plans().setInputFile(INPUT_DIR + "population.xml.gz");
		SignalSystemsConfigGroup signalSystemsConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		signalSystemsConfigGroup.setUseSignalSystems(true);
		config.qsim().setNodeOffset(20.0);
		config.controler().setMobsim("qsim");
		config.qsim().setSnapshotStyle( SnapshotStyle.queue ) ;;
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		SignalsData signalsData = SignalUtils.createSignalsData(signalSystemsConfigGroup);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, signalsData);
		
		this.createSignalSystemsAndGroups(scenario, signalsData.getSignalSystemsData(), signalsData.getSignalGroupsData());
		this.createSignalControl(scenario, signalsData.getSignalControlData());
		
		Files.createDirectories(Paths.get(OUTPUT_DIR));
		
		signalSystemsConfigGroup.setSignalSystemFile(OUTPUT_DIR  + "signal_systems.xml");
		signalSystemsConfigGroup.setSignalGroupsFile(OUTPUT_DIR  + "signal_groups.xml");
		signalSystemsConfigGroup.setSignalControlFile(OUTPUT_DIR  + "signal_control.xml");
		
		//write to file
		String configFile = OUTPUT_DIR  + "config.xml";
		ConfigWriter configWriter = new ConfigWriter(config);
		configWriter.write(configFile);		
		
		SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
		signalsWriter.setSignalSystemsOutputFilename(signalSystemsConfigGroup.getSignalSystemFile());
		signalsWriter.setSignalGroupsOutputFilename(signalSystemsConfigGroup.getSignalGroupsFile());
		signalsWriter.setSignalControlOutputFilename(signalSystemsConfigGroup.getSignalControlFile());
		signalsWriter.writeSignalsData(scenario);
		
		log.info("Config of simple traffic light scenario is written to " + configFile);
		log.info("Visualize scenario by calling VisSimpleTrafficSignalScenario in the same package.");
		return configFile;
	}
	
	public static void main(String[] args) throws IOException {
		new CreateSignalInputExample().run();
	}
}
