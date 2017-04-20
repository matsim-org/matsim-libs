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
import java.util.*;

import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.LoadMyScenarios;
import playground.vsp.airPollution.flatEmissions.EmissionCostFactors;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;

/**
 * @author amit
 */
public class AbsoluteEmissions {
	private final String outputDir;

	private AbsoluteEmissions(final String outputDir) {
		this.outputDir = outputDir;
	}

	public static void main(String[] args) {
		String clusterPathDesktop = "/Users/amit/Documents/cluster/ils4/kaddoura/cne/munich/output/";
		String [] runCases =  {"output_run0_muc_bc","output_run0b_muc_bc","output_run8_muc_e","output_run8b_muc_e"};
		new AbsoluteEmissions(clusterPathDesktop).runAndWrite(runCases);
	}

	private void runAndWrite(final String[] runCases){
		BufferedWriter writer = IOUtils.getBufferedWriter("/Users/amit/Desktop/analysis/absoluteEmissions.txt");

		SortedMap<String, SortedMap<String, Double>> emissions = new TreeMap<>();
		SortedMap<String, Double> emissionCost = new TreeMap<>();
		Set<String> pollutants = new TreeSet<>();

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
//			writer.write("emissionCost");
			writer.newLine();

			for(String runCase:emissions.keySet()){
				writer.write(runCase+"\t");
				for(String pollutant : pollutants){
					writer.write(emissions.get(runCase).get(pollutant)+"\t");
				}
//				writer.write(emissionCost.get(runCase));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e1) {
			throw new RuntimeException("Data is not written in file. Reason : "+e1);
		}
	}
	
	private double getEmissionCost(final SortedMap<String, Double> em){
		double cost = Arrays.stream(EmissionCostFactors.values())
							.mapToDouble(ecf -> em.get(ecf.toString()) * ecf.getCostFactor())
							.sum();
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