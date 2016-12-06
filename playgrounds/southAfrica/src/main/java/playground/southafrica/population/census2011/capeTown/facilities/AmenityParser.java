/* *********************************************************************** *
 * project: org.matsim.*
 * ParseCapeTownAmenities.java
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

/**
 * 
 */
package playground.southafrica.population.census2011.capeTown.facilities;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.osm.CombinedOsmReader;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesWriter;

import playground.southafrica.utilities.Header;

/**
 * Parsing the {@link ActivityFacilities} from OpenStreetMap data for the
 * City of Cape Town.
 * 
 * @author jwjoubert
 */
public class AmenityParser {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AmenityParser.class.toString(), args);
		
		String osmFile = args[0];
		String facilitiesFile = args[1];
		
		CombinedOsmReader cor = new CombinedOsmReader(
//				"EPSG:3857",
				"SA_Lo19",
				getLanduseToMatsimMap(), 
				getBuildingToMatsimMap(), 
				getAmenityToMatsimMap(),
				getLeisureToMatsimMap(),
				getTourismToMatsimMap(),
				getUnmannedList(), 
				20.0);
		cor.setIdPrefix("osm_");
		try {
			cor.parseFile(osmFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot parse OSM file " + osmFile);
		}
		new FacilitiesWriter(cor.getActivityFacilities()).write(facilitiesFile);
		
		Header.printFooter();
	}
	
	/**
	 * Land use types where nobody lives and works or where only somebody 
	 * lives occasionally or where only very few people per building area work 
	 * are ignored.
	 * 
	 * @see <a href="http://wiki.openstreetmap.org/wiki/Key:landuse">OpenStreetMap Wiki on landuse</a>
	 * 
	 * @return
	 */
	private static Map<String, String> getLanduseToMatsimMap(){
		Map<String, String> map = new TreeMap<String, String>();
		
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

	/**
	 * 
	 * @see <a href="http://wiki.openstreetmap.org/wiki/Key:building">OpenStreetMap Wiki on landuse</a>
	 *
	 * @return
	 */
	private static Map<String, String> getBuildingToMatsimMap(){
		Map<String, String> map = new TreeMap<String, String>();

		/* Accommodation */
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
		
		/* Commercial */
		map.put("commercial", FacilityTypes.WORK);
		map.put("office", FacilityTypes.WORK);
		map.put("industrial", FacilityTypes.WORK);
		map.put("retail", FacilityTypes.SHOPPING);
		map.put("warehouse", FacilityTypes.WORK);		

		/* Civic/Amenity */
		map.put("cathedral", FacilityTypes.OTHER);
		map.put("chapel", FacilityTypes.OTHER);
		map.put("church", FacilityTypes.OTHER);
		map.put("mosque", FacilityTypes.OTHER);
		map.put("temple", FacilityTypes.OTHER);
		map.put("synagogue", FacilityTypes.OTHER);
		map.put("shrine", FacilityTypes.IGNORE);
		map.put("civic", FacilityTypes.WORK);
		
		map.put("hospital", FacilityTypes.IGNORE);
		map.put("school", FacilityTypes.IGNORE);
		map.put("stadium", FacilityTypes.IGNORE);
		map.put("train_station", FacilityTypes.IGNORE);
		map.put("transportation", FacilityTypes.IGNORE);
		map.put("university", FacilityTypes.IGNORE);
		map.put("public", FacilityTypes.WORK);

		/* Other buildings */
		map.put("barn", FacilityTypes.IGNORE);
		map.put("bridge", FacilityTypes.IGNORE);
		map.put("bunker", FacilityTypes.IGNORE);
		map.put("cabin", FacilityTypes.IGNORE);
		map.put("construction", FacilityTypes.IGNORE);
		map.put("cowshed", FacilityTypes.IGNORE);
		map.put("digester", FacilityTypes.IGNORE);
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
		
		map.put("yes", FacilityTypes.IGNORE);
		return map;
	}
	
	/**
	 * @see <a href="http://wiki.openstreetmap.org/wiki/Key:amenity">OpenStreetMap Wiki on landuse</a>
	 *
	 * @return
	 */
	private static Map<String, String> getAmenityToMatsimMap(){
		Map<String, String> map = new TreeMap<String, String>();
		/* Sustenance */
		map.put("bar", FacilityTypes.LEISURE);
		map.put("bbq", FacilityTypes.LEISURE);
		map.put("biergarten", FacilityTypes.LEISURE);
		map.put("cafe", FacilityTypes.LEISURE);
		map.put("drinking_water", FacilityTypes.DRINKING_WATER);
		map.put("fast_food", FacilityTypes.LEISURE);
		map.put("food_court", FacilityTypes.LEISURE);
		map.put("ice_cream", FacilityTypes.LEISURE);
		map.put("pub", FacilityTypes.LEISURE); 
		map.put("restaurant", FacilityTypes.LEISURE);
		
		/* Education */
		map.put("college", FacilityTypes.EDUCATION);
		map.put("kindergarten", FacilityTypes.EDUCATION);
		map.put("library", FacilityTypes.OTHER);
		map.put("public_bookcase", FacilityTypes.IGNORE);
		map.put("school", FacilityTypes.EDUCATION);
		map.put("music_school", FacilityTypes.EDUCATION);
		map.put("driving_school", FacilityTypes.OTHER);
		map.put("language_school", FacilityTypes.OTHER);
		map.put("university", FacilityTypes.EDUCATION);

		/* Transportation */
		map.put("bicycle_parking", FacilityTypes.IGNORE);
		map.put("bicycle_repair_station", FacilityTypes.IGNORE);
		map.put("bicycle_rental", FacilityTypes.OTHER);
		map.put("boat_sharing", FacilityTypes.IGNORE);
		map.put("bus_station", FacilityTypes.IGNORE);
		map.put("car_rental", FacilityTypes.OTHER);
		map.put("car_sharing", FacilityTypes.IGNORE);
		map.put("car_wash", FacilityTypes.OTHER);
		map.put("charging_station", FacilityTypes.IGNORE);
		map.put("ferry_terminal", FacilityTypes.IGNORE);
		map.put("fuel", FacilityTypes.OTHER);
		map.put("grit_bin", FacilityTypes.IGNORE);
		map.put("motorcycle_parking", FacilityTypes.IGNORE);
		map.put("parking", FacilityTypes.IGNORE);
		map.put("parking_entrance", FacilityTypes.IGNORE);
		map.put("parking_space", FacilityTypes.IGNORE);
		map.put("taxi", FacilityTypes.IGNORE);

		/* Financial */
		map.put("atm", FacilityTypes.OTHER);
		map.put("bank", FacilityTypes.OTHER);
		map.put("bureau_de_change", FacilityTypes.OTHER);

		/* Healthcare */
		map.put("baby_hatch", FacilityTypes.IGNORE);
		map.put("clinic", FacilityTypes.MEDICAL); 
		map.put("dentist", FacilityTypes.MEDICAL);
		map.put("doctors", FacilityTypes.MEDICAL);
		map.put("hospital", FacilityTypes.HOSPITAL);
		map.put("nursing_home", FacilityTypes.MEDICAL);
		map.put("pharmacy", FacilityTypes.PHARMACY);
		map.put("social_facility", FacilityTypes.OTHER);
		map.put("veterinary", FacilityTypes.IGNORE);
		map.put("blood_donation", FacilityTypes.IGNORE);

		/* Entertainment, Arts & Culture */
		map.put("arts_centre", FacilityTypes.LEISURE);
		map.put("brothel", FacilityTypes.LEISURE);
		map.put("casino", FacilityTypes.LEISURE);
		map.put("cinema", FacilityTypes.LEISURE); 
		map.put("community_centre", FacilityTypes.OTHER);
		map.put("fountain", FacilityTypes.IGNORE);
		map.put("gambling", FacilityTypes.LEISURE);
		map.put("nightclub", FacilityTypes.LEISURE);
		map.put("planetarium", FacilityTypes.LEISURE);
		map.put("social_centre", FacilityTypes.OTHER);
		map.put("stripclub", FacilityTypes.LEISURE);
		map.put("studio", FacilityTypes.LEISURE);
		map.put("swingerclub", FacilityTypes.LEISURE);
		map.put("theatre", FacilityTypes.LEISURE);

		/* Other amenities */
		map.put("animal_boarding", FacilityTypes.IGNORE);
		map.put("animal_shelter", FacilityTypes.IGNORE);
		map.put("bench", FacilityTypes.IGNORE);
		map.put("clock", FacilityTypes.IGNORE);
		map.put("courthouse", FacilityTypes.OTHER);
		map.put("coworking_space", FacilityTypes.WORK);
		map.put("crematorium", FacilityTypes.IGNORE);
		map.put("crypt", FacilityTypes.IGNORE);
		map.put("dive_centre", FacilityTypes.LEISURE);
		map.put("dojo", FacilityTypes.OTHER);
		map.put("embassy", FacilityTypes.OTHER);
		map.put("fire_station", FacilityTypes.WORK);
		map.put("game_feeding", FacilityTypes.LEISURE);
		map.put("grave_yard", FacilityTypes.OTHER);
		map.put("gym", FacilityTypes.LEISURE);
		map.put("hunting_stand", FacilityTypes.IGNORE);
		map.put("internet_cafe", FacilityTypes.OTHER);
		map.put("kneipp_water_cure", FacilityTypes.IGNORE);
		map.put("marketplace", FacilityTypes.SHOPPING);
		map.put("photo_booth", FacilityTypes.LEISURE);
		map.put("place_of_worship", FacilityTypes.OTHER);
		map.put("police", FacilityTypes.POLICE);
		map.put("post_box", FacilityTypes.OTHER);
		map.put("post_office", FacilityTypes.OTHER);
		map.put("prison", FacilityTypes.WORK);
		map.put("ranger_station", FacilityTypes.IGNORE);
		map.put("register_office", FacilityTypes.IGNORE);
		map.put("recycling", FacilityTypes.IGNORE);
		map.put("rescue_station", FacilityTypes.IGNORE);
		map.put("sauna", FacilityTypes.LEISURE);
		map.put("shelter", FacilityTypes.IGNORE);
		map.put("shower", FacilityTypes.IGNORE);
		map.put("table", FacilityTypes.IGNORE);
		map.put("telephone", FacilityTypes.IGNORE);
		map.put("toilets", FacilityTypes.TOILETS);
		map.put("townhall", FacilityTypes.OTHER);
		map.put("vending_machine", FacilityTypes.SHOPPING);
		map.put("waste_basket", FacilityTypes.IGNORE);
		map.put("waste_disposal", FacilityTypes.IGNORE);
		map.put("waste_transfer_station", FacilityTypes.IGNORE);
		map.put("watering_place", FacilityTypes.IGNORE);
		map.put("water_point", FacilityTypes.IGNORE);		
		
		return map;
	}
	
	private static Map<String, String> getLeisureToMatsimMap(){
		Map<String, String> map = new TreeMap<String, String>();
		
		map.put("adult_gaming_centre", FacilityTypes.LEISURE);
		map.put("amusement_arcade", FacilityTypes.LEISURE);
		map.put("beach_resort", FacilityTypes.LEISURE);
		map.put("bandstand", FacilityTypes.IGNORE);
		map.put("bird_hide", FacilityTypes.LEISURE);
		map.put("common", FacilityTypes.LEISURE);
		map.put("dance", FacilityTypes.LEISURE);
		map.put("dog_park", FacilityTypes.LEISURE);
		map.put("firepit", FacilityTypes.LEISURE);
		map.put("fishing", FacilityTypes.LEISURE);
		map.put("fitness_centre", FacilityTypes.LEISURE);
		map.put("garden", FacilityTypes.LEISURE);
		map.put("golf_course", FacilityTypes.LEISURE);
		map.put("hackerspace", FacilityTypes.LEISURE);
		map.put("horse_riding", FacilityTypes.LEISURE);
		map.put("ice_rink", FacilityTypes.LEISURE);
		map.put("marina", FacilityTypes.LEISURE);
		map.put("miniature_golf", FacilityTypes.LEISURE);
		map.put("nature_reserve", FacilityTypes.LEISURE);
		map.put("park", FacilityTypes.LEISURE);
		map.put("picnic_table", FacilityTypes.LEISURE);
		map.put("pitch", FacilityTypes.LEISURE);
		map.put("playground", FacilityTypes.LEISURE);
		map.put("slipway", FacilityTypes.IGNORE);
		map.put("sports_centre", FacilityTypes.LEISURE);
		map.put("stadium", FacilityTypes.LEISURE);
		map.put("summer_camp", FacilityTypes.LEISURE);
		map.put("swimming_area", FacilityTypes.LEISURE);
		map.put("swimming_pool", FacilityTypes.LEISURE);
		map.put("track", FacilityTypes.LEISURE);
		map.put("water_park", FacilityTypes.LEISURE);
		map.put("wildlife_hide", FacilityTypes.IGNORE);

		return map;
	}
	
	private static Map<String, String> getTourismToMatsimMap(){
		Map<String, String> map = new TreeMap<String, String>();
		
		map.put("alpine_hut", FacilityTypes.LEISURE);
		map.put("apartment", FacilityTypes.IGNORE);
		map.put("aquarium", FacilityTypes.LEISURE);
		map.put("artwork", FacilityTypes.LEISURE);
		map.put("attraction", FacilityTypes.LEISURE);
		map.put("camp_site", FacilityTypes.IGNORE);
		map.put("caravan_site", FacilityTypes.IGNORE);
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
		map.put("viewpoint", FacilityTypes.LEISURE);
		map.put("wilderness_hut", FacilityTypes.LEISURE);
		map.put("zoo", FacilityTypes.LEISURE);
		
		return map;
	}
	
	private static List<String> getUnmannedList(){
		List<String> list = new ArrayList<>();
		
		list.add("atm");
		list.add("bench");
		list.add("bicycle_rental");
		list.add("bird_hide");
		list.add("camp_site");
		list.add("car_wash");
		list.add("common");
		list.add("dance");
		list.add("dog_park");
		list.add("drinking_water");
		list.add("firepit");
		list.add("fountain");
		list.add("garden");
		list.add("photo_booth");
		list.add("pitch");
		list.add("picnic_site");
		list.add("playground");
		list.add("post_box");
		list.add("recycling");
		list.add("table");
		list.add("telephone");
		list.add("toilets");
		list.add("track");
		list.add("swimming_area");
		list.add("vending_machine");
		list.add("waste_disposal");
		list.add("water_point");
		list.add("watering_place");
		list.add("wilderness_hut");
		
		return list;
	}
	
}
