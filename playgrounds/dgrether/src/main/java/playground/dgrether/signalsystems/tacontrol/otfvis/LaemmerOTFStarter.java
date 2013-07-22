/* *********************************************************************** *
 * project: org.matsim.*
 * LaemmerOTFStarter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.tacontrol.otfvis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.signalsystems.builder.DefaultSignalModelFactory;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.signalsystems.model.SignalSystemsManager;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.dgrether.signalsystems.DgSensorManager;
import playground.dgrether.signalsystems.tacontrol.model.DgTaSignalModelFactory;


/**
 * @author dgrether
 *
 */
public class LaemmerOTFStarter {

	
	public void playScenario(Scenario scenario){
		EventsManager events = EventsUtils.createEventsManager();

		DgSensorManager sensorManager = new DgSensorManager(scenario.getNetwork());
		sensorManager.setLaneDefinitions(scenario.getScenarioElement(LaneDefinitions20.class));
		events.addHandler(sensorManager);
		
		DefaultSignalModelFactory defaultSignalModelFactory = new DefaultSignalModelFactory();
		DgTaSignalModelFactory signalModelFactory = new DgTaSignalModelFactory(defaultSignalModelFactory, sensorManager);
		
		FromDataBuilder modelBuilder = new FromDataBuilder(scenario, signalModelFactory , events);
		SignalSystemsManager signalManager = modelBuilder.createAndInitializeSignalSystemsManager();
		
		SignalEngine engine = new QSimSignalEngine(signalManager);
		QSim otfVisQSim = (QSim) new QSimFactory().createMobsim(scenario, events);
		otfVisQSim.addQueueSimulationListeners(engine);
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, otfVisQSim);
		OTFClientLive.run(scenario.getConfig(), server);
		otfVisQSim.run();
	}

}
