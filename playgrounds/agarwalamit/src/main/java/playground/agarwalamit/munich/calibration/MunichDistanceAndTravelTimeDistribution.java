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

import playground.agarwalamit.analysis.tripDistance.LegModeRouteDistanceDistributionAnalyzer;
import playground.agarwalamit.analysis.tripTime.LegModeTripTimeDistributionAnalyzer;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class MunichDistanceAndTravelTimeDistribution {

	public static void main(String[] args) {
		String runDir = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9/";
		String [] runs = {"baseCaseCtd","ei","ci","eci"};
		
		for(String str:runs){
			MunichDistanceAndTravelTimeDistribution ana = new MunichDistanceAndTravelTimeDistribution();
//			ana.runRoutesDistance(runDir, str);
			ana.runTravelTimeDistribution(runDir, str);
		}
	}
	
	/**
	 * It will write route distance distribution from events	
	 */
	private void runRoutesDistance(String runDir, String runNr){
		String configFile = runDir+runNr+"/output_config.xml";
		int lastIteration = LoadMyScenarios.getLastIteration(configFile);
		String networkFile = runDir+runNr+"/output_network.xml.gz";
		String eventsFile = runDir+runNr+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";
		
		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(networkFile);
		LegModeRouteDistanceDistributionAnalyzer	lmdfed = new LegModeRouteDistanceDistributionAnalyzer(null);
		lmdfed.init(sc,eventsFile);
		lmdfed.preProcessData();
		lmdfed.postProcessData();
		new File(runDir+"/analysis/legModeDistributions/").mkdirs();
		lmdfed.writeResults(runDir+"/analysis/legModeDistributions/"+runNr+".");
	}
	
	
	private void runTravelTimeDistribution(String runDir, String run){
		String configFile = runDir+run+"/output_config.xml";
		int lastItr = LoadMyScenarios.getLastIteration(configFile);
		String eventsFile = runDir+run+"/ITERS/it."+lastItr+"/"+lastItr+".events.xml.gz";
		
		LegModeTripTimeDistributionAnalyzer lmttd = new LegModeTripTimeDistributionAnalyzer(eventsFile);
		lmttd.preProcessData();
		lmttd.postProcessData();
		lmttd.writeResults(runDir+"/analysis/legModeDistributions/"+run);
	}
}
