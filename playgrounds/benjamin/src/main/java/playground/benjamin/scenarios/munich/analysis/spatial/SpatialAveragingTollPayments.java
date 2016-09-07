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

/**
 * @author julia, benjamin
 *
 */
public class SpatialAveragingTollPayments {
	
	private String scenarioName = "exposureInternalization"; // exposureInternalization, latsis, 981
	private String analysisCase = "exposurePricing"; // base, zone30, pricing, exposurePricing, 983
	
	private static final Logger logger = Logger.getLogger(SpatialAveragingTollPayments.class);

	private SpatialAveragingInputData inputData;
	private LinkWeightUtil linkWeightUtil;
	
	private void run() throws IOException{
		// init
//		inputData = new SpatialAveragingInputData(scenarioName, analysisCase);
		SpatialAveragingWriter sar = new SpatialAveragingWriter(inputData);
		linkWeightUtil = new LinkPointWeightUtil(inputData);
		
		SpatialGrid spatialGrid = runCase(inputData.getPlansFileBaseCase());
		
//		logger.info(inputData.getScenarioInformation());
		logger.info("Writing R output to " + inputData.getAnalysisOutPathBaseCase());
		String outputPathForR = inputData.getSpatialAveragingOutPathForCompareCase() + ".Routput." ;
		sar.writeRoutput(spatialGrid.getWeightedValuesOfGrid(), outputPathForR + "TollPaymentsByHomeLocation.txt");
		sar.writeRoutput(spatialGrid.getAverageValuesOfGrid(), outputPathForR + "AverageTollPaymentsByHomeLocation.txt");

	}

	private SpatialGrid runCase(String plansFile) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		PopulationReader mpr = new PopulationReader(scenario);
		mpr.readFile(plansFile);
		Population pop = scenario.getPopulation();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		MoneyEventHandler moneyEventHandler = new MoneyEventHandler();
		eventsManager.addHandler(moneyEventHandler);
		MatsimEventsReader mer = new MatsimEventsReader(eventsManager);
		mer.readFile(inputData.getEventsFileCompareCase());
		
		Map<Id<Person>, Double> personId2paidToll = moneyEventHandler.getPersonId2TollMap();
		
		LocationFilter lf = new LocationFilter();
		
		logger.info("Starting to distribute toll payments. This may take a while.");
		
		SpatialGrid spatialGrid = new SpatialGrid(inputData, inputData.getNoOfXbins(), inputData.getNoOfYbins());
		for(Id<Person> personId: personId2paidToll.keySet()){
			Person person = pop.getPersons().get(personId);
			//multiply with -1.0 since toll payments are negative
			Double unscaledPaidTollValue = (-1.0)*personId2paidToll.get(personId);
			Coord homeCoord = lf.getHomeActivityCoord(person);
			spatialGrid.distributeAndAddWelfare(homeCoord, unscaledPaidTollValue, linkWeightUtil, inputData.getScalingFactor());
		}
		
		// normalize
		spatialGrid.multiplyAllCells(linkWeightUtil.getNormalizationFactor());
		return spatialGrid;
	}

	public static void main(String[] args) throws IOException{
		new SpatialAveragingTollPayments().run();
	}
}