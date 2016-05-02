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

package playground.ikaddoura.analysis.welfare;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.noise.events.NoiseEventsReader;
import org.matsim.contrib.noise.utils.NoiseEventAnalysisHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.analysis.modules.monetaryTransferPayments.MoneyEventHandler;

public class WelfareAnalysisMain {
	private static final Logger log = Logger.getLogger(WelfareAnalysisMain.class);
	
	private static String runDirectory1;
	private static String runDirectory2;
	
	private static String outputDirectory;
	private static String outputFile;
	private static int iteration;
	
	// if the following variable is null the standard events file is used
	private static String eventsFileWithNoiseEvents1;
	private static String eventsFileWithNoiseEvents2;
	
	// if the following variable is true, the events have to contain noise events.
	// if the following variable is false, money events are analyzed and assumed to correspond to the caused noise costs
	private static boolean useNoiseEvents1;
	private static boolean useNoiseEvents2;
	
	// output
	private Map<Id<Person>, Double> personId2Cost1;
	private Map<Id<Person>, Double> personId2Cost2;
	private Map<Id<Person>, Double> personId2userBenefits1;
	private Map<Id<Person>, Double> personId2userBenefits2;

	public static void main(String[] args) {
		
		if (args.length > 0) {
			runDirectory1 = args[0];		
			log.info("run directory1: " + runDirectory1);
			
			runDirectory2 = args[1];		
			log.info("run directory2: " + runDirectory2);
			
			iteration = Integer.valueOf(args[3]);
			log.info("iteration: " + iteration);
			
		} else {
			
			runDirectory1 = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalization_noise_averageVSmarginal/output/baseCase/";
			runDirectory2 = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalization_noise_averageVSmarginal/output/int_1_marginalCost/";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalization_noise_averageVSmarginal/output/";
			outputFile = "baseCase_marginalCostPricing_1";
			iteration = 100;
			eventsFileWithNoiseEvents1 = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalizationCar/output/baseCase_2_noiseAnalysis/r31341/noiseAnalysis_BlnBC2_1/analysis_it.100/100.events_NoiseImmission_Offline.xml.gz";
			eventsFileWithNoiseEvents2 = null;
			useNoiseEvents1 = true;
			useNoiseEvents2 = false;
		}
		
		WelfareAnalysisMain analysis = new WelfareAnalysisMain();
		analysis.run();		
	}

	private void run() {
		
		// preprocess
		
		Scenario scenario1 = loadScenario(runDirectory1);
		Scenario scenario2 = loadScenario(runDirectory2);

		Set<Id<Person>> invalidScorePersonIDs = new HashSet<>(); // score <= 0.0 or score == null
		Set<Id<Person>> stuckingPersonIDs = new HashSet<>(); // based on the events
		Set<Id<Person>> invalidScoreOrStuckingPersonIDs = new HashSet<>(); // stucking agents or invalid score

		Set<Id<Person>> invalidScorePersonIDs1 = new HashSet<>();
		Set<Id<Person>> invalidScorePersonIDs2 = new HashSet<>();
		Set<Id<Person>> stuckingPersonIDs1 = new HashSet<>();
		Set<Id<Person>> stuckingPersonIDs2 = new HashSet<>();


//		Set<Id<Person>> invalidScorePersonIDs1 = getInvalidScorePersonIDs(scenario1.getPopulation());
//		Set<Id<Person>> invalidScorePersonIDs2 = getInvalidScorePersonIDs(scenario2.getPopulation());
//
//		invalidScorePersonIDs.addAll(invalidScorePersonIDs1);		
//		invalidScorePersonIDs.addAll(invalidScorePersonIDs2);		
//		
//		Set<Id<Person>> stuckingPersonIDs1 = getStuckingAgentIDs(runDirectory1);
//		Set<Id<Person>> stuckingPersonIDs2 = getStuckingAgentIDs(runDirectory2);
//
//		stuckingPersonIDs.addAll(stuckingPersonIDs1);
//		stuckingPersonIDs.addAll(stuckingPersonIDs2);
//
//		invalidScoreOrStuckingPersonIDs.addAll(invalidScorePersonIDs);
//		invalidScoreOrStuckingPersonIDs.addAll(stuckingPersonIDs);
		
		// events analysis
		
		personId2Cost1 = getPersonId2amount(runDirectory1, eventsFileWithNoiseEvents1, useNoiseEvents1);
		personId2Cost2 = getPersonId2amount(runDirectory2, eventsFileWithNoiseEvents2, useNoiseEvents2);

		personId2userBenefits1 = getPersonId2userBenefit(scenario1);
		personId2userBenefits2 = getPersonId2userBenefit(scenario2);
		
		// write
		
		write(outputDirectory + outputFile + "_allPersons.csv", new HashSet<Id<Person>>());
		write(outputDirectory + outputFile + "_withoutInvalidScores.csv", invalidScorePersonIDs);
		write(outputDirectory + outputFile + "_withoutStuckingPersons.csv", stuckingPersonIDs);
		write(outputDirectory + outputFile + "_withoutStuckingOrInvalidScores.csv", invalidScoreOrStuckingPersonIDs);
		
		writeSummary(outputDirectory + outputFile + "_summary.csv", invalidScorePersonIDs, stuckingPersonIDs, invalidScoreOrStuckingPersonIDs, invalidScorePersonIDs1, invalidScorePersonIDs2, stuckingPersonIDs1, stuckingPersonIDs2 );
	}

	private void writeSummary(String fileName,
			Set<Id<Person>> invalidScorePersonIDs,
			Set<Id<Person>> stuckingPersonIDs,
			Set<Id<Person>> invalidScoreOrStuckingPersonIDs,
			Set<Id<Person>> invalidScorePersonIDs1,
			Set<Id<Person>> invalidScorePersonIDs2,
			Set<Id<Person>> stuckingPersonIDs1,
			Set<Id<Person>> stuckingPersonIDs2) {

		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("Parameter;Scenario1;Scenario 2;Number of excluded persons");
			bw.newLine();
			
			bw.write("Persons with invalid plan score");
			bw.write(";" + invalidScorePersonIDs1.size());
			bw.write(";" + invalidScorePersonIDs2.size());
			bw.write(";- ");
			bw.newLine();
			
			bw.write("Stucking persons");
			bw.write(";" + stuckingPersonIDs1.size());
			bw.write(";" + stuckingPersonIDs2.size());
			bw.write(";- ");
			bw.newLine();
			
			bw.write("User Benefits - all persons (selected plan's score, monetary units");
			bw.write(";" + getSum(this.personId2userBenefits1, new HashSet<Id<Person>>()));
			bw.write(";" + getSum(this.personId2userBenefits2, new HashSet<Id<Person>>()));
			bw.write(";0 ");
			bw.newLine();
			
			bw.write("Monetary payments - all persons (selected plan's score, monetary units");
			bw.write(";" + getSum(this.personId2Cost1, new HashSet<Id<Person>>()));
			bw.write(";" + getSum(this.personId2Cost2, new HashSet<Id<Person>>()));
			bw.write(";0 ");
			bw.newLine();
			
			bw.write("User Benefits - without persons with invalid plan score  (selected plan's score, monetary units");
			bw.write(";" + getSum(this.personId2userBenefits1, invalidScorePersonIDs));
			bw.write(";" + getSum(this.personId2userBenefits2, invalidScorePersonIDs));
			bw.write(";" + invalidScorePersonIDs.size());
			bw.newLine();
			
			bw.write("Monetary payments - without persons with invalid plan score (selected plan's score, monetary units");
			bw.write(";" + getSum(this.personId2Cost1, invalidScorePersonIDs));
			bw.write(";" + getSum(this.personId2Cost2, invalidScorePersonIDs));
			bw.write(";" + invalidScorePersonIDs.size());

			bw.newLine();
			
			bw.write("User Benefits - without stucking persons (selected plan's score, monetary units");
			bw.write(";" + getSum(this.personId2userBenefits1, stuckingPersonIDs));
			bw.write(";" + getSum(this.personId2userBenefits2, stuckingPersonIDs));
			bw.write(";" + stuckingPersonIDs.size());
			bw.newLine();
			
			bw.write("Monetary payments - without stucking persons (selected plan's score, monetary units");
			bw.write(";" + getSum(this.personId2Cost1, stuckingPersonIDs));
			bw.write(";" + getSum(this.personId2Cost2, stuckingPersonIDs));
			bw.write(";" + stuckingPersonIDs.size());
			bw.newLine();
			
			bw.write("User Benefits - without stucking persons or persons with invalid plan score  (selected plan's score, monetary units");
			bw.write(";" + getSum(this.personId2userBenefits1, invalidScoreOrStuckingPersonIDs));
			bw.write(";" + getSum(this.personId2userBenefits2, invalidScoreOrStuckingPersonIDs));
			bw.write(";" + invalidScoreOrStuckingPersonIDs.size());
			bw.newLine();
			
			bw.write("Monetary payments - without stucking persons or persons with invalid plan score (selected plan's score, monetary units");
			bw.write(";" + getSum(this.personId2Cost1, invalidScoreOrStuckingPersonIDs));
			bw.write(";" + getSum(this.personId2Cost2, invalidScoreOrStuckingPersonIDs));
			bw.write(";" + invalidScoreOrStuckingPersonIDs.size());
			bw.newLine();
					
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private double getSum(Map<Id<Person>, Double> personId2value, Set<Id<Person>> excludedPersons) {
		double sum = 0.;
		
		for (Id<Person> id : personId2value.keySet()){
			
			if (excludedPersons.contains(id)) {
				// ignore this person
				
			} else {
				sum = sum + personId2value.get(id);
			}
		}
		return sum;
	}

	private void write(String fileName, Set<Id<Person>> excludedPersons) {
		
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("Person ID;Scenario1 - Benefits (selected plan's score, monetary units);Scenario 1 - Monetary payments;Scenario2 - Benefits (selected plan's score, monetary units);Scenario 2 - Monetary payments");
			bw.newLine();
			
			for (Id<Person> id : this.personId2userBenefits1.keySet()){
				
				if (excludedPersons.contains(id)) {
					// ignore this person
					
				} else {
					double cost1 = 0.;
					double cost2 = 0.;
					
					if (this.personId2Cost1.containsKey(id)) {
						cost1 = this.personId2Cost1.get(id);
					}
					if (this.personId2Cost2.containsKey(id)) {
						cost2 = this.personId2Cost2.get(id);
					}
					
					bw.write(id
							+ ";" + this.personId2userBenefits1.get(id)
							+ ";" + cost1
							+ ";" + this.personId2userBenefits2.get(id)
							+ ";" + cost2
							);
					bw.newLine();
				}
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Map<Id<Person>, Double> getPersonId2userBenefit(Scenario scenario) {
		Map<Id<Person>, Double> personId2userBenefit = new HashMap<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			personId2userBenefit.put(person.getId(), person.getSelectedPlan().getScore() / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney());
		}
		return personId2userBenefit;
	}

	private Map<Id<Person>, Double> getPersonId2amount(String runDirectory, String eventsFileWithNoiseEvents, boolean useNoiseEvents) {
		
		String eventsFile;
		if (eventsFileWithNoiseEvents == null) {
			eventsFile = runDirectory + "ITERS/it." + iteration + "/" + iteration + ".events.xml.gz";
		} else {
			eventsFile = eventsFileWithNoiseEvents;
		}
		
		EventsManager events = EventsUtils.createEventsManager();
		
		log.info("Reading events file...");

		if (useNoiseEvents == false) {
			MoneyEventHandler moneyHandler = new MoneyEventHandler();
			events.addHandler(moneyHandler);
			
			MatsimEventsReader reader = new MatsimEventsReader(events);
			reader.readFile(eventsFile);
			
			log.info("Reading events file... Done.");
			
			return moneyHandler.getPersonId2amount();
		
		} else {			
			// assuming the provided events file to contain noise events (caused)
			NoiseEventAnalysisHandler noiseHandler = new NoiseEventAnalysisHandler();
			events.addHandler(noiseHandler);
			
			NoiseEventsReader noiseEventReader = new NoiseEventsReader(events);		
			noiseEventReader.parse(eventsFile);
			
			log.info("Reading events file... Done.");

			return noiseHandler.getPersonId2causedNoiseCost();
		}			
	}

	private Set<Id<Person>> getStuckingAgentIDs(String runDirectory) {
		
		Set<Id<Person>> invalidPersonIDs = new HashSet<>();

		// events analysis
		
		String eventsFile = runDirectory + "ITERS/it." + iteration + "/" + iteration + ".events.xml.gz";
		
		EventsManager events = EventsUtils.createEventsManager();
		StuckEventHandler stuckHandler = new StuckEventHandler();
		events.addHandler(stuckHandler);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		invalidPersonIDs.addAll(stuckHandler.getStuckingAgentIds());
		
		String stuckingAgentsString = null;
		for (Id<Person> id : invalidPersonIDs) {
			stuckingAgentsString = stuckingAgentsString + id.toString() + " ; ";
		}
		log.info("Stucking agents person Ids: " + stuckingAgentsString);
		return invalidPersonIDs;
	}
	
	private Set<Id<Person>> getInvalidScorePersonIDs(Population population) {
		log.info("Getting invalid person IDs from population...");

		Set<Id<Person>> invalidPersonIDs = new HashSet<>();
		
		for (Person person : population.getPersons().values()) {
			if (person.getSelectedPlan().getScore() <= 0.0 || person.getSelectedPlan().getScore() == null) {
				invalidPersonIDs.add(person.getId());
			}
		}
		log.info("Getting invalid person IDs from population... Done.");
		
		return invalidPersonIDs;
	}

	private Scenario loadScenario(String runDirectory) {
		
		log.info("Loading scenario from run directory " + runDirectory + "...");
		
		String configFile = runDirectory + "output_config.xml.gz";
		String populationFile = runDirectory + "output_plans.xml.gz";
//		String networkFile = runDirectory + "output_network.xml.gz";
		String networkFile = null;
	
		Config config = ConfigUtils.loadConfig(configFile);		
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		log.info("Loading scenario from run directory " + runDirectory + "... Done.");

		return scenario;
	}
}
		

