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
package tutorial.unsupported.example90TrafficLights;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.signalsystems.SignalUtils;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.model.DefaultPlanbasedSignalSystemController;


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
public class CreateSimpleTrafficSignalScenario {

	public Scenario loadScenario(){
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("examples/tutorial/unsupported/example90TrafficLights/network.xml.gz");
		config.plans().setInputFile("examples/tutorial/unsupported/example90TrafficLights/population.xml.gz");
		config.scenario().setUseSignalSystems(true);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	
	private void createSignalSystemsAndGroups(Scenario scenario, SignalsData signalsData){
		SignalSystemsData systems = signalsData.getSignalSystemsData();
		SignalGroupsData groups = signalsData.getSignalGroupsData();
		
		//signal system 3
		SignalSystemData sys = systems.getFactory().createSignalSystemData(scenario.createId("3"));
		systems.addSignalSystemData(sys);
		SignalData signal = systems.getFactory().createSignalData(scenario.createId("1"));
		sys.addSignalData(signal);
		signal.setLinkId(scenario.createId("23"));
		signal = systems.getFactory().createSignalData(scenario.createId("2"));
		sys.addSignalData(signal);
		signal.setLinkId(scenario.createId("43"));
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
		
		
		//signal system 4
		sys = systems.getFactory().createSignalSystemData(scenario.createId("4"));
		systems.addSignalSystemData(sys);
		signal = systems.getFactory().createSignalData(scenario.createId("1"));
		sys.addSignalData(signal);
		signal.setLinkId(scenario.createId("34"));
		signal = systems.getFactory().createSignalData(scenario.createId("2"));
		sys.addSignalData(signal);
		signal.setLinkId(scenario.createId("54"));
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);

		//signal system 7
		sys = systems.getFactory().createSignalSystemData(scenario.createId("7"));
		systems.addSignalSystemData(sys);
		signal = systems.getFactory().createSignalData(scenario.createId("1"));
		sys.addSignalData(signal);
		signal.setLinkId(scenario.createId("27"));
		signal = systems.getFactory().createSignalData(scenario.createId("2"));
		sys.addSignalData(signal);
		signal.setLinkId(scenario.createId("87"));
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
		
		//signal system 8
		sys = systems.getFactory().createSignalSystemData(scenario.createId("8"));
		systems.addSignalSystemData(sys);
		signal = systems.getFactory().createSignalData(scenario.createId("1"));
		sys.addSignalData(signal);
		signal.setLinkId(scenario.createId("78"));
		signal = systems.getFactory().createSignalData(scenario.createId("2"));
		sys.addSignalData(signal);
		signal.setLinkId(scenario.createId("58"));
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
	}
	
	private SignalControlData createSignalControl(Scenario scenario, SignalsData sd) {
		int cycle = 120;
		SignalControlData control = sd.getSignalControlData();
		
		//signal system 3, 4 control
		List<Id> ids = new LinkedList<Id>();
		ids.add(scenario.createId("3"));
		ids.add(scenario.createId("4"));
		for (Id id : ids){
			SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(id);
			control.addSignalSystemControllerData(controller);
			controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			SignalPlanData plan = control.getFactory().createSignalPlanData(scenario.createId("1"));
			controller.addSignalPlanData(plan);
			plan.setCycleTime(cycle);
			plan.setOffset(0);
			SignalGroupSettingsData settings1 = control.getFactory().createSignalGroupSettingsData(scenario.createId("1"));
			plan.addSignalGroupSettings(settings1);
			settings1.setOnset(0);
			settings1.setDropping(55);
			SignalGroupSettingsData settings2 = control.getFactory().createSignalGroupSettingsData(scenario.createId("2"));
			plan.addSignalGroupSettings(settings2);
			settings2.setOnset(0);
			settings2.setDropping(55);
		}
		// signal system 7, 8 control
		ids.clear();
		ids.add(scenario.createId("7"));
		ids.add(scenario.createId("8"));
		for (Id id : ids){
			SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(id);
			control.addSignalSystemControllerData(controller);
			controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			SignalPlanData plan = control.getFactory().createSignalPlanData(scenario.createId("1"));
			controller.addSignalPlanData(plan);
			plan.setCycleTime(cycle);
			plan.setOffset(0);
			SignalGroupSettingsData settings1 = control.getFactory().createSignalGroupSettingsData(scenario.createId("1"));
			plan.addSignalGroupSettings(settings1);
			settings1.setOnset(0);
			settings1.setDropping(55);
			SignalGroupSettingsData settings2 = control.getFactory().createSignalGroupSettingsData(scenario.createId("2"));
			plan.addSignalGroupSettings(settings2);
			settings2.setOnset(0);
			settings2.setDropping(55);
		}
		return control;
	}
	
	
	private void run() {
		Scenario scenario = this.loadScenario();
		SignalsData signalsData = scenario.getScenarioElement(SignalsData.class);
		this.createSignalSystemsAndGroups(scenario, signalsData);
		this.createSignalControl(scenario, signalsData);
		
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
		new CreateSimpleTrafficSignalScenario().run();
	}


}
