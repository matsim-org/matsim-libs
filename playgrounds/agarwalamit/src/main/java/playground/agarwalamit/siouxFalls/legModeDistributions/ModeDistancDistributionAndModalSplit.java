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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.analysis.modules.legModeDistanceDistribution.LegModeDistanceDistribution;

/**
 * @author amit
 */
public class ModeDistancDistributionAndModalSplit {

	private final static String runDir = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/outputMC/";//outputModalSplitSetUp
	private final static String run = "/run101/";
	private final static String [] runs = {"run33"};//{"run105","run106","run107","run108"};
	//	private  String initialPlanFile = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/input/SiouxFalls_population_probably_v3.xml";
	//	private  String initialPlanFile = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/outputMCOff/run33/output_plans.xml.gz";
	private static String finalPlanFileLocation = runDir+run+"/ITERS/";

	public static void main(String[] args) {
		ModeDistancDistributionAndModalSplit ms= new ModeDistancDistributionAndModalSplit();

		//		for(int i=1;i<2;i++){
		//			String itNr = String.valueOf(i*100);
		//			String finalPlanFile = finalPlanFileLocation+"it."+itNr+"/"+itNr+".plans.xml.gz";
		//			ms.runBeelineDistance(itNr, finalPlanFile);
		//		}

		for(String str:runs){
			String finalPlanFile = runDir+str+"/output_plans.xml.gz";
			ms.runRouteskDistance(str, finalPlanFile);
		}

	}

	private void runBeelineDistance(String runNr,String finalPlanFile){
		Scenario sc = loadScenario(finalPlanFile);
		LegModeDistanceDistribution	lmdd = new LegModeDistanceDistribution();
		lmdd.init(sc);
		lmdd.preProcessData();
		lmdd.postProcessData();
		lmdd.writeResults(runDir+run+"/analysis/legModeDistributions/"+runNr+".");
	}

	private void runRouteskDistance(String runNr,String finalPlanFile){
		Scenario sc = loadScenario(finalPlanFile);
		LegModeRouteDistanceDistributionHandler	lmdfed = new LegModeRouteDistanceDistributionHandler();
		lmdfed.init(sc);
		lmdfed.preProcessData();
		lmdfed.postProcessData();
		lmdfed.writeResults(runDir+"/analysis/legModeDistributions/"+runNr+".");
		lmdfed.writeResults(runDir+"/analysisExecutedPlans/legModeDistributions/"+runNr+".");
	}

	private Scenario loadScenario(String planFile) {
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(planFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}
}
