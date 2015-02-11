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
package playground.agarwalamit.siouxFalls.legModeDistributions;

import java.io.File;

import org.matsim.api.core.v01.Scenario;

import playground.agarwalamit.analysis.legMode.distributions.LegModeRouteDistanceDistributionAnalyzer;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.vsp.analysis.modules.legModeDistanceDistribution.LegModeDistanceDistribution;

/**
 * @author amit
 */
public class LegModeBeelineAndRouteDistancDistributionAndModalSplit {

	private final static String runDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/";//outputModalSplitSetUp
//	private final static String run = "/run201/";
	private final static String [] runs = {"baseCase","baseCaseCtd","ei","ci","eci"};
	//	private  String initialPlanFile = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/input/SiouxFalls_population_probably_v3.xml";
	//	private  String initialPlanFile = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/outputMCOff/run33/output_plans.xml.gz";
//	private static String finalPlanFileLocation = runDir+run+"/ITERS/";

	public static void main(String[] args) {
		LegModeBeelineAndRouteDistancDistributionAndModalSplit ms= new LegModeBeelineAndRouteDistancDistributionAndModalSplit();

		//		for(int i=1;i<2;i++){
		//			String itNr = String.valueOf(i*100);
		//			String finalPlanFile = finalPlanFileLocation+"it."+itNr+"/"+itNr+".plans.xml.gz";
		//			ms.runBeelineDistance(itNr, finalPlanFile);
		//		}
		
		for(String str:runs){
			String configFile = runDir+str+"/output_config.xml";
			int lastIteration = LoadMyScenarios.getLastIteration(configFile);
			String finalPlanFile = runDir+str+"/ITERS/it."+lastIteration+"/"+lastIteration+".plans.xml.gz";
			String networkFile = runDir+str+"/output_network.xml.gz";
			String eventsFile = runDir+str+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";
			ms.runRoutesDistance(str, networkFile, eventsFile);
//			ms.runBeelineDistance(str, finalPlanFile);
		}
	}
	
	/**
	 * It will write legModeShare and beeline distance distribution	from plans
	 */
	private void runBeelineDistance(String runNr,String finalPlanFile){
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(finalPlanFile);
		LegModeDistanceDistribution	lmdd = new LegModeDistanceDistribution();
		lmdd.init(sc);
		lmdd.preProcessData();
		lmdd.postProcessData();
		lmdd.writeResults(runDir+runNr+"/analysis/legModeDistributions/"+runNr+".");
	}
	/**
	 * It will write route distance distribution from events	
	 */
	private void runRoutesDistance(String runNr,String networkFile, String eventsFile){
		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(networkFile);
		LegModeRouteDistanceDistributionAnalyzer	lmdfed = new LegModeRouteDistanceDistributionAnalyzer();
		lmdfed.init(sc,eventsFile);
		lmdfed.preProcessData();
		lmdfed.postProcessData();
		new File(runDir+"/analysis/legModeDistributions/").mkdirs();
		lmdfed.writeResults(runDir+"/analysis/legModeDistributions/"+runNr+".");
	}
}
