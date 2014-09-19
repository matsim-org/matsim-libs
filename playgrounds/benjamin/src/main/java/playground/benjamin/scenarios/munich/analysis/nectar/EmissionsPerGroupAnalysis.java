/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionsPerGroupAnalysis.java
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
package playground.benjamin.scenarios.munich.analysis.nectar;

import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroupUtils;
import playground.benjamin.utils.BkNumberUtils;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;

/**
 * @author benjamin
 *
 */
public class EmissionsPerGroupAnalysis {
	private static final Logger logger = Logger.getLogger(EmissionsPerGroupAnalysis.class);

	private final static String runNumber = "985";
	private final static String runDirectory = "../../runs-svn/run" + runNumber + "/";
	private final String emissionEventsFile = runDirectory + runNumber + "." + "1500.emission.events.xml.gz";

	private void run(String[] args) {

		EmissionsAnalyzer ema = new EmissionsAnalyzer(emissionEventsFile);
		ema.init(null);
		ema.preProcessData();
		ema.postProcessData();
		
		UserGroupUtils ugu = new UserGroupUtils();
		Map<Id<Person>, SortedMap<String, Double>> person2totalEmissions = ema.getPerson2totalEmissions();
		SortedMap<UserGroup, SortedMap<String, Double>> group2FinalTotalEmissions = ugu.getEmissionsPerGroup(person2totalEmissions);

		for(UserGroup userGroup : group2FinalTotalEmissions.keySet()){
			System.out.println("\n*******************************************************************");
			System.out.println("VALUES FOR " + userGroup);
			System.out.println("*******************************************************************");
			Map<String, Double> pollutant2FinalEmissions = group2FinalTotalEmissions.get(userGroup);
			for(String pollutant : pollutant2FinalEmissions.keySet()){
				System.out.println("Final emissions for " + pollutant + " are calculated to\t" 
						+ BkNumberUtils.roundDouble(pollutant2FinalEmissions.get(pollutant), 3));
			}
		}
		
		SortedMap<String, Double> overallFinalTotalEmissions = ema.getTotalEmissions();
		System.out.println("\n*******************************************************************");
		System.out.println("VALUES FOR WHOLE POPULATION");
		System.out.println("*******************************************************************");
		for(String pollutant : overallFinalTotalEmissions.keySet()){
			System.out.println("Final emissions for " + pollutant + " are calculated to\t" 
					+ BkNumberUtils.roundDouble(overallFinalTotalEmissions.get(pollutant), 3));
		}
	}

	public static void main(String[] args) {
		EmissionsPerGroupAnalysis msa = new EmissionsPerGroupAnalysis();
		msa.run(args);
	}

}
