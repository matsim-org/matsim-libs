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
import org.matsim.contrib.noise.events.NoiseEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.NoiseAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.PersonMoneyLinkHandler;

/**
 * 
 * Provides the following analysis: 
 * 
 * aggregated results: number of trips, number of stuck trips, travel time, travel distance, caused/affected noise cost, toll payments, user benefits, welfare
 * 
 * trip-based information
 * person ; trip no.; leg mode ; stuckAbort (trip) ; departure time (trip) ; trip arrival time (trip) ; travel time (trip) ; travel distance (trip) ; toll payment (trip)
 * 
 * person-based information
 * person ; total no. of trips (day) ; travel time (day) ; travel distance (day) ; toll payments (day) ; affected noise cost (day)
 * 
 * 
 */
public class PersonTripNoiseAnalysisRun {
	private static final Logger log = Logger.getLogger(PersonTripNoiseAnalysisRun.class);

	private final String runDirectory;
	private final String noiseEventFile;
			
	public static void main(String[] args) {
			
		String runDirectory;
		
		if (args.length > 0) {
			runDirectory = args[0];
			log.info("Run directory: " + runDirectory);
		
		} else {
			
			runDirectory = "/Users/ihab/Documents/workspace/runs-svn/cne/munich/output-final/output_run4b_muc_cne_DecongestionPID";
			log.info("Run directory " + runDirectory);
		}
		
		PersonTripNoiseAnalysisRun analysis = new PersonTripNoiseAnalysisRun(runDirectory);
		analysis.run();
	}
	
	public PersonTripNoiseAnalysisRun(String runDirectory) {
		
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";
		
		this.runDirectory = runDirectory;
		this.noiseEventFile = null;
	}
	
	public PersonTripNoiseAnalysisRun(String runDirectory, String noiseEventsFile) {
		
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";
		
		this.runDirectory = runDirectory;
		this.noiseEventFile = noiseEventsFile;
	}

	public void run() {
		
		String networkFile = runDirectory + "output_network.xml.gz";
		String populationFile = runDirectory + "output_plans.xml.gz";
//		String eventsFile = runDirectory + "output_events.xml.gz";
		String eventsFile = runDirectory + "ITERS/it.1500/1500.events.xml.gz";

		Config config = ConfigUtils.createConfig();	
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);

		String outputPath = runDirectory + "detailed-person-trip-analysis/";
				
		String noiseEventsFileToAnalyze;
		if (this.noiseEventFile == null || this.noiseEventFile == "") {
			noiseEventsFileToAnalyze = eventsFile;
		} else {
			noiseEventsFileToAnalyze = this.noiseEventFile;
		}
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		File folder = new File(outputPath);			
		folder.mkdirs();
		
		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(outputPath);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	
		BasicPersonTripAnalysisHandler basicHandler = new BasicPersonTripAnalysisHandler();
		basicHandler.setScenario(scenario);

		NoiseAnalysisHandler noiseHandler = new NoiseAnalysisHandler();
		noiseHandler.setBasicHandler(basicHandler);
		
		PersonMoneyLinkHandler moneyHandler = new PersonMoneyLinkHandler();
		moneyHandler.setBasicHandler(basicHandler);
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(basicHandler);
		events.addHandler(noiseHandler);
		events.addHandler(moneyHandler);
		
		log.info("Reading the events file...");
		IKEventsReader reader = new IKEventsReader(events);
		reader.readFile(eventsFile);
		log.info("Reading the events file... Done.");
				
		// plans
		
		Map<Id<Person>, Double> personId2userBenefit = new HashMap<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			personId2userBenefit.put(person.getId(), person.getSelectedPlan().getScore() / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney());
		}
		
		// noise events analysis
	
		if (noiseHandler.isCaughtNoiseEvent()) {
			log.info("Noise events have already been analyzed based on the standard events file.");
	
		} else {
			
			EventsManager eventsNoise = EventsUtils.createEventsManager();
			eventsNoise.addHandler(noiseHandler);
					
			log.info("Reading noise events file...");
			NoiseEventsReader noiseEventReader = new NoiseEventsReader(eventsNoise);		
			noiseEventReader.readFile(noiseEventsFileToAnalyze);
			log.info("Reading noise events file... Done.");
		}	
		
		// print the results
		
		PersonTripNoiseAnalysis analysis = new PersonTripNoiseAnalysis();
		
		log.info("Print trip information...");
		analysis.printTripInformation(outputPath, TransportMode.car, basicHandler, noiseHandler, moneyHandler);
		log.info("Print trip information... Done.");

		log.info("Print person information...");
		analysis.printPersonInformation(outputPath, TransportMode.car, personId2userBenefit, basicHandler, noiseHandler);	
		log.info("Print person information... Done.");

		analysis.printAggregatedResults(outputPath, TransportMode.car, personId2userBenefit, basicHandler, noiseHandler, moneyHandler);
		analysis.printAggregatedResults(outputPath, null, personId2userBenefit, basicHandler, noiseHandler, moneyHandler);
		
		SortedMap<Double, List<Double>> departureTime2tolls = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2payment(), 3600., 30 * 3600.);
		analysis.printAvgValuePerParameter(outputPath + "tollsPerDepartureTime_car_3600.csv", departureTime2tolls);
		
		SortedMap<Double, List<Double>> departureTime2traveldistance = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2tripDistance(), 3600., 30 * 3600.);
		analysis.printAvgValuePerParameter(outputPath + "distancePerDepartureTime_car_3600.csv", departureTime2traveldistance);
		
		SortedMap<Double, List<Double>> departureTime2travelTime = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2travelTime(), 3600., 30 * 3600.);
		analysis.printAvgValuePerParameter(outputPath + "travelTimePerDepartureTime_car_3600.csv", departureTime2travelTime);
		
		SortedMap<Double, List<Double>> departureTime2congestionTolls = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), moneyHandler.getPersonId2tripNumber2congestionPayment(), 3600., 30 * 3600.);
		analysis.printAvgValuePerParameter(outputPath + "congestionTollsPerDepartureTime_car_3600.csv", departureTime2congestionTolls);
		
		SortedMap<Double, List<Double>> departureTime2noiseTolls = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), moneyHandler.getPersonId2tripNumber2noisePayment(), 3600., 30 * 3600.);
		analysis.printAvgValuePerParameter(outputPath + "noiseTollsPerDepartureTime_car_3600.csv", departureTime2noiseTolls);
		
		SortedMap<Double, List<Double>> departureTime2airPollutionTolls = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), moneyHandler.getPersonId2tripNumber2airPollutionPayment(), 3600., 30 * 3600.);
		analysis.printAvgValuePerParameter(outputPath + "airPollutionTollsPerDepartureTime_car_3600.csv", departureTime2airPollutionTolls);
	}
}
		

