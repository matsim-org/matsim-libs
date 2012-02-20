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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import playground.benjamin.emissions.events.EmissionEventsReader;
import playground.benjamin.emissions.types.ColdPollutant;
import playground.benjamin.emissions.types.WarmPollutant;
import playground.benjamin.scenarios.munich.analysis.EmissionSummarizer;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.benjamin.scenarios.munich.analysis.mobilTUM.EmissionsPerPersonColdEventHandler;
import playground.benjamin.scenarios.munich.analysis.mobilTUM.EmissionsPerPersonWarmEventHandler;
import playground.benjamin.utils.BkNumberUtils;

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
		EmissionSummarizer summarizer = new EmissionSummarizer();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		EmissionsPerPersonWarmEventHandler warmHandler = new EmissionsPerPersonWarmEventHandler();
		EmissionsPerPersonColdEventHandler coldHandler = new EmissionsPerPersonColdEventHandler();
		eventsManager.addHandler(warmHandler);
		eventsManager.addHandler(coldHandler);
		emissionReader.parse(emissionEventsFile);
		
		Map<Id, Map<WarmPollutant, Double>> person2FinalWarmEmissions = warmHandler.getWarmEmissionsPerPerson();
		Map<Id, Map<ColdPollutant, Double>> person2FinalColdEmissions = coldHandler.getColdEmissionsPerPerson();
		Map<Id, SortedMap<String, Double>> person2FinalTotalEmissions = summarizer.sumUpEmissionsPerPerson(person2FinalWarmEmissions, person2FinalColdEmissions);
		SortedMap<UserGroup, SortedMap<String, Double>> group2FinalTotalEmissions = summarizer.getEmissionsPerGroup(person2FinalTotalEmissions);
		
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
		SortedMap<String, Double> overallFinalTotalEmissions = summarizer.getTotalEmissions(person2FinalTotalEmissions);
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
