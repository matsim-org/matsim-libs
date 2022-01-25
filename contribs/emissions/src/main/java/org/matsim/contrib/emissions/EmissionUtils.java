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
public abstract class EmissionUtils {
	private static final Logger logger = Logger.getLogger(EmissionUtils.class);

	private enum EmissionSpecificationMarker {BEGIN_EMISSIONS, END_EMISSIONS}

	public static final String HBEFA_ROAD_TYPE = "hbefa_road_type";

	public static void setHbefaRoadType(Link link, String type) {
		link.getAttributes().putAttribute(HBEFA_ROAD_TYPE, type);
	}

	public static String getHbefaRoadType(Link link) {
		return (String) link.getAttributes().getAttribute(HBEFA_ROAD_TYPE);
	}

	public static Map<Pollutant, Double> sumUpEmissions(Map<Pollutant, Double> warmEmissions, Map<Pollutant, Double> coldEmissions) {

		return Stream.concat(warmEmissions.entrySet().stream(), coldEmissions.entrySet().stream())
				.collect(Collectors.toMap(
						Entry::getKey, // The key
						Entry::getValue, // The value
						Double::sum
						)
				);
	}

	public static <T> Map<Id<T>, Map<Pollutant, Double>> sumUpEmissionsPerId(
			Map<Id<T>, Map<Pollutant, Double>> warmEmissions,
			Map<Id<T>, Map<Pollutant, Double>> coldEmissions) {

		if (warmEmissions == null)
			return coldEmissions;
		if (coldEmissions == null)
			return warmEmissions;

		return warmEmissions.entrySet().stream().map(entry -> {
			Id<T> id = entry.getKey();
			Map<Pollutant, Double> warmEm = entry.getValue();
			Map<Pollutant, Double> coldEm = coldEmissions.getOrDefault(id, new HashMap<>());

			Map<Pollutant, Double> totalPollutantCounts = sumUpEmissions(warmEm, coldEm);
			return new Tuple<>(id, totalPollutantCounts);
		}).collect(Collectors.toMap(Tuple::getFirst, Tuple::getSecond));
	}

	public static Map<Id<Person>, SortedMap<Pollutant, Double>> setNonCalculatedEmissionsForPopulation(Population population, Map<Id<Person>, SortedMap<Pollutant, Double>> totalEmissions, Set<Pollutant> pollutants) {
		Map<Id<Person>, SortedMap<Pollutant, Double>> personId2Emissions = new HashMap<>();
		for (Person person : population.getPersons().values()) {
			Id<Person> personId = person.getId();
			SortedMap<Pollutant, Double> emissionType2Value;
			if (totalEmissions.get(personId) == null) { // person not in map (e.g. pt user)
				emissionType2Value = new TreeMap<>();
				for (Pollutant pollutant : pollutants) {
					emissionType2Value.put(pollutant, 0.0);
				}
			} else { // person in map, but some emissions are not set; setting these to 0.0 
				emissionType2Value = totalEmissions.get(personId);
				for (Pollutant pollutant : emissionType2Value.keySet()) {
					// else do nothing
					emissionType2Value.putIfAbsent(pollutant, 0.0);
				}
			}
			personId2Emissions.put(personId, emissionType2Value);
		}
		return personId2Emissions;
	}

	private static Set<String> getAllPollutants(Map<Id<Person>, SortedMap<String, Double>> totalEmissions) {
		return totalEmissions.values().stream().flatMap(m -> m.keySet().stream()).collect(Collectors.toSet());
	}

	public static Map<Id<Link>, SortedMap<Pollutant, Double>> setNonCalculatedEmissionsForNetwork(Network network, Map<Id<Link>, SortedMap<Pollutant, Double>> totalEmissions, Set<Pollutant> pollutants) {
		Map<Id<Link>, SortedMap<Pollutant, Double>> linkId2Emissions = new HashMap<>();

		for (Link link : network.getLinks().values()) {
			Id<Link> linkId = link.getId();
			SortedMap<Pollutant, Double> emissionType2Value;

			if (totalEmissions.get(linkId) == null) {
				emissionType2Value = new TreeMap<>();
				for (Pollutant pollutant : pollutants) {
					emissionType2Value.put(pollutant, 0.0);
				}
			} else {
				emissionType2Value = totalEmissions.get(linkId);
				for (Pollutant pollutant : pollutants) {
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

	public static <T> SortedMap<Pollutant, Double> getTotalEmissions(Map<Id<T>, SortedMap<Pollutant, Double>> person2TotalEmissions) {
		SortedMap<Pollutant, Double> totalEmissions = new TreeMap<>();

		for (Id<T> personId : person2TotalEmissions.keySet()) {
			SortedMap<Pollutant, Double> individualEmissions = person2TotalEmissions.get(personId);
			double sumOfPollutant;
			for (Pollutant pollutant : individualEmissions.keySet()) {
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
		Gbl.assertNotNull(VehicleUtils.getHbefaVehicleCategory(vehicleType.getEngineInformation()));

		HbefaVehicleCategory hbefaVehicleCategory = mapString2HbefaVehicleCategory( VehicleUtils.getHbefaVehicleCategory( vehicleType.getEngineInformation() ) ) ;

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
			HbefaTrafficSituation hbefaTrafficSituation = warmEmissionFactorKey.getTrafficSituation();
			double speed = emissionFactor.getSpeed();

			table.putIfAbsent(roadVehicleCategoryKey, new EnumMap<>(HbefaTrafficSituation.class));
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
		logger.debug("emissionsConfigGroup.getHbefaVehicleDescriptionSource=" + emissionsConfigGroup.getHbefaVehicleDescriptionSource());
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
				// (v1 but hbefa vehicle description is in vehicle type description (Amit's version))

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
				String oldString = EmissionSpecificationMarker.BEGIN_EMISSIONS + substring + EmissionSpecificationMarker.END_EMISSIONS;
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

	private static String getHbefaVehicleDescription( VehicleType vehicleType ) {
		// not yet clear if this can be public (without access to config). kai/kai, sep'19
		EngineInformation engineInfo = vehicleType.getEngineInformation();
		return VehicleUtils.getHbefaVehicleCategory(engineInfo) +
				";" +
				VehicleUtils.getHbefaTechnology(engineInfo) +
				";" +
				VehicleUtils.getHbefaSizeClass(engineInfo) +
				";" +
				VehicleUtils.getHbefaEmissionsConcept(engineInfo);
	}

	public static HbefaVehicleCategory mapString2HbefaVehicleCategory(String string) {
		HbefaVehicleCategory hbefaVehicleCategory;
		if(string.contains("pass. car")) hbefaVehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
		else if(string.contains("HGV")) hbefaVehicleCategory = HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
		else if(string.contains("LCV")) hbefaVehicleCategory = HbefaVehicleCategory.LIGHT_COMMERCIAL_VEHICLE;
		else if(string.contains("motorcycle")) hbefaVehicleCategory = HbefaVehicleCategory.MOTORCYCLE;
        else if(string.contains("coach")) hbefaVehicleCategory = HbefaVehicleCategory.COACH;
        else if(string.contains("urban bus")) hbefaVehicleCategory = HbefaVehicleCategory.URBAN_BUS;
		else{
			try{
				hbefaVehicleCategory = HbefaVehicleCategory.valueOf(string);
			} catch (IllegalArgumentException e) {
				logger.warn("Could not map String " + string + " to any HbefaVehicleCategory; please check syntax in hbefa input file.");
				throw new RuntimeException();
			}
		}
		return hbefaVehicleCategory;
	}

	public static String mapHbefaVehicleCategory2String(HbefaVehicleCategory category) {

		switch (category) {
			case COACH:
				return "coach";
			case HEAVY_GOODS_VEHICLE:
					return "HGV";
			case LIGHT_COMMERCIAL_VEHICLE:
				return "LCV";
			case MOTORCYCLE:
				return "motorcycle";
			case PASSENGER_CAR:
				return "pass. car";
			case URBAN_BUS:
				return "urban bus";
			default:
				throw new RuntimeException("Could not transform category to string: " + category);
		}
	}

        static Pollutant getPollutant( String pollutantString ){
		// for the time being, we just manually add alternative spellings here, and map them all to the established enums.  One option to make this
		// configurable would be to add corresponding maps into the emissions config, in the sense of
		//    setCo2TotalKeys( Set<String> keys )
		// as we have it, e.g., with network modes.  kai, feb'20

		Pollutant pollutant;
		switch( pollutantString ){
			case "CO2(total)":
				pollutant = Pollutant.CO2_TOTAL;
				break;
			case "CO2(rep)":
				pollutant = Pollutant.CO2_rep;
				break;
			case "PM2.5 (non-exhaust)":
				pollutant = Pollutant.PM2_5_non_exhaust;
				break;
			case "PM2.5":
				pollutant = Pollutant.PM2_5;
				break;
			case "PM (non-exhaust)":
				pollutant = Pollutant.PM_non_exhaust;
				break;
			case "BC (exhaust)":
				pollutant = Pollutant.BC_exhaust;
				break;
			case "BC (non-exhaust)":
				pollutant = Pollutant.BC_non_exhaust;
				break;
			default:
				pollutant = Pollutant.valueOf( pollutantString );
				// the Pollutant.valueOf(...) should fail if the incoming key is not consistent with what is available in the enum.  Two possibilities:
				// (1) it is a new pollutant.  In that case, just add to the enum.
				// (2) It is a different spelling of an already existing pollutant.  In that case, see above.
				// kai, jan'20
		}
		return pollutant;
	}

	/**
	 *  try to re-write the key from hbefa3.x to hbefa4.x:
	 */
	/*package-private*/
	static HbefaVehicleAttributes tryRewriteHbefa3toHbefa4(Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple) {
		// try to re-write the key from hbefa3.x to hbefa4.x:
		HbefaVehicleAttributes attribs2 = new HbefaVehicleAttributes();

		// technology is copied:
		attribs2.setHbefaTechnology(vehicleInformationTuple.getSecond().getHbefaTechnology());

		// size class is "not specified":
		attribs2.setHbefaSizeClass("not specified");

		// em concept is re-written with different dashes:
		switch (vehicleInformationTuple.getSecond().getHbefaEmConcept()) {
			case "PC-P-Euro-1":
				attribs2.setHbefaEmConcept("PC P Euro-1");
				break;
			case "PC-P-Euro-2":
				attribs2.setHbefaEmConcept("PC P Euro-2");
				break;
			case "PC-P-Euro-3":
				attribs2.setHbefaEmConcept("PC P Euro-3");
				break;
			case "PC-P-Euro-4":
				attribs2.setHbefaEmConcept("PC P Euro-4");
				break;
			case "PC-P-Euro-5":
				attribs2.setHbefaEmConcept("PC P Euro-5");
				break;
			case "PC-P-Euro-6":
				attribs2.setHbefaEmConcept("PC P Euro-6");
				break;
			case "PC-D-Euro-1":
				attribs2.setHbefaEmConcept("PC D Euro-1");
				break;
			case "PC-D-Euro-2":
				attribs2.setHbefaEmConcept("PC D Euro-2");
				break;
			case "PC-D-Euro-3":
				attribs2.setHbefaEmConcept("PC D Euro-3");
				break;
			case "PC-D-Euro-4":
				attribs2.setHbefaEmConcept("PC D Euro-4");
				break;
			case "PC-D-Euro-5":
				attribs2.setHbefaEmConcept("PC D Euro-5");
				break;
			case "PC-D-Euro-6":
				attribs2.setHbefaEmConcept("PC D Euro-6");
				break;
		}
		return attribs2;
	}

}
