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

package playground.ikaddoura.cottbus;

import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.osm.CombinedOsmReader;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.dziemke.utils.LogToOutputSaver;

/**
 * @author dziemke, ikaddoura
 */
public class RunCombinedOsmReaderCottbus {
	final private static Logger log = Logger.getLogger(RunCombinedOsmReaderCottbus.class);

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
				

		String osmFile = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/berlin/berlin-2017-05-10.osm";
		String outputBase = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/berlin/berlin-2017-05-10_facilities/";
		
		String facilityFile = "berlin-2017-05-10_facilities_DHDN_GK4.xml";
		String attributeFile = "berlin-2017-05-10_facilities_amenities_DHDN_GK4.xml";
		
		log.info("Parsing land use from OpenStreetMap.");
		LogToOutputSaver.setOutputDirectory(outputBase);
		
		String outputCRS = TransformationFactory.DHDN_GK4;
		
		// building types are either taken from the building itself and, if building does not have a type, taken from
		// the type of land use of the area which the build belongs to.
		double buildingTypeFromVicinityRange = 0.;
	
		CombinedOsmReader combinedOsmReader = new CombinedOsmReader(outputCRS,
				buildOsmLandUseToMatsimTypeMap(), buildOsmBuildingToMatsimTypeMap(),
				buildOsmAmenityToMatsimTypeMap(), buildOsmLeisureToMatsimTypeMap(),
				buildOsmTourismToMatsimTypeMap(), buildUnmannedEntitiesList(),
				buildingTypeFromVicinityRange);
		try {
			combinedOsmReader.parseFile(osmFile);
			combinedOsmReader.writeFacilities(outputBase + facilityFile);
			combinedOsmReader.writeFacilityAttributes(outputBase + attributeFile);
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
		
		map.put("allotments", FacilityTypes.IGNORE);
		map.put("basin", FacilityTypes.IGNORE);
		map.put("brownfield", FacilityTypes.IGNORE);
		map.put("cemetery", FacilityTypes.IGNORE);
		
		map.put("commercial", FacilityTypes.WORK);
		
		map.put("conservation", FacilityTypes.IGNORE);
		map.put("construction", FacilityTypes.IGNORE);
		map.put("farmland", FacilityTypes.IGNORE);
		map.put("farmyard", FacilityTypes.IGNORE);
		map.put("forest", FacilityTypes.IGNORE);
		map.put("garages", FacilityTypes.IGNORE);
		map.put("grass", FacilityTypes.IGNORE);
		map.put("greenfield", FacilityTypes.IGNORE);
		map.put("greenhouse_horticulture", FacilityTypes.IGNORE);
		
		map.put("industrial", FacilityTypes.WORK);
		
		map.put("landfill", FacilityTypes.IGNORE);
		map.put("meadow", FacilityTypes.IGNORE);
		map.put("military", FacilityTypes.IGNORE);
		map.put("orchard", FacilityTypes.IGNORE);
		map.put("pasture", FacilityTypes.IGNORE);
		map.put("peat_cutting", FacilityTypes.IGNORE);
		map.put("plant_nursery", FacilityTypes.IGNORE);
		
		map.put("port", FacilityTypes.WORK);
		
		map.put("quarry", FacilityTypes.IGNORE);
		map.put("railway", FacilityTypes.IGNORE);
		map.put("recreation_ground", FacilityTypes.IGNORE);
		map.put("reservoir", FacilityTypes.IGNORE);
		
		map.put("residential", FacilityTypes.HOME);
		map.put("retail", FacilityTypes.WORK);
		
		map.put("salt_pond", FacilityTypes.IGNORE);
		map.put("village_green", FacilityTypes.IGNORE);
		map.put("vineyard", FacilityTypes.IGNORE);

		return map;
	}
	
	
	private static Map<String, String> buildOsmBuildingToMatsimTypeMap(){
		Map<String, String> map = new TreeMap<String, String>();
		
		// see http://wiki.openstreetmap.org/wiki/DE:Key:building
		
		// building types where nobody lives and works or where only
		// somebody lives occasionally or where only very few people
		// per building area work are ignored
		
		map.put("apartments", FacilityTypes.HOME);
		map.put("farm", FacilityTypes.HOME);
		
		map.put("hotel", FacilityTypes.WORK);
		
		map.put("house", FacilityTypes.HOME);
		map.put("detached", FacilityTypes.HOME);
		map.put("residential", FacilityTypes.HOME);
		map.put("dormitory", FacilityTypes.HOME);
		map.put("terrace", FacilityTypes.HOME);
		map.put("houseboat", FacilityTypes.HOME);
		map.put("static_caravan", FacilityTypes.HOME);
		
		map.put("commercial", FacilityTypes.WORK);
		map.put("office", FacilityTypes.WORK);
		map.put("industrial", FacilityTypes.WORK);
		
		map.put("retail", FacilityTypes.SHOPPING);
		
		map.put("warehouse", FacilityTypes.IGNORE);		
		map.put("chapel", FacilityTypes.IGNORE);
		
//		map.put("church", FacilityTypes.OTHER);
//		map.put("mosque", FacilityTypes.OTHER);
//		map.put("temple", FacilityTypes.OTHER);
//		map.put("synagoge", FacilityTypes.OTHER);
		map.put("church", FacilityTypes.IGNORE);
		map.put("mosque", FacilityTypes.IGNORE);
		map.put("temple", FacilityTypes.IGNORE);
		map.put("synagoge", FacilityTypes.IGNORE);
		
		map.put("shrine", FacilityTypes.IGNORE);
	
		map.put("civic", FacilityTypes.WORK);
		
//		map.put("hospital", FacilityTypes.MEDICAL);
//		map.put("school", FacilityTypes.EDUCATION);
		map.put("hospital", FacilityTypes.IGNORE);
		map.put("school", FacilityTypes.IGNORE);
		
//		map.put("stadium", FacilityTypes.LEISURE);
		map.put("stadium", FacilityTypes.IGNORE);
		
		map.put("train_station", FacilityTypes.IGNORE);
		map.put("transportation", FacilityTypes.IGNORE);
		
//		map.put("university", FacilityTypes.EDUCATION);
		map.put("university", FacilityTypes.IGNORE);
		
		map.put("public", FacilityTypes.WORK);
		map.put("greenhouse", FacilityTypes.WORK);
		
		map.put("barn", FacilityTypes.IGNORE);
		map.put("bridge", FacilityTypes.IGNORE);
		map.put("bunker", FacilityTypes.IGNORE);
		map.put("cabin", FacilityTypes.IGNORE);
		map.put("construction", FacilityTypes.IGNORE);
		map.put("cowshed", FacilityTypes.IGNORE);
		map.put("farm_auxiliary", FacilityTypes.IGNORE);
		map.put("garage", FacilityTypes.IGNORE);
		map.put("garages", FacilityTypes.IGNORE);
		map.put("greenhouse", FacilityTypes.IGNORE);
		
		map.put("hangar", FacilityTypes.WORK);
		
		map.put("hut", FacilityTypes.IGNORE);
		map.put("roof", FacilityTypes.IGNORE);
		map.put("shed", FacilityTypes.IGNORE);
		map.put("stable", FacilityTypes.IGNORE);
		map.put("sty", FacilityTypes.IGNORE);
		map.put("transformer_tower", FacilityTypes.IGNORE);
		map.put("service", FacilityTypes.IGNORE);
		
		map.put("kiosk", FacilityTypes.WORK);
		
		map.put("ruins", FacilityTypes.IGNORE);

		return map;
	}
	
	
	// copied from "RunAmenityReaderForBe"
	private static Map<String, String> buildOsmAmenityToMatsimTypeMap(){
		Map<String, String> map = new TreeMap<String, String>();

		// "subsistence" section in osm wiki
		map.put("bar", FacilityTypes.LEISURE); // used to be "l"

		map.put("bbq", FacilityTypes.LEISURE);
		map.put("biergarten", FacilityTypes.LEISURE);

		map.put("cafe", FacilityTypes.LEISURE); // used to be "l"

		map.put("drinking_water", FacilityTypes.IGNORE);

		map.put("fast_food", FacilityTypes.LEISURE); // used to be "l"
		map.put("food_court", FacilityTypes.LEISURE); // used to be "l"
		map.put("ice_cream", FacilityTypes.LEISURE); // used to be "l"
		map.put("pub", FacilityTypes.LEISURE); // used to be "l"
		map.put("restaurant", FacilityTypes.LEISURE); // used to be "l"

		// "education" section in osm wiki
		map.put("college", FacilityTypes.EDUCATION); // used to be "e"
		map.put("kindergarten", FacilityTypes.IGNORE); // used to be "e"

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

		map.put("clinic", FacilityTypes.MEDICAL); // used to be "m"
		map.put("dentist", FacilityTypes.IGNORE); // used to be "m"
		map.put("doctors", FacilityTypes.IGNORE); // used to be "m"
		map.put("hospital", FacilityTypes.MEDICAL); // used to be "m"
		map.put("nursing_home", FacilityTypes.IGNORE); // used to be "m"
		map.put("pharmacy", FacilityTypes.IGNORE); // used to be "m"

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

		// "other" section in osm wiki
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

		map.put("fire_station", "fire_station");

		map.put("game_feeding", FacilityTypes.LEISURE);

		map.put("grave_yard", "grave_yard");

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
		map.put("toilets", FacilityTypes.IGNORE);

		map.put("townhall", FacilityTypes.OTHER); // used to be "t"

		map.put("vending_machine", FacilityTypes.IGNORE);
		map.put("waste_basket", FacilityTypes.IGNORE);
		map.put("waste_disposal", FacilityTypes.IGNORE);
		map.put("watering_place", FacilityTypes.IGNORE);
		map.put("water_point", FacilityTypes.IGNORE);

		return map;
	}
	//


	private static Map<String, String> buildOsmLeisureToMatsimTypeMap(){
		Map<String, String> map = new TreeMap<String, String>();

		map.put("adult_gaming_centre", FacilityTypes.LEISURE);
		map.put("amusement_arcade", FacilityTypes.LEISURE);
		map.put("beach_resort", FacilityTypes.LEISURE);
		
		map.put("bandstand", FacilityTypes.IGNORE);
		map.put("bird_hide", FacilityTypes.IGNORE);
		
		map.put("dance", FacilityTypes.LEISURE);
		map.put("dog_park", FacilityTypes.LEISURE);
		map.put("firepit", FacilityTypes.LEISURE);
		
		map.put("fishing", FacilityTypes.IGNORE);
		
		map.put("garden", FacilityTypes.LEISURE);
		map.put("golf_course", FacilityTypes.LEISURE);
		map.put("hackerspace", FacilityTypes.LEISURE);
		map.put("ice_rink", FacilityTypes.LEISURE);
		
		map.put("marina", FacilityTypes.IGNORE);
		
		map.put("miniature_golf", FacilityTypes.LEISURE);
		
		map.put("nature_reserve", FacilityTypes.IGNORE);
		map.put("park", FacilityTypes.IGNORE);
		
		map.put("pitch", FacilityTypes.LEISURE);
		map.put("playground", FacilityTypes.LEISURE);
		
		map.put("slipway", FacilityTypes.IGNORE);
		
		map.put("sports_centre", FacilityTypes.LEISURE);
		map.put("stadium", FacilityTypes.LEISURE);
		map.put("summer_camp", FacilityTypes.LEISURE);
		map.put("swimming_pool", FacilityTypes.LEISURE);
		map.put("swimming_area", FacilityTypes.LEISURE);
		map.put("track", FacilityTypes.LEISURE);
		map.put("water_park", FacilityTypes.LEISURE);
		
		map.put("wildlife_hide", FacilityTypes.IGNORE);

		return map;
	}
	
	
	private static Map<String, String> buildOsmTourismToMatsimTypeMap(){
		Map<String, String> map = new TreeMap<String, String>();
		
		map.put("alpine_hut", FacilityTypes.LEISURE);
		
		map.put("apartment", FacilityTypes.IGNORE);
		map.put("attraction", FacilityTypes.IGNORE);
		map.put("artwork", FacilityTypes.IGNORE);
		
		map.put("camp_site", FacilityTypes.LEISURE);
		
		map.put("chalet", FacilityTypes.IGNORE);
		
		map.put("gallery", FacilityTypes.LEISURE);
		
		map.put("guest_house", FacilityTypes.IGNORE);
		map.put("hostel", FacilityTypes.IGNORE);
		map.put("hotel", FacilityTypes.IGNORE);
		map.put("information", FacilityTypes.IGNORE);
		map.put("motel", FacilityTypes.IGNORE);
		
		map.put("museum", FacilityTypes.LEISURE);
		
		map.put("picnic_site", FacilityTypes.LEISURE);
		
		map.put("theme_park", FacilityTypes.LEISURE);
		
		map.put("viewpoint", FacilityTypes.IGNORE);
		
		map.put("wilderness_hut", FacilityTypes.LEISURE);
		
		map.put("zoo", FacilityTypes.LEISURE);

		return map;
	}
	
	
	private static List<String> buildUnmannedEntitiesList(){
		List<String> list = new LinkedList<String>();
		
		list.add("bicycle_rental");
		list.add("car_wash");
		list.add("atm");
		list.add("photo_booth");
		//list.add("vending_machine"); // currently ignored
		list.add("dance");
		list.add("dog_park");
		list.add("firepit");
		list.add("garden");
		list.add("pitch");
		list.add("playground");
		list.add("swimming_area");
		list.add("track");
		list.add("camp_site");
		list.add("picnic_site");
		list.add("wilderness_hut");
		
		return list;
	}
}