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
package playground.agarwalamit.analysis.congestion;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Simplified class to get the absolute delays for all the specified run cases
 * and write them to file.
 * @author amit
 */
public class AbsoluteDelays  {
	private final String outputDir;

	private AbsoluteDelays(final String outputDir) {
		this.outputDir = outputDir;
	}

	public static void main(String[] args) {
		String clusterPathDesktop = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run11/policies_0.05/";
		String [] runCases =  {"bau","implV3","implV4","implV6"};
		
		new AbsoluteDelays(clusterPathDesktop).runAndWrite(runCases);
	}

	private void runAndWrite(final String[] runCases){
		BufferedWriter writer =IOUtils.getBufferedWriter(outputDir+"/analysis/absoluteDelays.txt");

		try {
			writer.write("runCase \t absoluteDelaysInHr \t delaysCost \n");
			for(String runCase : runCases){
				double delayInHr = totalDelayInHoursFromEventsFile(runCase);
				writer.write(runCase+"\t"+delayInHr+"\t"+delayInHr * 3600 * getVTTSCar(runCase)+"\n");
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written into File. Reason : "+e);
		}
	}

	private double getVTTSCar(final String runCase){
		String configFile = outputDir+runCase+"/output_config.xml";
		Config config = new Config();
		config.addCoreModules();
		ConfigReader reader = new ConfigReader(config);
		reader.readFile(configFile);

		return ((config.planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() /3600) +
				(config.planCalcScore().getPerforming_utils_hr()/3600))
				/ (config.planCalcScore().getMarginalUtilityOfMoney());
	}
	
	private  double totalDelayInHoursFromEventsFile(final String runCase) {
		String configFile = outputDir+runCase+"/output_config.xml";
		String networkFile = outputDir+runCase+"/output_network.xml.gz";
		String plansFile = outputDir+runCase+"/output_plans.xml.gz";

		Scenario sc = LoadMyScenarios.loadScenarioFromPlansNetworkAndConfig(plansFile, networkFile,configFile);
		
		int lastIt = sc.getConfig().controler().getLastIteration();
		String eventFile = outputDir+runCase+"/ITERS/it."+lastIt+"/"+lastIt+".events.xml.gz";

		ExperiencedDelayAnalyzer congestionHandler = new ExperiencedDelayAnalyzer(eventFile, sc, 1);
		congestionHandler.run();

		return congestionHandler.getTotalDelaysInHours();
	}
}