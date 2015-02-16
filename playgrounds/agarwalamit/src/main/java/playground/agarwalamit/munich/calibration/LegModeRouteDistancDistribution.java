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

import org.matsim.api.core.v01.Scenario;

import playground.agarwalamit.analysis.legMode.distributions.LegModeRouteDistanceDistributionAnalyzer;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */
public class LegModeRouteDistancDistribution {

	private final static String runDir = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run10/";
	private final static String [] runs = {"c0"};

	public static void main(String[] args) {
		LegModeRouteDistancDistribution ms= new LegModeRouteDistancDistribution();

		for(String str:runs){
			String configFile = runDir+str+"/output_config.xml";
			int lastIteration = LoadMyScenarios.getLastIteration(configFile);
			String networkFile = runDir+str+"/output_network.xml.gz";
			String eventsFile = runDir+str+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";
			ms.runRoutesDistance(str, networkFile, eventsFile);
		}
	}
	
	/**
	 * It will write route distance distribution from events	
	 */
	private void runRoutesDistance(String runNr,String networkFile, String eventsFile){
		UserGroup ug =  UserGroup.URBAN;
		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(networkFile);
		LegModeRouteDistanceDistributionAnalyzer	lmdfed = new LegModeRouteDistanceDistributionAnalyzer(null);
		lmdfed.init(sc,eventsFile);
		lmdfed.preProcessData();
		lmdfed.postProcessData();
		new File(runDir+"/analysis/legModeDistributions/").mkdirs();
		lmdfed.writeResults(runDir+"/analysis/legModeDistributions/"+runNr+"_it.0_"+".");
	}
}
