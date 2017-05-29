/* *********************************************************************** *
* project: org.matsim.*
* firstControler
* *
* *********************************************************************** *
* *
* copyright : (C) 2007 by the members listed in the COPYING, *
* LICENSE and WARRANTY file. *
* email : info at matsim dot org *
* *
* *********************************************************************** *
* *
* This program is free software; you can redistribute it and/or modify *
* it under the terms of the GNU General Public License as published by *
* the Free Software Foundation; either version 2 of the License, or *
* (at your option) any later version. *
* See also COPYING, LICENSE and WARRANTY file *
* *
* *********************************************************************** */ 

package playground.ikaddoura.analysis.dynamicLinkDemand;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

public class DynamicLinkDemandAnalysisRun {
	
	private static final Logger log = Logger.getLogger(DynamicLinkDemandAnalysisRun.class);
	private static String OUTPUT_BASE_DIR;
	private String outputDirectory;

	public DynamicLinkDemandAnalysisRun(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public static void main(String[] args) {
		
		if (args.length > 0) {
			OUTPUT_BASE_DIR = args[0];
			log.info("Output base directory: " + OUTPUT_BASE_DIR);
			
		} else {
			OUTPUT_BASE_DIR = "/Users/ihab/Documents/workspace/runs-svn/vickrey-decongestion/output-FINAL/V9/";		
		}

		DynamicLinkDemandAnalysisRun analysis = new DynamicLinkDemandAnalysisRun(OUTPUT_BASE_DIR);
		analysis.run();
	}

	public void run() {
		
		if (!outputDirectory.endsWith("/")) {
			outputDirectory = outputDirectory + "/";
		}
		
		Config config = ConfigUtils.loadConfig(outputDirectory + "output_config.xml.gz");
		config.households().setInputHouseholdAttributesFile(null);
		config.transit().setTransitScheduleFile(null);
		config.transit().setVehiclesFile(null);
		config.plans().setInputFile(null);
		config.network().setChangeEventsInputFile(null);
		config.vehicles().setVehiclesFile(null);
		config.network().setInputFile(outputDirectory + "output_network.xml.gz");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
				
		DynamicLinkDemandEventHandler handler = new DynamicLinkDemandEventHandler(scenario.getNetwork());
		events.addHandler(handler);
				
		String eventsFile = outputDirectory + "ITERS/it." + config.controler().getLastIteration() + "/" + config.controler().getLastIteration() + ".events.xml.gz";
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		String analysis_output_dir = outputDirectory + "ITERS/it." + config.controler().getLastIteration() + "/";
		handler.printResults(analysis_output_dir);
	}
			 
}
		

