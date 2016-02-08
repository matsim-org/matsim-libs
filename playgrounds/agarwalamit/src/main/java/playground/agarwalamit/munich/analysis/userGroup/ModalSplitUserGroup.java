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

import playground.agarwalamit.analysis.modalShare.ModalShareFromPlans;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */
public class ModalSplitUserGroup {

	private SortedMap<UserGroup, SortedMap<String, Integer>> userGrp2Mode2Legs = new TreeMap<UserGroup, SortedMap<String,Integer>>();
	private SortedMap<UserGroup, SortedMap<String, Double>> userGrp2ModalSplit = new TreeMap<UserGroup, SortedMap<String,Double>>();

	private SortedMap<String, Double> wholePopPctModalShare ;
	private SortedMap<String, Integer> wholePopMode2Legs;

	public static void main(String[] args) {
		
		String outputDir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/otherRuns/output/1pct/run10/policies/backcasting/exposure/";
		String [] runCases =  {"ExI","5ExI","10ExI","15ExI","20ExI","25ExI"};
		
		for(String runCase :runCases){
			ModalSplitUserGroup msUG = new ModalSplitUserGroup();
			int it = 1500;
			msUG.run(outputDir+runCase+"/ITERS/it."+it+"/"+it+".plans.xml.gz");
			msUG.writeResults(outputDir+runCase+"/analysis/usrGrpToModalShare_it."+it+".txt");
		}
	}

	public void run(String populationFile){
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(populationFile);
		PersonFilter pf = new PersonFilter();
		
		ModalShareFromPlans msg = new ModalShareFromPlans(sc.getPopulation());

		wholePopPctModalShare = msg.getModeToPercentOfLegs();
		wholePopMode2Legs = msg.getModeToNumberOfLegs();

		for(UserGroup ug:UserGroup.values()){
			Population usrGrpPop = pf.getPopulation(sc.getPopulation(), ug);
			msg = new ModalShareFromPlans(usrGrpPop);
			SortedMap<String, Double> modalSplitPop = msg.getModeToPercentOfLegs();
			this.userGrp2ModalSplit.put(ug, modalSplitPop);
			this.userGrp2Mode2Legs.put(ug, msg.getModeToNumberOfLegs());
		}
	}

	public void writeResults(String outputFile){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			writer.write("UserGroup \t");

			for(String str:wholePopPctModalShare.keySet()){
				writer.write(str+"\t");
			}
			writer.write(" \n WholePopulation"+"\t");
			for(String str:wholePopMode2Legs.keySet()){ // write Absolute No Of Legs
				writer.write(wholePopMode2Legs.get(str)+"\t");
			}
			writer.write(" \n WholePopulation"+"\t");
			for(String str:wholePopPctModalShare.keySet()){ // write percentage no of legs
				writer.write(wholePopPctModalShare.get(str)+"\t");
			}
			writer.newLine();
			for(UserGroup ug:this.userGrp2Mode2Legs.keySet()){
				writer.write(ug+"\t");
				for(String str:wholePopPctModalShare.keySet()){
					if(this.userGrp2Mode2Legs.get(ug).get(str)!=null){
						writer.write(this.userGrp2Mode2Legs.get(ug).get(str)+"\t");
					} else
						writer.write(0.0+"\t");
				}
				writer.newLine();
				writer.write(ug+"\t");
				for(String str:wholePopPctModalShare.keySet()){
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
