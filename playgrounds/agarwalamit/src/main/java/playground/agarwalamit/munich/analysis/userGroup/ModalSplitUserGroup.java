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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.modalShare.ModalShareFromPlans;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.munich.utils.MunichPersonFilter.MunichUserGroup;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */
public class ModalSplitUserGroup {
	
	private final static Logger LOG = Logger.getLogger(ModalSplitUserGroup.class);

	private SortedMap<MunichUserGroup, SortedMap<String, Integer>> userGrp2Mode2Legs = new TreeMap<>();
	private SortedMap<MunichUserGroup, SortedMap<String, Double>> userGrp2ModalSplit = new TreeMap<>();

	private SortedMap<String, Double> wholePopPctModalShare ;
	private SortedMap<String, Integer> wholePopMode2Legs;

	public static void main(String[] args) {
		
		String outputDir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/diss/output/";
		String [] runCases =  {"baseCase"};

		ModalSplitUserGroup msUG = new ModalSplitUserGroup();
		
		for(String runCase :runCases){
			int it = 1000;
//			String outputEventsFile = outputDir+"/"+runCase+"/output_events.xml.gz";
			String outputEventsFile = outputDir+"/"+runCase+"/ITERS/it."+it+"/"+it+".events.xml.gz";

			String plansFile = outputDir+runCase+"/ITERS/it."+it+"/"+it+".plans.xml.gz";
			msUG.run(outputEventsFile, plansFile);
			msUG.writeResults(outputDir+runCase+"/analysis/usrGrpToModalShare_it."+it+".txt");
		}
	}

	public void run( String eventsFile, String plansFile){
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(plansFile);
		MunichPersonFilter pf = new MunichPersonFilter();
		
		for( MunichUserGroup ug : MunichUserGroup.values()) {
			ModalShareFromEvents msc = new ModalShareFromEvents(eventsFile, ug.toString(), pf);
			msc.run();
			SortedMap<String, Integer> mode2legs_events = msc.getModeToNumberOfLegs();
			SortedMap<String, Double> mode2share_events = msc.getModeToPercentOfLegs();
			
			this.userGrp2ModalSplit.put(ug, mode2share_events);
			this.userGrp2Mode2Legs.put(ug, mode2legs_events);
			
			// it is possible to get the modal share from plans and it could match with share from events provided plans are experienced plans.
			ModalShareFromPlans msp = new ModalShareFromPlans(sc.getPopulation(),ug.toString(), pf);
			msp.run();
			SortedMap<String, Integer> mode2legs_plans = msp.getModeToNumberOfLegs();
//			SortedMap<String, Double> mode2share_plans = msp.getModeToPercentOfLegs();
			
			LOG.info("The modal share from events and plans for user group "+ ug.toString()+ " are as follows.");
			LOG.info("Mode \t\t legsFromPlans \t\t legsFromEvents \n");
			
			for(String mode : mode2legs_events.keySet()){
				LOG.info(mode+"\t\t"+mode2legs_plans.get(mode)+"\t\t"+mode2legs_events.get(mode)+"\n");
			}
			LOG.warn("If any agent is stuck, aborted trips are counted in plans but not in events.");
		}
		
		ModalShareFromEvents msc = new ModalShareFromEvents(eventsFile);
		msc.run();
		
		wholePopPctModalShare = msc.getModeToPercentOfLegs();
		wholePopMode2Legs = msc.getModeToNumberOfLegs();
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
			for(MunichUserGroup ug:this.userGrp2Mode2Legs.keySet()){
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
