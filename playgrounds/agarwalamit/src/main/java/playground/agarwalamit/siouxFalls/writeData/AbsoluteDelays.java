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
package playground.agarwalamit.siouxFalls.writeData;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.ikaddoura.internalizationCar.MarginalCongestionHandlerImplV3;

/**
 * @author amit
 */
public class AbsoluteDelays {

	
//	static String configFile = "./output/run0/output_config.xml.gz";
	private static String clusterPathDesktop = "/Users/aagarwal/Desktop/ils/agarwal/siouxFalls/";
	private static String [] runNumber = new String [] {"run1","run2","run3","run4"};
	public static void main(String[] args) {
		
		BufferedWriter writer =IOUtils.getBufferedWriter(clusterPathDesktop+"/output/analysis/r/rAbsoluteDelays.txt");
		
		double [] delays = new double [runNumber.length];
		
		for (int i=0; i<delays.length;i++){
			delays[i] = totalDelayInHoursFromEventsFile(runNumber[i]);
		}
		
//		ConstructBarChart barChart = new ConstructBarChart();
//		String  legendKey = "% reduction in Delay w.r.t. base case";
//		String [] xLabel = {"only Emissions", "only Congestion", "Both"};
		double [] yValue = {getRelativeChange(delays[0], delays[1]),
				getRelativeChange(delays[0], delays[2]), 
				getRelativeChange(delays[0], delays[3])};
//		CategoryDataset dataset = barChart.createSingleBarDataSet(yValue, legendKey, xLabel);
//		barChart.createBarChart("% reduction in total Delay", "Internalization", "% reduction in total Delay", dataset);
//		barChart.saveAsPng(clusterPathDesktop+"/output/analysis/r/javaChangeInDelay.png", 600, 400);
		
		try {
//			writer.write("Total Delays in hours \n");
			writer.write("baseCase \t onlyEmissions \t onlyCongestion \t both \n");
			writer.write(delays[0]+"\t"+delays[1]+"\t"+delays[2]+"\t"+delays[3]+"\n");
//			writer.write("% reduction in total Delay w.r.t. base case");
//			writer.write("only emissions \t only congestion \t both \n");
			for(int i=0;i<yValue.length;i++){
//				writer.write(yValue[i]+"\t");
			}
//			writer.write("\n");
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written into File. Reason : "+e);
		}
		
	}

	public static double totalDelayInHoursFromEventsFile(String runNumber) {
		EventsManager eventManager = EventsUtils.createEventsManager();
		ScenarioImpl sc = loadScenario(runNumber);
		MarginalCongestionHandlerImplV3 congestionHandlerImplV3= new MarginalCongestionHandlerImplV3(eventManager, sc);

		eventManager.addHandler(congestionHandlerImplV3);

		MatsimEventsReader eventsReader = new MatsimEventsReader(eventManager);
		String inputEventsFile = clusterPathDesktop+"/output/"+runNumber+"/ITERS/it.100/100.events.xml.gz";
		eventsReader.readFile(inputEventsFile);

		return congestionHandlerImplV3.getTotalDelay()/3600;
	}
	
	private static ScenarioImpl loadScenario(String runNumber) {
//		String configFile = clusterPathDesktop+"/output/"+runNumber+"/output_config.xml.gz";
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(clusterPathDesktop+"/input/SiouxFalls_networkWithRoadType.xml.gz");
//		config.plans().setInputFile(clusterPathDesktop+"/input/SiouxFalls_population_probably_v3.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return (ScenarioImpl) scenario;
	}
	
	private static double getRelativeChange (double baseValue, double value) {
		return ((baseValue - value)*100)/baseValue;
	}
}
