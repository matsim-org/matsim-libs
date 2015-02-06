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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.congestion.events.CongestionEventsReader;

public class CongestionAnalysisMain {

	static String configFile = "/Users/Ihab/Desktop/output/output_config.xml.gz";
//	static String configFile = "/Users/Ihab/Desktop/noInternalization_output/output_config.xml.gz";

	private int iteration;
	private String outputDir;
	
	public static void main(String[] args) {
		CongestionAnalysisMain anaMain = new CongestionAnalysisMain();
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
		
		CongestionEventsReader congestionEventsReader = new CongestionEventsReader(events);		
		congestionEventsReader.parse(eventsFile);

		System.out.println("Delay sum from marginal congestion effects: " + handler1.getDelaySum() );
		System.out.println("Delay sum from each link leave event: " + handler2.getTActMinusT0Sum() );
		
		writeResults(handler1.getDelaySum(), handler2.getTActMinusT0Sum());
	}

	private void writeResults(double marginalCongestionDelaySum, double delaySum) {

		String fileName = outputDir + "/congestion_it" + iteration + ".txt";
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();
			bw.write("Delay sum from marginal congestion effects: " + marginalCongestionDelaySum + " sec /// " + (marginalCongestionDelaySum / 3600.) + " hours");
			bw.newLine();
			bw.write("Delay sum from each link leave event: " + delaySum +  " sec /// " + (delaySum / 3600.) + " hours");
			bw.newLine();
			bw.write("Difference: " + (delaySum - marginalCongestionDelaySum) +  " sec");
			bw.newLine();
			
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
			 
}
		

