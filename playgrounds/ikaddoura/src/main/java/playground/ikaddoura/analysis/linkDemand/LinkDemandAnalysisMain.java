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

package playground.ikaddoura.analysis.linkDemand;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

public class LinkDemandAnalysisMain {
	
//	private static String OUTPUT_BASE_DIR = "../../../runs-svn/berlin-1pct/";
	private static String OUTPUT_BASE_DIR = "/Users/ihab/Desktop/ils4/kaddoura/incidents/output/2b_reroute1.0/nce_0/";
	
	public static void main(String[] args) {
		LinkDemandAnalysisMain anaMain = new LinkDemandAnalysisMain();
		anaMain.run();
	}

	private void run() {
	
		Config config = ConfigUtils.loadConfig(OUTPUT_BASE_DIR + "output_config.xml.gz");
		config.plans().setInputFile(null);
		config.network().setChangeEventsInputFile(null);
		config.network().setInputFile(OUTPUT_BASE_DIR + "output_network.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
				
		LinkDemandEventHandler handler = new LinkDemandEventHandler(scenario.getNetwork());
		events.addHandler(handler);
		
		String eventsFile = OUTPUT_BASE_DIR + "ITERS/it." + config.controler().getLastIteration() + "/" + config.controler().getLastIteration() + ".events.xml.gz";
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		String analysis_output_file = OUTPUT_BASE_DIR + "ITERS/it." + config.controler().getLastIteration() + "/link_dailyDemand.csv";
		handler.printResults(analysis_output_file);
	}
			 
}
		

