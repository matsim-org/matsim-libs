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

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class RunLandUseReaderBerlin {
	final private static Logger LOG = Logger.getLogger(RunLandUseReaderBerlin.class);

	/**
	 * Implementing the {@link LandUseReader} class. 
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
//	public static void main(String[] args) {
//		LOG.info("Parsing land use from OpenStreetMap.");
//		String osmFile = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/2015-06-24_schlesische_str.osm";
//		String facilityFile = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/schlesische_str/facilities_landuse.xml";
//		String attributeFile = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/schlesische_str/facilitiy_attributes_landuse.xml";
//		String coordinateTransformation = "EPSG:31468";
//		
////		String osmFile = args[0];
////		String facilityFile = args[1];
////		String attributeFile = args[2];
////		String coordinateTransformation = "WGS84";
////		if(args.length > 3){
////			coordinateTransformation = args[3];
////		}
//		
//		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", coordinateTransformation);
//		LandUseReader landUseReader = new LandUseReader(ct, buildOsmToMatsimTypeMap());
//		try {
//			landUseReader.parseLandUse(osmFile);
//			landUseReader.writeFacilities(facilityFile);
//			landUseReader.writeFacilityAttributes(attributeFile);
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}		
//	}
//	
	
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
//	private void setFacilityDetails(ActivityOption ao){
//		if(ao.getType().equalsIgnoreCase("s")){
//			double r1 = MatsimRandom.getRandom().nextDouble();
//			double starttime = Math.round(r1)*28800 + (1-Math.round(r1))*32400; // either 08:00 or 09:00
//			double r2 = MatsimRandom.getRandom().nextDouble();
//			double closingtime = Math.round(r2)*54000 + (1-Math.round(r2))*68400; // either 17:00 or 19:00
//			ao.addOpeningTime(new OpeningTimeImpl(starttime, closingtime));
//			
//			double r3 = MatsimRandom.getRandom().nextDouble();
//			double capacity = (200 + r3*800) / 10; // random surface between 200 and 1000 m^2.
//			ao.setCapacity(capacity);
//		} else if(ao.getType().equalsIgnoreCase("l")){
//			double r1 = MatsimRandom.getRandom().nextDouble();
//			double starttime = Math.round(r1)*28800 + (1-Math.round(r1))*32400; // either 08:00 or 09:00
//			double r2 = MatsimRandom.getRandom().nextDouble();
//			double closingtime = Math.round(r2)*64800 + (1-Math.round(r2))*79200; // either 18:00 or 22:00
//			ao.addOpeningTime(new OpeningTimeImpl(starttime, closingtime));
//			
//			double r3 = MatsimRandom.getRandom().nextDouble();
//			double capacity = (50 + r3*1950) / 10; // random surface between 50 and 2000 m^2.
//			ao.setCapacity(capacity);
//		} else{
//			double r1 = MatsimRandom.getRandom().nextDouble();
//			double starttime = Math.round(r1)*28800 + (1-Math.round(r1))*32400; // either 08:00 or 09:00
//			double r2 = MatsimRandom.getRandom().nextDouble();
//			double closingtime = Math.round(r2)*54000 + (1-Math.round(r2))*68400; // either 17:00 or 19:00
//			ao.addOpeningTime(new OpeningTimeImpl(starttime, closingtime));
//			
//			double r3 = MatsimRandom.getRandom().nextDouble();
//			double capacity = (200 + r3*800) / 10; // random number between 200 and 1000 m^2.
//			ao.setCapacity(capacity);
//		}
//	}
	

//	private static Map<String, String> buildOsmToMatsimTypeMap(){
//		Map<String, String> map = new TreeMap<String, String>();
//		
//		map.put("school", "e");
//		map.put("kindergarten", "e");
//		map.put("college", "e");
//		map.put("university", "e");
//		
//		map.put("bar", "l");
//		map.put("cafe", "l");
//		map.put("fast_food", "l");
//		map.put("food_court", "l");
//		map.put("ice_cream", "l");
//		map.put("pub", "l");
//		map.put("restaurant", "l");
//		map.put("arts_centre", "l");
//		map.put("cinema", "l");
//		map.put("nightclub", "l");
//		map.put("stripclub", "l");
//		map.put("theatre", "l");
//		map.put("brothel", "l");
//		
//		map.put("clinic", "m");
//		map.put("dentist", "m");
//		map.put("doctors", "m");
//		map.put("hospital", "m");
//		map.put("nursing_home", "m");
//		map.put("pharmacy", "m");
//		
//		map.put("polic", "p");
//		
//		map.put("library", "t");
//		map.put("car_wash", "t");
//		map.put("fuel", "t");
//		map.put("atm", "t");
//		map.put("bank", "t");
//		map.put("bureau_de_change", "t");
//		map.put("social_centre", "t");
//		map.put("marketplace", "t");
//		map.put("place_of_worship", "t");
//		map.put("post_office", "t");
//		map.put("townhall", "t");
//		return map;
//	}
	
	
	private static Map<String, String> buildOsmToMatsimTypeMap(){
		Map<String, String> map = new TreeMap<String, String>();
		
		map.put("industrial", "work");
		map.put("commercial", "work");
		map.put("retail", "work");

		return map;
	}
}