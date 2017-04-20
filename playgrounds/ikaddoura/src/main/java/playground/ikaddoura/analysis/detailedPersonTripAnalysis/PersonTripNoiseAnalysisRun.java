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
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.events.NoiseEventsReader;
import org.matsim.contrib.taxi.run.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.NoiseAnalysisHandler;

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
			
		log.info("Searching for run-directory in args at index 0...");
		String runDirectory;
		
		if (args.length > 0) {
			runDirectory = args[0];
			log.info("Run directory: " + runDirectory);
		
		} else {
			
			runDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV/output/optAV_av-trip-share-0.01_av-20000_kp999999/";
			log.info("Run directory " + runDirectory);
		}
		
		PersonTripNoiseAnalysisRun analysis = new PersonTripNoiseAnalysisRun(runDirectory);
		analysis.run();
		
//		PersonTripNoiseAnalysisRun analysis1 = new PersonTripNoiseAnalysisRun(runDirectory, "/Users/ihab/Documents/workspace/runs-svn/optAV/output_baseCase/analysis_it.10/10.events_NoiseImmission_Offline.xml.gz");
//		analysis1.run();
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
		
		String configFile = runDirectory + "output_config.xml.gz";
		String networkFile = "output_network.xml.gz";
		String populationFile = "output_plans.xml.gz";

		Config config = ConfigUtils.loadConfig(configFile, new NoiseConfigGroup());	
		config.plans().setInputFile(populationFile);
		config.plans().setInputPersonAttributeFile(null);
		config.network().setInputFile(networkFile);
		config.network().setChangeEventsInputFile(null);
		config.vehicles().setVehiclesFile(null);

		int finalIteration = config.controler().getLastIteration();
		String eventsFile = runDirectory + "ITERS/it." + finalIteration + "/" + finalIteration + ".events.xml.gz";
		String outputPath = runDirectory + "person-trip-noise-analysis_it." + finalIteration + "/";
				
		String noiseEventsFileToAnalyze;
		if (this.noiseEventFile == null || this.noiseEventFile == "") {
			noiseEventsFileToAnalyze = runDirectory + "ITERS/it." + finalIteration + "/" + finalIteration + ".events.xml.gz";
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
		
		// standard events analysis
		
		BasicPersonTripAnalysisHandler basicHandler = new BasicPersonTripAnalysisHandler();
		basicHandler.setScenario(scenario);

		NoiseAnalysisHandler noiseHandler = new NoiseAnalysisHandler();
		noiseHandler.setBasicHandler(basicHandler);
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(basicHandler);
		events.addHandler(noiseHandler);
		
		log.info("Reading the events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
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
		analysis.printTripInformation(outputPath, TaxiModule.TAXI_MODE, basicHandler, noiseHandler);
		analysis.printTripInformation(outputPath, TransportMode.car, basicHandler, noiseHandler);
		analysis.printTripInformation(outputPath, null, basicHandler, noiseHandler);
		log.info("Print trip information... Done.");

		log.info("Print person information...");
		analysis.printPersonInformation(outputPath, TaxiModule.TAXI_MODE, personId2userBenefit, basicHandler, noiseHandler);	
		analysis.printPersonInformation(outputPath, TransportMode.car, personId2userBenefit, basicHandler, noiseHandler);	
		analysis.printPersonInformation(outputPath, null, personId2userBenefit, basicHandler, noiseHandler);	
		log.info("Print person information... Done.");

		analysis.printAggregatedResults(outputPath, TaxiModule.TAXI_MODE, personId2userBenefit, basicHandler, noiseHandler);
		analysis.printAggregatedResults(outputPath, TransportMode.car, personId2userBenefit, basicHandler, noiseHandler);
		analysis.printAggregatedResults(outputPath, null, personId2userBenefit, basicHandler, noiseHandler);
	}
}
		

