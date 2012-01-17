/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
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
package playground.benjamin.emissions.example;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.benjamin.emissions.EmissionModule;

/**
 * @author benjamin
 *
 */
public class RunEmissionToolOffline {
	private static final Logger logger = Logger.getLogger(RunEmissionToolOffline.class);

	final static String runNumber = "981";
	final static String runDirectory = "../../runs-svn/run" + runNumber + "/";
	static String configFile = runDirectory + runNumber + ".output_config.xml.gz";
	static final Integer lastIteration = getLastIteration(configFile);
	
	static String eventsPath = runDirectory + "ITERS/it." + lastIteration + "/" + runNumber + "." + lastIteration;
	static String eventsFile = eventsPath + ".events.xml.gz";
	private static String netFile = runDirectory + runNumber + ".output_network.xml.gz";
	static String emissionVehicleFile = "../../detailedEval/pop/merged/emissionVehicles_10pct.xml.gz";
	static String emissionEventOutputFile = eventsPath + ".emission.events.xml.gz";
	
//	static String eventsFile = "../../detailedEval/emissions/testScenario/output/ITERS/it.0/0.events.xml.gz";
//	static String netFile = "../../detailedEval/emissions/testScenario/output/output_network.xml.gz";
//	static String emissionVehicleFile = "../../detailedEval/emissions/testScenario/input/emissionVehicles_10pct.xml.gz";
//	static String emissionEventOutputFile = "../../detailedEval/emissions/testScenario/output/ITERS/it.0/0.emission.events.xml.gz";

	// ===
	static String emissionInputPath = "../../detailedEval/emissions/hbefaForMatsim/";
	static String roadTypeMappingFile = emissionInputPath + "roadTypeMapping.txt";
	
	static String averageFleetWarmEmissionFactorsFile = emissionInputPath + "EFA_HOT_vehcat_2005average.txt";
	static String averageFleetColdEmissionFactorsFile = emissionInputPath + "EFA_ColdStart_vehcat_2005average.txt";
	
	static boolean isUsingDetailedEmissionCalculation = true;
	static String detailedWarmEmissionFactorsFile = emissionInputPath + "EFA_HOT_SubSegm_2005detailed.txt";
	static String detailedColdEmissionFactorsFile = emissionInputPath + "EFA_ColdStart_SubSegm_2005detailed.txt";
	
	// =======================================================================================================		
	final Scenario scenario;


	public RunEmissionToolOffline(){
		Config config = ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.createScenario(config);
	}

	private void run(String[] args) throws IOException {
		
		loadScenario();
		
		setInputFiles();
		
		EmissionModule emissionModule = new EmissionModule(scenario);
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();
		

		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(emissionModule.getWarmEmissionsHandler());
		eventsManager.addHandler(emissionModule.getColdEmissionsHandler());
		
		EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
		emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);
		
		emissionEventWriter.closeFile();

		emissionModule.writeEmissionInformation(emissionEventOutputFile);
	}

	private void setInputFiles() {
		VspExperimentalConfigGroup vcg = scenario.getConfig().vspExperimental() ;
		vcg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);
		vcg.setEmissionVehicleFile(emissionVehicleFile);
		
		vcg.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
		vcg.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);
		
		vcg.setIsUsingDetailedEmissionCalculation(isUsingDetailedEmissionCalculation);
		vcg.setDetailedWarmEmissionFactorsFile(detailedWarmEmissionFactorsFile);
		vcg.setDetailedColdEmissionFactorsFile(detailedColdEmissionFactorsFile);
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