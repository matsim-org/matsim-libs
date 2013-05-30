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

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.internalizationCar.MarginalCongestionEventsReader;

public class CongestionAnalysisMain {

	static String configFile = "/Users/Ihab/Desktop/internalization_output/output_config.xml.gz";
	static String eventsFile = "/Users/Ihab/Desktop/internalization_output/ITERS/it.0/0.events.xml.gz";
	
	public static void main(String[] args) {
		CongestionAnalysisMain anaMain = new CongestionAnalysisMain();
		anaMain.run();
	}

	private void run() {
	
		Config config = ConfigUtils.loadConfig(configFile);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
						
		MarginalCongestionAnalyzer handler1 = new MarginalCongestionAnalyzer();
		events.addHandler(handler1);
		
		CarCongestionHandlerAdvanced handler2 = new CarCongestionHandlerAdvanced(scenario.getNetwork());
		events.addHandler(handler2);
		
		LinkFlowHandler handler3 = new LinkFlowHandler(scenario.getNetwork());
		events.addHandler(handler3);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		MarginalCongestionEventsReader congestionEventsReader = new MarginalCongestionEventsReader(events);		
		congestionEventsReader.parse(eventsFile);

		System.out.println("Summing up all marginal delay effects gives: " + handler1.getDelaySum() + " hours");
		System.out.println("Summing up all agent delays for each trip gives: " + handler2.getTActMinusT0Sum() + " hours");
		
		handler3.printResults();
	

	}
			 
}
		

