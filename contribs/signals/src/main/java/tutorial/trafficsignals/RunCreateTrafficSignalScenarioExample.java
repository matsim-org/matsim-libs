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
package tutorial.trafficsignals;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;


/**
 * This class contains some simple examples how to set up a simple scenario
 * with signalized intersections.
 * 
 * @author dgrether
 *
 * @see org.matsim.signalsystems
 * @see http://matsim.org/node/384
 *
 */
public class RunCreateTrafficSignalScenarioExample {

	
	private static final Logger log = Logger.getLogger(RunCreateTrafficSignalScenarioExample.class);

	
	private void createSignalSystemsAndGroups(Scenario scenario, SignalsData signalsData){
		SignalSystemsData systems = signalsData.getSignalSystemsData();
		SignalGroupsData groups = signalsData.getSignalGroupsData();
		
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
	
	private SignalControlData createSignalControl(Scenario scenario, SignalsData sd) {
		int cycle = 120;
		SignalControlData control = sd.getSignalControlData();
		
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
	

	
	
	public String run() {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("examples/tutorial/unsupported/example90TrafficLights/network.xml.gz");
		config.plans().setInputFile("examples/tutorial/unsupported/example90TrafficLights/population.xml.gz");
		config.scenario().setUseSignalSystems(true);
		config.qsim().setNodeOffset(20.0);
		config.controler().setMobsim("qsim");
		config.qsim().setSnapshotStyle("queue");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsScenarioLoader(config.signalSystems()).loadSignalsData());
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		this.createSignalSystemsAndGroups(scenario, signalsData);
		this.createSignalControl(scenario, signalsData);
		
		File outputDirectory = new File("output/example90TrafficLights/");
		if (! outputDirectory.exists()) {
			outputDirectory.mkdir();
		}
		
		//write to file
		String configFile = "output/example90TrafficLights/config.xml";
		String signalSystemsFile = "output/example90TrafficLights/signal_systems.xml";
		String signalGroupsFile = "output/example90TrafficLights/signal_groups.xml";
		String signalControlFile = "output/example90TrafficLights/signal_control.xml";

		
		SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
		signalsWriter.setSignalSystemsOutputFilename(signalSystemsFile);
		signalsWriter.setSignalGroupsOutputFilename(signalGroupsFile);
		signalsWriter.setSignalControlOutputFilename(signalControlFile);
		signalsWriter.writeSignalsData(signalsData);

		config.signalSystems().setSignalSystemFile(signalSystemsFile);
		config.signalSystems().setSignalGroupsFile(signalGroupsFile);
		config.signalSystems().setSignalControlFile(signalControlFile);
		ConfigWriter configWriter = new ConfigWriter(config);
		configWriter.write(configFile);
		
		log.info("Config of simple traffic light scenario is written to " + configFile);
		log.info("Visualize scenario by calling VisSimpleTrafficSignalScenario.main() of contrib.otfvis project.");
		return configFile;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new RunCreateTrafficSignalScenarioExample().run();
	}


}
