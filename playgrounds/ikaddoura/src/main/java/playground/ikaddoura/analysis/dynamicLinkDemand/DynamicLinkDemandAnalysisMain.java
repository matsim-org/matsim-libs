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
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class DynamicLinkDemandAnalysisMain {
	private static final Logger log = Logger.getLogger(DynamicLinkDemandAnalysisMain.class);
	
	private static String eventsFile;
	private static String networkFile;
	private static String outputPath;

	public static void main(String[] args) {
		
		if (args.length > 0) {
			eventsFile = args[0];		
			log.info("eventsFile: " + eventsFile);
			
			networkFile = args[1];		
			log.info("networkFile: " + networkFile);
			
			outputPath = args[2];		
			log.info("outputPath: " + outputPath);
			
		} else {
			
//			eventsFile = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalization_noise_averageVSmarginal/output/int_1_marginalCost/ITERS/it.100/100.events.xml.gz";
//			networkFile = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalization_noise_averageVSmarginal/output/int_1_marginalCost/output_network.xml.gz";
//			outputPath = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalization_noise_averageVSmarginal/output/int_1_marginalCost/ITERS/it.100/";
			
			eventsFile = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalization_noise_averageVSmarginal/output/int_1_averageCost/ITERS/it.100/100.events.xml.gz";
			networkFile = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalization_noise_averageVSmarginal/output/int_1_averageCost/output_network.xml.gz";
			outputPath = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalization_noise_averageVSmarginal/output/int_1_averageCost/ITERS/it.100/";
		}
		
		DynamicLinkDemandAnalysisMain analysis = new DynamicLinkDemandAnalysisMain();
		analysis.run();
	}

	private void run() {
		
		Config config = ConfigUtils.createConfig();		
		config.plans().setInputFile(null);		
		config.network().setInputFile(networkFile);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
				
		DynamicLinkDemandEventHandler handler = new DynamicLinkDemandEventHandler(scenario.getNetwork());
		events.addHandler(handler);
				
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		handler.printResults(outputPath);
	}
			 
}
		

