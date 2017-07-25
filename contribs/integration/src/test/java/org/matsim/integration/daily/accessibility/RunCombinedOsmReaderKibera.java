/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

package org.matsim.integration.daily.accessibility;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.osm.CombinedOsmReader;
import org.matsim.contrib.accessibility.utils.AccessibilityFacilityUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author dziemke
 */
public class RunCombinedOsmReaderKibera {
	final private static Logger LOG = Logger.getLogger(RunCombinedOsmReaderKibera.class);

	/**
	 * Implementing the {@link CombinedOsmReader} class. 
	 * @param args The main method requires three arguments:
	 * <ol>
	 * 	<li> the OpenStreetMap file, *.osm;
	 * 	<li> the output MATSim {@link Facility} file;
	 * 	<li> the output {@link ObjectAttributes} file containing the facility 
	 * 		 attributes.
	 * </ol>
	 * 
	 * An optional argument can be provided if you want the WGS84 coordinates
	 * converted into another (projected) coordinate reference system (CRS). 
	 * It is recommended that you <i>do</i> provide a projected CRS as MATSim
	 * works in metres.
	 */
	public static void main(String[] args) {
		String osmFile = "../../../shared-svn/projects/maxess/data/nairobi/osm/2017-04-25_nairobi_central_and_kibera";
		String outputBase = "../../../shared-svn/projects/maxess/data/nairobi/facilities/2017-04-25_nairobi_central_and_kibera/";
		String facilityFile = outputBase + "2017-04-25_facilities.xml";
		String attributeFile = outputBase + "2017-04-25_facilitiy_attributes.xml";
		
//		String outputCRS = "EPSG:31468"; // = DHDN GK4, for Berlin
		String outputCRS = "EPSG:21037"; // = Arc 1960 / UTM zone 37S, for Nairobi, Kenya
		
		// building types are either taken from the building itself and, if building does not have a type, taken from
		// the type of land use of the area which the build belongs to.
		double buildingTypeFromVicinityRange = 0.;
		
		createFacilitesAndWriteToFile(osmFile, facilityFile, attributeFile, outputCRS, buildingTypeFromVicinityRange);
	}
	
		
	public static void createFacilitesAndWriteToFile(String osmFile, String facilityFile, String attributeFile,
			String outputCRS, double buildingTypeFromVicinityRange) {
		LOG.info("Parsing facility information from OpenStreetMap input file.");

		CombinedOsmReader combinedOsmReader = new CombinedOsmReader(
				outputCRS,
				AccessibilityFacilityUtils.buildOsmLandUseToMatsimTypeMap(),
				AccessibilityFacilityUtils.buildOsmBuildingToMatsimTypeMap(),
				buildOsmAmenityToMatsimTypeMap(), // local
				AccessibilityFacilityUtils.buildOsmLeisureToMatsimTypeMap(),
				AccessibilityFacilityUtils.buildOsmTourismToMatsimTypeMap(),
				AccessibilityFacilityUtils.buildUnmannedEntitiesList(),
				buildingTypeFromVicinityRange);
		try {
			combinedOsmReader.parseFile(osmFile);
			combinedOsmReader.writeFacilities(facilityFile);
			combinedOsmReader.writeFacilityAttributes(attributeFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		LOG.info("Output will be wirtten to " + facilityFile);
	}
	
	
	public static ActivityFacilities createFacilites(InputStream osmInputStream, String outputCRS, double buildingTypeFromVicinityRange) {
		LOG.info("Parsing facility information from OpenStreetMap input stream.");

		CombinedOsmReader combinedOsmReader = new CombinedOsmReader(
				outputCRS,
				AccessibilityFacilityUtils.buildOsmLandUseToMatsimTypeMap(),
				AccessibilityFacilityUtils.buildOsmBuildingToMatsimTypeMap(),
				buildOsmAmenityToMatsimTypeMap(), //local
				AccessibilityFacilityUtils.buildOsmLeisureToMatsimTypeMap(),
				AccessibilityFacilityUtils.buildOsmTourismToMatsimTypeMap(),
				AccessibilityFacilityUtils.buildUnmannedEntitiesList(),
				buildingTypeFromVicinityRange);
			combinedOsmReader.parseFile(osmInputStream);
			ActivityFacilities facilities = combinedOsmReader.getActivityFacilities();

			return facilities;
	}

	
	private static Map<String, String> buildOsmAmenityToMatsimTypeMap(){
		Map<String, String> map = new TreeMap<String, String>();

		// "subsistence" section in osm wiki
		map.put("bar", FacilityTypes.LEISURE); // used to be "l"

		map.put("bbq", FacilityTypes.LEISURE);
		map.put("biergarten", FacilityTypes.LEISURE);

		map.put("cafe", FacilityTypes.LEISURE); // used to be "l"
		
		// -------------------------------------------------
		map.put("drinking_water", FacilityTypes.DRINKING_WATER); // activated for Kibera
		// -------------------------------------------------

		map.put("fast_food", FacilityTypes.LEISURE); // used to be "l"
		map.put("food_court", FacilityTypes.LEISURE); // used to be "l"
		map.put("ice_cream", FacilityTypes.LEISURE); // used to be "l"
		map.put("pub", FacilityTypes.LEISURE); // used to be "l"
		map.put("restaurant", FacilityTypes.LEISURE); // used to be "l"

		// "education" section in osm wiki
		map.put("college", FacilityTypes.EDUCATION); // used to be "e"
		map.put("kindergarten", FacilityTypes.EDUCATION); // used to be "e"

		map.put("library", FacilityTypes.OTHER); // used to be "t"
		
		map.put("public_bookcase", FacilityTypes.IGNORE);

		map.put("school", FacilityTypes.EDUCATION); // used to be "e"
		map.put("university", FacilityTypes.EDUCATION); // used to be "e"

		// "transportation" section in osm wiki
		map.put("bicycle_parking", FacilityTypes.IGNORE);
		map.put("bicycle_repair_station", FacilityTypes.IGNORE);

		map.put("bicycle_rental", FacilityTypes.OTHER);

		map.put("boat_sharing", FacilityTypes.IGNORE);
		map.put("bus_station", FacilityTypes.IGNORE);

		map.put("car_rental", FacilityTypes.OTHER);

		map.put("car_sharing", FacilityTypes.IGNORE);

		map.put("car_wash", FacilityTypes.OTHER); // used to be "t"

		map.put("charging_station", FacilityTypes.IGNORE);
		map.put("ferry_terminal", FacilityTypes.IGNORE);

		map.put("fuel", FacilityTypes.OTHER); // used to be "t"

		map.put("grit_bin", FacilityTypes.IGNORE);
		map.put("motorcycle_parking", FacilityTypes.IGNORE);
		map.put("parking", FacilityTypes.IGNORE);
		map.put("parking_entrance", FacilityTypes.IGNORE);
		map.put("taxi", FacilityTypes.IGNORE);

		// "financial" section in osm wiki
		map.put("atm", FacilityTypes.OTHER); // used to be "t"
		map.put("bank", FacilityTypes.OTHER); // used to be "t"
		map.put("bureau_de_change", FacilityTypes.OTHER); // used to be "t"

		// "healthcare" section in osm wiki
		map.put("baby_hatch", FacilityTypes.IGNORE);

		// -------------------------------------------------
		map.put("clinic", FacilityTypes.CLINIC); // used to be "m" // activated for Kibera
		// -------------------------------------------------
		map.put("dentist", FacilityTypes.MEDICAL); // used to be "m"
		map.put("doctors", FacilityTypes.MEDICAL); // used to be "m"
		// -------------------------------------------------
		map.put("hospital", FacilityTypes.HOSPITAL); // used to be "m" // activated for Kibera
		// -------------------------------------------------
		map.put("nursing_home", FacilityTypes.MEDICAL); // used to be "m"
		// -------------------------------------------------
		map.put("pharmacy", FacilityTypes.PHARMACY); // used to be "m" // activated for Kibera
		// -------------------------------------------------

		map.put("social_facility", FacilityTypes.IGNORE);
		map.put("veterinary", FacilityTypes.IGNORE);
		map.put("blood_donation", FacilityTypes.IGNORE);

		// "entertainment, arts & culture" section in osm wiki
		map.put("arts_centre", FacilityTypes.LEISURE); // used to be "l"
		map.put("brothel", FacilityTypes.LEISURE); // used to be "l"
		map.put("casino", FacilityTypes.LEISURE);
		map.put("cinema", FacilityTypes.LEISURE); // used to be "l"

		map.put("community_centre", FacilityTypes.IGNORE);
		map.put("fountain", FacilityTypes.IGNORE);

		map.put("gambling", FacilityTypes.LEISURE);
		map.put("nightclub", FacilityTypes.LEISURE); // used to be "l"
		map.put("planetarium", FacilityTypes.LEISURE);

		map.put("social_centre", FacilityTypes.OTHER); // used to be "t"

		map.put("stripclub", FacilityTypes.LEISURE); // used to be "l"
		map.put("studio", FacilityTypes.LEISURE);
		map.put("swingerclub", FacilityTypes.LEISURE);

		map.put("theatre", FacilityTypes.LEISURE); // used to be "l"

		// FaciiltyTypes.OTHER section in osm wiki
		map.put("animal_boarding", FacilityTypes.IGNORE);
		map.put("animal_shelter", FacilityTypes.IGNORE);
		map.put("bench", FacilityTypes.IGNORE);
		map.put("clock", FacilityTypes.IGNORE);
		map.put("courthouse", FacilityTypes.IGNORE);

		map.put("coworking_space", FacilityTypes.WORK);

		map.put("crematorium", FacilityTypes.IGNORE);
		map.put("crypt", FacilityTypes.IGNORE);
		map.put("dojo", FacilityTypes.IGNORE);

		map.put("embassy", FacilityTypes.OTHER);

		map.put("fire_station", FacilityTypes.WORK);

		map.put("game_feeding", FacilityTypes.LEISURE);

		map.put("grave_yard", FacilityTypes.IGNORE);

		map.put("gym", FacilityTypes.LEISURE);

		map.put("hunting_stand", FacilityTypes.IGNORE);
		map.put("kneipp_water_cure", FacilityTypes.IGNORE);

		map.put("marketplace", FacilityTypes.SHOPPING); // used to be "t" = other

		map.put("photo_booth", FacilityTypes.LEISURE);

		map.put("place_of_worship", FacilityTypes.OTHER); // used to be "t"

		map.put(FacilityTypes.POLICE, FacilityTypes.POLICE); // used to be "p"

		map.put("post_box", FacilityTypes.IGNORE);

		map.put("post_office", FacilityTypes.OTHER); // used to be "t"

		map.put("prison", FacilityTypes.WORK);
		
		map.put("ranger_station", FacilityTypes.IGNORE);
		map.put("register_office", FacilityTypes.IGNORE);
		map.put("recycling", FacilityTypes.IGNORE);
		map.put("rescue_station", FacilityTypes.IGNORE);

		map.put("sauna", FacilityTypes.LEISURE);

		map.put("shelter", FacilityTypes.IGNORE);
		map.put("shower", FacilityTypes.IGNORE);
		map.put("telephone", FacilityTypes.IGNORE);
		// -------------------------------------------------
		map.put("toilets", FacilityTypes.TOILETS);
		// -------------------------------------------------

		map.put("townhall", FacilityTypes.OTHER); // used to be "t"

		map.put("vending_machine", FacilityTypes.IGNORE);
		map.put("waste_basket", FacilityTypes.IGNORE);
		map.put("waste_disposal", FacilityTypes.IGNORE);
		map.put("watering_place", FacilityTypes.IGNORE);
		map.put("water_point", FacilityTypes.IGNORE);

		return map;
	}
}