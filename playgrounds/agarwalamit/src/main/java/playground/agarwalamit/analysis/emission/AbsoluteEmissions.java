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
package playground.agarwalamit.analysis.emission;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.internalization.EmissionCostFactors;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;

/**
 * @author amit
 */
public class AbsoluteEmissions {
	private final String outputDir;

	public AbsoluteEmissions(final String outputDir) {
		this.outputDir = outputDir;
	}

	public static void main(String[] args) {
		String clusterPathDesktop = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9/";
		String [] runCases =  {"baseCaseCtd","ei","ci","eci","ei_10"};
		new AbsoluteEmissions(clusterPathDesktop).runAndWrite(runCases);
	}

	public void runAndWrite(final String [] runCases){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputDir+"/analysis/absoluteEmissions.txt");

		SortedMap<String, SortedMap<String, Double>> emissions = new TreeMap<>();
		SortedMap<String, Double> emissionCost = new TreeMap<>();
		Set<String> pollutants = new HashSet<String>();

		for(String runCase:runCases){
			SortedMap<String, Double> em = calculateTotalEmissions(runCase);
			emissions.put(runCase, em);
			emissionCost.put(runCase, getEmissionCost(em));
			pollutants.addAll(em.keySet());
		}
		try {
			writer.write("runCase"+"\t");
			for(String runCase:pollutants){
				writer.write(runCase+"\t");
			}
			writer.write("emissionCost");
			writer.newLine();

			for(String runCase:emissions.keySet()){
				writer.write(runCase+"\t");
				for(String pollutant : pollutants){
					writer.write(emissions.get(runCase).get(pollutant)+"\t");
				}
				writer.write(emissionCost.get(runCase)+"\n");
			}
			writer.close();
		} catch (IOException e1) {
			throw new RuntimeException("Data is not written in file. Reason : "+e1);
		}
	}
	
	public double getEmissionCost(final SortedMap<String, Double> em){
		double cost = 0;
		for (EmissionCostFactors ecf : EmissionCostFactors.values()){
			cost += em.get(ecf.toString()) * ecf.getCostFactor();
		}
		return cost;
	}
	
	private SortedMap<String,Double> calculateTotalEmissions (final String runCase){
		String configFile = outputDir+runCase+"/output_config.xml";
		int lastIt = LoadMyScenarios.getLastIteration(configFile);
		String emissionEventFile = outputDir+runCase+"/ITERS/it."+lastIt+"/"+lastIt+".emission.events.xml.gz";	

		EmissionsAnalyzer analyzer = new EmissionsAnalyzer(emissionEventFile);
		analyzer.init(null);
		analyzer.preProcessData();
		analyzer.postProcessData();
		return analyzer.getTotalEmissions();
	}
}