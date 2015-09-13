/* *********************************************************************** *
 * project: org.matsim.*
 * RunAmenityReaderForNmbm.java
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

package playground.dziemke.accessibility;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.osm.CombinedOsmReader;
import org.matsim.contrib.accessibility.osm.LandUseBuildingReader;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.dziemke.utils.LogToOutputSaver;

/**
 * @author dziemke
 */
public class RunCombinedOsmReaderForBe {
	final private static Logger LOG = Logger.getLogger(RunCombinedOsmReaderForBe.class);
	//TODO write logger output somewhere

	/**
	 * Implementing the {@link LandUseBuildingReader} class. 
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
		// Input and output
		String osmFile = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/schlesische_str/2015-06-24_schlesische_str.osm";
//		String osmFile = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/berlin/2015-05-26_berlin.osm";
		
		String outputBase = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/schlesische_str/07/";
//		String outputBase = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/berlin/09/";
//		String outputBase = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/berlin/combined/01/";
		
//		String facilityFile = outputBase + "facilities_buildings.xml";
		String facilityFile = outputBase + "facilities.xml";
//		String attributeFile = outputBase + "facilitiy_attributes_buildings.xml";
		String attributeFile = outputBase + "facilitiy_attributes.xml";
		
		// Logging
		LOG.info("Parsing land use from OpenStreetMap.");
		LogToOutputSaver.setOutputDirectory(outputBase);
		
		// Parameters
		String outputCRS = "EPSG:31468"; // = DHDN GK4
		
		// building types are either taken from the building itself and, if building does not have a type, taken from
		// the type of land use of the area which the build belongs to.
		double buildingTypeFromVicinityRange = 0.;
		
		
		// String[] tagsToIgnoreBuildings = {"amenity", "historic"};
//		highway
		
//		String osmFile = args[0];
//		String facilityFile = args[1];
//		String attributeFile = args[2];
//		String outputCRS = "WGS84";
//		if(args.length > 3){
//			outputCRS = args[3];
//		}
		
//		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", outputCRS);
		
		// LandUseBuildingReader landUseBuildingReader = new LandUseBuildingReader(
		CombinedOsmReader combinedOsmReader = new CombinedOsmReader(
				//ct,
				outputCRS,
				buildOsmLandUseToMatsimTypeMap(), buildOsmBuildingToMatsimTypeMap(),
				//
				buildOsmAmenityToMatsimTypeMap(), buildOsmLeisureToMatsimTypeMap(),
				buildOsmTourismToMatsimTypeMap(),
				//
				buildingTypeFromVicinityRange
				//, tagsToIgnoreBuildings
				);
		try {
//			combinedOsmReader.parseLandUseAndBuildings(osmFile);
			combinedOsmReader.parseFile(osmFile);
			combinedOsmReader.writeFacilities(facilityFile);
			combinedOsmReader.writeFacilityAttributes(attributeFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}		
	}
	
	
	private static Map<String, String> buildOsmLandUseToMatsimTypeMap(){
		Map<String, String> map = new TreeMap<String, String>();
		
		// see http://wiki.openstreetmap.org/wiki/DE:Key:landuse
		
		// land use types where nobody lives and works or where only
		// somebody lives occasionally or where only very few people
		// per building area work are ignored
		
		map.put("allotments", "ignore");
		map.put("basin", "ignore");
		map.put("brownfield", "ignore");
		map.put("cemetery", "ignore");
		
		map.put("commercial", "work");
		
		map.put("conservation", "ignore");
		map.put("construction", "ignore");
		map.put("farmland", "ignore");
		map.put("farmyard", "ignore");
		map.put("forest", "ignore");
		map.put("garages", "ignore");
		map.put("grass", "ignore");
		map.put("greenfield", "ignore");
		map.put("greenhouse_horticulture", "ignore");
		
		map.put("industrial", "work");
		
		map.put("landfill", "ignore");
		map.put("meadow", "ignore");
		map.put("military", "ignore");
		map.put("orchard", "ignore");
		map.put("pasture", "ignore");
		map.put("peat_cutting", "ignore");
		map.put("plant_nursery", "ignore");
		
		map.put("port", "work");
		
		map.put("quarry", "ignore");
		map.put("railway", "ignore");
		map.put("recreation_ground", "ignore");
		map.put("reservoir", "ignore");
		
		map.put("residential", "home");
		map.put("retail", "work");
		
		map.put("salt_pond", "ignore");
		map.put("village_green", "ignore");
		map.put("vineyard", "ignore");

		return map;
	}
	
	
	private static Map<String, String> buildOsmBuildingToMatsimTypeMap(){
		Map<String, String> map = new TreeMap<String, String>();
		
		// see http://wiki.openstreetmap.org/wiki/DE:Key:building
		
		// building types where nobody lives and works or where only
		// somebody lives occasionally or where only very few people
		// per building area work are ignored
		
		map.put("apartments", "home");
		map.put("farm", "home");
		
		map.put("hotel", "work");
		
		map.put("house", "home");
		map.put("detached", "home");
		map.put("residential", "home");
		map.put("dormitory", "home");
		map.put("terrace", "home");
		map.put("houseboat", "home");
		map.put("static_caravan", "home");
		
		map.put("commercial", "work");
		map.put("office", "work");
		map.put("industrial", "work");
		map.put("retail", "work");
		
		map.put("warehouse", "ignore");		
		map.put("chapel", "ignore");
		map.put("church", "ignore");
		map.put("mosque", "ignore");
		map.put("temple", "ignore");
		map.put("synagoge", "ignore");
		map.put("shrine", "ignore");
	
		map.put("civic", "work");
		map.put("hospital", "work");
		map.put("school", "work");
		
		map.put("stadium", "ignore");
		map.put("train_station", "ignore");
		map.put("transportation", "ignore");
		
		map.put("university", "work");
		map.put("public", "work");
		map.put("greenhouse", "work");
		
		map.put("barn", "ignore");
		map.put("bridge", "ignore");
		map.put("bunker", "ignore");
		map.put("cabin", "ignore");
		map.put("construction", "ignore");
		map.put("cowshed", "ignore");
		map.put("farm_auxiliary", "ignore");
		map.put("garage", "ignore");
		map.put("garages", "ignore");
		map.put("greenhouse", "ignore");
		
		map.put("hangar", "work");
		
		map.put("hut", "ignore");
		map.put("roof", "ignore");
		map.put("shed", "ignore");
		map.put("stable", "ignore");
		map.put("sty", "ignore");
		map.put("transformer_tower", "ignore");
		map.put("service", "ignore");
		
		map.put("kiosk", "work");
		
		map.put("ruins", "ignore");

		return map;
	}
	
	
	//
	// copied from "RunAmenityReaderForBe"
	private static Map<String, String> buildOsmAmenityToMatsimTypeMap(){
		Map<String, String> map = new TreeMap<String, String>();

		// "subsistence" section in osm wiki
		map.put("bar", "leisure"); // used to be "l"

		map.put("bbq", "leisure");
		map.put("biergarten", "leisure");

		map.put("cafe", "leisure"); // used to be "l"

		map.put("drinking_water", "ignore");

		map.put("fast_food", "leisure"); // used to be "l"
		map.put("food_court", "leisure"); // used to be "l"
		map.put("ice_cream", "leisure"); // used to be "l"
		map.put("pub", "leisure"); // used to be "l"
		map.put("restaurant", "leisure"); // used to be "l"

		// "education" section in osm wiki
		map.put("college", "education"); // used to be "e"
		map.put("kindergarten", "education"); // used to be "e"

		map.put("library", "other"); // used to be "t"
		map.put("public_bookcase", "ignore");

		map.put("school", "education"); // used to be "e"
		map.put("university", "education"); // used to be "e"

		// "transportation" section in osm wiki
		map.put("bicycle_parking", "ignore");
		map.put("bicycle_repair_station", "ignore");

		map.put("bicycle_rental", "other");

		map.put("boat_sharing", "ignore");
		map.put("bus_station", "ignore");

		map.put("car_rental", "other");

		map.put("car_sharing", "ignore");

		map.put("car_wash", "other"); // used to be "t"

		map.put("charging_station", "ignore");
		map.put("ferry_terminal", "ignore");

		map.put("fuel", "other"); // used to be "t"

		map.put("grit_bin", "ignore");
		map.put("motorcycle_parking", "ignore");
		map.put("parking", "ignore");
		map.put("parking_entrance", "ignore");
		map.put("taxi", "ignore");

		// "financial" section in osm wiki
		map.put("atm", "other"); // used to be "t"
		map.put("bank", "other"); // used to be "t"
		map.put("bureau_de_change", "other"); // used to be "t"

		// "healthcare" section in osm wiki
		map.put("baby_hatch", "ignore");

		map.put("clinic", "medical"); // used to be "m"
		map.put("dentist", "medical"); // used to be "m"
		map.put("doctors", "medical"); // used to be "m"
		map.put("hospital", "medical"); // used to be "m"
		map.put("nursing_home", "medical"); // used to be "m"
		map.put("pharmacy", "medical"); // used to be "m"

		map.put("social_facility", "ignore");
		map.put("veterinary", "ignore");
		map.put("blood_donation", "ignore");

		// "entertainment, arts & culture" section in osm wiki
		map.put("arts_centre", "leisure"); // used to be "l"
		map.put("brothel", "leisure"); // used to be "l"
		map.put("casino", "leisure");
		map.put("cinema", "leisure"); // used to be "l"

		map.put("community_centre", "ignore");
		map.put("fountain", "ignore");

		map.put("gambling", "leisure");
		map.put("nightclub", "leisure"); // used to be "l"
		map.put("planetarium", "leisure");

		map.put("social_centre", "other"); // used to be "t"

		map.put("stripclub", "leisure"); // used to be "l"
		map.put("studio", "leisure");
		map.put("swingerclub", "leisure");

		map.put("theatre", "leisure"); // used to be "l"

		// "other" section in osm wiki
		map.put("animal_boarding", "ignore");
		map.put("animal_shelter", "ignore");
		map.put("bench", "ignore");
		map.put("clock", "ignore");
		map.put("courthouse", "ignore");

		map.put("coworking_space", "work");

		map.put("crematorium", "ignore");
		map.put("crypt", "ignore");
		map.put("dojo", "ignore");

		map.put("embassy", "other");

		map.put("fire_station", "work");

		map.put("game_feeding", "leisure");

		map.put("grave_yard", "ignore");

		map.put("gym", "leisure");

		map.put("hunting_stand", "ignore");
		map.put("kneipp_water_cure", "ignore");

		map.put("marketplace", "shopping"); // used to be "t" = other

		map.put("photo_booth", "leisure");

		map.put("place_of_worship", "other"); // used to be "t"

		map.put("polic", "police"); // used to be "p"

		map.put("post_box", "ignore");

		map.put("post_office", "other"); // used to be "t"

		map.put("prison", "ignore");
		map.put("ranger_station", "ignore");
		map.put("register_office", "ignore");
		map.put("recycling", "ignore");
		map.put("rescue_station", "ignore");

		map.put("sauna", "leisure");

		map.put("shelter", "ignore");
		map.put("shower", "ignore");
		map.put("telephone", "ignore");
		map.put("toilets", "ignore");

		map.put("townhall", "other"); // used to be "t"

		map.put("vending_machine", "shopping");

		map.put("waste_basket", "ignore");
		map.put("waste_disposal", "ignore");
		map.put("watering_place", "ignore");
		map.put("water_point", "ignore");

		return map;
	}
	//


	private static Map<String, String> buildOsmLeisureToMatsimTypeMap(){
		Map<String, String> map = new TreeMap<String, String>();

		map.put("adult_gaming_centre", "leisure");
		map.put("amusement_arcade", "leisure");
		map.put("beach_resort", "leisure");
		
		map.put("bandstand", "ignore");
		map.put("bird_hide", "ignore");
		
		map.put("dance", "leisure");
		map.put("dog_park", "leisure");
		map.put("firepit", "leisure");
		
		map.put("fishing", "ignore");
		
		map.put("garden", "leisure");
		map.put("hackerspace", "leisure");
		map.put("ice_rink", "leisure");
		
		map.put("marina", "ignore");
		
		map.put("miniature_golf", "leisure");
		
		map.put("nature_reserve", "ignore");
		map.put("park", "ignore");
		
		map.put("pitch", "leisure");
		map.put("playground", "leisure");
		
		map.put("slipway", "ignore");
		
		map.put("sports_centre", "leisure");
		map.put("stadium", "leisure");
		map.put("summer_camp", "leisure");
		map.put("swimming_pool", "leisure");
		map.put("swimming_area", "leisure");
		map.put("track", "leisure");
		map.put("water_park", "leisure");
		
		map.put("wildlife_hide", "ignore");

		return map;
	}
	
	
	private static Map<String, String> buildOsmTourismToMatsimTypeMap(){
		Map<String, String> map = new TreeMap<String, String>();
		
		map.put("alpine_hut", "leisure");
		
		map.put("apartment", "ignore");
		map.put("attraction", "ignore");
		map.put("artwork", "ignore");
		
		map.put("camp_site", "leisure");
		
		map.put("chalet", "ignore");
		
		map.put("gallery", "leisure");
		
		map.put("guest_house", "ignore");
		map.put("hostel", "ignore");
		map.put("hotel", "ignore");
		map.put("information", "ignore");
		map.put("motel", "ignore");
		
		map.put("museum", "leisure");
		
		map.put("picnic_site", "leisure");
		
		map.put("theme_park", "leisure");
		
		map.put("viewpoint", "ignore");
		
		map.put("wilderness_hut", "leisure");
		
		map.put("zoo", "leisure");

		return map;
	}
}