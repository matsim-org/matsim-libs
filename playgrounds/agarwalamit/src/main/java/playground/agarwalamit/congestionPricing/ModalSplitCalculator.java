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
package playground.agarwalamit.congestionPricing;

import java.io.BufferedWriter;
import java.util.SortedMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.legMode.ModalShareGenerator;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */
public class ModalSplitCalculator {

	private SortedMap<String, Double> pctModalShare ;
	private SortedMap<String, Integer> mode2Legs;

	public static void main(String[] args) {
		
		String outputDir = "../../../repos/runs-svn/siouxFalls/run204/calibration/";
		String [] runCases = {"c1","c2","c3","c4","c5","c7","c8","c9","c10"};
		
		for(String runCase :runCases){
			ModalSplitCalculator msUG = new ModalSplitCalculator();
			int it = 500;
			msUG.run(outputDir+runCase+"/ITERS/it."+it+"/"+it+".plans.xml.gz");
			msUG.writeResults(outputDir+runCase+"/modalShare_it."+it+".txt");
		}
	}

	public void run(String populationFile){
		ModalShareGenerator msg = new ModalShareGenerator();
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(populationFile);

		pctModalShare = msg.getMode2PctShareFromPlans(sc.getPopulation());
		mode2Legs = msg.getMode2NoOfLegs(sc.getPopulation());
	}

	public void writeResults(String outputFile){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			for(String str:mode2Legs.keySet()){
				writer.write(str+"\t");
			}
			writer.newLine();
			for(String str:mode2Legs.keySet()){ // write Absolute No Of Legs
				writer.write(mode2Legs.get(str)+"\t");
			}
			writer.newLine();
			for(String str:pctModalShare.keySet()){ // write percentage no of legs
				writer.write(pctModalShare.get(str)+"\t");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data can not be written to file. Reason - "+e);
		}
	}
}
