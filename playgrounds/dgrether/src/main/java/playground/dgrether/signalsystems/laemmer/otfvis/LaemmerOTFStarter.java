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
package playground.dgrether.signalsystems.laemmer.otfvis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.contrib.signals.builder.DefaultSignalModelFactory;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.mobsim.QSimSignalEngine;
import org.matsim.contrib.signals.mobsim.SignalEngine;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.signals.model.SignalSystemsManager;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;
import playground.dgrether.signalsystems.DgSensorManager;
import playground.dgrether.signalsystems.laemmer.model.LaemmerSignalModelFactory;
import playground.dgrether.utils.DgOTFVisUtils;


/**
 * @author dgrether
 *
 */
public class LaemmerOTFStarter {

	public void prepare4SimAndPlay(Scenario scenario){
		DgOTFVisUtils.preparePopulation4Simulation(scenario);
		this.playScenario(scenario);
	}
	
	public void playScenario(Scenario scenario){
		EventsManager events = EventsUtils.createEventsManager();

		DgSensorManager sensorManager = new DgSensorManager(scenario.getNetwork());
		sensorManager.setLaneDefinitions((LaneDefinitions20) scenario.getScenarioElement(LaneDefinitions20.ELEMENT_NAME));
		events.addHandler(sensorManager);
		
		DefaultSignalModelFactory defaultSignalModelFactory = new DefaultSignalModelFactory();
		LaemmerSignalModelFactory signalModelFactory = new LaemmerSignalModelFactory(defaultSignalModelFactory, sensorManager);
		
		FromDataBuilder modelBuilder = new FromDataBuilder(scenario, signalModelFactory , events);
		SignalSystemsManager signalManager = modelBuilder.createAndInitializeSignalSystemsManager();
		
		SignalEngine engine = new QSimSignalEngine(signalManager);
		QSim otfVisQSim = (QSim) QSimUtils.createDefaultQSim(scenario, events);
		otfVisQSim.addQueueSimulationListeners(engine);
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, otfVisQSim);
		OTFClientLive.run(scenario.getConfig(), server);
		otfVisQSim.run();
	}

}
