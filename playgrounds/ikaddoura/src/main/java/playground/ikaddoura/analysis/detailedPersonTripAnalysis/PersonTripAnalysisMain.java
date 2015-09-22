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

package playground.ikaddoura.analysis.detailedPersonTripAnalysis;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.CongestionAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.NoiseAnalysisHandler;
import playground.ikaddoura.analysis.vtts.VTTSHandler;
import playground.ikaddoura.noise2.events.NoiseEventsReader;
import playground.vsp.congestion.events.CongestionEventsReader;

/*
 * 
 * Provides the following analysis: 
 * 
 * aggregated results: number of trips, number of stuck trips, travel time, travel distance, caused/affected congestion, caused/affected noise cost, toll payments, user benefits, welfare
 * 
 * trip-based information
 * person ; trip no.; leg mode ; stuckAbort (trip) ; VTTS (trip) ; departure time (trip) ; trip arrival time (trip) ; travel time (trip) ; travel distance (trip) ; toll payment (trip) ; caused noise cost (trip) ; caused congestion (trip) ; affected congestion (trip)
 * 
 * person-based information
 * person ; total no. of trips (day) ; VTTS (avg. per trip) ; travel time (day) ; travel distance (day) ; toll payments (day) ; caused noise cost (day) ; affected noise cost (day) ; caused congestion (day) ; affected congestion (day)
 * 
 * avg X per Y (X = money payments, ... ; Y = time, distance, ...) 
 * 
 * Important Information:
 * 
 * - No guarantee that monetary payments are ascribed to the right trip (money events, i.e. tolls, may be charged after the person has started the next trip).
 * 
 */
public class PersonTripAnalysisMain {
	private static final Logger log = Logger.getLogger(PersonTripAnalysisMain.class);

	private static String networkFile;
	private static String configFile;
	private static String outputPath;
	private static String populationFile;

	private static String eventsFile;
	
	private static String noiseEventsFile;
	private static String congestionEventsFile;
		
	public static void main(String[] args) {
		
		if (args.length > 0) {
			throw new RuntimeException("Aborting...");
			
		} else {			
			
			String id = "c8";
			
			networkFile = "/Users/ihab/Documents/workspace/runs-svn/c/output/" + id + "/output_network.xml.gz";
			configFile = "/Users/ihab/Documents/workspace/runs-svn/c/output/" + id + "/output_config.xml";
			
			eventsFile = "/Users/ihab/Documents/workspace/runs-svn/c/output/" + id + "/ITERS/it.100/100.events.xml.gz";
			outputPath = "/Users/ihab/Documents/workspace/runs-svn/c/output/" + id + "/ITERS/it.100/detailedAnalysis/";
			populationFile = "/Users/ihab/Documents/workspace/runs-svn/c/output/" + id + "/output_plans.xml.gz";
			
			noiseEventsFile = "/Users/ihab/Documents/workspace/runs-svn/c/output/" + id + "/ITERS/it.100/100.events.xml.gz";
			congestionEventsFile = "/Users/ihab/Documents/workspace/runs-svn/c/output/" + id + "/ITERS/it.100/100.events.xml.gz";
		}
		
		PersonTripAnalysisMain analysis = new PersonTripAnalysisMain();
		analysis.run();
	}

	private void run() {
		
		File folder = new File(outputPath);			
		folder.mkdirs();
		
		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(outputPath);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
				
		Config config = ConfigUtils.loadConfig(configFile);	
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		
		// standard events analysis
		
		BasicPersonTripAnalysisHandler basicHandler = new BasicPersonTripAnalysisHandler(scenario);	
		VTTSHandler vttsHandler = new VTTSHandler(scenario);
		CongestionAnalysisHandler congestionHandler = new CongestionAnalysisHandler(basicHandler);
		NoiseAnalysisHandler noiseHandler = new NoiseAnalysisHandler(basicHandler);
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(basicHandler);
		events.addHandler(vttsHandler);
		events.addHandler(congestionHandler);
//		events.addHandler(noiseHandler);
		
		log.info("Reading the events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		log.info("Reading the events file... Done.");

//		log.info("Reading the events file...");
//		CNEventsReader reader = new CNEventsReader(events);
//		reader.parse(eventsFile);
//		log.info("Reading the events file... Done.");

		vttsHandler.computeFinalVTTS();
				
		// plans
		
		Map<Id<Person>, Double> personId2userBenefit = new HashMap<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			personId2userBenefit.put(person.getId(), person.getSelectedPlan().getScore() / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney());
		}

		// congestion events analysis
		
		if (congestionHandler.isCaughtCongestionEvent()) {
			log.info("Congestion events have already been analyzed based on the standard events file.");
			
		} else {
			EventsManager eventsCongestion = EventsUtils.createEventsManager();
			eventsCongestion.addHandler(congestionHandler);
	
			log.info("Reading the congestion events file...");
			CongestionEventsReader congestionEventsReader = new CongestionEventsReader(eventsCongestion);		
			congestionEventsReader.parse(congestionEventsFile);
			log.info("Reading the congestion events file... Done.");		
		}	
		
		// noise events analysis
	
//		if (noiseHandler.isCaughtNoiseEvent()) {
//			log.info("Noise events have already been analyzed based on the standard events file.");
//		} else {
//			EventsManager eventsNoise = EventsUtils.createEventsManager();
//			eventsNoise.addHandler(noiseHandler);
//					
//			log.info("Reading noise events file...");
//			NoiseEventsReader noiseEventReader = new NoiseEventsReader(eventsNoise);		
//			noiseEventReader.parse(noiseEventsFile);
//			log.info("Reading noise events file... Done.");	
//		}	
		
		// print the results
		
		PersonTripAnalysis analysis = new PersonTripAnalysis();
				
		log.info("Print trip information...");
		analysis.printTripInformation(outputPath, TransportMode.car, basicHandler, vttsHandler, congestionHandler, noiseHandler);
		analysis.printTripInformation(outputPath, null, basicHandler, vttsHandler, congestionHandler, noiseHandler);
		log.info("Print trip information... Done.");

		log.info("Print person information...");
		analysis.printPersonInformation(outputPath, TransportMode.car, personId2userBenefit, basicHandler, vttsHandler, congestionHandler, noiseHandler);	
		analysis.printPersonInformation(outputPath, null, personId2userBenefit, basicHandler, vttsHandler, congestionHandler, noiseHandler);	
		log.info("Print person information... Done.");
		
		SortedMap<Double, List<Double>> departureTime2tolls = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2amount(), 3600., 30 * 3600.);
		analysis.printAvgValuePerParameter(outputPath + "tollsPerDepartureTime_car.csv", departureTime2tolls);
		
		SortedMap<Double, List<Double>> tripDistance2tolls = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2tripDistance(), basicHandler.getPersonId2tripNumber2amount(), 2000., 40 * 1000.);
		analysis.printAvgValuePerParameter(outputPath + "tollsPerTripDistance_car.csv", tripDistance2tolls);
		
		analysis.printAggregatedResults(outputPath, TransportMode.car, personId2userBenefit, basicHandler, vttsHandler, congestionHandler, noiseHandler);
		analysis.printAggregatedResults(outputPath, null, personId2userBenefit, basicHandler, vttsHandler, congestionHandler, noiseHandler);
	}
}
		

