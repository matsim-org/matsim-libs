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
package playground.agarwalamit.siouxFalls.congestionAnalyzer;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.agarwalamit.analysis.congestion.CongestionLinkAnalyzer;

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
		String clusterPathDesktop = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct_msa_rSeed/";
		String [] runCases =  {"baseCaseCtd","ei","ci","eci"};
		
		new AbsoluteDelays(clusterPathDesktop).runAndWrite(runCases);
	}

	public void runAndWrite(String [] runCases){
		BufferedWriter writer =IOUtils.getBufferedWriter(outputDir+"/analysis/absoluteDelays.txt");

		try {
			for(String runCase : runCases){
				writer.write(runCase+"\t"+totalDelayInHoursFromEventsFile(runCase)+"\n");
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written into File. Reason : "+e);
		}
	}

	private  double totalDelayInHoursFromEventsFile(String runCase) {

		String configFile = outputDir+runCase+"/output_config.xml";
		String networkFile = outputDir+runCase+"/output_network.xml.gz";
		double simEndTime = LoadMyScenarios.getSimulationEndTime(configFile);
		int lastIt = LoadMyScenarios.getLastIteration(configFile);
		String eventFile = outputDir+runCase+"/ITERS/it."+lastIt+"/"+lastIt+".events.xml.gz";		

		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(networkFile);

		CongestionLinkAnalyzer congestionHandler = new CongestionLinkAnalyzer(simEndTime, eventFile, 1);
		congestionHandler.run(sc);

		return congestionHandler.getTotalDelaysInHours();
	}
}
