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

import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.contrib.signals.builder.DefaultSignalModelFactory;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.mobsim.QSimSignalEngine;
import org.matsim.contrib.signals.mobsim.SignalEngine;
import org.matsim.signals.model.SignalSystem;
import org.matsim.signals.model.SignalSystemsManager;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
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
		EventsManager events = EventsUtils.createEventsManager();
		String conf = DgFigure9Runner.signalsConfigFileGershenson;
		ScenarioLoaderImpl loader = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(conf);
		ScenarioImpl scenario = (ScenarioImpl) loader.loadScenario();
		ConfigUtils.addOrGetModule(scenario.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setAgentSize(40.0f);
		
		FromDataBuilder modelBuilder = new FromDataBuilder(scenario, new DgGershensonRoederSignalModelFactory(new DefaultSignalModelFactory()) , events);
		SignalSystemsManager signalManager = modelBuilder.createAndInitializeSignalSystemsManager();
		DgSensorManager sensorManager = new DgSensorManager(scenario.getNetwork());
		events.addHandler(sensorManager);
		for (SignalSystem ss : signalManager.getSignalSystems().values()){
			if (ss.getSignalController() instanceof DgRoederGershensonController){
				((DgRoederGershensonController)ss.getSignalController()).setStorageCapFactor(scenario.getConfig().qsim().getStorageCapFactor());
				((DgRoederGershensonController)ss.getSignalController()).initSignalGroupMetadata(scenario.getNetwork(), (LaneDefinitions20) scenario.getScenarioElement(LaneDefinitions20.ELEMENT_NAME));
				((DgRoederGershensonController)ss.getSignalController()).registerAndInitializeSensorManager(sensorManager);
			}
		}

		SignalEngine engine = new QSimSignalEngine(signalManager);
		QSim otfVisQSim = (QSim) new QSimFactory().createMobsim(scenario, events);
		otfVisQSim.addQueueSimulationListeners(engine);
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, otfVisQSim);
		OTFClientLive.run(scenario.getConfig(), server);
		otfVisQSim.run();
	}
	
	public static void main(String[] args) {
		new DgGershensonRoederLiveVisStarter().runCottbus();
	}
}
