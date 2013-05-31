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

package playground.ikaddoura.analysis.welfare;

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


public class WelfareAnalysisMain {
	
//	static String configkFile = "/Users/Ihab/Desktop/internalization_output/output_config.xml.gz";
	static String configFile = "/Users/Ihab/Desktop/no_internalization_output/output_config.xml.gz";
	
	private int iteration;
	private String outputDir;
	
	public static void main(String[] args) {
		WelfareAnalysisMain anaMain = new WelfareAnalysisMain();
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
		
		UserBenefitsCalculator users = new UserBenefitsCalculator(config);
		users.reset();
		
		MoneyEventHandler handler1 = new MoneyEventHandler();
		events.addHandler(handler1);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);

		double userPayments = handler1.getPaymentsSum();
		double userBenefits = users.getLogsum(scenario.getPopulation());
		double welfare = userBenefits + userPayments;
		
		System.out.println("User payments: " + userPayments);
		System.out.println("User benefits: " + userBenefits);	
		System.out.println("Welfare: " + welfare);
		
		writeResults(welfare, userPayments, userBenefits);
	}

	private void writeResults(double welfare, double userPayments, double userBenefits) {

		String fileName = outputDir + "_welfare_it" + iteration + ".txt";
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("userBenefits: " + userBenefits);
			bw.newLine();
			bw.write("userPayments: " + userPayments);
			bw.newLine();
			bw.write("welfare: " + welfare);
			bw.newLine();
			
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
			 
}
		

