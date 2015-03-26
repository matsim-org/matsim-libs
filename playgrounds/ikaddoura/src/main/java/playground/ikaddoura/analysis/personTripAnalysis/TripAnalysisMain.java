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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class TripAnalysisMain {
	
//	private String runDirectory = "/Users/ihab/Desktop/ils4/kaddoura/bln2/output/noise_int_1a_rndSeed2/";
	private String runDirectory = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalization_noise/output/baseCase/";
	
	private String configFile = runDirectory + "output_config.xml.gz";
	private String populationFile = runDirectory + "output_plans.xml.gz";
	private String networkFile = runDirectory + "output_network.xml.gz";
	
	public static void main(String[] args) {
		TripAnalysisMain analysis = new TripAnalysisMain();
		analysis.run();
	}

	private void run() {
	
		Config config = ConfigUtils.loadConfig(configFile);		
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		
		TripEventHandler tripHandler = new TripEventHandler(scenario);
		events.addHandler(tripHandler);
		
		int iteration = config.controler().getLastIteration();
		String eventsFile = this.runDirectory + "ITERS/it." + iteration + "/" + iteration + ".events.xml.gz";
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		TripWriter tripWriter = new TripWriter(tripHandler, runDirectory);
		tripWriter.writeDetailedResults(TransportMode.car);
		tripWriter.writeAvgTollPerDistance(TransportMode.car);
		tripWriter.writeAvgTollPerTimeBin(TransportMode.car);
		tripWriter.writeAvgTravelTimePerTimeBin(TransportMode.car);
		tripWriter.writePersonId2totalAmount();
	}
			 
}
		

