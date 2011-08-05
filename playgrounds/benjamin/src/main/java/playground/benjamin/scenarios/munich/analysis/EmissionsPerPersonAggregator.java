/* *********************************************************************** *
 * project: org.matsim.*
 * AnalysisEmissionsPerArea.java
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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import playground.benjamin.events.emissions.EmissionEventsReader;

/**
 * @author benjamin
 *
 */
public class EmissionsPerPersonAggregator {
	private static final Logger logger = Logger.getLogger(EmissionsPerPersonAggregator.class);

	private final Population population;
	private final String emissionFile;
	
	private EmissionsPerPersonWarmEventHandler warmHandler;
	private EmissionsPerPersonColdEventHandler coldHandler;
	private Map<Id, Map<String, Double>> warmEmissions;
	private Map<Id, Map<String, Double>> coldEmissions;
	private Map<Id, Map<String, Double>> totalEmissions;
	private SortedSet<String> listOfPollutants;

	EmissionsPerPersonAggregator(Population population, String emissionFile) {
		this.population = population;
		this.emissionFile = emissionFile;
	}

	void run() {
		processEmissions();
		warmEmissions = warmHandler.getWarmEmissionsPerPerson();
		coldEmissions = coldHandler.getColdEmissionsPerPerson();
		fillListOfPollutants(warmEmissions, coldEmissions);
		setNonCalculatedEmissions(this.population, warmEmissions);
		setNonCalculatedEmissions(this.population, coldEmissions);
		totalEmissions = sumUpEmissions(warmEmissions, coldEmissions);
	}

	Map<Id, Map<String, Double>> getTotalEmissions() {
		return totalEmissions;
	}
	
	Map<Id, Map<String, Double>> getColdEmissions() {
		return coldEmissions;
	}

	Map<Id, Map<String, Double>> getWarmEmissions() {
		return warmEmissions;
	}
	
	SortedSet<String> getListOfPollutants() {
		return listOfPollutants;
	}
	
	private Map<Id, Map<String, Double>> sumUpEmissions(Map<Id, Map<String, Double>> warmEmissions, Map<Id, Map<String, Double>> coldEmissions) {
		Map<Id, Map<String, Double>> totalEmissions = new HashMap<Id, Map<String, Double>>();
		for(Entry<Id, Map<String, Double>> entry : warmEmissions.entrySet()){
			Id personId = entry.getKey();
			Map<String, Double> individualWarmEmissions = entry.getValue();

			if(coldEmissions.containsKey(personId)){
				Map<String, Double> individualSumOfEmissions = new HashMap<String, Double>();
				Map<String, Double> individualColdEmissions = coldEmissions.get(personId);
				Double individualValue;

				for(String pollutant : listOfPollutants){
					if(individualWarmEmissions.containsKey(pollutant)){
						if(individualColdEmissions.containsKey(pollutant)){
							individualValue = individualWarmEmissions.get(pollutant) + individualColdEmissions.get(pollutant);
						} else{
							individualValue = individualWarmEmissions.get(pollutant);
						}
					} else{
						individualValue = individualColdEmissions.get(pollutant);
					}
					individualSumOfEmissions.put(pollutant, individualValue);
				}
				totalEmissions.put(personId, individualSumOfEmissions);
			} else{
				totalEmissions.put(personId, individualWarmEmissions);
			}
		}
		return totalEmissions;
	}

	private void setNonCalculatedEmissions(Population population, Map<Id, Map<String, Double>> emissionsPerPerson) {
		for(Person person : population.getPersons().values()){
			Id personId = person.getId();
			if(!emissionsPerPerson.containsKey(personId)){
				Map<String, Double> emissionType2Value = new HashMap<String, Double>();
				for(String pollutant : listOfPollutants){
					// setting emissions that are were not calculated to 0.0 
					emissionType2Value.put(pollutant, 0.0);
				}
				emissionsPerPerson.put(personId, emissionType2Value);
			} else{
				// do nothing
			}
		}
	}

	private void fillListOfPollutants(Map<Id, Map<String, Double>> warmEmissions, Map<Id, Map<String, Double>> coldEmissions) {
		listOfPollutants = new TreeSet<String>();
		for(Map<String, Double> emissionType2Value : warmEmissions.values()){
			for(String pollutant : emissionType2Value.keySet()){
				if(!listOfPollutants.contains(pollutant)){
					listOfPollutants.add(pollutant);
				}
			}
		}
		for(Map<String, Double> emissionType2Value : coldEmissions.values()){
			for(String pollutant : emissionType2Value.keySet()){
				if(!listOfPollutants.contains(pollutant)){
					listOfPollutants.add(pollutant);
				}
			}
		}
		logger.info("The following pollutants are considered: " + listOfPollutants);
	}

	private void processEmissions() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		warmHandler = new EmissionsPerPersonWarmEventHandler();
		coldHandler = new EmissionsPerPersonColdEventHandler();
		eventsManager.addHandler(warmHandler);
		eventsManager.addHandler(coldHandler);
		emissionReader.parse(this.emissionFile);
	}
}