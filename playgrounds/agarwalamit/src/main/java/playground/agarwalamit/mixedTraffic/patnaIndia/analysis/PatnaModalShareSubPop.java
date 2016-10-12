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
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.modalShare.ModalShareFromPlans;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter.PatnaUserGroup;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class PatnaModalShareSubPop {

	private final static Logger LOG = Logger.getLogger(PatnaModalShareSubPop.class);

	private final SortedMap<PatnaUserGroup, SortedMap<String, Integer>> userGrp2Mode2Legs = new TreeMap<>();
	private final SortedMap<PatnaUserGroup, SortedMap<String, Double>> userGrp2ModalSplit = new TreeMap<>();

	private SortedMap<String, Double> wholePopPctModalShare ;
	private SortedMap<String, Integer> wholePopMode2Legs;

	public static void main(String[] args) {
		String outputDir = "../../../../repos/runs-svn/patnaIndia/run108/jointDemand/policies/";
		String [] runCases =  {"baseCaseCtd","bikeTrack","both","trafficRestrain"};

		PatnaModalShareSubPop pmssp = new PatnaModalShareSubPop();
		for(String runCase :runCases){
			int it = 300;
			String outputEventsFile = outputDir+"/"+runCase+"/ITERS/it."+it+"/"+it+".events.xml.gz";
			String plansFile = outputDir+runCase+"/ITERS/it."+it+"/"+it+".plans.xml.gz";
			pmssp.run(outputEventsFile, plansFile);
			pmssp.writeResults(outputDir+runCase+"/analysis/usrGrpToModalShare_it."+it+"_2.txt");
		}
	}

	public void run( String eventsFile, String plansFile){
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(plansFile);
		PatnaPersonFilter pf = new PatnaPersonFilter();

		for( PatnaUserGroup ug : PatnaUserGroup.values()) {
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


	private void writeResults(String outFile){
		BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
		try {
			writer.write("userGroup \t");
			for(String mode : wholePopPctModalShare.keySet()){
				writer.write(mode+"\t");
			}

			writer.write("\n wholePop \t");
			for(String mode :wholePopMode2Legs.keySet()){
				writer.write(wholePopMode2Legs.get(mode)+"\t");
			}
			writer.newLine();

			writer.write("\n wholePop \t");
			for(String mode :wholePopPctModalShare.keySet()){
				writer.write(wholePopPctModalShare.get(mode)+"\t");
			}
			writer.newLine();

			for(PatnaUserGroup ug: this.userGrp2Mode2Legs.keySet()){
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
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
	}
}