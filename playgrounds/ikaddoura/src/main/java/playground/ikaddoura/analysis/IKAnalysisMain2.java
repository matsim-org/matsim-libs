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

package playground.ikaddoura.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;


public class IKAnalysisMain2 {
	
	static String configFile = "/Users/Ihab/Desktop/testScenario_output/output_config.xml.gz";
	
	private int iteration;
	private String outputDir;
	
	public static void main(String[] args) {
		IKAnalysisMain2 anaMain = new IKAnalysisMain2();
		anaMain.run();
	}

	private void run() {
	
		Config config = ConfigUtils.loadConfig(configFile);
		
		this.iteration = config.controler().getLastIteration();
		this.outputDir = config.controler().getOutputDirectory();
		
		String populationFile = outputDir + "/output_plans.xml.gz";
		String networkFile = outputDir + "/output_network.xml.gz";
		String eventsFile = outputDir + "/ITERS/it." + iteration + "/" + iteration + ".events.xml.gz";
		
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		
		IKEventHandler handler = new IKEventHandler(scenario.getNetwork());
		events.addHandler(handler);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
	}
			 
}
		

