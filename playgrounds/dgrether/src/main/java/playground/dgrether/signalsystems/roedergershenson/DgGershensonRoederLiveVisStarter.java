/* *********************************************************************** *
 * project: org.matsim.*
 * DgGershensonRoederLiveVisStarter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.roedergershenson;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.run.OTFVis;
import org.matsim.signalsystems.builder.DefaultSignalModelFactory;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.signalsystems.model.SignalSystem;
import org.matsim.signalsystems.model.SignalSystemsManager;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.dgrether.koehlerstrehlersignal.figure9scenario.DgFigure9Runner;
import playground.dgrether.signalsystems.DgSensorManager;


/**
 * @author dgrether
 *
 */
public class DgGershensonRoederLiveVisStarter {

	public DgGershensonRoederLiveVisStarter(){}
	
	public void runCottbus(){
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		String conf = DgFigure9Runner.signalsConfigFileGershenson;
		ScenarioLoaderImpl loader = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(conf);
		ScenarioImpl scenario = (ScenarioImpl) loader.loadScenario();
		scenario.getConfig().otfVis().setAgentSize(40.0f);
		SignalsData signalsData = scenario.getScenarioElement(SignalsData.class);
		
		FromDataBuilder modelBuilder = new FromDataBuilder(signalsData, new DgGershensonRoederSignalModelFactory(new DefaultSignalModelFactory()) , events);
		SignalSystemsManager signalManager = modelBuilder.createAndInitializeSignalSystemsManager();
		DgSensorManager sensorManager = new DgSensorManager(scenario.getNetwork());
		events.addHandler(sensorManager);
		for (SignalSystem ss : signalManager.getSignalSystems().values()){
			if (ss.getSignalController() instanceof DgRoederGershensonController){
				((DgRoederGershensonController)ss.getSignalController()).setStorageCapFactor(scenario.getConfig().getQSimConfigGroup().getStorageCapFactor());
				((DgRoederGershensonController)ss.getSignalController()).initSignalGroupMetadata(scenario.getNetwork(), scenario.getLaneDefinitions());
				((DgRoederGershensonController)ss.getSignalController()).registerAndInitializeSensorManager(sensorManager);
			}
		}

		SignalEngine engine = new QSimSignalEngine(signalManager);
		QSim otfVisQSim = new QSim(scenario, events);
		otfVisQSim.addQueueSimulationListeners(engine);
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, otfVisQSim);
		OTFClientLive.run(scenario.getConfig(), server);
		otfVisQSim.run();
	}
	
	public static void main(String[] args) {
		new DgGershensonRoederLiveVisStarter().runCottbus();
	}
}
