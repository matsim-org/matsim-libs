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
package playground.benjamin.scenarios.munich.analysis;

import java.util.SortedSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

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

	private static Scenario scenario;
	
	public static void main(String[] args) {
		loadScenario();
		
		Population population = scenario.getPopulation();
		EmissionsPerPersonAggregator epa = new EmissionsPerPersonAggregator(population, emissionFile);
		epa.run();
		
		SortedSet<String> listOfPollutants = epa.getListOfPollutants();
		EmissionWriter emissionWriter = new EmissionWriter();
		emissionWriter.writeHomeLocation2Emissions(
				population,
				listOfPollutants,
				epa.getWarmEmissions(),
				runDirectory + runNumber + "." + lastIteration + ".emissionsWarmPerHomeLocation.txt");
		emissionWriter.writeHomeLocation2Emissions(
				population,
				listOfPollutants,
				epa.getColdEmissions(),
				runDirectory + runNumber + "." + lastIteration + ".emissionsColdPerHomeLocation.txt");
		emissionWriter.writeHomeLocation2Emissions(
				population,
				listOfPollutants,
				epa.getTotalEmissions(),
				runDirectory + runNumber + "." + lastIteration + ".emissionsTotalPerHomeLocation.txt");
	}
	
	@SuppressWarnings("deprecation")
	private static void loadScenario() {
		Config config = ConfigUtils.createConfig();
		scenario = ScenarioUtils.createScenario(config);
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario) ;
		scenarioLoader.loadScenario() ;
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
