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
import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.analysis.modules.monetaryTransferPayments.MoneyEventHandler;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

public class WelfareAnalysisMain {
	
	private String runDirectory = "/Users/ihab/Desktop/ils4/kaddoura/bln2/output/noise_int_1a_rndSeed2/";
	
	private String configFile = runDirectory + "output_config.xml.gz";
	private String populationFile = runDirectory + "output_plans.xml.gz";
	private String networkFile = runDirectory + "output_network.xml.gz";

	private int iteration; // final iteration given in the config file
	
	private Map<Integer, Double> it2userBenefits_logsum = new TreeMap<Integer, Double>();
	private Map<Integer, Integer> it2invalidPersons_logsum = new TreeMap<Integer, Integer>();
	private Map<Integer, Integer> it2invalidPlans_logsum = new TreeMap<Integer, Integer>();

	private Map<Integer, Double> it2userBenefits_selected = new TreeMap<Integer, Double>();
	private Map<Integer, Integer> it2invalidPersons_selected = new TreeMap<Integer, Integer>();
	private Map<Integer, Integer> it2invalidPlans_selected = new TreeMap<Integer, Integer>();
	
	private Map<Integer, Double> it2tollSum = new TreeMap<Integer, Double>();
	private Map<Integer, Integer> it2stuckEvents = new TreeMap<Integer, Integer>();
	private Map<Integer, Double> it2totalTravelTimeAllModes = new TreeMap<Integer, Double>();
	private Map<Integer, Double> it2totalTravelTimeCarMode = new TreeMap<Integer, Double>();
		
	private Map<Integer, Double> it2carLegs = new TreeMap<Integer, Double>();
	private Map<Integer, Double> it2ptLegs = new TreeMap<Integer, Double>();
	private Map<Integer, Double> it2walkLegs = new TreeMap<Integer, Double>();
	
	public static void main(String[] args) {
		WelfareAnalysisMain analysis = new WelfareAnalysisMain();
		analysis.run();
	}

	private void run() {
	
		Config config = ConfigUtils.loadConfig(configFile);		
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		
		MoneyEventHandler moneyHandler = new MoneyEventHandler();
		events.addHandler(moneyHandler);
		
		TripAnalysisHandler tripAnalysisHandler = new TripAnalysisHandler();
		events.addHandler(tripAnalysisHandler);
		
		this.iteration = config.controler().getLastIteration();
		String eventsFile = this.runDirectory + "ITERS/it." + iteration + "/" + iteration + ".events.xml.gz";
				
		UserBenefitsCalculator userBenefitsCalculator_selected = new UserBenefitsCalculator(scenario.getConfig(), WelfareMeasure.SELECTED, true);
        this.it2userBenefits_selected.put(this.iteration, userBenefitsCalculator_selected.calculateUtility_money(scenario.getPopulation()));
		this.it2invalidPersons_selected.put(this.iteration, userBenefitsCalculator_selected.getPersonsWithoutValidPlanCnt());
		this.it2invalidPlans_selected.put(this.iteration, userBenefitsCalculator_selected.getInvalidPlans());
		System.out.println("user benefits (selected):" + userBenefitsCalculator_selected.calculateUtility_money(scenario.getPopulation()));
		System.out.println("invalid persons (selected):" + userBenefitsCalculator_selected.getPersonsWithoutValidPlanCnt());
		
		UserBenefitsCalculator userBenefitsCalculator_logsum = new UserBenefitsCalculator(scenario.getConfig(), WelfareMeasure.LOGSUM, true);
		this.it2userBenefits_logsum.put(this.iteration, userBenefitsCalculator_logsum.calculateUtility_money(scenario.getPopulation()));
		this.it2invalidPersons_logsum.put(this.iteration, userBenefitsCalculator_logsum.getPersonsWithoutValidPlanCnt());
		this.it2invalidPlans_logsum.put(this.iteration, userBenefitsCalculator_logsum.getInvalidPlans());

		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		double tollSum = moneyHandler.getSumOfMonetaryAmounts();
		this.it2tollSum.put(this.iteration, (-1) * tollSum);
		this.it2stuckEvents.put(this.iteration, tripAnalysisHandler.getAgentStuckEvents());
		this.it2totalTravelTimeAllModes.put(this.iteration, tripAnalysisHandler.getTotalTravelTimeAllModes());
		this.it2totalTravelTimeCarMode.put(this.iteration, tripAnalysisHandler.getTotalTravelTimeCarMode());
		this.it2carLegs.put(this.iteration, (double) tripAnalysisHandler.getCarLegs());
		this.it2ptLegs.put(this.iteration, (double) tripAnalysisHandler.getPtLegs());
		this.it2walkLegs.put(this.iteration, (double) tripAnalysisHandler.getWalkLegs());
						
		String fileName = this.runDirectory + "analysis_welfare.csv";
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("Iteration;" +
					"User Benefits (LogSum);Number of Invalid Persons (LogSum);Number of Invalid Plans (LogSum);" +
					"User Benefits (Selected);Number of Invalid Persons (Selected);Number of Invalid Plans (Selected);" +
					"Total Monetary Payments;Welfare (LogSum);Welfare (Selected);Total Travel Time All Modes (sec);Total Travel Time Car Mode (sec);Avg Travel Time Per Car Trip (sec);Number of Agent Stuck Events;" +
					"Car Trips;Pt Trips;Walk Trips");
			bw.newLine();
			for (Integer it : this.it2userBenefits_selected.keySet()){
				bw.write(it + ";" + this.it2userBenefits_logsum.get(it) + ";" + this.it2invalidPersons_logsum.get(it) + ";" + this.it2invalidPlans_logsum.get(it)
						+ ";" + this.it2userBenefits_selected.get(it) + ";" + this.it2invalidPersons_selected.get(it) + ";" + this.it2invalidPlans_selected.get(it)
						+ ";" + this.it2tollSum.get(it)
						+ ";" + (this.it2userBenefits_logsum.get(it) + this.it2tollSum.get(it))
						+ ";" + (this.it2userBenefits_selected.get(it) + this.it2tollSum.get(it))
						+ ";" + this.it2totalTravelTimeAllModes.get(it)
						+ ";" + this.it2totalTravelTimeCarMode.get(it)
						+ ";" + (this.it2totalTravelTimeCarMode.get(it) / this.it2carLegs.get(it))
						+ ";" + this.it2stuckEvents.get(it)
						+ ";" + this.it2carLegs.get(it)
						+ ";" + this.it2ptLegs.get(it)
						+ ";" + this.it2walkLegs.get(it)
						);
				bw.newLine();
			}
			
			bw.close();
			System.out.println("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
			 
}
		

