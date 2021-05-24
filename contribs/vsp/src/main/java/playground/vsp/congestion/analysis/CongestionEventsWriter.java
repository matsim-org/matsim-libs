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

package playground.vsp.congestion.analysis;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.MarginalCongestionPricingHandler;

/**
 * (1) Computes marginal congestion events based on a standard events file.
 * (2) Computes agent money events based on these marginal congestion events.
 * 
 * @author ikaddoura
 *
 */
public class CongestionEventsWriter {
	private static final Logger log = Logger.getLogger(CongestionEventsWriter.class);
	
	static String runDirectory;
	
	public static void main(String[] args) {
		
		if (args.length > 0) {
			
			runDirectory = args[0];		
			log.info("runDirectory: " + runDirectory);
			
		} else {
			runDirectory = "../../runs-svn/berlin_internalizationCar2/output/baseCase_2/";
		}
		
		CongestionEventsWriter congestionEventsWriter = new CongestionEventsWriter();
		congestionEventsWriter.run();
	}

	private void run() {
	
		log.info("Loading scenario...");
		Config config = ConfigUtils.loadConfig(runDirectory + "output_config.xml.gz");
		config.network().setInputFile(runDirectory + "output_network.xml.gz");
		config.plans().setInputFile(runDirectory + "output_plans.xml.gz");
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		log.info("Loading scenario... Done.");
		
		String outputDirectory = runDirectory + "analysis_it." + config.controler().getLastIteration() + "/";
		File file = new File(outputDirectory);
		file.mkdirs();
		
		EventsManager events = EventsUtils.createEventsManager();
		
		EventWriterXML eventWriter = new EventWriterXML(outputDirectory + config.controler().getLastIteration() + ".events_ExternalCongestionCost_Offline.xml.gz");
		CongestionHandlerImplV3 congestionHandler = new CongestionHandlerImplV3(events, scenario);
		MarginalCongestionPricingHandler marginalCostTollHandler = new MarginalCongestionPricingHandler(events, scenario);
		
		events.addHandler(eventWriter);
		events.addHandler(congestionHandler);
		events.addHandler(marginalCostTollHandler);
		
		log.info("Reading events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(runDirectory + "ITERS/it." + config.controler().getLastIteration() + "/" + config.controler().getLastIteration() + ".events.xml.gz");
		log.info("Reading events file... Done.");

		eventWriter.closeFile();
		
		congestionHandler.writeCongestionStats(outputDirectory + config.controler().getLastIteration() + ".congestionStats_Offline.csv");
	}
			 
}
		

