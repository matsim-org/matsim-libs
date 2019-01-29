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

package org.matsim.contrib.accessibility.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.contrib.accessibility.FacilityTypes;

/**
 * @author dziemke
 */
public class AccessibilityFacilityUtils {
	// Activites in SrV		
	// eigener Arbeitsplatz
	// anderer Dienstort/-weg
	// Kinderkrippe/-garten
	// Grundschule
	// weiterführende Schule (inkl. Berufs- und Hochschule)
	// andere Bildungseinrichtung
	// Einkauf täglicher Bedarf
	// sonstiger Einkauf
	// Öffentliche Einrichtung (z. B. Behörde, Ärztehaus, Post, Bank,...)
	// Kultur, Theater, Kino
	// Gaststätte/Kneipe
	// Privater Besuch (fremde	Wohnung)
	// Erholung/Sport im Freien (auch Wandern, Hund ausführen o. ä.)
	// Sportstätte (allgemein)
	// große Sonderveranstaltung (z.B. Rockkonzert, Sportereignis)
	// andere Freizeitaktivität
	// eigene Wohnung
	// sonstiges
	// keine Angabe
	
	/**
	 * @see <a href="http://wiki.openstreetmap.org/wiki/Key:amenity">OpenStreetMap: Amenity</a>
	 * **/
	public static Map<String, String> buildOsmAmenityToMatsimTypeMap(){
		Map<String, String> map = new TreeMap<String, String>();
		// "subsistence" section in osm wiki
		map.put("bar", FacilityTypes.LEISURE);
		map.put("bbq", FacilityTypes.LEISURE);
		map.put("biergarten", FacilityTypes.LEISURE);
		map.put("cafe", FacilityTypes.LEISURE);
		map.put("drinking_water", FacilityTypes.IGNORE);
		map.put("fast_food", FacilityTypes.LEISURE);
		map.put("food_court", FacilityTypes.LEISURE);
		map.put("ice_cream", FacilityTypes.LEISURE);
		map.put("pub", FacilityTypes.LEISURE);
		map.put("restaurant", FacilityTypes.LEISURE);
		// "education" section in osm wiki
		map.put("college", FacilityTypes.EDUCATION);
		map.put("kindergarten", FacilityTypes.EDUCATION);
		map.put("library", FacilityTypes.OTHER);
		map.put("public_bookcase", FacilityTypes.IGNORE);
		map.put("school", FacilityTypes.EDUCATION);
		map.put("university", FacilityTypes.EDUCATION);
//		map.put("research_institute", FacilityTypes.EDUCATION);
		// "transportation" section in osm wiki
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
		map.put("fuel", FacilityTypes.OTHER); // used to be "t"
		map.put("grit_bin", FacilityTypes.IGNORE);
		map.put("motorcycle_parking", FacilityTypes.IGNORE);
		map.put("parking", FacilityTypes.IGNORE);
		map.put("parking_entrance", FacilityTypes.IGNORE);
		map.put("taxi", FacilityTypes.IGNORE);
		// "financial" section in osm wiki
		map.put("atm", FacilityTypes.OTHER);
		map.put("bank", FacilityTypes.OTHER);
		map.put("bureau_de_change", FacilityTypes.OTHER);
		// "healthcare" section in osm wiki
		map.put("baby_hatch", FacilityTypes.IGNORE);
		map.put("clinic", FacilityTypes.MEDICAL);
		map.put("dentist", FacilityTypes.MEDICAL);
		map.put("doctors", FacilityTypes.MEDICAL);
		map.put("hospital", FacilityTypes.MEDICAL);
		map.put("nursing_home", FacilityTypes.MEDICAL);
		map.put("pharmacy", FacilityTypes.MEDICAL);
		map.put("social_facility", FacilityTypes.IGNORE);
		map.put("veterinary", FacilityTypes.IGNORE);
		map.put("blood_donation", FacilityTypes.IGNORE);
		// "entertainment, arts & culture" section in osm wiki
		map.put("arts_centre", FacilityTypes.LEISURE);
		map.put("brothel", FacilityTypes.LEISURE);
		map.put("casino", FacilityTypes.LEISURE);
		map.put("cinema", FacilityTypes.LEISURE);
		map.put("community_centre", FacilityTypes.IGNORE);
		map.put("fountain", FacilityTypes.IGNORE);
		map.put("gambling", FacilityTypes.LEISURE);
		map.put("nightclub", FacilityTypes.LEISURE);
		map.put("planetarium", FacilityTypes.LEISURE);
		map.put("social_centre", FacilityTypes.OTHER);
		map.put("stripclub", FacilityTypes.LEISURE);
		map.put("studio", FacilityTypes.LEISURE);
		map.put("swingerclub", FacilityTypes.LEISURE);
		map.put("theatre", FacilityTypes.LEISURE);
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
		map.put("fire_station", FacilityTypes.WORK);
		map.put("game_feeding", FacilityTypes.LEISURE);
		map.put("grave_yard", FacilityTypes.IGNORE);
		map.put("gym", FacilityTypes.LEISURE); // Use "leisure=fitness_centre" instead
		map.put("hunting_stand", FacilityTypes.IGNORE);
		map.put("kneipp_water_cure", FacilityTypes.IGNORE);
		map.put("marketplace", FacilityTypes.SHOPPING);
		map.put("photo_booth", FacilityTypes.LEISURE);
		map.put("place_of_worship", FacilityTypes.OTHER);
		map.put("police", FacilityTypes.POLICE);
		map.put("post_box", FacilityTypes.IGNORE);
		map.put("post_office", FacilityTypes.OTHER);
		map.put("prison", FacilityTypes.WORK);
		map.put("ranger_station", FacilityTypes.IGNORE);
		// map.put("register_office", FacilityTypes.IGNORE); // Removed on 2018-12-12
		map.put("recycling", FacilityTypes.IGNORE);
		map.put("rescue_station", FacilityTypes.IGNORE);
		map.put("sauna", FacilityTypes.LEISURE); // Use "leisure=sauna" instead
		map.put("shelter", FacilityTypes.IGNORE);
		map.put("shower", FacilityTypes.IGNORE);
		map.put("telephone", FacilityTypes.IGNORE);
		map.put("toilets", FacilityTypes.IGNORE);
		map.put("townhall", FacilityTypes.OTHER);
		map.put("vending_machine", FacilityTypes.IGNORE);
		map.put("waste_basket", FacilityTypes.IGNORE);
		map.put("waste_disposal", FacilityTypes.IGNORE);
		map.put("watering_place", FacilityTypes.IGNORE);
		map.put("water_point", FacilityTypes.IGNORE);
		return map;
	}
	
	public static Map<String, String> buildOsmAmenityToMatsimTypeMapV2(){
		Map<String, String> map = new TreeMap<String, String>();
		// "subsistence" section in osm wiki
		map.put("bar", FacilityTypes.LEISURE);
		map.put("bbq", FacilityTypes.LEISURE);
		map.put("biergarten", FacilityTypes.LEISURE);
		map.put("cafe", FacilityTypes.LEISURE);
		map.put("drinking_water", FacilityTypes.IGNORE);
		map.put("fast_food", FacilityTypes.LEISURE);
		map.put("food_court", FacilityTypes.LEISURE);
		map.put("ice_cream", FacilityTypes.LEISURE);
		map.put("pub", FacilityTypes.LEISURE);
		map.put("restaurant", FacilityTypes.LEISURE);
		// "education" section in osm wiki
		map.put("college", FacilityTypes.EDUCATION);
		map.put("kindergarten", FacilityTypes.EDUCATION);
		map.put("library", FacilityTypes.OTHER);
		map.put("archive", FacilityTypes.OTHER); // Added on 2018-12-12
		map.put("public_bookcase", FacilityTypes.IGNORE);
		map.put("music_school", FacilityTypes.EDUCATION); // Added on 2018-12-12
		map.put("driving_school", FacilityTypes.OTHER); // Added on 2018-12-12
		map.put("language_school", FacilityTypes.OTHER); // Added on 2018-12-12
		map.put("school", FacilityTypes.EDUCATION);
		map.put("university", FacilityTypes.EDUCATION);
		map.put("research_institute", FacilityTypes.EDUCATION);
		// "transportation" section in osm wiki
		map.put("bicycle_parking", FacilityTypes.IGNORE);
		map.put("bicycle_repair_station", FacilityTypes.IGNORE);
		map.put("bicycle_rental", FacilityTypes.OTHER);
		map.put("boat_rental", FacilityTypes.IGNORE); // Added on 2018-12-12
		map.put("boat_sharing", FacilityTypes.IGNORE);
		map.put("bus_station", FacilityTypes.IGNORE);
		map.put("car_rental", FacilityTypes.OTHER);
		map.put("car_sharing", FacilityTypes.IGNORE);
		map.put("car_wash", FacilityTypes.OTHER);
		map.put("vehicle_inspection", FacilityTypes.IGNORE); // Added on 2018-12-12
		map.put("charging_station", FacilityTypes.IGNORE);
		map.put("ferry_terminal", FacilityTypes.IGNORE);
		map.put("fuel", FacilityTypes.OTHER); // used to be "t"
		map.put("grit_bin", FacilityTypes.IGNORE);
		map.put("motorcycle_parking", FacilityTypes.IGNORE);
		map.put("parking", FacilityTypes.IGNORE);
		map.put("parking_entrance", FacilityTypes.IGNORE);
		map.put("parking_space", FacilityTypes.IGNORE); // Added on 2018-12-12
		map.put("taxi", FacilityTypes.IGNORE);
		map.put("ticket_validator", FacilityTypes.IGNORE); // Added on 2018-12-12
		// "financial" section in osm wiki
		map.put("atm", FacilityTypes.OTHER);
		map.put("bank", FacilityTypes.OTHER);
		map.put("bureau_de_change", FacilityTypes.OTHER);
		// "healthcare" section in osm wiki
		map.put("baby_hatch", FacilityTypes.IGNORE);
		map.put("clinic", FacilityTypes.MEDICAL);
		map.put("dentist", FacilityTypes.MEDICAL);
		map.put("doctors", FacilityTypes.MEDICAL);
		map.put("hospital", FacilityTypes.MEDICAL);
		map.put("nursing_home", FacilityTypes.MEDICAL);
		map.put("pharmacy", FacilityTypes.MEDICAL);
		map.put("social_facility", FacilityTypes.IGNORE);
		map.put("veterinary", FacilityTypes.IGNORE);
		map.put("blood_donation", FacilityTypes.IGNORE);
		// "entertainment, arts & culture" section in osm wiki
		map.put("arts_centre", FacilityTypes.LEISURE);
		map.put("brothel", FacilityTypes.LEISURE);
		map.put("casino", FacilityTypes.LEISURE);
		map.put("cinema", FacilityTypes.LEISURE);
		map.put("community_centre", FacilityTypes.IGNORE);
		map.put("fountain", FacilityTypes.IGNORE);
		map.put("gambling", FacilityTypes.LEISURE);
		map.put("nightclub", FacilityTypes.LEISURE);
		map.put("planetarium", FacilityTypes.LEISURE);
		map.put("social_centre", FacilityTypes.OTHER);
		map.put("stripclub", FacilityTypes.LEISURE);
		map.put("studio", FacilityTypes.LEISURE);
		map.put("swingerclub", FacilityTypes.LEISURE);
		map.put("theatre", FacilityTypes.LEISURE);
		// "other" section in osm wiki
		map.put("animal_boarding", FacilityTypes.IGNORE);
		map.put("animal_shelter", FacilityTypes.IGNORE);
		map.put("baking_oven", FacilityTypes.IGNORE); // Added on 2018-12-12
		map.put("bench", FacilityTypes.IGNORE);
		map.put("clock", FacilityTypes.IGNORE);
		map.put("courthouse", FacilityTypes.IGNORE);
		map.put("coworking_space", FacilityTypes.WORK);
		map.put("crematorium", FacilityTypes.IGNORE);
		map.put("crypt", FacilityTypes.IGNORE);
		map.put("dive_centre", FacilityTypes.IGNORE); // Added on 2018-12-12
		map.put("dojo", FacilityTypes.IGNORE);
		map.put("embassy", FacilityTypes.OTHER);
		map.put("fire_station", FacilityTypes.WORK);
		map.put("game_feeding", FacilityTypes.LEISURE);
		map.put("grave_yard", FacilityTypes.IGNORE);
		map.put("gym", FacilityTypes.LEISURE); // Use "leisure=fitness_centre" instead
		map.put("hunting_stand", FacilityTypes.IGNORE);
		map.put("internet_cafe", FacilityTypes.IGNORE); // Added on 2018-12-12
		map.put("kneipp_water_cure", FacilityTypes.IGNORE);
		map.put("marketplace", FacilityTypes.SHOPPING);
		map.put("photo_booth", FacilityTypes.LEISURE);
		map.put("place_of_worship", FacilityTypes.OTHER);
		map.put("police", FacilityTypes.POLICE);
		map.put("post_box", FacilityTypes.IGNORE);
		map.put("post_office", FacilityTypes.OTHER);
		map.put("prison", FacilityTypes.WORK);
		map.put("public_bath", FacilityTypes.LEISURE); // Added on 2018-12-12
		map.put("ranger_station", FacilityTypes.IGNORE);
		// map.put("register_office", FacilityTypes.IGNORE); // Removed on 2018-12-12
		map.put("recycling", FacilityTypes.IGNORE);
		map.put("rescue_station", FacilityTypes.IGNORE);
		map.put("sanitary_dump_station", FacilityTypes.IGNORE); // Added on 2018-12-12
		map.put("sauna", FacilityTypes.LEISURE); // Use "leisure=sauna" instead
		map.put("shelter", FacilityTypes.IGNORE);
		map.put("shower", FacilityTypes.IGNORE);
		map.put("table", FacilityTypes.IGNORE); // Added on 2018-12-12
		map.put("telephone", FacilityTypes.IGNORE);
		map.put("toilets", FacilityTypes.IGNORE);
		map.put("townhall", FacilityTypes.OTHER);
		map.put("vending_machine", FacilityTypes.IGNORE);
		map.put("waste_basket", FacilityTypes.IGNORE);
		map.put("waste_disposal", FacilityTypes.IGNORE);
		map.put("waste_transfer_station", FacilityTypes.IGNORE); // Added on 2018-12-12
		map.put("watering_place", FacilityTypes.IGNORE);
		map.put("water_point", FacilityTypes.IGNORE);

		return map;
	}
	
	// New version with some updated amenity types, 2018-12-12
	// A selection which covers 49.15% of all amenities
	public static Map<String, String> buildOsmAmenityToMatsimTypeMapV2FinerClassification(){
		Map<String, String> map = new TreeMap<String, String>();
		// "subsistence" section in osm wiki
		map.put("bar", FacilityTypes.BAR); // 1.12%
		map.put("biergarten", FacilityTypes.BAR); // 0.07%
		map.put("cafe", FacilityTypes.CAFE); // 2.57%
		map.put("drinking_water", FacilityTypes.DRINKING_WATER); // 1.31%
		map.put("fast_food", FacilityTypes.FAST_FOOD); // 2.35%
		map.put("food_court", FacilityTypes.FAST_FOOD); // 0.05%
		map.put("ice_cream", FacilityTypes.ICE_CREAM); // 0.15%
		map.put("pub", FacilityTypes.BAR); // 1.09%
		map.put("restaurant", FacilityTypes.RESTAURANT); // 6.65%
		// "education" section in osm wiki
		map.put("college", FacilityTypes.HIGHER_EDUCATION); // 0.34%
		map.put("kindergarten", FacilityTypes.KINDERGARTEN); // 1.69%
		map.put("library", FacilityTypes.LIBRARY); // 0.53%
		map.put("school", FacilityTypes.SCHOOL); // 7.03%
		map.put("university", FacilityTypes.HIGHER_EDUCATION); // 0.35%
		// "transportation" section in osm wiki
		map.put("charging_station", FacilityTypes.CHARGING_STATION); // 0.16%
		map.put("fuel", FacilityTypes.FUEL_STATION); // 2.86%
		// "financial" section in osm wiki
		map.put("atm", FacilityTypes.ATM); // 1.07%
		map.put("bank", FacilityTypes.BANK); // 2.17%
		// "healthcare" section in osm wiki
		map.put("clinic", FacilityTypes.PHYSICIAN); // 0.58%
		map.put("doctors", FacilityTypes.PHYSICIAN); // 0.67%
		map.put("hospital", FacilityTypes.HOSPITAL); // 1.04%
		map.put("pharmacy", FacilityTypes.PHARMACY); // 1.83%
		// "entertainment, arts & culture" section in osm wiki
		map.put("cinema", FacilityTypes.CINEMA); // 0.17%
		map.put("theatre", FacilityTypes.THEATRE); // 0.23%
		// "other" section in osm wiki
		map.put("place_of_worship", FacilityTypes.WORSHIP); // 7.45%
		map.put("police", FacilityTypes.POLICE); // 0.82%
		map.put("post_box", FacilityTypes.POST_BOX); // 1.88%
		map.put("post_office", FacilityTypes.POST_OFFICE); // 1.11%
		map.put("toilets", FacilityTypes.TOILETS); // 1.68%
		map.put("water_point", FacilityTypes.DRINKING_WATER); // 0.13%
		return map;
	}	
	
	public static Map<String, String> buildOsmLeisureToMatsimTypeMap(){
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
	
	// New version with some updated leisure types, 2018-12-12
	public static Map<String, String> buildOsmLeisureToMatsimTypeMapV2(){
		Map<String, String> map = new TreeMap<String, String>();
		map.put("adult_gaming_centre", FacilityTypes.LEISURE);
		map.put("amusement_arcade", FacilityTypes.LEISURE);
		map.put("beach_resort", FacilityTypes.LEISURE);
		map.put("bandstand", FacilityTypes.IGNORE);
		map.put("bird_hide", FacilityTypes.IGNORE);
		map.put("bowling_alley", FacilityTypes.LEISURE); // Added on 2018-12-12
		map.put("common", FacilityTypes.IGNORE); // Added on 2018-12-12
		map.put("dance", FacilityTypes.LEISURE);
		map.put("disc_golf_course", FacilityTypes.LEISURE); // Added on 2018-12-12
		map.put("dog_park", FacilityTypes.LEISURE);
		map.put("escape_game", FacilityTypes.LEISURE); // Added on 2018-12-12
		map.put("firepit", FacilityTypes.LEISURE);
		map.put("fishing", FacilityTypes.IGNORE);
		map.put("fitness_centre", FacilityTypes.LEISURE); // Added on 2018-12-12
		map.put("garden", FacilityTypes.LEISURE);
		map.put("golf_course", FacilityTypes.LEISURE);
		map.put("hackerspace", FacilityTypes.LEISURE);
		map.put("horse_riding", FacilityTypes.LEISURE); // Added on 2018-12-12
		map.put("ice_rink", FacilityTypes.LEISURE);
		map.put("marina", FacilityTypes.IGNORE);
		map.put("miniature_golf", FacilityTypes.LEISURE);
		map.put("nature_reserve", FacilityTypes.IGNORE);
		map.put("park", FacilityTypes.IGNORE);
		map.put("picnic_table", FacilityTypes.IGNORE); // Added on 2018-12-12
		map.put("pitch", FacilityTypes.LEISURE);
		map.put("playground", FacilityTypes.LEISURE);
		map.put("recreation_ground", FacilityTypes.LEISURE); // not in Wiki, but in tags; Added on 2018-12-12
		map.put("sauna", FacilityTypes.LEISURE); // Added on 2018-12-12
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
	
	// New version with some updated leisure types, 2018-12-12
	// A selection which cover 93.44% of all leisure sites
	public static Map<String, String> buildOsmLeisureToMatsimTypeMapV2FinerClassification(){
		Map<String, String> map = new TreeMap<String, String>();
		map.put("common", FacilityTypes.PARK); // 1.16%
		map.put("fitness_centre", FacilityTypes.SPORT); // 0.56%
		map.put("garden", FacilityTypes.GARDEN); // 9.62%
		map.put("golf_course", FacilityTypes.GOLF); // 0.70%
		map.put("ice_rink", FacilityTypes.ICE_RINK); // 0.10%
		map.put("miniature_golf", FacilityTypes.MINIATURE_GOLF); // 0.12%
		map.put("nature_reserve", FacilityTypes.PARK); // 1.68%
		map.put("park", FacilityTypes.PARK); // 17.08%
		map.put("pitch", FacilityTypes.SPORT_PUBLIC); // 28.49%
		map.put("playground", FacilityTypes.PLAYGROUND); // 9.76%
		map.put("recreation_ground", FacilityTypes.PARK); // 0.59%
		map.put("sports_centre", FacilityTypes.SPORT); // 3.48%
		map.put("stadium", FacilityTypes.STADIUM); // 0.88%
		map.put("swimming_pool", FacilityTypes.SWIMMING); // 19.20%
		map.put("swimming_area", FacilityTypes.SWIMMING); // 0.02%
		return map;
	}
	
	public static Map<String, String> buildOsmTourismToMatsimTypeMap(){
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
	
	// New version with some updated tourism types, 2018-12-12
	public static Map<String, String> buildOsmTourismToMatsimTypeMapV2(){
		Map<String, String> map = new TreeMap<String, String>();
		map.put("alpine_hut", FacilityTypes.LEISURE);
		map.put("apartment", FacilityTypes.IGNORE);
		map.put("aquarium", FacilityTypes.LEISURE);
		map.put("attraction", FacilityTypes.IGNORE);
		map.put("artwork", FacilityTypes.IGNORE);
		map.put("camp_site", FacilityTypes.LEISURE);
		map.put("caravan_site", FacilityTypes.LEISURE);
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
	
	// New version with some updated tourism types, 2018-12-12
	// A selection which cover 96.92% of all tourism sites
	public static Map<String, String> buildOsmTourismToMatsimTypeMapV2FinerClassification(){
		Map<String, String> map = new TreeMap<String, String>();
		map.put("apartment", FacilityTypes.HOLIDAY_HOME); // 1.62%
		map.put("attraction", FacilityTypes.ATTRACTION); // 9.34%
		map.put("artwork", FacilityTypes.ARTWORK); // 5.25%
		map.put("camp_site", FacilityTypes.CAMPING); // 5.14%
		map.put("chalet", FacilityTypes.HOLIDAY_HOME); // 2.02%
		map.put("gallery", FacilityTypes.GALLERY); // 0.30%
		map.put("guest_house", FacilityTypes.GUEST_HOUSE); // 6.25%
		map.put("hostel", FacilityTypes.HOSTEL); // 2.20%
		map.put("hotel", FacilityTypes.HOTEL); // 16.30%
		map.put("information", FacilityTypes.TOURIST_INFO); // 28.15%
		map.put("motel", FacilityTypes.MOTEL); // 2.01%
		map.put("museum", FacilityTypes.MUSEUM); // 3.96%
		map.put("picnic_site", FacilityTypes.PICNIC_SITE); // 6.13%
		map.put("theme_park", FacilityTypes.THEME_PARK); // 0.40%
		map.put("viewpoint", FacilityTypes.VIEWPOINT); // 7.51%
		map.put("zoo", FacilityTypes.ZOO); // 0.34%
		return map;
	}
	
	// New for SHOPPINGs, 2018-12-12
	public static Map<String, String> buildOsmShopToMatsimTypeMapV2(){
		Map<String, String> map = new TreeMap<String, String>();
		// "food, beverages" section in osm wiki
		map.put("alcohol", FacilityTypes.SHOPPING);
		map.put("bakery", FacilityTypes.SHOPPING);
		map.put("beverages", FacilityTypes.SHOPPING);
		map.put("brewing_supplies", FacilityTypes.SHOPPING);
		map.put("butcher", FacilityTypes.SHOPPING);
		map.put("cheese", FacilityTypes.SHOPPING);
		map.put("chocolate", FacilityTypes.SHOPPING);
		map.put("coffee", FacilityTypes.SHOPPING);
		map.put("confectionery", FacilityTypes.SHOPPING);
		map.put("convenience", FacilityTypes.SHOPPING);
		map.put("deli", FacilityTypes.SHOPPING);
		map.put("dairy", FacilityTypes.SHOPPING);
		map.put("farm", FacilityTypes.SHOPPING);
		map.put("frozen_food", FacilityTypes.SHOPPING);
		map.put("greengrocer", FacilityTypes.SHOPPING);
		map.put("health_food", FacilityTypes.SHOPPING);
		map.put("ice_cream", FacilityTypes.SHOPPING);
		map.put("pasta", FacilityTypes.SHOPPING);
		map.put("pastry", FacilityTypes.SHOPPING);
		map.put("seafood", FacilityTypes.SHOPPING);
		map.put("spices", FacilityTypes.SHOPPING);
		map.put("tea", FacilityTypes.SHOPPING);
		map.put("water", FacilityTypes.SHOPPING);
		// "general" section in osm wiki
		map.put("department_store", FacilityTypes.SHOPPING);
		map.put("general", FacilityTypes.SHOPPING);
		map.put("kiosk", FacilityTypes.SHOPPING);
		map.put("mall", FacilityTypes.SHOPPING);
		map.put("supermarket", FacilityTypes.SHOPPING);
		map.put("wholesale", FacilityTypes.SHOPPING);
		// "clothing, shoes, ..." section in osm wiki
		map.put("baby_goods", FacilityTypes.SHOPPING);
		map.put("bag", FacilityTypes.SHOPPING);
		map.put("boutique", FacilityTypes.SHOPPING);
		map.put("clothes", FacilityTypes.SHOPPING);
		map.put("fabric", FacilityTypes.SHOPPING);
		map.put("fashion", FacilityTypes.SHOPPING);
		map.put("jewelry", FacilityTypes.SHOPPING);
		map.put("leather", FacilityTypes.SHOPPING);
		map.put("sewing", FacilityTypes.SHOPPING);
		map.put("shoes", FacilityTypes.SHOPPING);
		map.put("tailor", FacilityTypes.SHOPPING);
		map.put("watches", FacilityTypes.SHOPPING);
		// "discount, charity" section in osm wiki
		map.put("charity", FacilityTypes.SHOPPING);
		map.put("second_hand", FacilityTypes.SHOPPING);
		map.put("variety_store", FacilityTypes.SHOPPING);
		// "health and beauty" section in osm wiki
		map.put("beauty", FacilityTypes.SHOPPING);
		map.put("chemist", FacilityTypes.SHOPPING);
		map.put("cosmetics", FacilityTypes.SHOPPING);
		map.put("erotic", FacilityTypes.SHOPPING);
		map.put("hairdresser", FacilityTypes.SHOPPING);
		map.put("hairdresser_supply", FacilityTypes.SHOPPING);
		map.put("hearing_aids", FacilityTypes.SHOPPING);
		map.put("herbalist", FacilityTypes.SHOPPING);
		map.put("massage", FacilityTypes.SHOPPING);
		map.put("medical_supply", FacilityTypes.SHOPPING);
		map.put("nutrition_supplements", FacilityTypes.SHOPPING);
		map.put("optician", FacilityTypes.SHOPPING);
		map.put("perfumery", FacilityTypes.SHOPPING);
		map.put("tattoo", FacilityTypes.SHOPPING);
		// "DIY" section in osm wiki
		map.put("agrarian", FacilityTypes.SHOPPING);
		map.put("appliance", FacilityTypes.SHOPPING);
		map.put("bathroom_furnishing", FacilityTypes.SHOPPING);
		map.put("doityourself", FacilityTypes.SHOPPING);
		map.put("electrical", FacilityTypes.SHOPPING);
		map.put("energy", FacilityTypes.SHOPPING);
		map.put("fireplace", FacilityTypes.SHOPPING);
		map.put("florist", FacilityTypes.SHOPPING);
		map.put("garden_centre", FacilityTypes.SHOPPING);
		map.put("garden_furniture", FacilityTypes.SHOPPING);
		map.put("gas", FacilityTypes.SHOPPING);
		map.put("glaziery", FacilityTypes.SHOPPING);
		map.put("hardware", FacilityTypes.SHOPPING);
		map.put("houseware", FacilityTypes.SHOPPING);
		map.put("locksmith", FacilityTypes.SHOPPING);
		map.put("paint", FacilityTypes.SHOPPING);
		map.put("security", FacilityTypes.SHOPPING);
		map.put("trade", FacilityTypes.SHOPPING);
		// "Furniture and interior" section in osm wiki
		map.put("antiques", FacilityTypes.SHOPPING);
		map.put("bed", FacilityTypes.SHOPPING);
		map.put("candles", FacilityTypes.SHOPPING);
		map.put("carpet", FacilityTypes.SHOPPING);
		map.put("curtain", FacilityTypes.SHOPPING);
		map.put("doors", FacilityTypes.SHOPPING);
		map.put("flooring", FacilityTypes.SHOPPING);
		map.put("furniture", FacilityTypes.SHOPPING);
		map.put("interior_decoration", FacilityTypes.SHOPPING);
		map.put("kitchen", FacilityTypes.SHOPPING);
		map.put("lamps", FacilityTypes.SHOPPING);
		map.put("tiles", FacilityTypes.SHOPPING);
		map.put("window_blind", FacilityTypes.SHOPPING);
		// "Electronics" section in osm wiki
		map.put("computer", FacilityTypes.SHOPPING);
		map.put("robot", FacilityTypes.SHOPPING);
		map.put("electronics", FacilityTypes.SHOPPING);
		map.put("hifi", FacilityTypes.SHOPPING);
		map.put("mobile_phone", FacilityTypes.SHOPPING);
		map.put("radiotechnics", FacilityTypes.SHOPPING);
		map.put("vacuum_cleaner", FacilityTypes.SHOPPING);
		// "Outdoors and sport, vehicles" section in osm wiki
		map.put("atv", FacilityTypes.SHOPPING);
		map.put("bicycle", FacilityTypes.SHOPPING);
		map.put("boat", FacilityTypes.SHOPPING);
		map.put("car", FacilityTypes.SHOPPING);
		map.put("car_repair", FacilityTypes.SHOPPING);
		map.put("car_parts", FacilityTypes.SHOPPING);
		map.put("fuel", FacilityTypes.SHOPPING);
		map.put("fishing", FacilityTypes.SHOPPING);
		map.put("free_flying", FacilityTypes.SHOPPING);
		map.put("hunting", FacilityTypes.SHOPPING);
		map.put("jetski", FacilityTypes.SHOPPING);
		map.put("motorcycle", FacilityTypes.SHOPPING);
		map.put("outdoor", FacilityTypes.SHOPPING);
		map.put("scuba_diving", FacilityTypes.SHOPPING);
		map.put("ski", FacilityTypes.SHOPPING);
		map.put("snowmobile", FacilityTypes.SHOPPING);
		map.put("sports", FacilityTypes.SHOPPING);
		map.put("swimming_pool", FacilityTypes.SHOPPING);
		map.put("tyres", FacilityTypes.SHOPPING);
		// "Art, music, hobbies" section in osm wiki
		map.put("art", FacilityTypes.SHOPPING);
		map.put("collector", FacilityTypes.SHOPPING);
		map.put("craft", FacilityTypes.SHOPPING);
		map.put("frame", FacilityTypes.SHOPPING);
		map.put("games", FacilityTypes.SHOPPING);
		map.put("model", FacilityTypes.SHOPPING);
		map.put("music", FacilityTypes.SHOPPING);
		map.put("musical_instrument", FacilityTypes.SHOPPING);
		map.put("photo", FacilityTypes.SHOPPING);
		map.put("camera", FacilityTypes.SHOPPING);
		map.put("trophy", FacilityTypes.SHOPPING);
		map.put("video", FacilityTypes.SHOPPING);
		map.put("video_games", FacilityTypes.SHOPPING);
		// "Gifts, books, ..." section in osm wiki
		map.put("anime", FacilityTypes.SHOPPING);
		map.put("books", FacilityTypes.SHOPPING);
		map.put("gift", FacilityTypes.SHOPPING);
		map.put("lottery", FacilityTypes.SHOPPING);
		map.put("newsagent", FacilityTypes.SHOPPING);
		map.put("stationery", FacilityTypes.SHOPPING);
		map.put("ticket", FacilityTypes.SHOPPING);
		// "Others" section in osm wiki
		map.put("bookmaker", FacilityTypes.SHOPPING);
		map.put("cannabis", FacilityTypes.SHOPPING);
		map.put("copyshop", FacilityTypes.SHOPPING);
		map.put("dry_cleaning", FacilityTypes.SHOPPING);
		map.put("e-cigarette", FacilityTypes.SHOPPING);
		map.put("funeral_decors", FacilityTypes.SHOPPING);
		map.put("laundry", FacilityTypes.SHOPPING);
		map.put("money_lender", FacilityTypes.SHOPPING);
		map.put("party", FacilityTypes.SHOPPING);
		map.put("pawnbroker", FacilityTypes.SHOPPING);
		map.put("pet", FacilityTypes.SHOPPING);
		map.put("pyrotechnics", FacilityTypes.SHOPPING);
		map.put("religion", FacilityTypes.SHOPPING);
		map.put("storage_rental", FacilityTypes.SHOPPING);
		map.put("tobacco", FacilityTypes.SHOPPING);
		map.put("toys", FacilityTypes.SHOPPING);
		map.put("travel_agency", FacilityTypes.SHOPPING);
		map.put("vacant", FacilityTypes.SHOPPING);
		return map;
	}
	
	// New for SHOPPINGs, 2018-12-12
	public static Map<String, String> buildOsmShopToMatsimTypeMapV2FinerClassification(){
		Map<String, String> map = new TreeMap<String, String>();
		// "food, beverages" section in osm wiki
		map.put("alcohol", FacilityTypes.SHOP_BEVERAGES); // 1.33%
		map.put("bakery", FacilityTypes.BAKERY); // 4.29%
		map.put("beverages", FacilityTypes.SHOP_BEVERAGES);
		map.put("brewing_supplies", FacilityTypes.SHOP_HOBBY);
		map.put("butcher", FacilityTypes.BUTCHER); // 1.69%
		map.put("cheese", FacilityTypes.SHOP_FOOD); // 0.07%
		map.put("chocolate", FacilityTypes.SHOP_FOOD);
		map.put("coffee", FacilityTypes.SHOP_FOOD);
		map.put("confectionery", FacilityTypes.SHOP_FOOD);
		map.put("convenience", FacilityTypes.SHOP_FOOD); // 12.75%
		map.put("deli", FacilityTypes.SHOP_FOOD);
		map.put("dairy", FacilityTypes.SHOP_FOOD);
		map.put("farm", FacilityTypes.SHOP_FOOD);
		map.put("frozen_food", FacilityTypes.SHOP_FOOD);
		map.put("greengrocer", FacilityTypes.SHOP_FOOD); // 0.94%
		map.put("health_food", FacilityTypes.SHOP_FOOD);
		map.put("ice_cream", FacilityTypes.SHOP_FOOD);
		map.put("pasta", FacilityTypes.SHOP_FOOD);
		map.put("pastry", FacilityTypes.SHOP_FOOD);
		map.put("seafood", FacilityTypes.SHOP_FOOD);
		map.put("spices", FacilityTypes.SHOP_FOOD);
		map.put("tea", FacilityTypes.SHOP_FOOD);
		map.put("water", FacilityTypes.SHOP_BEVERAGES);
		// "general" section in osm wiki
		map.put("department_store", FacilityTypes.DEPARTMENT_STORE); // 1.06%
		map.put("general", FacilityTypes.KIOSK); // 0.09%
		map.put("kiosk", FacilityTypes.KIOSK); // 2.01%
		map.put("mall", FacilityTypes.MALL); // 1.36%
		map.put("supermarket", FacilityTypes.SUPERMARKET); // 9.60%
		map.put("wholesale", FacilityTypes.WHOLESALE); // 0.05%
		// "clothing, shoes, ..." section in osm wiki
		map.put("baby_goods", FacilityTypes.SHOP_CLOTHING);
		map.put("bag", FacilityTypes.SHOP_CLOTHING);
		map.put("boutique", FacilityTypes.SHOP_CLOTHING);
		map.put("clothes", FacilityTypes.SHOP_CLOTHING); // 6.21%
		map.put("fabric", FacilityTypes.SHOP_CLOTHING);
		map.put("fashion", FacilityTypes.SHOP_CLOTHING);
		map.put("jewelry", FacilityTypes.JEWELRY); // 1.08%
		map.put("leather", FacilityTypes.SHOP_CLOTHING);
		map.put("sewing", FacilityTypes.SHOP_CLOTHING);
		map.put("shoes", FacilityTypes.SHOP_CLOTHING); // 1.34%
		map.put("tailor", FacilityTypes.SHOP_CLOTHING);
		map.put("watches", FacilityTypes.SHOP_CLOTHING);
		// "health and beauty" section in osm wiki
		map.put("beauty", FacilityTypes.SHOP_BEAUTY); // 1.73%
		map.put("chemist", FacilityTypes.SHOP_MEDICAL);
		map.put("cosmetics", FacilityTypes.SHOP_BEAUTY);
		map.put("erotic", FacilityTypes.SHOP_EROTIC); // 0.07%
		map.put("hairdresser", FacilityTypes.HAIRDRESSER); // 5.04%
		map.put("hearing_aids", FacilityTypes.SHOP_MEDICAL);
		map.put("herbalist", FacilityTypes.SHOP_MEDICAL);
		map.put("massage", FacilityTypes.SHOP_MEDICAL);
		map.put("medical_supply", FacilityTypes.SHOP_MEDICAL);
		map.put("optician", FacilityTypes.OPTICIAN); // 1.07%
		map.put("perfumery", FacilityTypes.SHOP_BEAUTY); // 0.10%
		map.put("tattoo", FacilityTypes.SHOP_TATTOO); // 0.17%
		// "DIY" section in osm wiki
		map.put("agrarian", FacilityTypes.SHOP_DIY);
		map.put("appliance", FacilityTypes.SHOP_DIY);
		map.put("bathroom_furnishing", FacilityTypes.SHOP_DIY);
		map.put("doityourself", FacilityTypes.SHOP_DIY); // 1.32%
		map.put("electrical", FacilityTypes.SHOP_DIY);
		map.put("energy", FacilityTypes.SHOP_DIY);
		map.put("fireplace", FacilityTypes.SHOP_DIY);
		map.put("florist", FacilityTypes.FLORIST); // 1.47%
		map.put("garden_centre", FacilityTypes.SHOP_GARDEN); // 0.49%
		map.put("garden_furniture", FacilityTypes.SHOP_GARDEN);
		map.put("gas", FacilityTypes.SHOP_DIY);
		map.put("glaziery", FacilityTypes.SHOP_DIY);
		map.put("hardware", FacilityTypes.SHOP_DIY); // 1.67%
		map.put("houseware", FacilityTypes.SHOP_DIY);
		map.put("locksmith", FacilityTypes.SHOP_DIY);
		map.put("paint", FacilityTypes.SHOP_DIY);
		map.put("security", FacilityTypes.SHOP_DIY);
		// "Furniture and interior" section in osm wiki
		map.put("antiques", FacilityTypes.SHOP_FURNITURE);
		map.put("bed", FacilityTypes.SHOP_FURNITURE);
		map.put("candles", FacilityTypes.SHOP_FURNITURE);
		map.put("carpet", FacilityTypes.SHOP_FURNITURE);
		map.put("curtain", FacilityTypes.SHOP_FURNITURE);
		map.put("doors", FacilityTypes.SHOP_FURNITURE);
		map.put("flooring", FacilityTypes.SHOP_FURNITURE);
		map.put("furniture", FacilityTypes.SHOP_FURNITURE); // 1.53%
		map.put("interior_decoration", FacilityTypes.SHOP_FURNITURE);
		map.put("kitchen", FacilityTypes.SHOP_FURNITURE);
		map.put("lamps", FacilityTypes.SHOP_FURNITURE);
		map.put("tiles", FacilityTypes.SHOP_FURNITURE);
		map.put("window_blind", FacilityTypes.SHOP_FURNITURE);
		// "Electronics" section in osm wiki
		map.put("computer", FacilityTypes.SHOP_ELECTRONICS);
		map.put("robot", FacilityTypes.SHOP_ELECTRONICS);
		map.put("electronics", FacilityTypes.SHOP_ELECTRONICS); // 1.41%
		map.put("hifi", FacilityTypes.SHOP_ELECTRONICS);
		map.put("mobile_phone", FacilityTypes.SHOP_ELECTRONICS); // 1.37%
		map.put("radiotechnics", FacilityTypes.SHOP_ELECTRONICS);
		map.put("vacuum_cleaner", FacilityTypes.SHOP_ELECTRONICS);
		// "Outdoors and sport, vehicles" section in osm wiki
		map.put("bicycle", FacilityTypes.SHOP_BICYCLE); // 1.05%
		map.put("car", FacilityTypes.SHOP_AUTOMOTIVE); // 2.68%
		map.put("car_repair", FacilityTypes.SHOP_AUTOMOTIVE); // 4.14%
		map.put("car_parts", FacilityTypes.SHOP_AUTOMOTIVE); // 1.27%
		map.put("motorcycle", FacilityTypes.SHOP_AUTOMOTIVE);
		map.put("outdoor", FacilityTypes.SHOP_SPORT);
		map.put("scuba_diving", FacilityTypes.SHOP_SPORT);
		map.put("ski", FacilityTypes.SHOP_SPORT);
		map.put("snowmobile", FacilityTypes.SHOP_AUTOMOTIVE);
		map.put("sports", FacilityTypes.SHOP_SPORT);
		map.put("swimming_pool", FacilityTypes.SHOP_SPORT);
		map.put("tyres", FacilityTypes.SHOP_AUTOMOTIVE);
		// "Art, music, hobbies" section in osm wiki
		map.put("art", FacilityTypes.SHOP_HOBBY);
		map.put("collector", FacilityTypes.SHOP_HOBBY);
		map.put("craft", FacilityTypes.SHOP_HOBBY);
		map.put("frame", FacilityTypes.SHOP_HOBBY);
		map.put("games", FacilityTypes.SHOP_HOBBY);
		map.put("model", FacilityTypes.SHOP_HOBBY);
		map.put("music", FacilityTypes.SHOP_HOBBY);
		map.put("musical_instrument", FacilityTypes.SHOP_HOBBY);
		map.put("photo", FacilityTypes.SHOP_HOBBY);
		map.put("camera", FacilityTypes.SHOP_HOBBY);
		map.put("trophy", FacilityTypes.SHOP_HOBBY);
		map.put("video", FacilityTypes.SHOP_HOBBY);
		map.put("video_games", FacilityTypes.SHOP_HOBBY);
		// "Gifts, books, ..." section in osm wiki
		map.put("anime", FacilityTypes.SHOP_BOOKS);
		map.put("books", FacilityTypes.SHOP_BOOKS); // 1.06%
		map.put("newsagent", FacilityTypes.NEWSAGENT); // 0.54%
		return map;
	}
	
//	---------------------------------------------------------------------------------------------------------
	
	// TODO Revise, dz, dec'18
	public static Map<String, String> buildOsmBuildingToMatsimTypeMap(){
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
		//			map.put("church", FacilityTypes.OTHER);
		//			map.put("mosque", FacilityTypes.OTHER);
		//			map.put("temple", FacilityTypes.OTHER);
		//			map.put("synagoge", FacilityTypes.OTHER);
		map.put("church", FacilityTypes.IGNORE);
		map.put("mosque", FacilityTypes.IGNORE);
		map.put("temple", FacilityTypes.IGNORE);
		map.put("synagoge", FacilityTypes.IGNORE);
		map.put("shrine", FacilityTypes.IGNORE);
		map.put("civic", FacilityTypes.WORK);
		//			map.put("hospital", FacilityTypes.MEDICAL);
		//			map.put("school", FacilityTypes.EDUCATION);
		map.put("hospital", FacilityTypes.IGNORE);
		map.put("school", FacilityTypes.IGNORE);
		//			map.put("stadium", FacilityTypes.LEISURE);
		map.put("stadium", FacilityTypes.IGNORE);
		map.put("train_station", FacilityTypes.IGNORE);
		map.put("transportation", FacilityTypes.IGNORE);
		//			map.put("university", FacilityTypes.EDUCATION);
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
	
	// TODO Revise, dz, dec'18
	public static Map<String, String> buildOsmLandUseToMatsimTypeMap(){
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
	
	public static List<String> buildUnmannedEntitiesList(){
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