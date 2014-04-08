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

package playground.ikaddoura.internalizationCar;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.internalizationCar.MarginalCongestionHandlerImplV3;
import playground.ikaddoura.internalizationCar.MarginalCostPricingCarHandler;

/**
 * (1) Writes marginal congestion events based on a standard events file.
 * (2) Writes agent money events based on these marginal congestion events.
 * 
 * @author ikaddoura
 *
 */
public class MarginalCongestionEventsWriter {

	static String configFile = "../../runs-svn/berlin_internalizationCar/output/internalization_4/output_config.xml.gz";
	static String networkFile = "../../runs-svn/berlin_internalizationCar/output/internalization_4/output_network.xml.gz";
	static String inputEventsFile = "../../runs-svn/berlin_internalizationCar/output/internalization_4/ITERS/it.100/100.events.xml.gz";
	static String outputEventsFile = "../../runs-svn/berlin_internalizationCar/output/internalization_4/ITERS/it.100/100.eventsCongestionPrices.xml.gz";
	
	public static void main(String[] args) {
		MarginalCongestionEventsWriter anaMain = new MarginalCongestionEventsWriter();
		anaMain.run();
	}

	private void run() {
	
		Config config = ConfigUtils.loadConfig(configFile);
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(null);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		
		EventsManager events = EventsUtils.createEventsManager();		
		
		EventWriterXML eventWriter = new EventWriterXML(outputEventsFile);
		MarginalCongestionHandlerImplV3 congestionHandler = new MarginalCongestionHandlerImplV3(events, scenario);
		MarginalCostPricingCarHandler marginalCostTollHandler = new MarginalCostPricingCarHandler(events, scenario);

		events.addHandler(eventWriter);
		events.addHandler(congestionHandler);
		events.addHandler(marginalCostTollHandler);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputEventsFile);
		
		eventWriter.closeFile();
		
	}
			 
}
		

