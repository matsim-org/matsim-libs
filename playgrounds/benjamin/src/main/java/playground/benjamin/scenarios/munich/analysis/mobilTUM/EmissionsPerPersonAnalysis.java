/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionsPerPersonAnalysis.java
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
package playground.benjamin.scenarios.munich.analysis.mobilTUM;

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.benjamin.emissions.events.EmissionEventsReader;
import playground.benjamin.emissions.types.ColdPollutant;
import playground.benjamin.emissions.types.WarmPollutant;
import playground.benjamin.scenarios.munich.analysis.EmissionSummarizer;
import playground.benjamin.scenarios.munich.analysis.EmissionWriter;


/**
 * @author benjamin
 *
 */
public class EmissionsPerPersonAnalysis {

	private final static String runNumber = "973";
	private final static String runDirectory = "../../runs-svn/run" + runNumber + "/";
	
	private final static String netFile = runDirectory + runNumber + ".output_network.xml.gz";
	private final static String plansFile = runDirectory + runNumber + ".output_plans.xml.gz";
	private static String configFile = runDirectory + runNumber + ".output_config.xml.gz";
	private final static Integer lastIteration = getLastIteration(configFile);
	private final static String emissionFile = runDirectory + runNumber + "." + lastIteration + ".emission.events.xml.gz";
	
//	private final static String netFile = runDirectory + "output_network.xml.gz";
//	private final static String plansFile = runDirectory + "output_plans.xml.gz";
//	private final static String emissionFile = runDirectory + runNumber + ".emission.events.xml.gz";

	public static void main(String[] args) {
		Scenario scenario = loadScenario(netFile, plansFile);
		
		Population population = scenario.getPopulation();
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		EmissionsPerPersonWarmEventHandler warmHandler = new EmissionsPerPersonWarmEventHandler();
		EmissionsPerPersonColdEventHandler coldHandler = new EmissionsPerPersonColdEventHandler();
		eventsManager.addHandler(warmHandler);
		eventsManager.addHandler(coldHandler);
		emissionReader.parse(emissionFile);
		
		Map<Id, Map<WarmPollutant, Double>> warmEmissions = warmHandler.getWarmEmissionsPerPerson();
		Map<Id, Map<ColdPollutant, Double>> coldEmissions = coldHandler.getColdEmissionsPerPerson();
		
		EmissionSummarizer ems = new EmissionSummarizer();
		Map<Id, SortedMap<String, Double>> totalEmissions = ems.sumUpEmissionsPerPerson(warmEmissions, coldEmissions);
		Map<Id, SortedMap<String, Double>> filledTotalEmissions = ems.setNonCalculatedEmissions(population, totalEmissions);
		
		SortedSet<String> listOfPollutants = ems.getListOfPollutants();
		EmissionWriter emissionWriter = new EmissionWriter();
//		emissionWriter.writeHomeLocation2TotalEmissions(
//				population,
//				listOfPollutants,
//				filledWarmEmissions,
//				runDirectory + runNumber + "." + lastIteration + ".emissionsWarmPerHomeLocation.txt");
//		emissionWriter.writeHomeLocation2TotalEmissions(
//				population,
//				listOfPollutants,
//				filledColdEmissions,
//				runDirectory + runNumber + "." + lastIteration + ".emissionsColdPerHomeLocation.txt");
		emissionWriter.writeHomeLocation2TotalEmissions(
				population,
				listOfPollutants,
				filledTotalEmissions,
				runDirectory + runNumber + "." + lastIteration + ".emissionsTotalPerHomeLocation.txt");
	}
	
	private static Scenario loadScenario(String netfile, String plansfile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	private static Integer getLastIteration(String configFile) {
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configFile);
		Integer lastIteration = config.controler().getLastIteration();
		return lastIteration;
	}
}
