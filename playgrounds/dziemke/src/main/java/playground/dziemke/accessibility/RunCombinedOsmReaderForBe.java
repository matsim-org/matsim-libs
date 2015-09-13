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
		
		// used to be "e"
		map.put("school", "education");
		map.put("kindergarten", "education");
		map.put("college", "education");
		map.put("university", "education");
		
		// used to be "l"
		map.put("bar", "leisure");
		map.put("cafe", "leisure");
		map.put("fast_food", "leisure");
		map.put("food_court", "leisure");
		map.put("ice_cream", "leisure");
		map.put("pub", "leisure");
		map.put("restaurant", "leisure");
		map.put("arts_centre", "leisure");
		map.put("cinema", "leisure");
		map.put("nightclub", "leisure");
		map.put("stripclub", "leisure");
		map.put("theatre", "leisure");
		map.put("brothel", "leisure");
		
		// used to be "m"
		map.put("clinic", "medical");
		map.put("dentist", "medical");
		map.put("doctors", "medical");
		map.put("hospital", "medical");
		map.put("nursing_home", "medical");
		map.put("pharmacy", "medical");
		
		// used to be "p"
		map.put("polic", "police");
		
		// used to be "t"
		map.put("library", "other");
		map.put("car_wash", "other");
		map.put("fuel", "other");
		map.put("atm", "other");
		map.put("bank", "other");
		map.put("bureau_de_change", "other");
		map.put("social_centre", "other");
		map.put("marketplace", "other");
		map.put("place_of_worship", "other");
		map.put("post_office", "other");
		map.put("townhall", "other");
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