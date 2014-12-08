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
package playground.benjamin.scenarios.munich.analysis.spatialAvg;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.benjamin.scenarios.munich.analysis.filter.LocationFilter;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.LinkPointWeightUtil;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.LinkWeightUtil;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.SpatialAveragingInputData;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.SpatialAveragingParameters;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.SpatialAveragingWriter;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.SpatialGrid;
import playground.benjamin.scenarios.zurich.analysis.MoneyEventHandler;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

/**
 * @author julia, benjamin
 *
 */
public class SpatialAveragingWelfare {
	
	private String baseCase = "exposureInternalization"; // exposureInternalization, latsis, 981
	private String compareCase = "exposurePricing"; // zone30, pricing, exposurePricing, 983
	private final boolean compareToBaseCase = true;
	
	private static final Logger logger = Logger.getLogger(SpatialAveragingWelfare.class);

	private SpatialAveragingInputData inputData;
	private LinkWeightUtil linkWeightUtil;
	private SpatialAveragingParameters parameters;
	
	private void run() throws IOException{
		
		// init
		inputData = new SpatialAveragingInputData(baseCase, compareCase);
		parameters = new SpatialAveragingParameters();
		SpatialAveragingWriter sar = new SpatialAveragingWriter(inputData, parameters);
		linkWeightUtil = new LinkPointWeightUtil(inputData, parameters);
		
		// base case
		SpatialGrid spatialGrid = runCase(inputData.getPlansFileBaseCase());
		SpatialGrid baseCaseGrid = spatialGrid;
		
		logger.info("Writing R output to " + inputData.getAnalysisOutPathForBaseCase());
		String outputPathForR = inputData.getAnalysisOutPathForBaseCase() + ".Routput." ;
		sar.writeRoutput(spatialGrid.getWeightedValuesOfGrid(), outputPathForR + "UserBenefits.txt");
		sar.writeRoutput(spatialGrid.getAverageValuesOfGrid(), outputPathForR + "UserBenefitsAverage.txt");

		// policy case
		if(compareToBaseCase){
			
			SpatialGrid compareGrid = runCase(inputData.getPlansFileCompareCase());
			
			logger.info("Writing R output for differences to " + inputData.getAnalysisOutPathForBaseCase());
			outputPathForR = inputData.getAnalysisOutPathForSpatialComparison() + ".Routput." ;
						
			SpatialGrid differences = compareGrid.getDifferencesAAverages(baseCaseGrid); //TODO document special average calculation this should result in weights =0
			sar.writeRoutput(differences.getWeightedValuesOfGrid(), outputPathForR + "UserBenefitsDifferences.txt");
			sar.writeRoutput(differences.getAverageValuesOfGrid(), outputPathForR + "UserBenefitsAverageDifferences.txt");
		}
	}

	private SpatialGrid runCase(String plansFile) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		MatsimPopulationReader mpr = new MatsimPopulationReader(scenario);
		mpr.readFile(plansFile);
		Population pop = scenario.getPopulation();
		UserBenefitsCalculator ubc = new UserBenefitsCalculator(config, WelfareMeasure.LOGSUM, false);
		ubc.calculateUtility_money(pop);
		Map<Id, Double> personId2Utility = ubc.getPersonId2MonetizedUtility();
//		// calculate personal / average refund
//		MoneyEventHandler moneyEventHandler = new MoneyEventHandler();
//		eventsManager.addHandler(moneyEventHandler);
//		eventsReader.parse(eventsFile);
//		// sign correct?
//		Map<Id, Double> personId2PersonalRefund = moneyEventHandler.getPersonId2TollMap();
//		double sumOfTollPayments = moneyEventHandler.getSumOfTollPayments();
//		Map<Id, Double> personId2AverageRefund = calculateAvgRefund(sumOfTollPayments, pop);
//		Map<Id, Double> personId2UtilityPersonalRefund = sumUpUtilities(personId2Utility, personId2PersonalRefund);
//		Map<Id, Double> personId2UtilityAverageRefund = sumUpUtilities(personId2Utility, personId2AverageRefund);
		
		LocationFilter lf = new LocationFilter();
		logger.info("There were " + ubc.getPersonsWithoutValidPlanCnt() + " persons without any valid plan.");
		logger.info("Starting to distribute welfare. This may take a while.");
		
		SpatialGrid spatialGrid = new SpatialGrid(inputData, parameters.getNoOfXbins(), parameters.getNoOfYbins());
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

	private Map<Id, Double> calculateAvgRefund(double sumOfTollPayments, Population pop) {
		Map<Id, Double> personId2AvgRefund = new HashMap<Id, Double>();
		double avgRefund = sumOfTollPayments / pop.getPersons().size();
		for(Id personId : pop.getPersons().keySet()){
			personId2AvgRefund.put(personId, avgRefund);
		}
		return personId2AvgRefund;
	}

	private Map<Id, Double> sumUpUtilities(Map<Id, Double> personId2Utility, Map<Id, Double> personId2Refund) {
		Map<Id, Double> sumOfUtilities = new HashMap<Id, Double>();
		for(Id personId : personId2Utility.keySet()){
			double sumOfUtility;
			if(personId2Refund.get(personId) != null){
				sumOfUtility = personId2Utility.get(personId) + personId2Refund.get(personId);
			} else {
				sumOfUtility = personId2Utility.get(personId);
			}
			sumOfUtilities.put(personId, sumOfUtility);
		}
		return sumOfUtilities;
	}

	public static void main(String[] args) throws IOException{
		new SpatialAveragingWelfare().run();
	}
}