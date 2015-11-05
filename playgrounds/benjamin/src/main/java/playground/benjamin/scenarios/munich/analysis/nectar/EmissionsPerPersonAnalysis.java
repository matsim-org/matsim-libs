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
package playground.benjamin.scenarios.munich.analysis.nectar;

import java.util.Map;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.utils.EmissionUtils;
import org.matsim.contrib.emissions.utils.EmissionWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;

/**
 * @author benjamin
 *
 */
public class EmissionsPerPersonAnalysis {

	private final String runNumber = "981";
	private final String runDirectory = "../../runs-svn/run" + runNumber + "/";

	private final String netFile = runDirectory + runNumber + ".output_network.xml.gz";
	private final String plansFile = runDirectory + runNumber + ".output_plans.xml.gz";
	private final String configFile = runDirectory + runNumber + ".output_config.xml.gz";
	private final Integer lastIteration = getLastIteration(configFile);
	private final String emissionFile = runDirectory + "ITERS/it." + lastIteration + "/"+ runNumber + "." + lastIteration + ".emission.events.xml.gz";

//	private final String runNumber = "0";
//	private final String runDirectory = "../../runs-svn/detEval/latsis/output/output_baseCase_ctd_newCode/";
//	
//	private final String netFile = runDirectory + "output_network.xml.gz";
//	private final String plansFile = runDirectory + "output_plans.xml.gz";
//	private final String configFile = runDirectory + "output_config.xml.gz";
//	private final Integer lastIteration = getLastIteration(configFile);
//	private final String emissionFile = runDirectory + "ITERS/it." + lastIteration + "/" + lastIteration + ".emission.events.xml.gz";

	private void run() {
		Scenario scenario = loadScenario(netFile, plansFile);

		EmissionsAnalyzer ema = new EmissionsAnalyzer(emissionFile);
		ema.init((MutableScenario) scenario);
		ema.preProcessData();
		ema.postProcessData();
		
		EmissionUtils emu = new EmissionUtils();
		Map<Id<Person>, SortedMap<String, Double>> totalEmissions = ema.getPerson2totalEmissions();
		Map<Id<Person>, SortedMap<String, Double>> filledTotalEmissions = emu.setNonCalculatedEmissionsForPopulation(scenario.getPopulation(), totalEmissions);

		EmissionWriter emissionWriter = new EmissionWriter();
		//		emissionWriter.writeHomeLocation2TotalEmissions(
		//				population,
		//				filledWarmEmissions,
		//				runDirectory + runNumber + "." + lastIteration + ".emissionsWarmPerHomeLocation.txt");
		//		emissionWriter.writeHomeLocation2TotalEmissions(
		//				population,
		//				filledColdEmissions,
		//				runDirectory + runNumber + "." + lastIteration + ".emissionsColdPerHomeLocation.txt");
		emissionWriter.writeHomeLocation2TotalEmissions(
				scenario.getPopulation(),
				filledTotalEmissions,
				runDirectory + runNumber + "." + lastIteration + ".emissionsTotalPerHomeLocation.txt");
	}

	private static Scenario loadScenario(String netFile, String plansFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	private static Integer getLastIteration(String configFile) {
		Config config = new Config();
		config.addCoreModules();
		ConfigReader configReader = new ConfigReader(config);
		configReader.readFile(configFile);
		Integer lastIt = config.controler().getLastIteration();
		return lastIt;
	}

	public static void main(String[] args) {
		EmissionsPerPersonAnalysis eppa = new EmissionsPerPersonAnalysis();
		eppa.run();
	}
}
