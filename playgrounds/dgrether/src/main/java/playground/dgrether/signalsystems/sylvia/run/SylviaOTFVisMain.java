/* *********************************************************************** *
 * project: org.matsim.*
 * SylviaOTFVisMain
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
package playground.dgrether.signalsystems.sylvia.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.contrib.signals.builder.DefaultSignalModelFactory;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.mobsim.QSimSignalEngine;
import org.matsim.contrib.signals.mobsim.SignalEngine;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;
import playground.dgrether.signalsystems.DgSensorManager;
import playground.dgrether.signalsystems.sylvia.controler.DgSylviaConfig;
import playground.dgrether.signalsystems.sylvia.model.DgSylviaSignalModelFactory;
import playground.dgrether.utils.DgOTFVisUtils;



/**
 * @author dgrether
 *
 */
public class SylviaOTFVisMain {

	public void prepare4SimAndPlay(String configFileName){
		Config config = ConfigUtils.loadConfig(configFileName);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		this.prepare4SimAndPlay(scenario);
	}	

	public void playConfig(String configFileName) {
		Config config = ConfigUtils.loadConfig(configFileName);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		this.playScenario(scenario);
	}

	
	public void prepare4SimAndPlay(Scenario scenario){
		DgOTFVisUtils.preparePopulation4Simulation(scenario);
		this.playScenario(scenario);
	}

	public void playScenario(Scenario scenario){
		EventsManager events = EventsUtils.createEventsManager();
//		scenario.getConfig().otfVis().setAgentSize(40.0f);

		DgSensorManager sensorManager = new DgSensorManager(scenario.getNetwork());
		sensorManager.setLaneDefinitions((LaneDefinitions20) scenario.getScenarioElement(LaneDefinitions20.ELEMENT_NAME));
		events.addHandler(sensorManager);
		
		FromDataBuilder modelBuilder = new FromDataBuilder(scenario, 
				new DgSylviaSignalModelFactory(new DefaultSignalModelFactory(), sensorManager, new DgSylviaConfig()) , events);
		SignalSystemsManager signalManager = modelBuilder.createAndInitializeSignalSystemsManager();
		
		
		SignalEngine engine = new QSimSignalEngine(signalManager);
		QSim otfVisQSim = (QSim) QSimUtils.createDefaultQSim(scenario, events);
		otfVisQSim.addQueueSimulationListeners(engine);
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, otfVisQSim);
		OTFClientLive.run(scenario.getConfig(), server);
		otfVisQSim.run();
	}
	
	
	public static void main(String[] args) {
		if (args.length < 1){
			System.out.println("Expecting config as first parameter");
			System.out.println("Optionally use --doPrepare4Sim to call PersonPrepareForSim before playing the config.");
			System.exit(0);
		}
		String configFileName = args[0];
		SylviaOTFVisMain sylviaMain = new SylviaOTFVisMain();
		if (args.length < 2){
			sylviaMain.playConfig(configFileName);
		}
		else if (args.length == 2){
			if (args[1].equalsIgnoreCase("--doPrepare4Sim")){
				sylviaMain.prepare4SimAndPlay(configFileName);
			}
		}
	}


}
