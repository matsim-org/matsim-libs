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

import org.matsim.api.core.v01.Id;
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
	
	static String networkFile = "../../shared-svn/studies/ihab/parkAndRide/input/testScenario_PRnetwork_2.xml";
	static String eventFile = "../../shared-svn/studies/ihab/parkAndRide/output/testScenario_test6/ITERS/it.1000/1000.events.xml.gz";
//	static String prTimesFile = "../../shared-svn/studies/ihab/parkAndRide/output/prTimes.txt";
	
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
		
		int prUsers = 0;
		System.out.println();
		System.out.println("************************");
		for (Id id : linkHandler.getLinkId2prActs().keySet()){
			System.out.println(id + ": " + (int) (linkHandler.getLinkId2prActs().get(id)/2.0) + " Users");
			prUsers = prUsers + (int) (linkHandler.getLinkId2prActs().get(id)/2.0);
		}
		System.out.println("************************");		
		System.out.println("PR-Users: " + prUsers);
		System.out.println("Pt-Users: " + (int) (linkHandler.getPtLegs()/2.0));
		System.out.println("Only-Car-Legs: " + (int) (linkHandler.getCarLegs()/2.0 - prUsers));
		System.out.println("Stucking Agents: " + linkHandler.getStuckEvents());
		System.out.println("************************");
		
//		PRTimesWriter prTimesWriter = new PRTimesWriter();
//		prTimesWriter.write(linkHandler.getLinkId2prEndTimes(), prTimesFile);
	}

}
