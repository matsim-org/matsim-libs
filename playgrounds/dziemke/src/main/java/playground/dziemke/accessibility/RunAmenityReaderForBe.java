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
	
	

}
