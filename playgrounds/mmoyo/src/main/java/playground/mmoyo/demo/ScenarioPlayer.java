/* *********************************************************************** *
OTFVisMobsimFeature * project: org.matsim.*
 * ScenarioPlayer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mmoyo.demo;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;

import playground.mmoyo.utils.DataLoader;

public class ScenarioPlayer {

	public static void play(final Scenario scenario) {
		final EventsManager events = EventsUtils.createEventsManager() ;
		EventWriterXML writer = new EventWriterXML(scenario.getConfig().controler().getOutputDirectory() + "/testEvents.xml");
		EventWriterTXT writertxt = new EventWriterTXT(scenario.getConfig().controler().getOutputDirectory() + "/testEvents.txt");
		events.addHandler(writer);
		events.addHandler(writertxt);
		
		final QSim sim = new QSim(scenario, events);
		OTFVisMobsimFeature oTFVisMobsimFeature = new OTFVisMobsimFeature(sim);
		sim.addQueueSimulationListeners(oTFVisMobsimFeature);
		sim.getEventsManager().addHandler(oTFVisMobsimFeature);
		sim.run();
		
		writer.closeFile();
		writertxt.closeFile();
	}

	public static void main(final String[] args) {
		String configFile = null;
		if (args.length==0){
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml"; 
		}else{
			configFile = args[0];
		}
		Scenario scenario = new DataLoader().loadScenario(configFile);
		play(scenario);
	}

}
