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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import playground.ikaddoura.analysis.vtts.VTTSHandler;

/**
 * 
 * Provides the following analysis: 
 * 
 * aggregated results: number of trips, number of stuck trips, travel time, travel distance, toll payments, user benefits, welfare
 * 
 * trip-based information
 * person ; trip no.; leg mode ; stuckAbort (trip) ; VTTS (trip) ; departure time (trip) ; trip arrival time (trip) ; travel time (trip) ; travel distance (trip) ; toll payment (trip)
 * 
 * person-based information
 * person ; total no. of trips (day) ; VTTS (avg. per trip) ; travel time (day) ; travel distance (day) ; toll payments (day)
 * 
 * avg X per Y (X = money payments, ... ; Y = time, distance, ...) 
 * 
 * Important Information:
 * 
 * - No guarantee that monetary payments are ascribed to the right trip (money events, i.e. tolls, may be charged after the person has started the next trip).
 * 
 */
public class PersonTripBasicAnalysisRun {
	private static final Logger log = Logger.getLogger(PersonTripBasicAnalysisRun.class);

	private String runDirectory;
			
	public static void main(String[] args) {
			
		log.info("Searching for run-directory in args at index 0...");
		String runDirectory;
		
		if (args.length > 0) {
			runDirectory = args[0];
			log.info("Run-directory found at index 0.");
			
		} else {
			
			String baiscDirectoryPath = "../../../runs-svn/vickrey-decongestion/output-FINAL/V9/";
						
			runDirectory = baiscDirectoryPath;
			log.info("Could not find run-directory in args. Using the directory " + runDirectory);
		}
		
		PersonTripBasicAnalysisRun analysis = new PersonTripBasicAnalysisRun(runDirectory);
		analysis.run();
	}
	
	public PersonTripBasicAnalysisRun(String runDirectory) {
		
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";
		
		this.runDirectory = runDirectory;
	}

	public void run() {

		String configFile = runDirectory + "output_config.xml.gz";
		String networkFile = "output_network.xml.gz";
		String populationFile = "output_plans.xml.gz";

		Config config = ConfigUtils.loadConfig(configFile);	
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		config.network().setChangeEventsInputFile(null);
		
		int finalIteration = config.controler().getLastIteration();
		String eventsFile = runDirectory + "ITERS/it." + finalIteration + "/" + finalIteration + ".events.xml.gz";
		String outputPath = runDirectory + "ITERS/it." + finalIteration + "/person-trip-analysis/";
				
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		File folder = new File(outputPath);			
		folder.mkdirs();
		
		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(outputPath);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// standard events analysis
		
		BasicPersonTripAnalysisHandler basicHandler = new BasicPersonTripAnalysisHandler();
		basicHandler.setScenario(scenario);

//		VTTSHandler vttsHandler = new VTTSHandler(scenario);
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(basicHandler);
//		events.addHandler(vttsHandler);
		
		log.info("Reading the events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		log.info("Reading the events file... Done.");

//		vttsHandler.computeFinalVTTS();
				
		// plans
		
//		Map<Id<Person>, Double> personId2userBenefit = new HashMap<>();
//		for (Person person : scenario.getPopulation().getPersons().values()) {
//			personId2userBenefit.put(person.getId(), person.getSelectedPlan().getScore() / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney());
//		}
		
		// print the results
		
		PersonTripBasicAnalysis analysis = new PersonTripBasicAnalysis();
				
//		log.info("Print trip information...");
//		analysis.printTripInformation(outputPath, TransportMode.car, basicHandler, vttsHandler);
//		analysis.printTripInformation(outputPath, null, basicHandler, vttsHandler);
//		log.info("Print trip information... Done.");
//
//		log.info("Print person information...");
//		analysis.printPersonInformation(outputPath, TransportMode.car, personId2userBenefit, basicHandler, vttsHandler);	
//		analysis.printPersonInformation(outputPath, null, personId2userBenefit, basicHandler, vttsHandler);	
//		log.info("Print person information... Done.");
		
		SortedMap<Double, List<Double>> departureTime2tolls1 = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2payment(), 3600., 30 * 3600.);
		analysis.printAvgValuePerParameter(outputPath + "tollsPerDepartureTime_car_3600.csv", departureTime2tolls1);
		
		SortedMap<Double, List<Double>> departureTime2tolls2 = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2payment(), 1800., 30 * 3600.);
		analysis.printAvgValuePerParameter(outputPath + "tollsPerDepartureTime_car_1800.csv", departureTime2tolls2);
		
		SortedMap<Double, List<Double>> departureTime2tolls3 = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2payment(), 900., 30 * 3600.);
		analysis.printAvgValuePerParameter(outputPath + "tollsPerDepartureTime_car_900.csv", departureTime2tolls3);
		
		SortedMap<Double, List<Double>> departureTime2tolls4 = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2payment(), 300., 30 * 3600.);
		analysis.printAvgValuePerParameter(outputPath + "tollsPerDepartureTime_car_300.csv", departureTime2tolls4);
		
//		SortedMap<Double, List<Double>> tripDistance2tolls = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2tripDistance(), basicHandler.getPersonId2tripNumber2payment(), 2000., 40 * 1000.);
//		analysis.printAvgValuePerParameter(outputPath + "tollsPerTripDistance_car.csv", tripDistance2tolls);
//		
//		analysis.printAggregatedResults(outputPath, TransportMode.car, personId2userBenefit, basicHandler, vttsHandler);
//		analysis.printAggregatedResults(outputPath, null, personId2userBenefit, basicHandler, vttsHandler);
//		
//		String[] excludedIdPrefixes = {"wv", "lkw"};
//		SortedMap<Double, List<Double>> departureTime2tolls_excluded = analysis.getParameter2Values(TransportMode.car, excludedIdPrefixes, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2payment(), 3600., 30 * 3600.);
//		analysis.printAvgValuePerParameter(outputPath + "tollsPerDepartureTime_car_without-wv-lkw_3600.csv", departureTime2tolls_excluded);
//		
//		SortedMap<Double, List<Double>> departureTime2vtts_excluded = analysis.getParameter2Values(TransportMode.car, excludedIdPrefixes, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), vttsHandler.getPersonId2TripNr2VTTSh(), 3600., 30 * 3600.);
//		analysis.printAvgValuePerParameter(outputPath + "VTTSPerDepartureTime_car_without-wv-lkw_3600.csv", departureTime2vtts_excluded);
//		
//		SortedMap<Double, List<Double>> departureTime2vtts = analysis.getParameter2Values(TransportMode.car, excludedIdPrefixes, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), vttsHandler.getPersonId2TripNr2VTTSh(), 3600., 30 * 3600.);
//		analysis.printAvgValuePerParameter(outputPath + "VTTSPerDepartureTime_car_3600.csv", departureTime2vtts);
//		
//		SortedMap<Double, List<Double>> departureTime2tollPerDistance_excluded = analysis.getTollPerDistancePerTime(TransportMode.car, excludedIdPrefixes, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2payment(), basicHandler.getPersonId2tripNumber2tripDistance(), 3600., 30 * 3600.);
//		analysis.printAvgValuePerParameter(outputPath + "tollsPerDistancePerDepartureTime_car_without-wv-lkw_3600.csv", departureTime2tollPerDistance_excluded);
//		
//		String[] excludedIdPrefixes2 = {"wv", "lkw", "t", "fh"};
//		SortedMap<Double, List<Double>> departureTime2tolls_excluded2 = analysis.getParameter2Values(TransportMode.car, excludedIdPrefixes2, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2payment(), 3600., 30 * 3600.);
//		analysis.printAvgValuePerParameter(outputPath + "tollsPerDepartureTime_car_without-wv-lkw-t-fh_3600.csv", departureTime2tolls_excluded2);
//		
//		SortedMap<Double, List<Double>> departureTime2tollPerDistance_excluded2 = analysis.getTollPerDistancePerTime(TransportMode.car, excludedIdPrefixes2, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2payment(), basicHandler.getPersonId2tripNumber2tripDistance(), 3600., 30 * 3600.);
//		analysis.printAvgValuePerParameter(outputPath + "tollsPerDistancePerDepartureTime_car_without-wv-lkw-t-fh_3600.csv", departureTime2tollPerDistance_excluded2);
				
	}
}
		

