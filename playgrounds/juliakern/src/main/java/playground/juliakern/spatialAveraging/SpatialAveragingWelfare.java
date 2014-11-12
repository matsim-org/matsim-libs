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
package playground.juliakern.spatialAveraging;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
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
import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

/**
 * @author julia, benjamin
 *
 */
public class SpatialAveragingWelfare {
	private static final Logger logger = Logger.getLogger(SpatialAveragingWelfare.class);
	
	private SpatialAveragingInputData inputData;
	private String baseCase = "exposureInternalization"; // exposureInternalization, latsis, 981
	private String compareCase = "exposurePricing"; // zone30, pricing, exposurePricing, 983
	
	private SpatialAveragingParameters parameters;
	
	final boolean compareToBaseCase = true;
	
	LocationFilter lf;
	Network network;

	private LinkWeightUtil linkWeightUtil;

	private SpatialGrid baseCaseGrid;

	private SpatialAveragingWriter sar;

	private void run() throws IOException{
		
		inputData = new SpatialAveragingInputData(baseCase, compareCase);
		parameters = new SpatialAveragingParameters();
		sar = new SpatialAveragingWriter(inputData, parameters);
		lf = new LocationFilter();
		linkWeightUtil = new LinkPointWeightUtil(inputData, parameters);
		
		Scenario scenario = loadScenario(inputData.getNetworkFile());
		this.network = scenario.getNetwork();		
		MatsimPopulationReader mpr = new MatsimPopulationReader(scenario);
		mpr.readFile(inputData.getPlansFileBaseCase());
		
		Config config = scenario.getConfig();
		Population pop = scenario.getPopulation();
		UserBenefitsCalculator ubc = new UserBenefitsCalculator(config, WelfareMeasure.LOGSUM, false);
		ubc.calculateUtility_money(pop);
		Map<Id, Double> personId2Utility = ubc.getPersonId2MonetizedUtility();
		logger.info("There were " + ubc.getPersonsWithoutValidPlanCnt() + " persons without any valid plan.");
		
		SpatialGrid spatialGrid = new SpatialGrid(inputData, parameters.getNoOfXbins(), parameters.getNoOfYbins());
		for(Id<Person> personId: personId2Utility.keySet()){
			Person person = pop.getPersons().get(personId);
			Double unscaledUtilityValue = personId2Utility.get(personId);
			Coord homeCoord = this.lf.getHomeActivityCoord(person);
			spatialGrid.distributeAndAddWelfare(homeCoord, unscaledUtilityValue, linkWeightUtil, inputData.getScalingFactor());
		}
		
		// normalize 
		spatialGrid.multiplyAllCells(linkWeightUtil.getNormalizationFactor());
		this.baseCaseGrid = spatialGrid;
		
		logger.info("Writing R output to " + inputData.getAnalysisOutPathForBaseCase());
		String outputPathForR = inputData.getAnalysisOutPathForBaseCase() + ".Routput." ;
		this.sar.writeRoutput(spatialGrid.getWeightedValuesOfGrid(), outputPathForR + "UserBenefits.txt");
		this.sar.writeRoutput(spatialGrid.getAverageValuesOfGrid(), outputPathForR + "UserBenefitsAverage.txt");

		if(compareToBaseCase){
			Scenario scenario2 = loadScenario(inputData.getNetworkFile());
			MatsimPopulationReader mpr2 = new MatsimPopulationReader(scenario2);
			mpr2.readFile(inputData.getPlansFileCompareCase());
			
			Config config2 = scenario2.getConfig();
			Population pop2 = scenario2.getPopulation();
			UserBenefitsCalculator ubc2 = new UserBenefitsCalculator(config2, WelfareMeasure.LOGSUM, false);
			ubc2.calculateUtility_money(pop2);
			Map<Id, Double> personId2Utility2 = ubc2.getPersonId2MonetizedUtility();
			logger.info("There were " + ubc2.getPersonsWithoutValidPlanCnt() + " persons without any valid plan.");
			
			SpatialGrid spatialGridCompareCase = new SpatialGrid(inputData, parameters.getNoOfXbins(), parameters.getNoOfYbins());
			for(Id<Person> personId: personId2Utility2.keySet()){
				Person person = pop2.getPersons().get(personId);
				Double unscaledUtilityValue = personId2Utility2.get(personId);
				Coord homeCoord = this.lf.getHomeActivityCoord(person);
				spatialGridCompareCase.distributeAndAddWelfare(homeCoord, unscaledUtilityValue, linkWeightUtil, inputData.getScalingFactor());
			}
			
			// normalize
			spatialGridCompareCase.multiplyAllCells(linkWeightUtil.getNormalizationFactor());
			
			logger.info("Writing R output for differences to " + inputData.getAnalysisOutPathForBaseCase());
			outputPathForR = inputData.getAnalysisOutPathForCompareCase() + ".Routput." ;
						
			SpatialGrid differences = spatialGridCompareCase.getDifferencesAAverages(baseCaseGrid); //TODO document special average calculation this should result in weights =0
			sar.writeRoutput(differences.getWeightedValuesOfGrid(), outputPathForR + "UserBenefitsDifferences.txt");
			sar.writeRoutput(differences.getAverageValuesOfGrid(), outputPathForR + "UserBenefitsAverageDifferences.txt");
		}
	}

	private Scenario loadScenario(String netFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	public static void main(String[] args) throws IOException{
		new SpatialAveragingWelfare().run();
	}
}