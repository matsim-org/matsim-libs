/* *********************************************************************** *
 * project: org.matsim.*
 * RunLegModeDistanceDistribution.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.santiago.analysis;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;

import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.benjamin.scenarios.munich.analysis.modular.legModeDistanceDistribution.RunLegModeDistanceDistribution;
import playground.vsp.analysis.modules.AbstractAnalysisModule;
import playground.vsp.analysis.modules.legModeDistanceDistribution.LegModeDistanceDistribution;

/**
 * @author benjamin
 *
 */
public class LegModeDistanceDistributionSantiagoComparison {
	private final static Logger logger = Logger.getLogger(LegModeDistanceDistributionSantiagoComparison.class);
	
//	static String baseFolder1 = "../../../runs-svn/santiago/baseCase8/";
	static String baseFolder1 = "../../../runs-svn/santiago/triangleCordon/";
	static String configFile1 = baseFolder1 + "output_config.xml.gz";
	static String iteration1 = "100";
	
	// ===
	static String baseFolder2 = baseFolder1;
	static String configFile2 = configFile1;
	static String iteration2 = "200";
	
	//TODO: adapt this to agentAttributes
	static UserGroup userGroup = null;
	
	public static void main(String[] args) {
		Gbl.startMeasurement();
		RunLegModeDistanceDistribution rlmdd1 = new RunLegModeDistanceDistribution(baseFolder1, configFile1, iteration1, userGroup);
		rlmdd1.run();
		
		RunLegModeDistanceDistribution rlmdd2 = new RunLegModeDistanceDistribution(baseFolder2, configFile2, iteration2, userGroup);
		rlmdd2.run();
		
		SortedMap<String, Map<Integer, Integer>> initialMode2DistanceClassLegCount = getMode2DistanceClassLegCount(rlmdd1);
		SortedMap<String, Map<Integer, Integer>> finalMode2DistanceClassLegCount = getMode2DistanceClassLegCount(rlmdd2);
		SortedMap<String, Map<Integer, Integer>> modeDifference2DistanceClassLegCount = calculateDifferenceMode2DistanceClassLegCount(initialMode2DistanceClassLegCount, finalMode2DistanceClassLegCount);
		
		SortedMap<String, Double> initialLegMode2Share = getMode2Share(rlmdd1);
		SortedMap<String, Double> finalLegMode2Share = getMode2Share(rlmdd2);
		SortedMap<String, Double> mode2ShareDifference = calculateLegModeShareDifference(initialLegMode2Share, finalLegMode2Share);
	
		// begin ugly code...
		List<AbstractAnalysisModule> anaModules = rlmdd1.getAnalysis().getAnaModules();
		for(AbstractAnalysisModule anaModule : anaModules){
			if(anaModule instanceof LegModeDistanceDistribution){
				((LegModeDistanceDistribution) anaModule).setMode2DistanceClass2LegCount(modeDifference2DistanceClassLegCount);
				((LegModeDistanceDistribution) anaModule).setMode2Share(mode2ShareDifference);

				anaModule.writeResults(baseFolder1 + iteration2 + "-" + iteration1 + ".");
//				anaModule.writeResults(baseFolder1  + "ITERS/it." + iteration1 + 
//						"/defaultAnalysis/" + anaModule.getName() + "/"
//						+ iteration2 + "-" + iteration1 + ".");
			}	
		}
		// end ugly code...
	}
	
	private static SortedMap<String, Double> getMode2Share(RunLegModeDistanceDistribution rlmdd) {
		SortedMap<String, Double> mode2Share = null;
		List<AbstractAnalysisModule> anaModules = rlmdd.getAnalysis().getAnaModules();
		for(AbstractAnalysisModule anaModule : anaModules){
			if(anaModule instanceof LegModeDistanceDistribution){
				mode2Share = ((LegModeDistanceDistribution) anaModule).getMode2Share();
			}
		}
		return mode2Share;
	}

	private static SortedMap<String, Map<Integer, Integer>> getMode2DistanceClassLegCount(RunLegModeDistanceDistribution rlmdd) {
		SortedMap<String, Map<Integer, Integer>> mode2DistanceClassLegCount = null;
		List<AbstractAnalysisModule> anaModules = rlmdd.getAnalysis().getAnaModules();
		for(AbstractAnalysisModule anaModule : anaModules){
			if(anaModule instanceof LegModeDistanceDistribution){
				mode2DistanceClassLegCount = ((LegModeDistanceDistribution) anaModule).getMode2DistanceClass2LegCount();
			}
		}
		return mode2DistanceClassLegCount;
	}

	private static SortedMap<String, Map<Integer, Integer>> calculateDifferenceMode2DistanceClassLegCount(SortedMap<String, Map<Integer, Integer>> initialMode2DistanceClassLegCount, SortedMap<String, Map<Integer, Integer>> finalMode2DistanceClassLegCount) {
		SortedMap<String, Map<Integer, Integer>> modeDifference2DistanceClassLegCount = new TreeMap<String, Map<Integer, Integer>>();
		for(String mode : finalMode2DistanceClassLegCount.keySet()){
			Map<Integer, Integer> finalMap = new TreeMap<Integer, Integer>();
			for(Entry<Integer, Integer> entry: finalMode2DistanceClassLegCount.get(mode).entrySet()){
				Integer distanceClass = entry.getKey();
				Integer difference = entry.getValue() - initialMode2DistanceClassLegCount.get(mode).get(entry.getKey());
				finalMap.put(distanceClass, difference);
			}
			modeDifference2DistanceClassLegCount.put(mode, finalMap);
		}
		return modeDifference2DistanceClassLegCount;
	}
	
	private static SortedMap<String, Double> calculateLegModeShareDifference(SortedMap<String, Double> initialLegMode2Share, SortedMap<String, Double> finalLegMode2Share) {
		SortedMap<String, Double> mode2ShareDiff = new TreeMap<String, Double>();
		
		for(String mode : finalLegMode2Share.keySet()){
			double finalModeShare = finalLegMode2Share.get(mode);
			double initialModeShare = initialLegMode2Share.get(mode);
			double modeShareDiff = finalModeShare - initialModeShare;
			mode2ShareDiff.put(mode, modeShareDiff);
		}
		return mode2ShareDiff;
	}
}