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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.benjamin.emissions.events.EmissionEventsReader;
import playground.benjamin.emissions.types.ColdPollutant;
import playground.benjamin.emissions.types.WarmPollutant;
import playground.benjamin.scenarios.munich.analysis.EmissionUtils;
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

//	private static String initialIterationNo = "1000";
	private static String finalIterationNo = "1500";

	private static String baseCaseName = "ctd";
	private static String policyCaseName = "zone30";
//	private static String policyCaseName = "pricing";
//	private static String policyCaseName = "pricing_x5";
//	private static String policyCaseName = "pricing_x10";

	private static String runDirectory1 = "../../runs-svn/detEval/kuhmo/output/output_baseCase_" + baseCaseName + "/";
	private static String runDirectory2 = "../../runs-svn/detEval/kuhmo/output/output_policyCase_" + policyCaseName + "/";
	private static String netFile = runDirectory1 + "output_network.xml.gz";
	private static String configFile = runDirectory1 + "output_config.xml.gz";

	private static String plansFile1 = runDirectory1 + "ITERS/it." + finalIterationNo + "/" + finalIterationNo + ".plans.xml.gz";
	private static String plansFile2 = runDirectory2 + "ITERS/it." + finalIterationNo + "/" + finalIterationNo + ".plans.xml.gz";

	private static String eventsFile1 = runDirectory1 + "ITERS/it." + finalIterationNo + "/" + finalIterationNo + ".events.xml.gz";
	private static String eventsFile2 = runDirectory2 + "ITERS/it." + finalIterationNo + "/" + finalIterationNo + ".events.xml.gz";

	private static String emissionEventsFile1 = runDirectory1 + "ITERS/it." + finalIterationNo + "/" + finalIterationNo + ".emission.events.xml.gz";
	private static String emissionEventsFile2 = runDirectory2 + "ITERS/it." + finalIterationNo + "/" + finalIterationNo + ".emission.events.xml.gz";

	private final PersonFilter personFilter;
	private final int decimalPlace;

	private final MultiAnalyzerWriter writer;

	MultiAnalyzer(){
		this.personFilter = new PersonFilter();
		this.decimalPlace = 3;
		this.writer = new MultiAnalyzerWriter(runDirectory1);
	}

	private void run() {
//		calculateUserWelfareChangeAndTollRevenueByUserGroup(netFile, configFile, plansFile1, plansFile2, eventsFile2);
		
		calculateDistanceTimeStatisticsByUserGroup(netFile, eventsFile1, baseCaseName);
		calculateDistanceTimeStatisticsByUserGroup(netFile, eventsFile2, policyCaseName);
		
//		calculateEmissionChangesByUserGroup(emissionEventsFile1, emissionEventsFile2);
	}

	private void calculateUserWelfareChangeAndTollRevenueByUserGroup(String netFile, String configFile, String initialPlansFile, String finalPlansFile, String finalEventsFile) {
		UserWelfareCalculator userWelfareCalculator = new UserWelfareCalculator(configFile);

		Scenario initialScenario = loadScenario(netFile, initialPlansFile);
		Scenario finalScenario = loadScenario(netFile, finalPlansFile);
		Population initialPop = initialScenario.getPopulation();
		Population finalPop = finalScenario.getPopulation();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsReaderXMLv1 eventsReader = new EventsReaderXMLv1(eventsManager);
		MoneyEventHandler moneyEventHandler = new MoneyEventHandler();
		eventsManager.addHandler(moneyEventHandler);
		eventsReader.parse(finalEventsFile);

		Map<Id, Double> personId2Toll = moneyEventHandler.getPersonId2TollMap();
		
//		writer.setRunName(policyCaseName);
//		writer.writeWelfareInformation(initialPop, personId2Toll);
		
		
		for(UserGroup userGroup : UserGroup.values()){
			Population initialUserGroupPop = personFilter.getPopulation(initialPop, userGroup);
			Population finalUserGroupPop = personFilter.getPopulation(finalPop, userGroup);

			double initialUserWelfare = userWelfareCalculator.calculateLogsum(initialUserGroupPop);
			int initialPersonWithNoValidPlanCnt = userWelfareCalculator.getNoValidPlanCnt();
			userWelfareCalculator.reset();

			double finalUserWelfare = userWelfareCalculator.calculateLogsum(finalUserGroupPop);
			int finalPersonWithNoValidPlanCnt = userWelfareCalculator.getNoValidPlanCnt();
			userWelfareCalculator.reset();

			double userWelfareDiff = finalUserWelfare - initialUserWelfare;
			double userWelfareDiffPct = 100. * (userWelfareDiff / initialUserWelfare);

			System.out.println("\n*******************************************************************");
			System.out.println("VALUES FOR " + userGroup);
			System.out.println("*******************************************************************");
			System.out.println("Final user welfare:\t\t" + BkNumberUtils.roundDouble(finalUserWelfare, decimalPlace));
			System.out.println("Initial user welfare:\t\t" + BkNumberUtils.roundDouble(initialUserWelfare, decimalPlace));
			System.out.println("===================================================================");
			System.out.println("Change in user welfare:\t\t" + BkNumberUtils.roundDouble(userWelfareDiff, decimalPlace) 
					+ " or " + BkNumberUtils.roundDouble(userWelfareDiffPct, decimalPlace) + "%");
			System.out.println("*******************************************************************");
			System.out.println("Final users with no valid plan (all scores ``== null'' or ``<= 0.0''):\t" + initialPersonWithNoValidPlanCnt);
			System.out.println("Initial users with no valid plan (all scores ``== null'' or ``<= 0.0''):\t" + finalPersonWithNoValidPlanCnt);
			System.out.println("*******************************************************************\n");

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
			System.out.println("Toll revenue from ``" + userGroup + "'' (" + groupSize + " toll payers):\t" + 
					BkNumberUtils.roundDouble(absoluteTollRevenueUserGroup, decimalPlace));
		}

		double initialUserWelfare = userWelfareCalculator.calculateLogsum(initialPop);
		int initialPersonWithNoValidPlanCnt = userWelfareCalculator.getNoValidPlanCnt();
		userWelfareCalculator.reset();

		double finalUserWelfare = userWelfareCalculator.calculateLogsum(finalPop);
		int finalPersonWithNoValidPlanCnt = userWelfareCalculator.getNoValidPlanCnt();
		userWelfareCalculator.reset();

		double userWelfareDiff = finalUserWelfare - initialUserWelfare;
		double userWelfareDiffPct = 100 * (userWelfareDiff / initialUserWelfare);

		System.out.println("\n*******************************************************************");
		System.out.println("VALUES FOR WHOLE POPULATION");
		System.out.println("*******************************************************************");
		System.out.println("Final user welfare:\t\t" + BkNumberUtils.roundDouble(finalUserWelfare, decimalPlace));
		System.out.println("Initial user welfare:\t\t" + BkNumberUtils.roundDouble(initialUserWelfare, decimalPlace));
		System.out.println("===================================================================");
		System.out.println("Change in user welfare:\t\t" + BkNumberUtils.roundDouble(userWelfareDiff, decimalPlace) 
				+ " or " + BkNumberUtils.roundDouble(userWelfareDiffPct, decimalPlace) + "%");
		System.out.println("*******************************************************************");
		System.out.println("Final users with no valid plan (all scores ``== null'' or ``<= 0.0''):\t" + initialPersonWithNoValidPlanCnt);
		System.out.println("Initial users with no valid plan (all scores ``== null'' or ``<= 0.0''):\t" + finalPersonWithNoValidPlanCnt);
		System.out.println("*******************************************************************\n");
		
		double tollRevenue = 0.0;
		for(Id personId : personId2Toll.keySet()){
			tollRevenue += personId2Toll.get(personId);
		}
		double absoluteTollRevenue = Math.abs(tollRevenue);
		System.out.println("===================================================================");
		System.out.println("Total toll revenue from " + personId2Toll.size() + " toll payers:\t\t" +
				BkNumberUtils.roundDouble(absoluteTollRevenue, decimalPlace));
		System.out.println("*******************************************************************\n");
	}

	private void calculateEmissionChangesByUserGroup(String initialEmissionEventsFile, String finalEmissionEventsFile) {
		EmissionUtils summarizer = new EmissionUtils();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		EmissionsPerPersonWarmEventHandler warmHandler = new EmissionsPerPersonWarmEventHandler();
		EmissionsPerPersonColdEventHandler coldHandler = new EmissionsPerPersonColdEventHandler();
		eventsManager.addHandler(warmHandler);
		eventsManager.addHandler(coldHandler);
		emissionReader.parse(finalEmissionEventsFile);

		Map<Id, Map<WarmPollutant, Double>> person2FinalWarmEmissions = warmHandler.getWarmEmissionsPerPerson();
		Map<Id, Map<ColdPollutant, Double>> person2FinalColdEmissions = coldHandler.getColdEmissionsPerPerson();
		Map<Id, SortedMap<String, Double>> person2FinalTotalEmissions = summarizer.sumUpEmissionsPerId(person2FinalWarmEmissions, person2FinalColdEmissions);
		SortedMap<UserGroup, SortedMap<String, Double>> group2FinalTotalEmissions = summarizer.getEmissionsPerGroup(person2FinalTotalEmissions);

		writer.setRunName(policyCaseName);
		writer.writeEmissionInformation(group2FinalTotalEmissions);

		warmHandler.reset(0);
		coldHandler.reset(0);
		emissionReader.parse(initialEmissionEventsFile);

		Map<Id, Map<WarmPollutant, Double>> person2InitialWarmEmissions = warmHandler.getWarmEmissionsPerPerson();
		Map<Id, Map<ColdPollutant, Double>> person2InitialColdEmissions = coldHandler.getColdEmissionsPerPerson();
		Map<Id, SortedMap<String, Double>> person2InitialTotalEmissions = summarizer.sumUpEmissionsPerId(person2InitialWarmEmissions, person2InitialColdEmissions);
		SortedMap<UserGroup, SortedMap<String, Double>> group2InitialTotalEmissions = summarizer.getEmissionsPerGroup(person2InitialTotalEmissions);

		writer.setRunName(baseCaseName);
		writer.writeEmissionInformation(group2InitialTotalEmissions);

		for(UserGroup userGroup : group2FinalTotalEmissions.keySet()){
			System.out.println("\n*******************************************************************");
			System.out.println("VALUES FOR " + userGroup);
			System.out.println("*******************************************************************");
			Map<String, Double> pollutant2FinalEmissions = group2FinalTotalEmissions.get(userGroup);
			for(String pollutant : pollutant2FinalEmissions.keySet()){
				double pollutantDiff = BkNumberUtils.roundDouble(pollutant2FinalEmissions.get(pollutant) - group2InitialTotalEmissions.get(userGroup).get(pollutant), decimalPlace);
				double pollutantDiffPct = BkNumberUtils.roundDouble(100 * (pollutantDiff / group2InitialTotalEmissions.get(userGroup).get(pollutant)), decimalPlace);
				System.out.println("Final emissions for " + pollutant + ":\t" 
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
			System.out.println("Final emissions for " + pollutant + ":\t" 
					+ BkNumberUtils.roundDouble(overallFinalTotalEmissions.get(pollutant), decimalPlace) + " [ Change: " + pollutantDiff + " or " + pollutantDiffPct + "% ]");
		}
	}

	private void calculateDistanceTimeStatisticsByUserGroup(String netFile, String eventsFile, String runName) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc).readFile(netFile);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsReaderXMLv1 eventsReader = new EventsReaderXMLv1(eventsManager);
		
		CarDistanceEventHandler carDistanceEventHandler = new CarDistanceEventHandler(sc.getNetwork());
		TravelTimePerModeEventHandler ttHandler = new TravelTimePerModeEventHandler();

		eventsManager.addHandler(carDistanceEventHandler);
		eventsManager.addHandler(ttHandler);

		eventsReader.parse(eventsFile);
		
		Map<Id, Double> personId2carDistance = carDistanceEventHandler.getPersonId2CarDistance();
		Map<UserGroup, Double> userGroup2carTrips = carDistanceEventHandler.getUserGroup2carTrips();
		Map<String, Map<Id, Double>> mode2personId2TravelTime = ttHandler.getMode2personId2TravelTime();
		Map<UserGroup, Map<String, Double>> userGroup2mode2noOfTrips = ttHandler.getUserGroup2mode2noOfTrips();
		
		logger.warn("number of car users in distance map (users with departure events): " + personId2carDistance.size());
//		int depArrOnSameLinkCnt = carDistanceEventHandler.getDepArrOnSameLinkCnt().size();
//		logger.warn("number of car users with two activities followed one by another on the same link: +" + depArrOnSameLinkCnt);
//		int personIsDrivingADistance = 0;
//		for(Id personId : carDistanceEventHandler.getDepArrOnSameLinkCnt().keySet()){
//			if(personId2carDistance.get(personId) == null){
//				// do nothing
//			} else {
//				personIsDrivingADistance ++;
//			}
//		}
//		logger.warn("number of car users with two activities followed one by another on the same link BUT driving to other acts: -" + personIsDrivingADistance);
		logger.warn("number of car users in traveltime map (users with departure and arrival events): " + mode2personId2TravelTime.get(TransportMode.car).size());
		
		
		writer.setRunName(runName);
		writer.writeCarDistanceInformation(personId2carDistance, userGroup2carTrips);
		writer.writeAvgTTInformation(mode2personId2TravelTime, userGroup2mode2noOfTrips);
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