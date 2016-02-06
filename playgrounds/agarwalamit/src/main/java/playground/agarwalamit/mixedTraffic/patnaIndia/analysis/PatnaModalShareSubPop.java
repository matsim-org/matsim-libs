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
package playground.agarwalamit.mixedTraffic.patnaIndia.analysis;

import java.io.BufferedWriter;
import java.util.SortedMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.modalShare.ModalShareFromPlans;
import playground.agarwalamit.mixedTraffic.patnaIndia.subPop.PopulationFilter;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class PatnaModalShareSubPop {

	public static void main(String[] args) {
		new PatnaModalShareSubPop().run();
	}
	
	private void run(){
		String outFolder = "c24";
//		String plansFile = "/Users/amit/Documents/repos/runs-svn/patnaIndia/inputs/plansSubPop.xml.gz";
		String plansFile = "/Users/amit/Documents/repos/runs-svn/patnaIndia/run104/"+outFolder+"/output_plans.xml.gz";
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(plansFile);
		Population pop = sc.getPopulation();
		ModalShareFromPlans msg = new ModalShareFromPlans(pop);
		
		String outFile = "/Users/amit/Documents/repos/runs-svn/patnaIndia/run104/"+outFolder+"/"+outFolder+"_modalSplit.txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
		SortedMap<String, Double> wholePopModalSplit = msg.getModeToPercentOfLegs();
		try {
			writer.write("pop \t");
			for(String mode : wholePopModalSplit.keySet()){
				writer.write(mode+"\t");
			}
			writer.newLine();
			
			writer.write("wholePop \t");
			for(String mode :wholePopModalSplit.keySet()){
				writer.write(wholePopModalSplit.get(mode)+"\t");
			}
			writer.newLine();
			
			Population slumPop = PopulationFilter.getSlumPopulation(pop);
			msg = new ModalShareFromPlans(slumPop);
			
			writer.write("slum \t");
			for(String mode :wholePopModalSplit.keySet()){
				writer.write(msg.getModeToPercentOfLegs().get(mode)+"\t");
			}
			writer.newLine();
			
			Population nonSlum = PopulationFilter.getNonSlumPopulation(pop);
			msg = new ModalShareFromPlans(nonSlum);
			
			writer.write("nonSlum \t");
			for(String mode :wholePopModalSplit.keySet()){
				writer.write(msg.getModeToPercentOfLegs().get(mode)+"\t");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
	}
}