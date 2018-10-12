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
package org.matsim.contrib.emissions.utils;

import com.google.inject.Provides;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.emissions.roadTypeMapping.HbefaRoadTypeMapping;
import org.matsim.contrib.emissions.roadTypeMapping.VisumHbefaRoadTypeMapping;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.config.Config;

import java.net.URL;
import java.util.*;
import java.util.Map.Entry;


/**
 * @author ikaddoura, benjamin
 *
 */
public class EmissionUtils {
	private static final Logger logger = Logger.getLogger(EmissionUtils.class);

	private static final SortedSet<String> listOfPollutants;

	static {
		
		listOfPollutants = new TreeSet<>();
		for(WarmPollutant wp : WarmPollutant.values()){
			listOfPollutants.add(wp.toString());
		}
		for(ColdPollutant cp : ColdPollutant.values()){
			listOfPollutants.add(cp.toString());
		}
	}

	public static Map<String, Integer> createIndexFromKey(String strLine) {
		String[] keys = strLine.split(";") ;

		Map<String, Integer> indexFromKey = new HashMap<>() ;
		for ( int ii = 0; ii < keys.length; ii++ ) {
			indexFromKey.put(keys[ii], ii ) ;
		}
		return indexFromKey ;
	}

	public static final String HBEFA_ROAD_TYPE = "hbefa_road_type";
	public static void setHbefaRoadType(Link link, String type){
		if (type!=null){
			link.getAttributes().putAttribute(HBEFA_ROAD_TYPE, type);
		}
	}
	public static String getHbefaRoadType (Link link) {
		return (String) link.getAttributes().getAttribute(HBEFA_ROAD_TYPE);
	}

	public static SortedMap<String, Double> sumUpEmissions(Map<WarmPollutant, Double> warmEmissions, Map<ColdPollutant, Double> coldEmissions) {
		SortedMap<String, Double> pollutant2sumOfEmissions = new TreeMap<>();
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
	
	public static <T> Map<Id<T>, SortedMap<String, Double>> sumUpEmissionsPerId(
			Map<Id<T>, Map<WarmPollutant, Double>> warmEmissions,
			Map<Id<T>, Map<ColdPollutant, Double>> coldEmissions) {

		Map<Id<T>, SortedMap<String, Double>> totalEmissions = new HashMap<>();
		Set<Id<T>> warmColdIds = new HashSet<>();
		warmColdIds.addAll(warmEmissions.keySet());
		warmColdIds.addAll(coldEmissions.keySet());

		for (Id<T> id : warmColdIds) {
			if (warmEmissions.containsKey(id)) {
				if (coldEmissions.containsKey(id)) {
					SortedMap<String, Double> idSumOfEmissions = sumUpEmissions(warmEmissions.get(id), coldEmissions.get(id));
					totalEmissions.put(id, idSumOfEmissions);
				} else {
					SortedMap<String, Double> warmPollutantString2Values = convertWarmPollutantMap2String(warmEmissions.get(id));
					totalEmissions.put(id, warmPollutantString2Values);
				}
			} else {
				SortedMap<String, Double> coldPollutantString2Values = convertColdPollutantMap2String(coldEmissions.get(id));
				totalEmissions.put(id, coldPollutantString2Values);
			}
		}
		return totalEmissions;
	}

	public static Map<Id<Person>, SortedMap<String, Double>> setNonCalculatedEmissionsForPopulation(Population population, Map<Id<Person>, SortedMap<String, Double>> totalEmissions) {
		Map<Id<Person>, SortedMap<String, Double>> personId2Emissions = new HashMap<>();

		for(Person person : population.getPersons().values()){
			Id<Person> personId = person.getId();
			SortedMap<String, Double> emissionType2Value;
			if(totalEmissions.get(personId) == null){ // person not in map (e.g. pt user)
				emissionType2Value = new TreeMap<>();
				for(String pollutant : listOfPollutants){
					emissionType2Value.put(pollutant, 0.0);
				}
			} else { // person in map, but some emissions are not set; setting these to 0.0 
				emissionType2Value = totalEmissions.get(personId);
				for(String pollutant : listOfPollutants){ 
					if(emissionType2Value.get(pollutant) == null){
						emissionType2Value.put(pollutant, 0.0);
					} // else do nothing
				}
			}
			personId2Emissions.put(personId, emissionType2Value);
		}
		return personId2Emissions;
	}
	
	public static Map<Id<Link>, SortedMap<String, Double>> setNonCalculatedEmissionsForNetwork(Network network, Map<Id<Link>, SortedMap<String, Double>> totalEmissions) {
		Map<Id<Link>, SortedMap<String, Double>> linkId2Emissions = new HashMap<>();

		for(Link link: network.getLinks().values()){
			Id<Link> linkId = link.getId();
			SortedMap<String, Double> emissionType2Value;
			
			if(totalEmissions.get(linkId) == null){
				emissionType2Value = new TreeMap<>();
				for(String pollutant : listOfPollutants){
					emissionType2Value.put(pollutant, 0.0);
				}
			} else {
				emissionType2Value = totalEmissions.get(linkId);
				for(String pollutant : listOfPollutants){ 
					if(emissionType2Value.get(pollutant) == null){
						emissionType2Value.put(pollutant, 0.0);
					} else {
						emissionType2Value.put(pollutant, emissionType2Value.get(pollutant));
					}
				}
			}
			linkId2Emissions.put(linkId, emissionType2Value);
		}
		return linkId2Emissions;
	}

	public static <T> SortedMap<String, Double> getTotalEmissions(Map<Id<T>, SortedMap<String, Double>> person2TotalEmissions) {
		SortedMap<String, Double> totalEmissions = new TreeMap<>();

		for(Id<T> personId : person2TotalEmissions.keySet()){
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

	public static SortedMap<String, Double> convertWarmPollutantMap2String(Map<WarmPollutant, Double> warmEmissions) {
		SortedMap<String, Double> warmPollutantString2Values = new TreeMap<>();
		for (Entry<WarmPollutant, Double> entry: warmEmissions.entrySet()){
			String pollutant = entry.getKey().toString();
			Double value = entry.getValue();
			warmPollutantString2Values.put(pollutant, value);
		}
		return warmPollutantString2Values;
	}

	public static SortedMap<String, Double> convertColdPollutantMap2String(Map<ColdPollutant, Double> coldEmissions) {
		SortedMap<String, Double> coldPollutantString2Values = new TreeMap<>();
		for (Entry<ColdPollutant, Double> entry: coldEmissions.entrySet()){
			String pollutant = entry.getKey().toString();
			Double value = entry.getValue();
			coldPollutantString2Values.put(pollutant, value);
		}
		return coldPollutantString2Values;
	}

	public static SortedSet<String> getListOfPollutants() {
		return listOfPollutants;
	}
}
