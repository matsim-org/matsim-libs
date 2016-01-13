/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.patnaIndia.input.combined;

import java.io.BufferedWriter;
import java.util.SortedMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.legMode.ModalShareGenerator;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class JointDemandModalSplit {

	public static void main(String[] args) {
		new JointDemandModalSplit().run();
	}
	
	private void run (){
		String dir = "/Users/amit/Documents/cluster/ils4/agarwal/patnaIndia/run108/calibration/";
		String folder = "c1/ITERS/it.";
		String itNr = "100";
		String plansFile = dir+folder+itNr+"/"+itNr+".plans.xml.gz";
		
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(plansFile);
		Population pop = sc.getPopulation();
		ModalShareGenerator msg = new ModalShareGenerator();
		
		String outFile = dir+folder+itNr+"/"+itNr+".modalSplit.txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
		SortedMap<String, Integer> absoluteLeg = msg.getMode2NoOfLegs(pop);
		SortedMap<String, Double> modalShare = msg.getMode2PctShareFromPlans(pop);
		
		try {
			writer.write("absolute/Share \t");
			for(String mode : absoluteLeg.keySet()){
				writer.write(mode+"\t");
			}
			writer.newLine();
			
			writer.write("absolute \t");
			for(String mode :absoluteLeg.keySet()){
				writer.write(absoluteLeg.get(mode)+"\t");
			}
			writer.newLine();
			
			writer.write("share \t");
			for(String mode :modalShare.keySet()){
				writer.write(modalShare.get(mode)+"\t");
			}
			writer.newLine();
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
		
		
	}
	
}
