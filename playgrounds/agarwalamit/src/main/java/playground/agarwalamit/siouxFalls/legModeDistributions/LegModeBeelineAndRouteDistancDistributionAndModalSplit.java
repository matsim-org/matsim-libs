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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;

import playground.agarwalamit.analysis.legMode.distributions.LegModeRouteDistanceDistributionAnalyzer;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.vsp.analysis.modules.legModeDistanceDistribution.LegModeDistanceDistribution;

/**
 * @author amit
 */
public class LegModeBeelineAndRouteDistancDistributionAndModalSplit {
	
	private static final Logger log = Logger.getLogger(LegModeBeelineAndRouteDistancDistributionAndModalSplit.class);

	private final static String runDir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/iatbr/output/";//outputModalSplitSetUp
	private final static String [] runs = {"bau","ei","ci","eci"};

	public static void main(String[] args) {
		LegModeBeelineAndRouteDistancDistributionAndModalSplit ms= new LegModeBeelineAndRouteDistancDistributionAndModalSplit();

		for(String str:runs){
			String configFile = runDir+str+"/output_config.xml.gz";
			int lastIteration = LoadMyScenarios.getLastIteration(configFile);
			String finalPlanFile = runDir+str+"/ITERS/it."+lastIteration+"/"+lastIteration+".plans.xml.gz";
//			String networkFile = runDir+str+"/output_network.xml.gz";
//			String eventsFile = runDir+str+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";
//			ms.runRoutesDistance(str, networkFile, eventsFile);
			ms.runBeelineDistance(str, finalPlanFile);
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
	 * It will write route distance distribution from events and take the beeline distance for teleported modes
	 */
	private void runRoutesDistance(String runNr,String networkFile, String eventsFile){
		log.warn("Be careful, use this for distribution only if all modes are network modes.");
		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(networkFile);
		LegModeRouteDistanceDistributionAnalyzer	lmdfed = new LegModeRouteDistanceDistributionAnalyzer(null);
		lmdfed.init(sc,eventsFile);
		lmdfed.preProcessData();
		lmdfed.postProcessData();
		new File(runDir+"/analysis/legModeDistributions/").mkdirs();
		lmdfed.writeResults(runDir+"/analysis/legModeDistributions/"+runNr+".");
	}
}
