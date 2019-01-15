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
package org.matsim.contrib.emissions;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.VehicleType;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author ikaddoura, benjamin
 *
 */
public class EmissionUtils {
	private static final Logger logger = Logger.getLogger(EmissionUtils.class);

	static Map<String, Integer> createIndexFromKey( String strLine ) {
		String[] keys = strLine.split(";") ;

		Map<String, Integer> indexFromKey = new HashMap<>() ;
		for ( int ii = 0; ii < keys.length; ii++ ) {
			indexFromKey.put(keys[ii], ii ) ;
		}
		return indexFromKey ;
	}

	private static final String HBEFA_ROAD_TYPE = "hbefa_road_type";
	static void setHbefaRoadType( Link link, String type ){
		if (type!=null){
			link.getAttributes().putAttribute(HBEFA_ROAD_TYPE, type);
		}
	}

	static String getHbefaRoadType( Link link ) {
		return (String) link.getAttributes().getAttribute(HBEFA_ROAD_TYPE);
	}

	public static Map<String, Double> sumUpEmissions(Map<String, Double> warmEmissions, Map<String, Double> coldEmissions) {

		Map<String, Double> pollutant2sumOfEmissions =
				Stream.concat(warmEmissions.entrySet().stream(), coldEmissions.entrySet().stream())
					.collect(Collectors.toMap(
							Entry::getKey, // The key
							Entry::getValue, // The value
							Double::sum
							)
					);

		return pollutant2sumOfEmissions;
	}
	
	public static <T> Map<Id<T>, Map<String, Double>> sumUpEmissionsPerId(
			Map<Id<T>, Map<String, Double>> warmEmissions,
			Map<Id<T>, Map<String, Double>> coldEmissions) {

		Map<Id<T>, Map<String, Double>> totalEmissions = warmEmissions.entrySet().stream().map(entry -> {
			Id<T> id = entry.getKey();
			Map<String, Double> warmEm = entry.getValue();
			Map<String, Double> coldEm = coldEmissions.getOrDefault(id, new HashMap<>());

			Map<String, Double> totalPollutantCounts = sumUpEmissions(warmEm, coldEm);
			return new Tuple<>(id, totalPollutantCounts);
		}).collect(Collectors.toMap(Tuple::getFirst, Tuple::getSecond));

		return totalEmissions;
	}

	public static Map<Id<Person>, SortedMap<String, Double>> setNonCalculatedEmissionsForPopulation(Population population, Map<Id<Person>, SortedMap<String, Double>> totalEmissions, Set<String> pollutants) {
		Map<Id<Person>, SortedMap<String, Double>> personId2Emissions = new HashMap<>();
		for(Person person : population.getPersons().values()){
			Id<Person> personId = person.getId();
			SortedMap<String, Double> emissionType2Value;
			if(totalEmissions.get(personId) == null){ // person not in map (e.g. pt user)
				emissionType2Value = new TreeMap<>();
				for(String pollutant :  pollutants){
					emissionType2Value.put(pollutant, 0.0);
				}
			} else { // person in map, but some emissions are not set; setting these to 0.0 
				emissionType2Value = totalEmissions.get(personId);
				for(String pollutant :  emissionType2Value.keySet()){
					// else do nothing
					emissionType2Value.putIfAbsent(pollutant, 0.0);
				}
			}
			personId2Emissions.put(personId, emissionType2Value);
		}
		return personId2Emissions;
	}

	private static Set<String> getAllPollutants(Map<Id<Person>, SortedMap<String, Double>> totalEmissions) {
		Set<String> pollutants = totalEmissions.values().stream().flatMap(m -> m.keySet().stream()).collect(Collectors.toSet());
		return pollutants;
	}

	public static Map<Id<Link>, SortedMap<String, Double>> setNonCalculatedEmissionsForNetwork(Network network, Map<Id<Link>, SortedMap<String, Double>> totalEmissions, Set<String> pollutants) {
		Map<Id<Link>, SortedMap<String, Double>> linkId2Emissions = new HashMap<>();

		for(Link link: network.getLinks().values()){
			Id<Link> linkId = link.getId();
			SortedMap<String, Double> emissionType2Value;
			
			if(totalEmissions.get(linkId) == null){
				emissionType2Value = new TreeMap<>();
				for(String pollutant : pollutants){
					emissionType2Value.put(pollutant, 0.0);
				}
			} else {
				emissionType2Value = totalEmissions.get(linkId);
				for(String pollutant :  pollutants){
					if(emissionType2Value.get(pollutant) == null){
						emissionType2Value.put(pollutant, 0.0);
					} else {
						//TODO: is this redundant?
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


	public static void setHbefaVehicleDescription( final VehicleType vt, final String hbefaVehicleDescription ) {
		// yyyy maybe this should use the vehicle information tuple (see below)?
		// yyyy replace this by using Attributes.  kai, oct'18
	    vt.setDescription(  vt.getDescription() + " " + EmissionSpecificationMarker.BEGIN_EMISSIONS.toString()+
							hbefaVehicleDescription +
			EmissionSpecificationMarker.END_EMISSIONS.toString() );
	}
	
	static Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> convertVehicleDescription2VehicleInformationTuple( String vehicleDescription ) {
		// yyyy what is the advantage of having this as a tuple over just using a class with four entries?  kai, oct'18
		
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple;
		HbefaVehicleCategory hbefaVehicleCategory = null;
		
		// yyyy replace this by using Attributes.  kai, oct'18
		int startIndex = vehicleDescription.indexOf(EmissionSpecificationMarker.BEGIN_EMISSIONS.toString()) + EmissionSpecificationMarker.BEGIN_EMISSIONS.toString().length();
		int endIndex = vehicleDescription.lastIndexOf(EmissionSpecificationMarker.END_EMISSIONS.toString());

		String[] vehicleInformationArray = vehicleDescription.substring(startIndex, endIndex).split(";");

		for(HbefaVehicleCategory vehCat : HbefaVehicleCategory.values()){
			if(vehCat.toString().equals(vehicleInformationArray[0])){
				hbefaVehicleCategory = vehCat;
			}
		}
		
		HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();
		if(vehicleInformationArray.length == 4){
			hbefaVehicleAttributes.setHbefaTechnology(vehicleInformationArray[1]);
			hbefaVehicleAttributes.setHbefaSizeClass(vehicleInformationArray[2]);
			hbefaVehicleAttributes.setHbefaEmConcept(vehicleInformationArray[3]);
		} // else interpretation as "average vehicle"

		vehicleInformationTuple = new Tuple<>(hbefaVehicleCategory, hbefaVehicleAttributes);
		return vehicleInformationTuple;
	}


	static Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>>
			createHBEFASpeedsTable( Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable ) {

		Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>> table = new HashMap<>();

		avgHbefaWarmTable.forEach((warmEmissionFactorKey, emissionFactor) -> {
			HbefaRoadVehicleCategoryKey roadVehicleCategoryKey = new HbefaRoadVehicleCategoryKey(warmEmissionFactorKey);
			HbefaTrafficSituation hbefaTrafficSituation = warmEmissionFactorKey.getHbefaTrafficSituation();
			double speed = emissionFactor.getSpeed();

			table.putIfAbsent(roadVehicleCategoryKey, new HashMap<>());
			table.get(roadVehicleCategoryKey).put(hbefaTrafficSituation, speed);
		});


		return table;
	}
}
