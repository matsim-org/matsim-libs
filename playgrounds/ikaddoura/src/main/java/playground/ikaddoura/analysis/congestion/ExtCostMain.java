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

package playground.ikaddoura.analysis.congestion;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.congestion.analysis.CongestionAnalysisEventHandler;
import playground.vsp.congestion.analysis.CongestionAnalysisWriter;
import playground.vsp.congestion.events.CongestionEventsReader;

/**
 * 
 * @author ikaddoura
 *
 */
public class ExtCostMain {
	
	private static final Logger log = Logger.getLogger(ExtCostMain.class);
	
	private String eventsFile = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalizationCar2/output/baseCase_2/ITERS/it.100/100.events.xml.gz";
	private String configFile = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalizationCar2/input/config_baseCase_2.xml";
	private String netFile = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalizationCar2/output/internalization_2/output_network.xml.gz";
	private String outputFolder = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalizationCar2/output/internalization_2/analysis";
	
	public static void main(String[] args) {
		ExtCostMain anaMain = new ExtCostMain();
		anaMain.run();
	}

	private void run() {
		
		Config config = ConfigUtils.loadConfig(configFile);
		config.network().setInputFile(netFile);
		config.plans().setInputFile(null);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();

		CongestionAnalysisEventHandler extCostTripHandler = new CongestionAnalysisEventHandler(scenario, true);
		events.addHandler(extCostTripHandler);
		
		log.info("Reading events file...");

		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
//		MarginalCongestionEventsReader congestionEventsReader = new MarginalCongestionEventsReader(events);		
//		congestionEventsReader.parse(eventsFile);
		
		log.info("Reading events file... Done.");
		
		log.info("Writing output files...");

		CongestionAnalysisWriter writer = new CongestionAnalysisWriter(extCostTripHandler, outputFolder);
		writer.writeDetailedResults(TransportMode.car);
		writer.writeAvgTollPerDistance(TransportMode.car);
		writer.writeAvgTollPerTimeBin(TransportMode.car);
		writer.writeDetailedResults(TransportMode.pt);
		writer.writeAvgTollPerDistance(TransportMode.pt);
		writer.writeAvgTollPerTimeBin(TransportMode.pt);
		writer.writeCausingAgentId2totalAmount();
		writer.writeAffectedAgentId2totalAmount();
		
		log.info("Writing output files... Done.");

	}
			 
}