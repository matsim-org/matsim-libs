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
package playground.vsp.emissions.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.WarmPollutant;

/**
 * @author ikaddoura, benjamin
 *
 */
public class EmissionUtils {
	private static final Logger logger = Logger.getLogger(EmissionUtils.class);

	final SortedSet<String> listOfPollutants;

	public EmissionUtils(){
		
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
			} else if(coldPollutant2emissions.containsKey(pollutant)){
				sumOfPollutant = coldPollutant2emissions.get(pollutant);
			} else {
				sumOfPollutant = 0.0;
				logger.warn("Pollutant " + pollutant + " is not found in the emission events file [probably an old file]." +
						"Therefore setting it to " + sumOfPollutant);
			}
			pollutant2sumOfEmissions.put(pollutant, sumOfPollutant);
		}
		return pollutant2sumOfEmissions;
	}

	public Map<Id, SortedMap<String, Double>> sumUpEmissionsPerId(
			Map<Id, Map<WarmPollutant, Double>> warmEmissions,
			Map<Id, Map<ColdPollutant, Double>> coldEmissions) {

		Map<Id, SortedMap<String, Double>> totalEmissions = new HashMap<Id, SortedMap<String, Double>>();
		for(Entry<Id, Map<WarmPollutant, Double>> entry : warmEmissions.entrySet()){
			Id id = entry.getKey();
			Map<WarmPollutant, Double> idWarmEmissions = entry.getValue();

			if(coldEmissions.containsKey(id)){
				Map<ColdPollutant, Double> idColdEmissions = coldEmissions.get(id);
				SortedMap<String, Double> idSumOfEmissions = sumUpEmissions(idWarmEmissions, idColdEmissions);
				totalEmissions.put(id, idSumOfEmissions);
			} else{
				SortedMap<String, Double> warmPollutantString2Values = convertWarmPollutantMap2String(idWarmEmissions);
				totalEmissions.put(id, warmPollutantString2Values);
			}
		}
		return totalEmissions;
	}

	public Map<Id, SortedMap<String, Double>> setNonCalculatedEmissionsForPopulation(Population population, Map<Id, SortedMap<String, Double>> totalEmissions) {
		Map<Id, SortedMap<String, Double>> personId2Emissions = new HashMap<Id, SortedMap<String, Double>>();

		for(Person person : population.getPersons().values()){
			Id personId = person.getId();
			SortedMap<String, Double> emissionType2Value;
			if(totalEmissions.get(personId) == null){ // person not in map (e.g. pt user)
				emissionType2Value = new TreeMap<String, Double>();
				for(String pollutant : listOfPollutants){
					emissionType2Value.put(pollutant, 0.0);
				}
			} else { // person in map, but some emissions are not set; setting these to 0.0 
				emissionType2Value = totalEmissions.get(personId);
				for(String pollutant : listOfPollutants){ 
					if(emissionType2Value.get(pollutant) == null){
						emissionType2Value.put(pollutant, 0.0);
					} else {
						// do nothing
					}
				}
			}
			personId2Emissions.put(personId, emissionType2Value);
		}
		return personId2Emissions;
	}
	
	public Map<Id, SortedMap<String, Double>> setNonCalculatedEmissionsForNetwork(Network network, Map<Id, SortedMap<String, Double>> totalEmissions) {
		Map<Id, SortedMap<String, Double>> linkId2Emissions = new HashMap<Id, SortedMap<String, Double>>();

		for(Link link: network.getLinks().values()){
			Id linkId = link.getId();
			SortedMap<String, Double> emissionType2Value;
			if(!totalEmissions.containsKey(linkId)){ //link not in map (e.g. no cars on link)
				emissionType2Value = new TreeMap<String, Double>();
				for(String pollutant : listOfPollutants){
					emissionType2Value.put(pollutant, 0.0);
				}
				totalEmissions.put(linkId, emissionType2Value);
			}
			else if(totalEmissions.get(linkId) == null){ // link in map but emissions not set (e.g. no cars on link)
				emissionType2Value = new TreeMap<String, Double>();
				for(String pollutant : listOfPollutants){
					emissionType2Value.put(pollutant, 0.0);
				}
			} else { // setting emissions that are not set for this link to 0.0 
				emissionType2Value = totalEmissions.get(linkId);
				for(String pollutant : listOfPollutants){ 
					if(emissionType2Value.get(pollutant) == null){
						emissionType2Value.put(pollutant, 0.0);
					} else {
						// do nothing
					}
				}
			}
			linkId2Emissions.put(linkId, emissionType2Value);
		}
		return linkId2Emissions;
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

	public SortedMap<String, Double> convertWarmPollutantMap2String(Map<WarmPollutant, Double> warmEmissions) {
		SortedMap<String, Double> warmPollutantString2Values = new TreeMap<String, Double>();
		for (Entry<WarmPollutant, Double> entry: warmEmissions.entrySet()){
			String pollutant = entry.getKey().toString();
			Double value = entry.getValue();
			warmPollutantString2Values.put(pollutant, value);
		}
		return warmPollutantString2Values;
	}

	public SortedMap<String, Double> convertColdPollutantMap2String(Map<ColdPollutant, Double> coldEmissions) {
		SortedMap<String, Double> coldPollutantString2Values = new TreeMap<String, Double>();
		for (Entry<ColdPollutant, Double> entry: coldEmissions.entrySet()){
			String pollutant = entry.getKey().toString();
			Double value = entry.getValue();
			coldPollutantString2Values.put(pollutant, value);
		}
		return coldPollutantString2Values;
	}

}
