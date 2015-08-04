/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis.personTripAnalysis;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;


/*
 * 
 * trip-based analysis
 * person ; trip no.; VTTS (trip) ; departure time (trip) ; trip arrival time (trip) ; travel time (trip) ; toll payment (trip) ; caused noise cost (trip) ; caused congestion cost (trip) ; affected congestion cost (trip)
 * 
 * person-based analysis
 * person ; total no. of trips (day) ; VTTS (avg. per trip) ; travel time (day); toll payments (day) ; caused noise cost (day) ; affected noise cost (day) ; caused congestion cost (day) ; affected congestion cost (day)
 * 
 * aggregated analysis
 * total travel time: XXX
 * total congestion cost: XXX
 * total noise damages: XXX
 * total travel related user benefits: XXX
 * total toll revenues: XXX
 * system welfare: XXX
 * 
 * time-specific analysis
 * time ; avg. toll (per time) ; avg. travel time (per time)
 * 
 */
public class PersonTripAnalysisMain {
	private static final Logger log = Logger.getLogger(PersonTripAnalysisMain.class);

	// Provide the run directory and the iteration number.
	private static String runDirectory;
	private static int iteration;
	
	public static void main(String[] args) {
		
		if (args.length > 0) {
			runDirectory = args[0];		
			log.info("run directory: " + runDirectory);
			
			iteration = Integer.valueOf(args[1]);		
			log.info("iteration number: " + iteration);
			
		} else {
			runDirectory = "/Users/ihab/Documents/workspace/runs-svn/berlin_equal_vs_different_VTTS/output/internalization_differentVTTS/";
			iteration = 100;
		}
		
		PersonTripAnalysisMain analysis = new PersonTripAnalysisMain();
		analysis.run();
	}

	private void run() {
		
		String populationFile = runDirectory + "output_plans.xml.gz";
		String networkFile = runDirectory + "output_network.xml.gz";
		
		Config config = ConfigUtils.createConfig();		
		config.plans().setInputFile(populationFile);
		config.plans().setInputFile(null);
		config.network().setInputFile(networkFile);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		
		TripEventHandler tripHandler = new TripEventHandler(scenario);
		events.addHandler(tripHandler);
				
		String eventsFile = runDirectory + "ITERS/it." + iteration + "/" + iteration + ".events.xml.gz";
		
		log.info("Reading the event file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		log.info("Reading the event file... Done.");
				
	}
			 
}
		

