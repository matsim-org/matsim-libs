/* *********************************************************************** *
 * project: org.matsim.*
 * PREventReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.parkAndRide.analyze;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author Ihab
 *
 */
public class PREventReader {
	
	static String networkFile = "../../shared-svn/studies/ihab/parkAndRide/input/prNetwork.xml";
	static String eventFile = "../../shared-svn/studies/ihab/parkAndRide/output4PRcap20_run3/ITERS/it.100/100.events.xml.gz";

	public static void main(String[] args) {
		PREventReader reader = new PREventReader();
		reader.run();
	}
	
	private void run() {

		Scenario scen = ScenarioUtils.createScenario(ConfigUtils.createConfig());	
		Config config = scen.getConfig();
		config.network().setInputFile(networkFile);
		ScenarioUtils.loadScenario(scen);		
		
		EventsManager events = EventsUtils.createEventsManager();
		
		PREventHandler linkHandler = new PREventHandler(scen.getNetwork());
		events.addHandler((EventHandler) linkHandler);		
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		
		System.out.println(linkHandler.getLinkId2cars().toString());
		System.out.println(linkHandler.getLinkId2prActs().toString());

	}

}
