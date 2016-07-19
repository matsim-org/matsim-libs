/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialAveragingForLinkEmissions.java
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
package playground.benjamin.scenarios.munich.analysis.spatial;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.benjamin.scenarios.munich.analysis.filter.LocationFilter;
import playground.benjamin.scenarios.zurich.analysis.MoneyEventHandler;
import playground.benjamin.utils.spatialAvg.LinkPointWeightUtil;
import playground.benjamin.utils.spatialAvg.LinkWeightUtil;
import playground.benjamin.utils.spatialAvg.SpatialAveragingInputData;
import playground.benjamin.utils.spatialAvg.SpatialAveragingWriter;
import playground.benjamin.utils.spatialAvg.SpatialGrid;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

/**
 * @author julia, benjamin
 *
 */
public class SpatialAveragingWelfare {
	
	private String baseCase = "exposureInternalization"; // exposureInternalization, latsis, 981
	private String compareCase = "pricing"; // zone30, pricing, exposurePricing, 983
	private final boolean compareToBaseCase = true;
	
	private static final Logger logger = Logger.getLogger(SpatialAveragingWelfare.class);

	private SpatialAveragingInputData inputData;
	private LinkWeightUtil linkWeightUtil;
	private SpatialGrid baseCaseGrid, currentCaseGridNoRefund, currentCaseGridAvgRefund, currentCaseGridPersonalRefund;
	
	private void run() throws IOException{
		
		// init
//		inputData = new SpatialAveragingInputData(baseCase, compareCase);
		SpatialAveragingWriter sar = new SpatialAveragingWriter(inputData);
		linkWeightUtil = new LinkPointWeightUtil(inputData);
		
		// base case
		runCase(inputData.getPlansFileBaseCase(), inputData.getEventsFileBaseCase());
		baseCaseGrid = currentCaseGridNoRefund;
		
		logger.info("Writing R output to " + inputData.getAnalysisOutPathBaseCase());
		String outputPathForR = inputData.getAnalysisOutPathBaseCase() + ".Routput." ;
		sar.writeRoutput(baseCaseGrid.getWeightedValuesOfGrid(), outputPathForR + "UserBenefits.txt");
		sar.writeRoutput(baseCaseGrid.getAverageValuesOfGrid(), outputPathForR + "UserBenefitsAverage.txt");
		sar.writeRoutput(currentCaseGridNoRefund.getWeightsOfGrid(), outputPathForR + "UserBenefitsWeights.txt");

		// policy case
		if(compareToBaseCase){
			
			runCase(inputData.getPlansFileCompareCase(), inputData.getEventsFileCompareCase());
			
			logger.info("Writing R output for policy case to " + inputData.getAnalysisOutPathBaseCase());
			outputPathForR = inputData.getAnalysisOutPathCompareCase() + ".Routput." ;
			
			// write R output for policy case
			sar.writeRoutput(currentCaseGridNoRefund.getWeightedValuesOfGrid(), outputPathForR + "UserBenefits_NoRefund.txt");
			sar.writeRoutput(currentCaseGridPersonalRefund.getWeightedValuesOfGrid(), outputPathForR + "UserBenefits_PersonalRefund.txt");
			sar.writeRoutput(currentCaseGridAvgRefund.getWeightedValuesOfGrid(), outputPathForR + "UserBenefits_AverageRefund.txt");
			sar.writeRoutput(currentCaseGridNoRefund.getAverageValuesOfGrid(), outputPathForR + "UserBenefitsAverage_NoRefund.txt");
			sar.writeRoutput(currentCaseGridPersonalRefund.getAverageValuesOfGrid(), outputPathForR + "UserBenefitsAverage_PersonalRefund.txt");
			sar.writeRoutput(currentCaseGridAvgRefund.getAverageValuesOfGrid(), outputPathForR + "UserBenefitsAverage_AverageRefund.txt");
			
			// write differences policy <-> base case
			logger.info("Writing R output for differences to " + inputData.getAnalysisOutPathBaseCase());
			SpatialGrid differencesNoRefund = currentCaseGridNoRefund.getDifferencesAAverages(baseCaseGrid);
			sar.writeRoutput(differencesNoRefund.getWeightedValuesOfGrid(), outputPathForR + "UserBenefitsDifferencesNoRefund.txt");
			sar.writeRoutput(differencesNoRefund.getAverageValuesOfGrid(), outputPathForR + "AvgUserBenefitsDifferencesNoRefund.txt");
			SpatialGrid differencesPersonalRefund = currentCaseGridPersonalRefund.getDifferencesAAverages(baseCaseGrid);
			sar.writeRoutput(differencesPersonalRefund.getWeightedValuesOfGrid(), outputPathForR + "UserBenefitsDifferencesPersonalRefund.txt");
			sar.writeRoutput(differencesPersonalRefund.getAverageValuesOfGrid(), outputPathForR + "AvgUserBenefitsDifferencesAverageRefund.txt");
			SpatialGrid differencesAvgRefund = currentCaseGridAvgRefund.getDifferencesAAverages(baseCaseGrid);
			sar.writeRoutput(differencesAvgRefund.getWeightedValuesOfGrid(), outputPathForR + "UserBenefitsDifferencesAverageRefund.txt");
			sar.writeRoutput(differencesAvgRefund.getAverageValuesOfGrid(), outputPathForR + "AvgUserBenefitsDifferencesAvgerageRefund.txt");
		}
	}

	private void runCase(String plansFile, String eventsFile) {
		// init, calculate basic utility without refund per person
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		PopulationReader mpr = new PopulationReader(scenario);
		mpr.readFile(plansFile);
		Population pop = scenario.getPopulation();
		UserBenefitsCalculator ubc = new UserBenefitsCalculator(config, WelfareMeasure.LOGSUM, false);
		ubc.calculateUtility_money(pop);
		Map<Id<Person>, Double> personId2UtilityWithoutRefund = ubc.getPersonId2MonetizedUtility();
		
		
		// calculate personal / average refund
		MoneyEventHandler moneyEventHandler = new MoneyEventHandler();
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(moneyEventHandler);
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		eventsReader.readFile(eventsFile);
		// sign correct?
		// personal refund
		Map<Id<Person>, Double> personId2PersonalRefund = moneyEventHandler.getPersonId2TollMap();
		Map<Id<Person>, Double> personId2UtilityWithPersonalRefund = sumUpUtilities(personId2UtilityWithoutRefund, personId2PersonalRefund);
		// avg refund
		Double avgRefund = moneyEventHandler.getSumOfTollPayments()/pop.getPersons().size();
		
		Map<Id<Person>, Double> personId2UtilityWithAverageRefund = addAvgRefundToUtilities(personId2UtilityWithoutRefund, avgRefund);
		
		LocationFilter lf = new LocationFilter();
		logger.info("There were " + ubc.getPersonsWithoutValidPlanCnt() + " persons without any valid plan.");
		logger.info("Starting to distribute welfare. This may take a while.");
		
		currentCaseGridNoRefund = getGridFromUtilities(pop, personId2UtilityWithoutRefund, lf);
		currentCaseGridAvgRefund = getGridFromUtilities(pop, personId2UtilityWithAverageRefund, lf);
		currentCaseGridPersonalRefund = getGridFromUtilities(pop, personId2UtilityWithPersonalRefund, lf);
	}

	private Map<Id<Person>, Double> addAvgRefundToUtilities(
			Map<Id<Person>, Double> personId2UtilityWithoutRefund, Double avgRefund) {
		Map<Id<Person>, Double> personId2UtilityWithAvgRefund = new HashMap<Id<Person>, Double>();
		for(Id<Person> personId : personId2UtilityWithoutRefund.keySet()){
			personId2UtilityWithAvgRefund.put(personId, personId2UtilityWithoutRefund.get(personId)+avgRefund);
		}
		return personId2UtilityWithAvgRefund;
	}

	private SpatialGrid getGridFromUtilities(Population pop,
			Map<Id<Person>, Double> personId2Utility, LocationFilter lf) {
		SpatialGrid spatialGrid = new SpatialGrid(inputData, inputData.getNoOfXbins(), inputData.getNoOfYbins());
		for(Id<Person> personId: personId2Utility.keySet()){
			Person person = pop.getPersons().get(personId);
			Double unscaledUtilityValue = personId2Utility.get(personId);
			Coord homeCoord = lf.getHomeActivityCoord(person);
			spatialGrid.distributeAndAddWelfare(homeCoord, unscaledUtilityValue, linkWeightUtil, inputData.getScalingFactor());
		}
		
		// normalize 
		spatialGrid.multiplyAllCells(linkWeightUtil.getNormalizationFactor());
		return spatialGrid;
	}

	private Map<Id<Person>, Double> sumUpUtilities(Map<Id<Person>, Double> personId2Utility, Map<Id<Person>, Double> personId2Refund) {
		Map<Id<Person>, Double> sumOfUtilities = new HashMap<Id<Person>, Double>();
		for(Id<Person> personId : personId2Utility.keySet()){
			double sumOfUtility = personId2Utility.get(personId);;
			if(personId2Refund.get(personId) != null){
				sumOfUtility += personId2Refund.get(personId);
			}
			sumOfUtilities.put(personId, sumOfUtility);
		}
		return sumOfUtilities;
	}

	public static void main(String[] args) throws IOException{
		new SpatialAveragingWelfare().run();
	}
}