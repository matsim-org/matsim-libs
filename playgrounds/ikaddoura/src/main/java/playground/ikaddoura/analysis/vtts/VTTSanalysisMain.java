/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis.vtts;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 *
 * Analyze the actual VTTS for each trip (applying a linearization for each activity) 
 * 
 * @author ikaddoura
 */
public class VTTSanalysisMain {
	private static final Logger log = Logger.getLogger(VTTSanalysisMain.class);

	private static String runDirectory;
	
	public static void main(String[] args) {
		
		if (args.length > 0) {
			runDirectory = args[0];		
			log.info("run directory: " + runDirectory);
			
		} else {
			runDirectory = "/Users/ihab/Documents/workspace/runs-svn/berlin_equal_vs_different_VTTS/output/baseCase/";
		}
		
		VTTSanalysisMain analysis = new VTTSanalysisMain();
		analysis.run();
	}

	private void run() {
		
//		String configFile = runDirectory + "output_config.xml.gz";
		String configFile = runDirectory + "output_config_withoutUnknownParameters.xml";

		Config config = ConfigUtils.loadConfig(configFile);	
		int iteration = config.controler().getLastIteration();
				
		String populationFile = null;
		String networkFile = null;
//		String networkFile = runDirectory + "output_network.xml.gz";
		
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		
		VTTSHandler vttsHandler = new VTTSHandler(scenario);
		events.addHandler(vttsHandler);
						
		String eventsFile = runDirectory + "ITERS/it." + iteration + "/" + iteration + ".events.xml.gz";

		log.info("Reading the events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		log.info("Reading the events file... Done.");
		
		vttsHandler.computeFinalVTTS();
				
		vttsHandler.printVTTS(runDirectory + "ITERS/it." + iteration + "/" + iteration + ".VTTS.csv");
		vttsHandler.printCarVTTS(runDirectory + "ITERS/it." + iteration + "/" + iteration + ".VTTS_car.csv");
		vttsHandler.printAvgVTTSperPerson(runDirectory + "ITERS/it." + iteration + "/" + iteration + ".avgVTTS.csv"); 
	}
			 
}
		

