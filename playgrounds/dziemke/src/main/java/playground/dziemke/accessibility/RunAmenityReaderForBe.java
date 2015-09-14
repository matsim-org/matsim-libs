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
import org.matsim.contrib.accessibility.osm.AmenityReader;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.Facility;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class RunAmenityReaderForBe {
	final private static Logger LOG = Logger.getLogger(RunAmenityReaderForBe.class);

	/**
	 * Implementing the {@link AmenityReader} class. 
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
		LOG.info("Parsing amenity facilities from OpenStreetMap.");
		
		// Input and output
//		String osmFile = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/2015-05-26_berlin.osm";
//		String facilityFile = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/facilities_amenities.xml";
//		String attributeFile = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/facilitiy_attributes_amenities.xml";
		String osmFile = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/2015-06-24_schlesische_str.osm";
		String facilityFile = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/schlesische_str/facilities_amenities.xml";
		String attributeFile = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/schlesische_str/facilitiy_attributes_amenities.xml";
		
		// Parameters
		String crs = "EPSG:31468";		
		
//		String osmFile = args[0];
//		String facilityFile = args[1];
//		String attributeFile = args[2];
//		String coordinateTransformation = "WGS84";
//		if(args.length > 3){
//			coordinateTransformation = args[3];
//		}
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", crs);
		AmenityReader msr = new AmenityReader(osmFile, ct, buildOsmAmentityTypeToMatsimTypeMap());
		try {
			msr.parseAmenity(osmFile);
			msr.writeFacilities(facilityFile);
			msr.writeFacilityAttributes(attributeFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}		
	}
	
	
//	log.info("------------------------------------------------");
//	log.info("Facilities parsed:");
//	log.info("  nodes    : " + nodeFacilities);
//	log.info("  ways     : " + wayFacilities);
//	log.info("  relations: " + relationFacilities);
//	log.info("------------------------------------------------");
//	log.info("Done creating facilities.");
//	log.info("  education  : " + educationCounter);
//	log.info("  leisure    : " + leisureCounter);
//	log.info("  shopping   : " + shoppingCounter);		
//	log.info("  healthcare : " + healthcareCounter);
//	log.info("  police     : " + policeCounter);
//	log.info("  other      : " + otherCounter);
//	log.info("------------------------------------------------");
//	log.info("Level of education:");
//	log.info(" primary  : " + educationLevelMap.get("primary") );
//	log.info(" secondary: " + educationLevelMap.get("secondary") );
//	log.info(" tertiary : " + educationLevelMap.get("tertiary"));
//	log.info(" unknown  : " + educationLevelMap.get("unknown"));
//	log.info("------------------------------------------------");
//	log.info("Errors and warnings:");
//	log.info("  errors  : " + errorCounter);
//	log.info("  warnings: " + warningCounter);
	private void setFacilityDetails(ActivityOption ao){
		if(ao.getType().equalsIgnoreCase("s")){
			double r1 = MatsimRandom.getRandom().nextDouble();
			double starttime = Math.round(r1)*28800 + (1-Math.round(r1))*32400; // either 08:00 or 09:00
			double r2 = MatsimRandom.getRandom().nextDouble();
			double closingtime = Math.round(r2)*54000 + (1-Math.round(r2))*68400; // either 17:00 or 19:00
			ao.addOpeningTime(new OpeningTimeImpl(starttime, closingtime));
			
			double r3 = MatsimRandom.getRandom().nextDouble();
			double capacity = (200 + r3*800) / 10; // random surface between 200 and 1000 m^2.
			ao.setCapacity(capacity);
		} else if(ao.getType().equalsIgnoreCase("l")){
			double r1 = MatsimRandom.getRandom().nextDouble();
			double starttime = Math.round(r1)*28800 + (1-Math.round(r1))*32400; // either 08:00 or 09:00
			double r2 = MatsimRandom.getRandom().nextDouble();
			double closingtime = Math.round(r2)*64800 + (1-Math.round(r2))*79200; // either 18:00 or 22:00
			ao.addOpeningTime(new OpeningTimeImpl(starttime, closingtime));
			
			double r3 = MatsimRandom.getRandom().nextDouble();
			double capacity = (50 + r3*1950) / 10; // random surface between 50 and 2000 m^2.
			ao.setCapacity(capacity);
		} else{
			double r1 = MatsimRandom.getRandom().nextDouble();
			double starttime = Math.round(r1)*28800 + (1-Math.round(r1))*32400; // either 08:00 or 09:00
			double r2 = MatsimRandom.getRandom().nextDouble();
			double closingtime = Math.round(r2)*54000 + (1-Math.round(r2))*68400; // either 17:00 or 19:00
			ao.addOpeningTime(new OpeningTimeImpl(starttime, closingtime));
			
			double r3 = MatsimRandom.getRandom().nextDouble();
			double capacity = (200 + r3*800) / 10; // random number between 200 and 1000 m^2.
			ao.setCapacity(capacity);
		}
	}
	

	private static Map<String, String> buildOsmAmentityTypeToMatsimTypeMap(){
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
}