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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.benjamin.emissions.events.EmissionEventsReader;
import playground.benjamin.emissions.types.ColdPollutant;
import playground.benjamin.emissions.types.WarmPollutant;
import playground.benjamin.scenarios.munich.analysis.cupum.EmissionsPerGroupColdEventHandler;
import playground.benjamin.scenarios.munich.analysis.cupum.EmissionsPerGroupWarmEventHandler;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.zurich.analysis.MoneyEventHandler;

/**
 * @author benjamin
 *
 */
public class MultiAnalyzer {
	private static final Logger logger = Logger.getLogger(MultiAnalyzer.class);
	
	private static String runDirectory = "../../detailedEval/testRuns/output/1pct/v0-default/internalize/output_policyCase_pricing_x10/";
	private static String netFile = runDirectory + "output_network.xml.gz";

	private static String initialPlansFile = runDirectory + "ITERS/it.1000/1000.plans.xml.gz";
	private static String finalPlansFile = runDirectory + "ITERS/it.1100/1100.plans.xml.gz";
	
	private static String initialEventsFile = runDirectory + "ITERS/it.1000/1000.events.xml.gz";
	private static String finalEventsFile = runDirectory + "ITERS/it.1100/1100.events.xml.gz";
	
	private static String initialEmissionEventsFile = runDirectory + "ITERS/it.1000/1000.emission.events.xml.gz";
	private static String finalEmissionEventsFile = runDirectory + "ITERS/it.1100/1100.emission.events.xml.gz";
	
	private final Scenario initialScenario;
	private final Scenario finalScenario;
	private final PersonFilter personFilter;
	private static SortedSet<String> listOfPollutants;
	
	MultiAnalyzer(){
		Config config = ConfigUtils.createConfig();
		this.initialScenario = ScenarioUtils.createScenario(config);
		this.finalScenario = ScenarioUtils.createScenario(config);
		this.personFilter = new PersonFilter();
	}

	private void run() {
//		loadScenario(this.initialScenario, netFile, initialPlansFile);
//		loadScenario(this.finalScenario, netFile, finalPlansFile);
		
		calculateTollRevenueByUserGroup(finalEventsFile);
		calculateAverageTripTravelTimePerMode(initialEventsFile, finalEventsFile);
		calculateAverageTripLengthCar(initialEventsFile, finalEventsFile);
		calculateEmissionChangesByUserGroup(initialEmissionEventsFile, finalEmissionEventsFile);
		calculateWelfareChange(netFile, initialPlansFile, finalPlansFile);
	}

	private void calculateWelfareChange(String netFile, String initialPlansFile, String finalPlansFile) {
		// TODO Auto-generated method stub
		
	}

	private void calculateEmissionChangesByUserGroup(String initialEmissionEventsFile, String finalEmissionEventsFile) {
		listOfPollutants = new TreeSet<String>();
		for(WarmPollutant wp : WarmPollutant.values()){
			listOfPollutants.add(wp.toString());
		}
		for(ColdPollutant cp : ColdPollutant.values()){
			listOfPollutants.add(cp.toString());
		}

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		EmissionsPerGroupWarmEventHandler warmHandler = new EmissionsPerGroupWarmEventHandler();
		EmissionsPerGroupColdEventHandler coldHandler = new EmissionsPerGroupColdEventHandler();
		eventsManager.addHandler(warmHandler);
		eventsManager.addHandler(coldHandler);
		emissionReader.parse(finalEmissionEventsFile);
		
		Map<String, Map<String, Double>> group2FinalWarmEmissions = warmHandler.getWarmEmissionsPerGroup();
		Map<String, Map<String, Double>> group2FinalColdEmissions = coldHandler.getColdEmissionsPerGroup();
		Map<String, Map<String, Double>> group2FinalTotalEmissions = sumUpEmissions(group2FinalWarmEmissions, group2FinalColdEmissions);

		warmHandler.reset(0);
		coldHandler.reset(0);
		emissionReader.parse(initialEmissionEventsFile);
		
		Map<String, Map<String, Double>> group2InitialWarmEmissions = warmHandler.getWarmEmissionsPerGroup();
		Map<String, Map<String, Double>> group2InitialColdEmissions = coldHandler.getColdEmissionsPerGroup();
		Map<String, Map<String, Double>> group2InitialTotalEmissions = sumUpEmissions(group2InitialWarmEmissions, group2InitialColdEmissions);
		
		logger.info("*******************************************************************");
		for(String group : group2FinalTotalEmissions.keySet()){
			Map<String, Double> pollutant2Emissions = group2FinalTotalEmissions.get(group);
			for(String pollutant : pollutant2Emissions.keySet()){
				double pollutantDiff = pollutant2Emissions.get(pollutant) - group2InitialTotalEmissions.get(group).get(pollutant);
				double pollutantDiffPct = 100 * (pollutantDiff / group2InitialTotalEmissions.get(group).get(pollutant));
				logger.info("Final emissions for pollutant " + pollutant + " from ``" + group + "'' are calculated to " 
						+ pollutant2Emissions.get(pollutant) + " [ Change: "	+ pollutantDiff + " or " + pollutantDiffPct + "% ]");
			}
			logger.info("*******************************************************************");
		}
	}

	private void calculateAverageTripLengthCar(String initialEventsFile, String finalEventsFile) {
		// TODO Auto-generated method stub
	}

	private void calculateAverageTripTravelTimePerMode(String initialEventsFile, String finalEventsFile) {
		// TODO Auto-generated method stub
	}

	private void calculateTollRevenueByUserGroup(String finalEventsFile) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsReaderXMLv1 eventsReader = new EventsReaderXMLv1(eventsManager);
		MoneyEventHandler moneyEventHandler = new MoneyEventHandler();
		eventsManager.addHandler(moneyEventHandler);
		eventsReader.parse(finalEventsFile);
		
		Map<Id, Double> personId2Toll = moneyEventHandler.getPersonId2TollMap();

		double tollRevenue = 0.0;
		double tollRevenueFromMiD = 0.0;
		double tollRevenueFromInnCommuter = 0.0;
		double tollRevenueFromOutCommuter = 0.0;
		double tollRevenueFromFreight = 0.0;
		
		for(Id personId : personId2Toll.keySet()){
			tollRevenue += personId2Toll.get(personId);
			
			if(personFilter.isPersonFromMID(personId)) tollRevenueFromMiD += personId2Toll.get(personId);
			else if(personFilter.isPersonInnCommuter(personId)) tollRevenueFromInnCommuter += personId2Toll.get(personId);
			else if(personFilter.isPersonOutCommuter(personId)) tollRevenueFromOutCommuter += personId2Toll.get(personId);
			else if(personFilter.isPersonFreight(personId)) tollRevenueFromFreight += personId2Toll.get(personId);
			else logger.warn("Person " + personId + " cannot be matched to any user group!");
			
		}
		logger.info("*******************************************************************");
		// need to take the absolute value since money events are negative from the users' perspective.
		double absoluteTollRevenueFromMiD = Math.abs(tollRevenueFromMiD);
		logger.info("Toll revenue from ``MID'' is calculated to " + absoluteTollRevenueFromMiD);
		
		double absoluteTollRevenueFromInnCommuter = Math.abs(tollRevenueFromInnCommuter);
		logger.info("Toll revenue from ``innCommuter'' is calculated to " + absoluteTollRevenueFromInnCommuter);
		
		double absoluteTollRevenueFromOutCommuter = Math.abs(tollRevenueFromOutCommuter);
		logger.info("Toll revenue from ``outCommuter'' is calculated to " + absoluteTollRevenueFromOutCommuter);
		
		double absoluteTollRevenueFromFreight = Math.abs(tollRevenueFromFreight);
		logger.info("Toll revenue from ``freight'' is calculated to " + absoluteTollRevenueFromFreight);
		
		logger.info("*******************************************************************");
		double absoluteTollRevenue = Math.abs(tollRevenue);
		logger.info("Total toll revenue is calculated to " + absoluteTollRevenue);
		logger.info("*******************************************************************");
	}

	private static Map<String, Map<String, Double>> sumUpEmissions(Map<String, Map<String, Double>> warmEmissions, Map<String, Map<String, Double>> coldEmissions) {
		Map<String, Map<String, Double>> totalEmissions = new HashMap<String, Map<String, Double>>();
		for(Entry<String, Map<String, Double>> entry : warmEmissions.entrySet()){
			String group = entry.getKey();
			Map<String, Double> individualWarmEmissions = entry.getValue();

			if(coldEmissions.containsKey(group)){
				Map<String, Double> groupSumOfEmissions = new HashMap<String, Double>();
				Map<String, Double> groupColdEmissions = coldEmissions.get(group);
				Double individualValue;

				for(String pollutant : listOfPollutants){
					if(individualWarmEmissions.containsKey(pollutant)){
						if(groupColdEmissions.containsKey(pollutant)){
							individualValue = individualWarmEmissions.get(pollutant) + groupColdEmissions.get(pollutant);
						} else{
							individualValue = individualWarmEmissions.get(pollutant);
						}
					} else{
						individualValue = groupColdEmissions.get(pollutant);
					}
					groupSumOfEmissions.put(pollutant, individualValue);
				}
				totalEmissions.put(group, groupSumOfEmissions);
			} else{
				totalEmissions.put(group, individualWarmEmissions);
			}
		}
		return totalEmissions;
	}
	
	private void loadScenario(Scenario scenario, String netFile, String plansFile) {
		Config config = scenario.getConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario) ;
		scenarioLoader.loadScenario();
	}

	public static void main(String[] args) {
		MultiAnalyzer ma = new MultiAnalyzer();
		ma.run();
	}

}
