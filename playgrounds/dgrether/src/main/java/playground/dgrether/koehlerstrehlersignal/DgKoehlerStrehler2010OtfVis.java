/* *********************************************************************** *
 * project: org.matsim.*
 * DgKoehlerStrehler2010OtfVis
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
package playground.dgrether.koehlerstrehlersignal;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.signalsystems.model.SignalSystemsManager;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;

import playground.dgrether.DgOTFVis;



public class DgKoehlerStrehler2010OtfVis {
	
	private void runFromConfig() {
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
//		String conf = DgKoehlerStrehler2010Runner.signalsConfigSol800;
//		String conf = "/media/data/work/repos/shared-svn/studies/dgrether/koehlerStrehler2010/scenario2/config_signals_coordinated.xml";
		String conf = "/media/data/work/repos/shared-svn/studies/dgrether/koehlerStrehler2010/scenario5/config_signals_coordinated.xml";
		ScenarioLoaderImpl loader = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(conf);
		Scenario scenario = loader.loadScenario();
		scenario.getConfig().otfVis().setAgentSize(40.0f);
		
		FromDataBuilder builder = new FromDataBuilder(scenario.getScenarioElement(SignalsData.class), events);
		SignalSystemsManager manager = builder.createAndInitializeSignalSystemsManager();
		SignalEngine engine = new QSimSignalEngine(manager);
		
		QSim otfVisQSim = new QSim(scenario, events);
		otfVisQSim.addQueueSimulationListeners(engine);
		OTFVisMobsimFeature qSimFeature = new OTFVisMobsimFeature(otfVisQSim);
		otfVisQSim.addFeature(qSimFeature);
		
		DgOTFVis.printClasspath();
		
		QSim client = otfVisQSim;
		client.run();
	}
	
	
	public static void main(String[] args) {
		new DgKoehlerStrehler2010OtfVis().runFromConfig();
	}
}
