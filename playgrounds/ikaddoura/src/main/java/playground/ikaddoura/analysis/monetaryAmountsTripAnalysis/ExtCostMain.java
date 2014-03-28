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

package playground.ikaddoura.analysis.monetaryAmountsTripAnalysis;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class ExtCostMain {
	
	private static final Logger log = Logger.getLogger(ExtCostMain.class);
	
	private String eventsFile = "/Users/ihab/Documents/workspace/runs-svn/agentBasedFlowBasedInternalization/output/int_flows_8_250_flow0.75/ITERS/it.250/250.events.xml.gz";
	private static String netFile = "/Users/ihab/Documents/workspace/runs-svn/agentBasedFlowBasedInternalization/output/int_flows_8_250_flow0.75/output_network.xml.gz";
	private String outputFolder = "/Users/ihab/Documents/workspace/runs-svn/agentBasedFlowBasedInternalization/output/int_flows_8_250_flow0.75/ITERS/it.250/analysis";
	
	public static void main(String[] args) {
		ExtCostMain anaMain = new ExtCostMain();
		anaMain.run();
	}

	private void run() {
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);

		EventsManager events = EventsUtils.createEventsManager();
		
		ExtCostEventHandler handler = new ExtCostEventHandler(scenario.getNetwork());
		events.addHandler(handler);

		log.info("Reading events file...");

		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		log.info("Reading events file... Done.");
		
		log.info("Writing output files...");

		CarTripInfoWriter writer = new CarTripInfoWriter(handler, outputFolder);
		writer.writeResults1();
		writer.writeAvgAmounts();
		writer.writeAvgFares3();
		
		log.info("Writing output files... Done.");

	}
			 
}
		

