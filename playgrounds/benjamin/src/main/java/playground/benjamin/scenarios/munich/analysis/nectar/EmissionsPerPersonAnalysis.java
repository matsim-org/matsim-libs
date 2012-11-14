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
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsPerPersonColdEventHandler;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsPerPersonWarmEventHandler;
import playground.vsp.emissions.events.EmissionEventsReader;
import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.WarmPollutant;
import playground.vsp.emissions.utils.EmissionUtils;
import playground.vsp.emissions.utils.EmissionWriter;

/**
 * @author benjamin
 *
 */
public class EmissionsPerPersonAnalysis {

	private final String runNumber = "973";
	private final String runDirectory = "../../runs-svn/run" + runNumber + "/";

	private final String netFile = runDirectory + runNumber + ".output_network.xml.gz";
	private final String plansFile = runDirectory + runNumber + ".output_plans.xml.gz";
	private final String configFile = runDirectory + runNumber + ".output_config.xml.gz";
	private final Integer lastIteration = getLastIteration(configFile);
	private final String emissionFile = runDirectory + runNumber + "." + lastIteration + ".emission.events.xml.gz";

	//	private final String netFile = runDirectory + "output_network.xml.gz";
	//	private final String plansFile = runDirectory + "output_plans.xml.gz";
	//	private final String emissionFile = runDirectory + runNumber + ".emission.events.xml.gz";

	private void run() {
		Scenario scenario = loadScenario(netFile, plansFile);

		// TODO: this might be possible through the standardized framework:
//		EmissionsAnalyzer ema = new EmissionsAnalyzer(null, emissionFile);
//		ema.init((ScenarioImpl) scenario);
//		ema.preProcessData();
//		ema.postProcessData();
//		
//		Map<Id, Map<WarmPollutant, Double>> warmEmissions = ema.getWarmHandler.getWarmEmissionsPerPerson();
//		Map<Id, Map<ColdPollutant, Double>> coldEmissions = ema.getColdHandler.getColdEmissionsPerPerson();
		
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

		
		EmissionUtils emu = new EmissionUtils();
		Map<Id, SortedMap<String, Double>> totalEmissions = emu.sumUpEmissionsPerId(warmEmissions, coldEmissions);
		Map<Id, SortedMap<String, Double>> filledTotalEmissions = emu.setNonCalculatedEmissionsForPopulation(population, totalEmissions);

		EmissionWriter emissionWriter = new EmissionWriter(emu);
		//		emissionWriter.writeHomeLocation2TotalEmissions(
		//				population,
		//				filledWarmEmissions,
		//				runDirectory + runNumber + "." + lastIteration + ".emissionsWarmPerHomeLocation.txt");
		//		emissionWriter.writeHomeLocation2TotalEmissions(
		//				population,
		//				filledColdEmissions,
		//				runDirectory + runNumber + "." + lastIteration + ".emissionsColdPerHomeLocation.txt");
		emissionWriter.writeHomeLocation2TotalEmissions(
				population,
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
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configFile);
		Integer lastIt = config.controler().getLastIteration();
		return lastIt;
	}

	public static void main(String[] args) {
		EmissionsPerPersonAnalysis eppa = new EmissionsPerPersonAnalysis();
		eppa.run();
	}
}
