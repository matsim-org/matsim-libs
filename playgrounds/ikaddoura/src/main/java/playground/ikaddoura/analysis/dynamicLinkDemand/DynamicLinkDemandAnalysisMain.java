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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class DynamicLinkDemandAnalysisMain {
	
	private static final Logger log = Logger.getLogger(DynamicLinkDemandAnalysisMain.class);
	private static String OUTPUT_BASE_DIR;

	public static void main(String[] args) {
		
		if (args.length > 0) {
			
			OUTPUT_BASE_DIR = args[0];
			log.info("Output base directory: " + OUTPUT_BASE_DIR);
			
		} else {
			
//			OUTPUT_BASE_DIR = "../../../runs-svn/berlin-1pct/";
			OUTPUT_BASE_DIR = "../../../public-svn/matsim/scenarios/countries/de/cottbus/cottbus-with-pt/output/cb02/";		
		}

		DynamicLinkDemandAnalysisMain analysis = new DynamicLinkDemandAnalysisMain();
		analysis.run();
	}

	private void run() {
		
		
		Config config = ConfigUtils.loadConfig(OUTPUT_BASE_DIR + "output_config.xml.gz");
		config.households().setInputHouseholdAttributesFile(null);
		config.transit().setTransitScheduleFile(null);
		config.transit().setVehiclesFile(null);
		config.plans().setInputFile(null);
		config.network().setChangeEventsInputFile(null);
		config.network().setInputFile(OUTPUT_BASE_DIR + "output_network.xml.gz");
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
				
		DynamicLinkDemandEventHandler handler = new DynamicLinkDemandEventHandler(scenario.getNetwork());
		events.addHandler(handler);
				
		String eventsFile = OUTPUT_BASE_DIR + "ITERS/it." + config.controler().getLastIteration() + "/" + config.controler().getLastIteration() + ".events.xml.gz";
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		String analysis_output_dir = OUTPUT_BASE_DIR + "ITERS/it." + config.controler().getLastIteration() + "/";
		handler.printResults(analysis_output_dir);
	}
			 
}
		

