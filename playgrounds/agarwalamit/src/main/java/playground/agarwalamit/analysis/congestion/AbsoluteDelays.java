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
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Simplified class to get the absolute delays for all the specified run cases
 * and write them to file.
 * @author amit
 */
public class AbsoluteDelays  {

	private String outputDir;

	public AbsoluteDelays(String outputDir) {
		this.outputDir = outputDir;
	}

	public static void main(String[] args) {
		String clusterPathDesktop = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9/";
		String [] runCases =  {"baseCaseCtd","ei","ci","eci"};
		
		new AbsoluteDelays(clusterPathDesktop).runAndWrite(runCases);
	}

	public void runAndWrite(String [] runCases){
		BufferedWriter writer =IOUtils.getBufferedWriter(outputDir+"/analysis/absoluteDelays.txt");

		try {
			writer.write("runCase \t absoluteDelaysInHr \t delaysCost \n");
			for(String runCase : runCases){
				double delayInHr = totalDelayInHoursFromEventsFile(runCase);
				writer.write(runCase+"\t"+delayInHr+"\t"+delayInHr*3600*getVTTS_car(runCase)+"\n");
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written into File. Reason : "+e);
		}
	}

	private double getVTTS_car(String runCase){
		String configFile = outputDir+runCase+"/output_config.xml";
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader reader = new MatsimConfigReader(config);
		reader.readFile(configFile);

		double vtts_car = ((config.planCalcScore().getTraveling_utils_hr()/3600) + 
				(config.planCalcScore().getPerforming_utils_hr()/3600)) 
				/ (config.planCalcScore().getMarginalUtilityOfMoney());
		return vtts_car;
	}
	
	private  double totalDelayInHoursFromEventsFile(String runCase) {

		String configFile = outputDir+runCase+"/output_config.xml";
		String networkFile = outputDir+runCase+"/output_network.xml.gz";
		int lastIt = LoadMyScenarios.getLastIteration(configFile);
		String eventFile = outputDir+runCase+"/ITERS/it."+lastIt+"/"+lastIt+".events.xml.gz";		

		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(networkFile);

		ExperiencedDelayAnalyzer congestionHandler = new ExperiencedDelayAnalyzer(eventFile, 1);
		congestionHandler.run(sc);

		return congestionHandler.getTotalDelaysInHours();
	}
}
