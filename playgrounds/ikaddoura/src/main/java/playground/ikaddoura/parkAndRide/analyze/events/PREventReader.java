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
package playground.ikaddoura.parkAndRide.analyze.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.parkAndRide.pR.PRFileReader;
import playground.ikaddoura.parkAndRide.pR.ParkAndRideFacility;

/**
 * @author Ihab
 *
 */
public class PREventReader {
	
	static String prFacilityFile = "/Users/Ihab/ils4/kaddoura/parkAndRide/input/PRfacilities_berlin.txt";
	static String networkFile = "/Users/Ihab/ils4/kaddoura/parkAndRide/input/PRnetwork_berlin.xml";
	static String eventFile = "/Users/Ihab/ils4/kaddoura/parkAndRide/output/berlin_run1_transferPenalty0/ITERS/it.100/100.events.xml.gz";
	
	// output
	static String prTimesFile = "/Users/Ihab/ils4/kaddoura/parkAndRide/output/PRana_times_run1.txt";
	static String prUsersFile = "/Users/Ihab/ils4/kaddoura/parkAndRide/output/PRana_users_run1.txt";
	
	public static void main(String[] args) {
		PREventReader reader = new PREventReader();
		reader.run();
	}
	
	private void run() {

		Scenario scen = ScenarioUtils.createScenario(ConfigUtils.createConfig());	
		Config config = scen.getConfig();
		config.network().setInputFile(networkFile);
		ScenarioUtils.loadScenario(scen);
		
		PRFileReader prFileReader = new PRFileReader(prFacilityFile);
		
		Map<Id, ParkAndRideFacility> id2PRFacility = prFileReader.getId2prFacility();
		
		EventsManager events = EventsUtils.createEventsManager();
		
		PREventHandler linkHandler = new PREventHandler(scen.getNetwork());
		events.addHandler((EventHandler) linkHandler);		
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		
		PRAnalysisWriter prWriter = new PRAnalysisWriter();
		prWriter.writeTimes(linkHandler.getLinkId2prEndTimes(), prTimesFile);
		prWriter.writePRusers(linkHandler.getLinkId2prActs(), id2PRFacility, prUsersFile);
		
	}
}
