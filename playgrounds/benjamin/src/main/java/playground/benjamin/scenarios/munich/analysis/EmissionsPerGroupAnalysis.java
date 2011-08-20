/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionsPerGroupAggregator.java
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
package playground.benjamin.scenarios.munich.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;

import playground.benjamin.events.emissions.ColdPollutant;
import playground.benjamin.events.emissions.EmissionEventsReader;
import playground.benjamin.events.emissions.WarmPollutant;

/**
 * @author benjamin
 *
 */
public class EmissionsPerGroupAnalysis {
	private static final Logger logger = Logger.getLogger(EmissionsPerGroupAnalysis.class);
	
	private final static String runNumber = "985";
	private final static String runDirectory = "../../runs-svn/run" + runNumber + "/";
	
	private static String configFile = runDirectory + runNumber + ".output_config.xml.gz";
	private final static Integer lastIteration = getLastIteration(configFile);
	private final static String emissionFile = runDirectory + runNumber + "." + lastIteration + ".emission.events.xml.gz";
	
	private static EmissionsPerGroupWarmEventHandler warmHandler;
	private static EmissionsPerGroupColdEventHandler coldHandler;
	private static SortedSet<String> listOfPollutants;

	public static void main(String[] args) {
		processEmissions();
		defineListOfPollutants();
		
		Map<String, Map<String, Double>> group2WarmEmissions = warmHandler.getWarmEmissionsPerGroup();
		Map<String, Map<String, Double>> group2ColdEmissions = coldHandler.getColdEmissionsPerGroup();
		
		Map<String, Map<String, Double>> group2TotalEmissions = sumUpEmissions(group2WarmEmissions, group2ColdEmissions);
		
		for(String group : group2TotalEmissions.keySet()){
			System.out.println(group + " population exhausts: " + group2TotalEmissions.get(group));
		}
	}

	private static Map<String, Map<String, Double>> sumUpEmissions(Map<String, Map<String, Double>> warmEmissions, Map<String, Map<String, Double>> coldEmissions) {
		Map<String, Map<String, Double>> totalEmissions = new HashMap<String, Map<String, Double>>();
		for(Entry<String, Map<String, Double>> entry : warmEmissions.entrySet()){
			String group = entry.getKey();
			Map<String, Double> individualWarmEmissions = entry.getValue();

			if(coldEmissions.containsKey(group)){
				Map<String, Double> groupSumOfEmissions = new HashMap<String, Double>();
				Map<String, Double> groupColdEmissions = coldEmissions.get(group);
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

	private static void defineListOfPollutants() {
		listOfPollutants = new TreeSet<String>();
		for(WarmPollutant wp : WarmPollutant.values()){
			listOfPollutants.add(wp.toString());
		}
		for(ColdPollutant cp : ColdPollutant.values()){
			listOfPollutants.add(cp.toString());
		}
		logger.info("The following pollutants are considered: " + listOfPollutants);
	}

	private static void processEmissions() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		warmHandler = new EmissionsPerGroupWarmEventHandler();
		coldHandler = new EmissionsPerGroupColdEventHandler();
		eventsManager.addHandler(warmHandler);
		eventsManager.addHandler(coldHandler);
		emissionReader.parse(emissionFile);
	}

	private static Integer getLastIteration(String configFile2) {
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configFile);
		Integer lastIteration = config.controler().getLastIteration();
		return lastIteration;
	}
}