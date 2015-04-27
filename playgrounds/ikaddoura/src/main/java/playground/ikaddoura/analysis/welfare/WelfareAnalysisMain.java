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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.analysis.modules.monetaryTransferPayments.MoneyEventHandler;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;

public class WelfareAnalysisMain {
	private static final Logger log = Logger.getLogger(WelfareAnalysisMain.class);
	
	private static String runDirectory1;
	private static String runDirectory2;
	private static String runDirectory3;
	
	private static int iteration;
	
	private List<Id<Person>> invalidPersonIDs = new ArrayList<Id<Person>>();
	
	public static void main(String[] args) {
		
		if (args.length > 0) {
			runDirectory1 = args[0];		
			log.info("run directory1: " + runDirectory1);
			
			runDirectory2 = args[1];		
			log.info("run directory2: " + runDirectory2);
			
			runDirectory3 = args[2];		
			log.info("run directory3: " + runDirectory3);
			
			iteration = Integer.valueOf(args[3]);
			log.info("iteration: " + iteration);
			
		} else {
			
			runDirectory1 = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalization_noise/output/baseCase/";
			runDirectory2 = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalization_noise/output/noise_int_1a/";
			runDirectory3 = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalization_noise/output/noise_int_2a/";
			iteration = 100;
		}
		
		WelfareAnalysisMain analysis = new WelfareAnalysisMain();
		analysis.run();		
	}

	private void run() {
		
		getStuckingAgentIDs(runDirectory1);
		getStuckingAgentIDs(runDirectory2);
		getStuckingAgentIDs(runDirectory3);
		
		Scenario scenario1 = loadScenario(runDirectory1);
		analyze(runDirectory1, scenario1);
		scenario1 = null;

		Scenario scenario2 = loadScenario(runDirectory2);
		analyze(runDirectory2, scenario2);
		scenario2 = null;

		Scenario scenario3 = loadScenario(runDirectory3);
		analyze(runDirectory3, scenario3);
		scenario3 = null;
	}

	private void analyze(String runDirectory, Scenario scenario) {

		Map<Integer, Double> it2userBenefits_selected = new TreeMap<Integer, Double>();
		Map<Integer, Integer> it2invalidPersons_selected = new TreeMap<Integer, Integer>();
		Map<Integer, Integer> it2invalidPlans_selected = new TreeMap<Integer, Integer>();
		
		Map<Integer, Double> it2tollSum = new TreeMap<Integer, Double>();
		Map<Integer, Double> it2totalTravelTimeAllModes = new TreeMap<Integer, Double>();
		Map<Integer, Double> it2totalTravelTimeCarMode = new TreeMap<Integer, Double>();
			
		Map<Integer, Integer> it2stuckEvents = new TreeMap<Integer, Integer>();
		Map<Integer, Double> it2carLegs = new TreeMap<Integer, Double>();
		Map<Integer, Double> it2ptLegs = new TreeMap<Integer, Double>();
		Map<Integer, Double> it2walkLegs = new TreeMap<Integer, Double>();

		// events analysis
		
		String eventsFile = runDirectory + "ITERS/it." + iteration + "/" + iteration + ".events.xml.gz";
		
		EventsManager events = EventsUtils.createEventsManager();
		
		MoneyEventHandler moneyHandler = new MoneyEventHandler(this.invalidPersonIDs);
		events.addHandler(moneyHandler);
		
		TripAnalysisHandler tripAnalysisHandler = new TripAnalysisHandler();
		events.addHandler(tripAnalysisHandler);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		double tollSum = moneyHandler.getSumOfMonetaryAmounts();
		it2tollSum.put(iteration, (-1) * tollSum);
		it2stuckEvents.put(iteration, tripAnalysisHandler.getAgentStuckEvents());
		it2totalTravelTimeAllModes.put(iteration, tripAnalysisHandler.getTotalTravelTimeAllModes());
		it2totalTravelTimeCarMode.put(iteration, tripAnalysisHandler.getTotalTravelTimeCarMode());
		it2carLegs.put(iteration, (double) tripAnalysisHandler.getCarLegs());
		it2ptLegs.put(iteration, (double) tripAnalysisHandler.getPtLegs());
		it2walkLegs.put(iteration, (double) tripAnalysisHandler.getWalkLegs());
				
		// plans
		
		UserBenefitsCalculator userBenefitsCalculator_selected = new UserBenefitsCalculator(scenario.getConfig(), invalidPersonIDs);
        it2userBenefits_selected.put(iteration, userBenefitsCalculator_selected.calculateUtility_money(scenario.getPopulation()));
		it2invalidPersons_selected.put(iteration, userBenefitsCalculator_selected.getPersonsWithoutValidPlanCnt());
		it2invalidPlans_selected.put(iteration, userBenefitsCalculator_selected.getInvalidPlans());
		
		log.info("user benefits (selected):" + userBenefitsCalculator_selected.calculateUtility_money(scenario.getPopulation()));
		log.info("invalid persons (selected):" + userBenefitsCalculator_selected.getPersonsWithoutValidPlanCnt());
		
		// results
				
		String fileName = runDirectory + "analysis_welfare.csv";
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("Iteration;" +
					"User Benefits (Selected);Number of Invalid Persons (Selected);Number of Invalid Plans (Selected);" +
					"Total Monetary Payments;Welfare (Selected);Total Travel Time All Modes (sec);Total Travel Time Car Mode (sec);Number of Agent Stuck Events;" +
					"Car Trips;Pt Trips;Walk Trips");
			bw.newLine();
			for (Integer it : it2userBenefits_selected.keySet()){
				bw.write(it + ";" + it2userBenefits_selected.get(it) + ";" + it2invalidPersons_selected.get(it) + ";" + it2invalidPlans_selected.get(it)
						+ ";" + it2tollSum.get(it)
						+ ";" + (it2userBenefits_selected.get(it) + it2tollSum.get(it))
						+ ";" + it2totalTravelTimeAllModes.get(it)
						+ ";" + it2totalTravelTimeCarMode.get(it)
						+ ";" + it2stuckEvents.get(it)
						+ ";" + it2carLegs.get(it)
						+ ";" + it2ptLegs.get(it)
						+ ";" + it2walkLegs.get(it)
						);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String fileName2 = runDirectory + "analysis_welfare_ignoredAgentIds.csv";
		File file2 = new File(fileName2);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
			for (Id<Person> id : this.invalidPersonIDs){
				bw.write(id.toString());
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void getStuckingAgentIDs(String runDirectory) {
		
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
	}

	private Scenario loadScenario(String runDirectory) {
		
		String configFile = runDirectory + "output_config.xml.gz";
		String populationFile = runDirectory + "output_plans.xml.gz";
		String networkFile = runDirectory + "output_network.xml.gz";
	
		Config config = ConfigUtils.loadConfig(configFile);		
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		return scenario;
	}
}
		

