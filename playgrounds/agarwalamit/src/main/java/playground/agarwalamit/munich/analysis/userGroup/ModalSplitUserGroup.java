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
package playground.agarwalamit.munich.analysis.userGroup;

import java.io.BufferedWriter;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.agarwalamit.analysis.ModalShareGenerator;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */
public class ModalSplitUserGroup {

	private SortedMap<UserGroup, SortedMap<String, double[]>> userGrp2ModalSplit = new TreeMap<UserGroup, SortedMap<String,double[]>>();
	
	public static void main(String[] args) {
		String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/outputTest/run6/";/*"./output/run2/";*/
		String [] runCases = {"baseCaseCtd","ei","ci","eci"};
		for(String runCase :runCases){
		ModalSplitUserGroup msUG = new ModalSplitUserGroup();
		msUG.run(outputDir+runCase);
		}
	}
	
	private void run(String outputDir){
		ModalShareGenerator msg = new ModalShareGenerator();
		PersonFilter pf = new PersonFilter();
		String populationFile = outputDir +"/output_plans.xml.gz";
		String networkFile = outputDir+"/output_network.xml.gz";
		Scenario sc = LoadMyScenarios.loadScenarioFromPlansAndNetwork(populationFile, networkFile);
		
		SortedMap<String, double[]> modalSplit = msg.getModalShareFromPlans(sc.getPopulation());
		
		for(UserGroup ug:UserGroup.values()){
			Population usrGrpPop = pf.getPopulation(sc.getPopulation(), ug);
			SortedMap<String, double[]> modalSplitPop = msg.getModalShareFromPlans(usrGrpPop);
			this.userGrp2ModalSplit.put(ug, modalSplitPop);
		}

		BufferedWriter writer = IOUtils.getBufferedWriter(outputDir+"/analysis/usrGrpToModalShare.txt");
		try {
			writer.write("UserGroup \t");
			
			for(String str:modalSplit.keySet()){
				writer.write(str+"\t");
			}
			writer.write(" \n WholePopulation"+"\t");
			for(String str:modalSplit.keySet()){ // write Absolute No Of Legs
				writer.write(modalSplit.get(str)[0]+"\t");
			}
			writer.write(" \n WholePopulation"+"\t");
			for(String str:modalSplit.keySet()){ // write percentage no of legs
				writer.write(modalSplit.get(str)[1]+"\t");
			}
			writer.newLine();
			for(UserGroup ug:this.userGrp2ModalSplit.keySet()){
				writer.write(ug+"\t");
				for(String str:modalSplit.keySet()){
					if(this.userGrp2ModalSplit.get(ug).get(str)!=null){
						writer.write(this.userGrp2ModalSplit.get(ug).get(str)[0]+"\t");
					} else
					writer.write(0.0+"\t");
				}
				writer.newLine();
				writer.write(ug+"\t");
				for(String str:modalSplit.keySet()){
					if(this.userGrp2ModalSplit.get(ug).get(str)!=null){
						writer.write(this.userGrp2ModalSplit.get(ug).get(str)[1]+"\t");
					} else
					writer.write(0.0+"\t");
				}
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data can not be written to file. Reason - "+e);
		}
	}
}
