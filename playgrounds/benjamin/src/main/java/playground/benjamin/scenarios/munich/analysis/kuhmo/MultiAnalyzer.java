/* *********************************************************************** *
 * project: org.matsim.*
 * MulitAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.scenarios.munich.analysis.kuhmo;

import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.benjamin.emissions.events.EmissionEventsReader;
import playground.benjamin.emissions.types.ColdPollutant;
import playground.benjamin.emissions.types.WarmPollutant;
import playground.benjamin.scenarios.munich.analysis.EmissionSummarizer;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.benjamin.scenarios.munich.analysis.mobilTUM.EmissionsPerPersonColdEventHandler;
import playground.benjamin.scenarios.munich.analysis.mobilTUM.EmissionsPerPersonWarmEventHandler;
import playground.benjamin.scenarios.zurich.analysis.MoneyEventHandler;
import playground.benjamin.utils.BkNumberUtils;

/**
 * @author benjamin
 *
 */
public class MultiAnalyzer {
	private static final Logger logger = Logger.getLogger(MultiAnalyzer.class);
	
//	private static String runDirectory = "../../detailedEval/testRuns/output/1pct/v0-default/internalize/output_policyCase_zone30/short/";
	private static String runDirectory = "../../detailedEval/testRuns/output/1pct/v0-default/internalize/output_policyCase_pricing_x10/short/";
//	private static String runDirectory = "../../detailedEval/testRuns/output/1pct/v0-default/internalize/output_policyCase_pricing_x100/short/";
	private static String netFile = runDirectory + "output_network.xml.gz";
	private static String configFile = runDirectory + "output_config.xml.gz";

	private static String initialPlansFile = runDirectory + "ITERS/it.1000/1000.plans.xml.gz";
	private static String finalPlansFile = runDirectory + "ITERS/it.1100/1100.plans.xml.gz";
	
	private static String initialEventsFile = runDirectory + "ITERS/it.1000/1000.events.xml.gz";
	private static String finalEventsFile = runDirectory + "ITERS/it.1100/1100.events.xml.gz";
	
	private static String initialEmissionEventsFile = runDirectory + "ITERS/it.1000/1000.emission.events.xml.gz";
	private static String finalEmissionEventsFile = runDirectory + "ITERS/it.1100/1100.emission.events.xml.gz";
	
	private final PersonFilter personFilter;
	private final int decimalPlace;
	
	MultiAnalyzer(){
		this.personFilter = new PersonFilter();
		this.decimalPlace = 3;
	}

	private void run() {
//		calculateUserWelfareChange(netFile, configFile, initialPlansFile, finalPlansFile);
//		calculateTollRevenueByUserGroup(finalEventsFile);
		calculateAverageTripTravelTimePerMode(initialEventsFile, finalEventsFile);
		calculateAverageTripLengthCar(initialEventsFile, finalEventsFile);
		calculateEmissionChangesByUserGroup(initialEmissionEventsFile, finalEmissionEventsFile);
	}

	private void calculateUserWelfareChange(String netFile, String configFile, String initialPlansFile, String finalPlansFile) {
		UserWelfareCalculator userWelfareCalculator = new UserWelfareCalculator(configFile);
		
		Scenario initialScenario = loadScenario(netFile, initialPlansFile);
		Scenario finalScenario = loadScenario(netFile, finalPlansFile);
		Population initialPop = initialScenario.getPopulation();
		Population finalPop = finalScenario.getPopulation();

		for(UserGroup userGroup : UserGroup.values()){
			Population initialUserGroupPop = personFilter.getPopulation(initialPop, userGroup);
			Population finalUserGroupPop = personFilter.getPopulation(finalPop, userGroup);
			
			double initialUserWelfare = userWelfareCalculator.calculateLogsum(initialUserGroupPop);
			int initialInvalidPlanCnt = userWelfareCalculator.getNoValidPlanCnt();
			userWelfareCalculator.reset();
			
			double finalUserWelfare = userWelfareCalculator.calculateLogsum(finalUserGroupPop);
			int finalInvalidPlanCnt = userWelfareCalculator.getNoValidPlanCnt();
			userWelfareCalculator.reset();
			
			double userWelfareDiff = finalUserWelfare - initialUserWelfare;
			double userWelfareDiffPct = 100 * (userWelfareDiff / initialUserWelfare);

			System.out.println("\n*******************************************************************");
			System.out.println("VALUES FOR " + userGroup);
			System.out.println("*******************************************************************");
			System.out.println("Final user welfare is calculated to\t\t" + BkNumberUtils.roundDouble(finalUserWelfare, decimalPlace));
			System.out.println("Initial user welfare is calculated to\t\t" + BkNumberUtils.roundDouble(initialUserWelfare, decimalPlace));
			System.out.println("===================================================================");
			System.out.println("Change in user welfare is calculated to\t\t" + BkNumberUtils.roundDouble(userWelfareDiff, decimalPlace) 
						+ " or " + BkNumberUtils.roundDouble(userWelfareDiffPct, decimalPlace) + "%");
			System.out.println("*******************************************************************");
			System.out.println("Final users with invalid score (none or negative):\t" + initialInvalidPlanCnt);
			System.out.println("Initial users with invalid score (none or negative):\t" + finalInvalidPlanCnt);
			System.out.println("*******************************************************************\n");
		}
		
		double initialUserWelfare = userWelfareCalculator.calculateLogsum(initialPop);
		int initialInvalidPlanCnt = userWelfareCalculator.getNoValidPlanCnt();
		userWelfareCalculator.reset();
		
		double finalUserWelfare = userWelfareCalculator.calculateLogsum(finalPop);
		int finalInvalidPlanCnt = userWelfareCalculator.getNoValidPlanCnt();
		userWelfareCalculator.reset();
		
		double userWelfareDiff = finalUserWelfare - initialUserWelfare;
		double userWelfareDiffPct = 100 * (userWelfareDiff / initialUserWelfare);

		System.out.println("\n*******************************************************************");
		System.out.println("VALUES FOR WHOLE POPULATION");
		System.out.println("*******************************************************************");
		System.out.println("Final user welfare is calculated to\t\t" + BkNumberUtils.roundDouble(finalUserWelfare, decimalPlace));
		System.out.println("Initial user welfare is calculated to\t\t" + BkNumberUtils.roundDouble(initialUserWelfare, decimalPlace));
		System.out.println("===================================================================");
		System.out.println("Change in user welfare is calculated to\t\t" + BkNumberUtils.roundDouble(userWelfareDiff, decimalPlace) 
					+ " or " + BkNumberUtils.roundDouble(userWelfareDiffPct, decimalPlace) + "%");
		System.out.println("*******************************************************************");
		System.out.println("Final users with invalid score (none or negative):\t" + initialInvalidPlanCnt);
		System.out.println("Initial users with invalid score (none or negative):\t" + finalInvalidPlanCnt);
		System.out.println("*******************************************************************\n");
	}

	private void calculateTollRevenueByUserGroup(String finalEventsFile) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsReaderXMLv1 eventsReader = new EventsReaderXMLv1(eventsManager);
		MoneyEventHandler moneyEventHandler = new MoneyEventHandler();
		eventsManager.addHandler(moneyEventHandler);
		eventsReader.parse(finalEventsFile);
		
		Map<Id, Double> personId2Toll = moneyEventHandler.getPersonId2TollMap();
	
		System.out.println("\n*******************************************************************");
		for(UserGroup userGroup : UserGroup.values()){
			double tollRevenueFromGroup = 0.0;
			int groupSize = 0;
			
			for(Id personId : personId2Toll.keySet()){
				if(personFilter.isPersonIdFromUserGroup(personId, userGroup)){
					tollRevenueFromGroup += personId2Toll.get(personId);
					groupSize++;
				}
			}
			// need to take the absolute value since money events are negative from the users' perspective.
			double absoluteTollRevenueUserGroup = Math.abs(tollRevenueFromGroup);
			System.out.println("Toll revenue from ``" + userGroup + "'' (" + groupSize + " users) is calculated to\t" + 
							   BkNumberUtils.roundDouble(absoluteTollRevenueUserGroup, decimalPlace));
		}
		
		double tollRevenue = 0.0;
		for(Id personId : personId2Toll.keySet()){
			tollRevenue += personId2Toll.get(personId);
		}
		double absoluteTollRevenue = Math.abs(tollRevenue);
		System.out.println("===================================================================");
		System.out.println("Total toll revenue from " + personId2Toll.size() + " users is calculated to\t\t" +
						   BkNumberUtils.roundDouble(absoluteTollRevenue, decimalPlace));
		System.out.println("*******************************************************************\n");
	}

	private void calculateEmissionChangesByUserGroup(String initialEmissionEventsFile, String finalEmissionEventsFile) {
		EmissionSummarizer summarizer = new EmissionSummarizer();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		EmissionsPerPersonWarmEventHandler warmHandler = new EmissionsPerPersonWarmEventHandler();
		EmissionsPerPersonColdEventHandler coldHandler = new EmissionsPerPersonColdEventHandler();
		eventsManager.addHandler(warmHandler);
		eventsManager.addHandler(coldHandler);
		emissionReader.parse(finalEmissionEventsFile);
		
		Map<Id, Map<WarmPollutant, Double>> person2FinalWarmEmissions = warmHandler.getWarmEmissionsPerPerson();
		Map<Id, Map<ColdPollutant, Double>> person2FinalColdEmissions = coldHandler.getColdEmissionsPerPerson();
		Map<Id, SortedMap<String, Double>> person2FinalTotalEmissions = summarizer.sumUpEmissionsPerPerson(person2FinalWarmEmissions, person2FinalColdEmissions);
		SortedMap<UserGroup, SortedMap<String, Double>> group2FinalTotalEmissions = summarizer.getEmissionsPerGroup(person2FinalTotalEmissions);

		warmHandler.reset(0);
		coldHandler.reset(0);
		emissionReader.parse(initialEmissionEventsFile);
		
		Map<Id, Map<WarmPollutant, Double>> person2InitialWarmEmissions = warmHandler.getWarmEmissionsPerPerson();
		Map<Id, Map<ColdPollutant, Double>> person2InitialColdEmissions = coldHandler.getColdEmissionsPerPerson();
		Map<Id, SortedMap<String, Double>> person2InitialTotalEmissions = summarizer.sumUpEmissionsPerPerson(person2InitialWarmEmissions, person2InitialColdEmissions);
		SortedMap<UserGroup, SortedMap<String, Double>> group2InitialTotalEmissions = summarizer.getEmissionsPerGroup(person2InitialTotalEmissions);
		
		for(UserGroup userGroup : group2FinalTotalEmissions.keySet()){
			System.out.println("\n*******************************************************************");
			System.out.println("VALUES FOR " + userGroup);
			System.out.println("*******************************************************************");
			Map<String, Double> pollutant2FinalEmissions = group2FinalTotalEmissions.get(userGroup);
			for(String pollutant : pollutant2FinalEmissions.keySet()){
				double pollutantDiff = BkNumberUtils.roundDouble(pollutant2FinalEmissions.get(pollutant) - group2InitialTotalEmissions.get(userGroup).get(pollutant), decimalPlace);
				double pollutantDiffPct = BkNumberUtils.roundDouble(100 * (pollutantDiff / group2InitialTotalEmissions.get(userGroup).get(pollutant)), decimalPlace);
				System.out.println("Final emissions " + pollutant + " are calculated to\t" 
						+ BkNumberUtils.roundDouble(pollutant2FinalEmissions.get(pollutant), decimalPlace) + " [ Change: " + pollutantDiff + " or " + pollutantDiffPct + "% ]");
			}
		}
		SortedMap<String, Double> overallFinalTotalEmissions = summarizer.getTotalEmissions(person2FinalTotalEmissions);
		SortedMap<String, Double> overallInitialTotalEmissions = summarizer.getTotalEmissions(person2InitialTotalEmissions);
		System.out.println("\n*******************************************************************");
		System.out.println("VALUES FOR WHOLE POPULATION");
		System.out.println("*******************************************************************");
		for(String pollutant : overallFinalTotalEmissions.keySet()){
			double pollutantDiff = BkNumberUtils.roundDouble(overallFinalTotalEmissions.get(pollutant) - overallInitialTotalEmissions.get(pollutant), decimalPlace);
			double pollutantDiffPct = BkNumberUtils.roundDouble(100 * (pollutantDiff / overallInitialTotalEmissions.get(pollutant)), decimalPlace);
			System.out.println("Final emissions " + pollutant + " are calculated to\t" 
					+ BkNumberUtils.roundDouble(overallFinalTotalEmissions.get(pollutant), decimalPlace) + " [ Change: " + pollutantDiff + " or " + pollutantDiffPct + "% ]");
		}
	}

	private void calculateAverageTripLengthCar(String initialEventsFile, String finalEventsFile) {
		// TODO Auto-generated method stub
	}

	private void calculateAverageTripTravelTimePerMode(String initialEventsFile, String finalEventsFile) {
		// TODO Auto-generated method stub
	}

	private Scenario loadScenario(String netFile, String plansFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	public static void main(String[] args) {
		MultiAnalyzer ma = new MultiAnalyzer();
		ma.run();
	}

}
