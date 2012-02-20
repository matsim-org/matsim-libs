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
package playground.benjamin.scenarios.munich.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

import playground.benjamin.emissions.types.ColdPollutant;
import playground.benjamin.emissions.types.WarmPollutant;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author benjamin
 *
 */
public class EmissionSummarizer {
	private static final Logger logger = Logger.getLogger(EmissionSummarizer.class);

	private final PersonFilter personFilter;
	private final SortedSet<String> listOfPollutants;

	public EmissionSummarizer(){
		personFilter = new PersonFilter();
		listOfPollutants = new TreeSet<String>();
		for(WarmPollutant wp : WarmPollutant.values()){
			listOfPollutants.add(wp.toString());
		}
		for(ColdPollutant cp : ColdPollutant.values()){
			listOfPollutants.add(cp.toString());
		}
	}

	public SortedMap<String, Double> sumUpEmissions(Map<WarmPollutant, Double> warmEmissions, Map<ColdPollutant, Double> coldEmissions) {
		SortedMap<String, Double> pollutant2sumOfEmissions = new TreeMap<String, Double>();
		SortedMap<String, Double> warmPollutant2emissions = convertWarmPollutantMap2String(warmEmissions);
		SortedMap<String, Double> coldPollutant2emissions = convertColdPollutantMap2String(coldEmissions);
		double sumOfPollutant;
	
		for(String pollutant : listOfPollutants){
			if(warmPollutant2emissions.containsKey(pollutant)){
				if(coldPollutant2emissions.containsKey(pollutant)){
					sumOfPollutant = warmPollutant2emissions.get(pollutant) + coldPollutant2emissions.get(pollutant);
				} else{
					sumOfPollutant = warmPollutant2emissions.get(pollutant);
				}
			} else{
				sumOfPollutant = coldPollutant2emissions.get(pollutant);
			}
			pollutant2sumOfEmissions.put(pollutant, sumOfPollutant);
		}
		return pollutant2sumOfEmissions;
	}

	public Map<Id, SortedMap<String, Double>> sumUpEmissionsPerPerson(
			Map<Id, Map<WarmPollutant, Double>> warmEmissions,
			Map<Id, Map<ColdPollutant, Double>> coldEmissions) {

		Map<Id, SortedMap<String, Double>> totalEmissions = new HashMap<Id, SortedMap<String, Double>>();
		for(Entry<Id, Map<WarmPollutant, Double>> entry : warmEmissions.entrySet()){
			Id personId = entry.getKey();
			Map<WarmPollutant, Double> individualWarmEmissions = entry.getValue();

			if(coldEmissions.containsKey(personId)){
				Map<ColdPollutant, Double> individualColdEmissions = coldEmissions.get(personId);
				SortedMap<String, Double> individualSumOfEmissions = sumUpEmissions(individualWarmEmissions, individualColdEmissions);
				totalEmissions.put(personId, individualSumOfEmissions);
			} else{
				SortedMap<String, Double> warmPollutantString2Values = convertWarmPollutantMap2String(individualWarmEmissions);
				totalEmissions.put(personId, warmPollutantString2Values);
			}
		}
		return totalEmissions;
	}

	public Map<Id, SortedMap<String, Double>> setNonCalculatedEmissions(Population population, Map<Id, SortedMap<String, Double>> totalEmissions) {
		Map<Id, SortedMap<String, Double>> filledEmissionsPerPerson = new HashMap<Id, SortedMap<String, Double>>();

		for(Person person : population.getPersons().values()){
			Id personId = person.getId();
			if(!totalEmissions.containsKey(personId)){
				Map<String, Double> emissionType2Value = new HashMap<String, Double>();
				for(String pollutant : listOfPollutants){
					// setting emissions that are were not calculated to 0.0 
					emissionType2Value.put(pollutant, 0.0);
				}
				filledEmissionsPerPerson.put(personId, totalEmissions.get(personId));
			} else{
				// do nothing
			}
		}
		return filledEmissionsPerPerson;
	}

	public SortedMap<UserGroup, SortedMap<String, Double>> getEmissionsPerGroup(Map<Id, SortedMap<String, Double>> person2TotalEmissions) {
		SortedMap<UserGroup, SortedMap<String, Double>> userGroup2Emissions = new TreeMap<UserGroup, SortedMap<String, Double>>();

		for(UserGroup userGroup : UserGroup.values()){
			SortedMap<String, Double> totalEmissions = new TreeMap<String,Double>();
			for(Id personId : person2TotalEmissions.keySet()){
				SortedMap<String, Double> individualEmissions = person2TotalEmissions.get(personId);
				if(personFilter.isPersonIdFromUserGroup(personId, userGroup)){
					double sumOfPollutant;
					for(String pollutant : individualEmissions.keySet()){
						if(totalEmissions.containsKey(pollutant)){
							sumOfPollutant = totalEmissions.get(pollutant) + individualEmissions.get(pollutant);
						} else {
							sumOfPollutant = individualEmissions.get(pollutant);
						}
						totalEmissions.put(pollutant, sumOfPollutant);
					}
				}
			}
			userGroup2Emissions.put(userGroup, totalEmissions);
		}
		return userGroup2Emissions;
	}

	public SortedMap<String, Double> getTotalEmissions(Map<Id, SortedMap<String, Double>> person2TotalEmissions) {
		SortedMap<String, Double> totalEmissions = new TreeMap<String, Double>();

		for(Id personId : person2TotalEmissions.keySet()){
			SortedMap<String, Double> individualEmissions = person2TotalEmissions.get(personId);
			double sumOfPollutant;
			for(String pollutant : individualEmissions.keySet()){
				if(totalEmissions.containsKey(pollutant)){
					sumOfPollutant = totalEmissions.get(pollutant) + individualEmissions.get(pollutant);
				} else {
					sumOfPollutant = individualEmissions.get(pollutant);
				}
				totalEmissions.put(pollutant, sumOfPollutant);
			}
		}
		return totalEmissions;
	}

	public SortedSet<String> getListOfPollutants() {
		return listOfPollutants;
	}

	private SortedMap<String, Double> convertWarmPollutantMap2String(Map<WarmPollutant, Double> warmEmissions) {
		SortedMap<String, Double> warmPollutantString2Values = new TreeMap<String, Double>();
		for (Entry<WarmPollutant, Double> entry: warmEmissions.entrySet()){
			String pollutant = entry.getKey().toString();
			Double value = entry.getValue();
			warmPollutantString2Values.put(pollutant, value);
		}
		return warmPollutantString2Values;
	}

	private SortedMap<String, Double> convertColdPollutantMap2String(Map<ColdPollutant, Double> coldEmissions) {
		SortedMap<String, Double> coldPollutantString2Values = new TreeMap<String, Double>();
		for (Entry<ColdPollutant, Double> entry: coldEmissions.entrySet()){
			String pollutant = entry.getKey().toString();
			Double value = entry.getValue();
			coldPollutantString2Values.put(pollutant, value);
		}
		return coldPollutantString2Values;
	}

}
