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
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author ikaddoura, benjamin
 */
public final class EmissionUtils {
	private static final Logger logger = Logger.getLogger(EmissionUtils.class);

	private static final String HBEFA_VEHICLE_DESCRIPTION = "hbefaVehicleTypeDescription";
	private enum EmissionSpecificationMarker {BEGIN_EMISSIONS , END_EMISSIONS }

	static Map<String, Integer> createIndexFromKey(String strLine) {
		String[] keys = strLine.split(";");

		Map<String, Integer> indexFromKey = new HashMap<>();
		for (int ii = 0; ii < keys.length; ii++) {
			indexFromKey.put(keys[ii], ii);
		}
		return indexFromKey;
	}

	private static final String HBEFA_ROAD_TYPE = "hbefa_road_type";

	/*package-private*/ static void setHbefaRoadType(Link link, String type) {
		if (type != null) {
			link.getAttributes().putAttribute(HBEFA_ROAD_TYPE, type);
		}
	}

	/*package-private*/ static String getHbefaRoadType(Link link) {
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

		if (warmEmissions == null)
			return coldEmissions;
		if (coldEmissions == null)
			return warmEmissions;

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
		for (Person person : population.getPersons().values()) {
			Id<Person> personId = person.getId();
			SortedMap<String, Double> emissionType2Value;
			if (totalEmissions.get(personId) == null) { // person not in map (e.g. pt user)
				emissionType2Value = new TreeMap<>();
				for (String pollutant : pollutants) {
					emissionType2Value.put(pollutant, 0.0);
				}
			} else { // person in map, but some emissions are not set; setting these to 0.0 
				emissionType2Value = totalEmissions.get(personId);
				for (String pollutant : emissionType2Value.keySet()) {
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

		for (Link link : network.getLinks().values()) {
			Id<Link> linkId = link.getId();
			SortedMap<String, Double> emissionType2Value;

			if (totalEmissions.get(linkId) == null) {
				emissionType2Value = new TreeMap<>();
				for (String pollutant : pollutants) {
					emissionType2Value.put(pollutant, 0.0);
				}
			} else {
				emissionType2Value = totalEmissions.get(linkId);
				for (String pollutant : pollutants) {
					if (emissionType2Value.get(pollutant) == null) {
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

		for (Id<T> personId : person2TotalEmissions.keySet()) {
			SortedMap<String, Double> individualEmissions = person2TotalEmissions.get(personId);
			double sumOfPollutant;
			for (String pollutant : individualEmissions.keySet()) {
				if (totalEmissions.containsKey(pollutant)) {
					sumOfPollutant = totalEmissions.get(pollutant) + individualEmissions.get(pollutant);
				} else {
					sumOfPollutant = individualEmissions.get(pollutant);
				}
				totalEmissions.put(pollutant, sumOfPollutant);
			}
		}
		return totalEmissions;
	}


	public static void setHbefaVehicleDescription( final VehicleType vehicleType, final String hbefaVehicleDescription ) {
		// yyyy maybe this should use the vehicle information tuple (see below)?
		// yyyy replace this by using Attributes.  kai, oct'18

//		vehicleType.getAttributes().putAttribute( HBEFA_VEHICLE_DESCRIPTION, hbefaVehicleDescription ) ;

		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> result = convertVehicleDescription2VehicleInformationTuple( vehicleType );

		EngineInformation engineInformation = vehicleType.getEngineInformation();
		VehicleUtils.setHbefaEmissionsConcept( engineInformation, result.getSecond().getHbefaEmConcept() );
		VehicleUtils.setHbefaSizeClass( engineInformation, result.getSecond().getHbefaSizeClass() );
		VehicleUtils.setHbefaTechnology( engineInformation, result.getSecond().getHbefaTechnology() );
		VehicleUtils.setHbefaVehicleCategory( engineInformation, result.getFirst().name() );
	}

	static Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> convertVehicleDescription2VehicleInformationTuple( VehicleType vehicleType ) {
		// yyyy what is the advantage of having this as a tuple over just using a class with four entries?  kai, oct'18

		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple;

		Gbl.assertNotNull(vehicleType);
		Gbl.assertNotNull(vehicleType.getEngineInformation());
		logger.info(vehicleType.getEngineInformation().getAttributes().toString());
		Gbl.assertNotNull(VehicleUtils.getHbefaVehicleCategory( vehicleType.getEngineInformation() ));
		HbefaVehicleCategory hbefaVehicleCategory = HbefaVehicleCategory.valueOf( VehicleUtils.getHbefaVehicleCategory( vehicleType.getEngineInformation() ) ) ;

		HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();

		final String hbefaTechnology = VehicleUtils.getHbefaTechnology( vehicleType.getEngineInformation() );
		if ( hbefaTechnology!=null ){
			hbefaVehicleAttributes.setHbefaTechnology( hbefaTechnology );
			hbefaVehicleAttributes.setHbefaSizeClass( VehicleUtils.getHbefaSizeClass( vehicleType.getEngineInformation() ) );
			hbefaVehicleAttributes.setHbefaEmConcept( VehicleUtils.getHbefaEmissionsConcept( vehicleType.getEngineInformation() ) );
		}
		// yyyy we are as of now not catching the case where the information is not there. kai/kai, sep'19

		vehicleInformationTuple = new Tuple<>(hbefaVehicleCategory, hbefaVehicleAttributes);
		return vehicleInformationTuple;
	}


	static Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>>
	createHBEFASpeedsTable(Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable) {

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

	/*package-private*/ static String getHbefaVehicleDescription(VehicleType vehicleType, EmissionsConfigGroup emissionsConfigGroup){
		if( vehicleType == null ){
			throw new RuntimeException( "vehicleType is null; not possible for emissions contrib." );
		}

		EngineInformation engineInformation;
		// get information from where it used to be in previous versions and move to where it should be now:
		logger.debug("found following hbefaDescriptionSource in emissionsConfigGroup ... " + emissionsConfigGroup.getHbefaVehicleDescriptionSource());
		switch( emissionsConfigGroup.getHbefaVehicleDescriptionSource() ) {
			case usingVehicleTypeId:
				// (v1, hbefa vehicle description is in vehicle type id.  Copy to where it is expected now)

				//VehicleTypeId can contain ; to provide more specific information, e.g. for HbefaTechnology, HbefaSizeClass, HbefaEmissionsConcept
				if (vehicleType.getId().toString().contains(";")) {
					String[] vehicleInformationArray = vehicleType.getId().toString().split(";");

					engineInformation = vehicleType.getEngineInformation();
					VehicleUtils.setHbefaVehicleCategory(engineInformation, vehicleInformationArray[0]);

					if (vehicleInformationArray.length == 4) {
						VehicleUtils.setHbefaTechnology(engineInformation, vehicleInformationArray[1]);
						VehicleUtils.setHbefaSizeClass(engineInformation, vehicleInformationArray[2]);
						VehicleUtils.setHbefaEmissionsConcept(engineInformation, vehicleInformationArray[3]);
					}
				} else {
					//VehicleTypeId contains only the hbefaVehicleCategory
					VehicleUtils.setHbefaVehicleCategory(vehicleType.getEngineInformation(), vehicleType.getId().toString());
				}

				break;
			case fromVehicleTypeDescription:

				if ( VehicleUtils.getHbefaTechnology( vehicleType.getEngineInformation() ) != null ) {
					// information has already been moved to correct location
					break ;
					// yy Note: If the information is already at the new location (as engine attributes), then the following code will silently take
					// it from there, and not complain that the config setting says it should be in the "description".  kai/kai, sep'19
				}

				// v2, hbefa vehicle description is in vehicle type description.  Move to where it is expected now:

				// copy to new location:
				if( vehicleType.getDescription() == null ){
					throw new RuntimeException( "vehicleType.getDescription() is null; not possible for selected config setting" );
				}
				int startIndex = vehicleType.getDescription().indexOf( EmissionSpecificationMarker.BEGIN_EMISSIONS.toString() ) + EmissionSpecificationMarker.BEGIN_EMISSIONS.toString().length();
				int endIndex = vehicleType.getDescription().lastIndexOf( EmissionSpecificationMarker.END_EMISSIONS.toString() );
				final String substring = vehicleType.getDescription().substring( startIndex, endIndex );

				String[] vehicleInformationArray = substring.split( ";" ) ;

				engineInformation = vehicleType.getEngineInformation();
				VehicleUtils.setHbefaVehicleCategory( engineInformation, vehicleInformationArray[0] );

				if ( vehicleInformationArray.length==4 ){
					VehicleUtils.setHbefaTechnology( engineInformation, vehicleInformationArray[1] );
					VehicleUtils.setHbefaSizeClass( engineInformation, vehicleInformationArray[2] );
					VehicleUtils.setHbefaEmissionsConcept( engineInformation, vehicleInformationArray[3] );
				}

				// delete at old location:
				String oldString = EmissionSpecificationMarker.BEGIN_EMISSIONS.toString() + substring + EmissionSpecificationMarker.END_EMISSIONS.toString();
				String result2 = vehicleType.getDescription().replace( oldString, "" );
				vehicleType.setDescription( result2 ) ;

				break;
			case asEngineInformationAttributes:
				// v3, info is already where it should be
				break;
			default:
				throw new IllegalStateException( "Unexpected value: " + emissionsConfigGroup.getHbefaVehicleDescriptionSource() );
		}

		// return information from where it should be:
		return getHbefaVehicleDescription( vehicleType ) ;
	}

	public static String getHbefaVehicleDescription(VehicleType vehicleType) {
		// not yet clear if this can be public (without access to config). kai/kai, sep'19
		EngineInformation engineInfo = vehicleType.getEngineInformation();;
		StringBuffer strb = new StringBuffer();
		strb.append( VehicleUtils.getHbefaVehicleCategory( engineInfo ) ) ;
		strb.append( ";" ) ;
		strb.append( VehicleUtils.getHbefaTechnology( engineInfo ) ) ;
		strb.append( ";" ) ;
		strb.append( VehicleUtils.getHbefaSizeClass( engineInfo ) ) ;
		strb.append( ";" ) ;
		strb.append( VehicleUtils.getHbefaEmissionsConcept( engineInfo ) );
		return strb.toString() ;
	}
}
