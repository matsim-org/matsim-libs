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

import playground.agarwalamit.analysis.legMode.ModalShareGenerator;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */
public class ModalSplitUserGroup {

	private SortedMap<UserGroup, SortedMap<String, Integer>> userGrp2Mode2Legs = new TreeMap<UserGroup, SortedMap<String,Integer>>();
	private SortedMap<UserGroup, SortedMap<String, Double>> userGrp2ModalSplit = new TreeMap<UserGroup, SortedMap<String,Double>>();

	private SortedMap<String, Double> wholePop_pctModalShare ;
	private SortedMap<String, Integer> wholePop_Mode2Legs;

	public static void main(String[] args) {
		
		String outputDir = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run10/";/*"./output/run2/";*/
		String [] runCases = {"c0","c1","c2","c3","c4","c5","c6"};
		
		for(String runCase :runCases){
			ModalSplitUserGroup msUG = new ModalSplitUserGroup();
			int it = 1000;
			msUG.run(outputDir+runCase+"/ITERS/it."+it+"/"+it+".plans.xml.gz");
			msUG.writeResults(outputDir+runCase+"/analysis/usrGrpToModalShare_it."+it+".txt");
		}
	}

	public void run(String populationFile){
		ModalShareGenerator msg = new ModalShareGenerator();
		PersonFilter pf = new PersonFilter();
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(populationFile);

		wholePop_pctModalShare = msg.getMode2PctShareFromPlans(sc.getPopulation());
		wholePop_Mode2Legs = msg.getMode2NoOfLegs(sc.getPopulation());

		for(UserGroup ug:UserGroup.values()){
			Population usrGrpPop = pf.getPopulation(sc.getPopulation(), ug);
			SortedMap<String, Double> modalSplitPop = msg.getMode2PctShareFromPlans(usrGrpPop);
			this.userGrp2ModalSplit.put(ug, modalSplitPop);
			this.userGrp2Mode2Legs.put(ug, msg.getMode2NoOfLegs(usrGrpPop));
		}
	}

	public void writeResults(String outputFile){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			writer.write("UserGroup \t");

			for(String str:wholePop_pctModalShare.keySet()){
				writer.write(str+"\t");
			}
			writer.write(" \n WholePopulation"+"\t");
			for(String str:wholePop_Mode2Legs.keySet()){ // write Absolute No Of Legs
				writer.write(wholePop_Mode2Legs.get(str)+"\t");
			}
			writer.write(" \n WholePopulation"+"\t");
			for(String str:wholePop_pctModalShare.keySet()){ // write percentage no of legs
				writer.write(wholePop_pctModalShare.get(str)+"\t");
			}
			writer.newLine();
			for(UserGroup ug:this.userGrp2Mode2Legs.keySet()){
				writer.write(ug+"\t");
				for(String str:wholePop_pctModalShare.keySet()){
					if(this.userGrp2Mode2Legs.get(ug).get(str)!=null){
						writer.write(this.userGrp2Mode2Legs.get(ug).get(str)+"\t");
					} else
						writer.write(0.0+"\t");
				}
				writer.newLine();
				writer.write(ug+"\t");
				for(String str:wholePop_pctModalShare.keySet()){
					if(this.userGrp2ModalSplit.get(ug).get(str)!=null){
						writer.write(this.userGrp2ModalSplit.get(ug).get(str)+"\t");
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
