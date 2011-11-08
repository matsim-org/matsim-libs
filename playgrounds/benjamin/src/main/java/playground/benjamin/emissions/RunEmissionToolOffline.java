/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionTool.java
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
package playground.benjamin.emissions;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class RunEmissionToolOffline {
	private static final Logger logger = Logger.getLogger(RunEmissionToolOffline.class);

//	private final static String runNumber = "985";
//	private final static String runDirectory = "../../runs-svn/run" + runNumber + "/";
//	private static String configFile = runDirectory + runNumber + ".output_config.xml.gz";
//	private static final Integer lastIteration = getLastIteration(configFile);
//	
//	private static String eventsFile = runDirectory + "ITERS/it." + lastIteration + "/" + runNumber + "." + lastIteration + ".events.xml.gz";
//	private static String netFile = runDirectory + runNumber + ".output_network.xml.gz";

//	private static String eventsFile = runDirectory + "ITERS/it.500/500.events.txt.gz";
//	private static String netFile = runDirectory + "output_network.xml.gz";
	
//	private static String eventsFile = runDirectory + "ITERS/it.300/300.events.txt.gz";
//	private static String netFile = runDirectory + "output_network.xml.gz";
	
	private static String eventsFile = "../../detailedEval/emissions/testScenario/output/ITERS/it.0/0.events.xml.gz";
	private static String netFile = "../../detailedEval/emissions/testScenario/output/output_network.xml.gz";
	
	private static String emissionEventOutputFile = "../../detailedEval/emissions/testScenario/output/ITERS/it.0/0.emission.events.xml.gz";

	// =======================================================================================================		
	private final Scenario scenario;

	public RunEmissionToolOffline(){
		Config config = ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.createScenario(config);
	}

	private void run(String[] args) throws IOException {
		
		loadScenario();
		
		Network network = scenario.getNetwork();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		EmissionHandler emissionHandler = new EmissionHandler();
		emissionHandler.installEmissionEventHandler(network, eventsManager, emissionEventOutputFile);
		
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);
		
		EventWriterXML emissionEventWriter = emissionHandler.getEmissionEventWriter();
		emissionEventWriter.closeFile();
		logger.info("Vehicle-specific warm emission calculation was not possible in " + WarmEmissionAnalysisModule.getVehInfoWarnCnt() + " cases.");
		logger.info("Terminated. Output can be found in " + emissionEventOutputFile);
	}

	@SuppressWarnings("deprecation")
	private void loadScenario() {
		Config config = scenario.getConfig();
		config.network().setInputFile(netFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario);
		scenarioLoader.loadScenario();
	}

	public static void main (String[] args) throws Exception{
		RunEmissionToolOffline runEmissionToolOffline = new RunEmissionToolOffline();
		runEmissionToolOffline.run(args);
	}

	private static int getLastIteration(String configFile) {
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configFile);
		Integer lastIteration = config.controler().getLastIteration();
		return lastIteration;
	}
}