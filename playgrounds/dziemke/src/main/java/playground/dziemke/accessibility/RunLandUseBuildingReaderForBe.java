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
import org.matsim.contrib.accessibility.osm.LandUseBuildingReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author dziemke
 */
public class RunLandUseBuildingReaderForBe {
	final private static Logger LOG = Logger.getLogger(RunLandUseBuildingReaderForBe.class);

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
		LOG.info("Parsing land use from OpenStreetMap.");
		String osmFile = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/schlesische_str/2015-06-24_schlesische_str.osm";
		String facilityFile = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/schlesische_str/facilities_landuse6.xml";
		String attributeFile = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/schlesische_str/facilitiy_attributes_landuse6.xml";
		String coordinateTransformation = "EPSG:31468";
		
//		String osmFile = args[0];
//		String facilityFile = args[1];
//		String attributeFile = args[2];
//		String coordinateTransformation = "WGS84";
//		if(args.length > 3){
//			coordinateTransformation = args[3];
//		}
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", coordinateTransformation);
//		LandUseReader landUseReader = new LandUseReader(osmFile, ct, buildOsmToMatsimTypeMap());
		LandUseBuildingReader landUseReader = new LandUseBuildingReader(ct, buildOsmToMatsimTypeMap());
		try {
			landUseReader.parseLandUse(osmFile);
			landUseReader.writeFacilities(facilityFile);
			landUseReader.writeFacilityAttributes(attributeFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}		
	}
	
	
	private static Map<String, String> buildOsmToMatsimTypeMap(){
		Map<String, String> map = new TreeMap<String, String>();
		
		map.put("industrial", "work");
		map.put("commercial", "work");
		map.put("retail", "work");
		
		map.put("residential", "home");

		return map;
	}
}