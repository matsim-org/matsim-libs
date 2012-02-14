/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionSummarizer.java
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
package playground.benjamin.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import playground.benjamin.emissions.types.ColdPollutant;
import playground.benjamin.emissions.types.WarmPollutant;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author benjamin
 *
 */
public class EmissionSummarizer {
	
	private final SortedSet<String> listOfPollutants = new TreeSet<String>();
	
	public EmissionSummarizer(){
		for(WarmPollutant wp : WarmPollutant.values()){
			listOfPollutants.add(wp.toString());
		}
		for(ColdPollutant cp : ColdPollutant.values()){
			listOfPollutants.add(cp.toString());
		}
	}

	public SortedMap<UserGroup, Map<String, Double>> sumUpEmissionsPerGroup(
			SortedMap<UserGroup, Map<String, Double>> group2FinalWarmEmissions,
			SortedMap<UserGroup, Map<String, Double>> group2FinalColdEmissions) {
		
		SortedMap<UserGroup, Map<String, Double>> totalEmissions = new TreeMap<UserGroup, Map<String, Double>>();
		for(Entry<UserGroup, Map<String, Double>> entry : group2FinalWarmEmissions.entrySet()){
			UserGroup group = entry.getKey();
			Map<String, Double> individualWarmEmissions = entry.getValue();

			if(group2FinalColdEmissions.containsKey(group)){
				Map<String, Double> groupSumOfEmissions = new HashMap<String, Double>();
				Map<String, Double> groupColdEmissions = group2FinalColdEmissions.get(group);
				Double individualValue;

				for(String pollutant : listOfPollutants){
					if(individualWarmEmissions.containsKey(pollutant)){
						if(groupColdEmissions.containsKey(pollutant)){
							individualValue = individualWarmEmissions.get(pollutant) + groupColdEmissions.get(pollutant);
						} else{
							individualValue = individualWarmEmissions.get(pollutant);
						}
					} else{
						individualValue = groupColdEmissions.get(pollutant);
					}
					groupSumOfEmissions.put(pollutant, individualValue);
				}
				totalEmissions.put(group, groupSumOfEmissions);
			} else{
				totalEmissions.put(group, individualWarmEmissions);
			}
		}
		return totalEmissions;
	}

	public SortedMap<String, Double> sumUpEmissions(
			SortedMap<String, Double> overallFinalWarmEmissions,
			SortedMap<String, Double> overallFinalColdEmissions) {
		
		SortedMap<String, Double> pollutant2sumOfEmissions = new TreeMap<String, Double>();
		Double sumOfEmissions;

		for(String pollutant : listOfPollutants){
			if(overallFinalWarmEmissions.containsKey(pollutant)){
				if(overallFinalColdEmissions.containsKey(pollutant)){
					sumOfEmissions = overallFinalWarmEmissions.get(pollutant) + overallFinalColdEmissions.get(pollutant);
				} else{
					sumOfEmissions = overallFinalWarmEmissions.get(pollutant);
				}
			} else{
				sumOfEmissions = overallFinalColdEmissions.get(pollutant);
			}
			pollutant2sumOfEmissions.put(pollutant, sumOfEmissions);
		}
		return pollutant2sumOfEmissions;
	}

}
