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
package playground.agarwalamit.munich.calibration;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;

import playground.agarwalamit.analysis.tripDistance.LegModeRouteDistanceDistributionAnalyzer;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.vsp.analysis.modules.legModeDistanceDistribution.LegModeDistanceDistribution;

/**
 * @author amit
 */
public class LegModeRouteDistancDistribution {

	private final static String RUN_DIR = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run10/policies/";
	private final static String [] RUNS_CASES = {"bau","ei","ci","eci","10ei"};
	private static final Logger LOG = Logger.getLogger(LegModeRouteDistancDistribution.class);

	public static void main(String[] args) {
		LegModeRouteDistancDistribution ms= new LegModeRouteDistancDistribution();

		for(String str:RUNS_CASES){
			String configFile = RUN_DIR+str+"/output_config.xml";
			String networkFile = RUN_DIR+str+"/output_network.xml.gz";
			Scenario sc = LoadMyScenarios.loadScenarioFromNetworkAndConfig(networkFile,configFile);
			sc.getConfig().controler().setOutputDirectory(RUN_DIR+str);
			ms.runRoutesDistance(str, sc);
		}
	}
	
	/**
	 * It will write route distance distribution from events and take the beeline distance for teleported modes	
	 */
	private void runRoutesDistance(final String runNr, final Scenario sc){
		UserGroup ug =  UserGroup.URBAN;
		int lastIteration = sc.getConfig().controler().getLastIteration();
		String eventsFile = sc.getConfig().controler().getOutputDirectory()+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";
		LegModeRouteDistanceDistributionAnalyzer lmdfed = new LegModeRouteDistanceDistributionAnalyzer(/*ug*/);
		lmdfed.init(sc,eventsFile);
		lmdfed.preProcessData();
		lmdfed.postProcessData();
		new File(RUN_DIR+"/analysis/legModeDistributions/").mkdirs();
		lmdfed.writeResults(RUN_DIR+"/analysis/legModeDistributions/"+runNr+"_it."+lastIteration+"_");
	}
	
	/**
	 * It will write legModeShare and beeline distance distribution	from plans
	 */
	private void runBeelineDistance(final String runNr, final String finalPlanFile){
		LOG.warn("This does not factor in the beeline distance factor used in the config. "
				+ "It means, this distance and teleportation distance from events are different.");
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(finalPlanFile);
		LegModeDistanceDistribution	lmdd = new LegModeDistanceDistribution();
		lmdd.init(sc);
		lmdd.preProcessData();
		lmdd.postProcessData();
		lmdd.writeResults(RUN_DIR+runNr+"/analysis/legModeDistributions/"+runNr+".");
	}
}