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

package playground.smetzler.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;



public class AnalysisMain {

	static String configFile = "C:/Users/Ettan/8. Sem - Uni SS13/MATSim/MATSimTutorial/workspace/myProject/input/config.xml";
	static String eventsFile = "C:/Users/Ettan/8. Sem - Uni SS13/MATSim/MATSimTutorial/workspace/myProject/output/mitBr/ITERS/it.20/run0.20.events.xml.gz";
	
	//Map<Id, Double> onLinkTime = new HashMap<Id, Double>();
	
	List<Double> onLinkTime = new ArrayList <Double>();
	
	double topf = 0;
	
	public static void main(String[] args) {
		AnalysisMain anaMain = new AnalysisMain();
		anaMain.run();
	}

	private void run() {
	
		Config config = ConfigUtils.loadConfig(configFile);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		
		CongestionEventHandler congestionHandler = new CongestionEventHandler(scenario.getNetwork());
		events.addHandler(congestionHandler);
		// ggf. hier weitere handler
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
				
		System.out.println("pott: " + congestionHandler.getPott_h());
		
		congestionHandler.printResults();
	
	}
			 
}
		

