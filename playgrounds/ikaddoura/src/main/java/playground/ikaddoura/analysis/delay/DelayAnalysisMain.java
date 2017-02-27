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

package playground.ikaddoura.analysis.delay;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.decongestion.handler.DelayAnalysis;



public class DelayAnalysisMain {
	private static final Logger log = Logger.getLogger(DelayAnalysisMain.class);

	static String runDirectory = "/Users/ihab/Desktop/ils4i/kaddoura/congestion-pricing/output/output_2016-07-22_14-03-53_DecongestionNoPricing/";
				
	public static void main(String[] args) {
		DelayAnalysisMain anaMain = new DelayAnalysisMain();
		anaMain.run();
	}

	private void run() {
		
		String networkFile = "output_network.xml.gz";
		String configFile = runDirectory + "output_config.xml.gz";

		Config config = ConfigUtils.loadConfig(configFile);	
		config.network().setInputFile(networkFile);
		config.network().setChangeEventsInputFile(null);
		config.plans().setInputFile(null);
		
		int finalIteration = config.controler().getLastIteration();
		String eventsFile = runDirectory + "ITERS/it." + finalIteration + "/" + finalIteration + ".events.xml.gz";
	
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		
		DelayAnalysis delayAnalysis = new DelayAnalysis();
		delayAnalysis.setScenario(scenario);
		events.addHandler(delayAnalysis);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		log.info("Total delay (hours): " + delayAnalysis.getTotalDelay() / 3600.);
		log.info("Total travel time (hours): " + delayAnalysis.getTotalTravelTime() / 3600.);
	
	}
			 
}
		

