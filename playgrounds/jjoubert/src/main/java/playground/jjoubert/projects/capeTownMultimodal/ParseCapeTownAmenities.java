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
package playground.jjoubert.projects.capeTownMultimodal;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.contrib.accessibility.osm.CombinedOsmReader;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesWriter;

import playground.southafrica.utilities.Header;

/**
 * Parsing the {@link ActivityFacilities} from OpenStreetMap data for the
 * City of Cape Town 
 * @author jwjoubert
 */
public class ParseCapeTownAmenities {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ParseCapeTownAmenities.class.toString(), args);
		
		String osmFile = args[0];
		String facilitiesFile = args[1];
		
		CombinedOsmReader cor = new CombinedOsmReader(
				"EPSG:3857",
				getLanduseToMatsimMap(), 
				getBuildingToMatsimMap(), 
				getAmenityToMatsimMap(),
				getLeisureToMatsimMap(),
				getTourismToMatsimMap(),
				getUnmannedList(), 
				20.0);
		try {
			cor.parseFile(osmFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot parse OSM file " + osmFile);
		}
		new FacilitiesWriter(cor.getActivityFacilities()).write(facilitiesFile);
		
		Header.printFooter();
	}
	
	private static Map<String, String> getLanduseToMatsimMap(){
		Map<String, String> map = new HashMap<String, String>();
		/* TODO Complete. */
		return map;
	}

	private static Map<String, String> getBuildingToMatsimMap(){
		Map<String, String> map = new HashMap<String, String>();
		/* TODO Complete. */
		return map;
	}
	
	private static Map<String, String> getAmenityToMatsimMap(){
		Map<String, String> map = new HashMap<String, String>();
		/* TODO Complete. */
		return map;
	}
	
	private static Map<String, String> getLeisureToMatsimMap(){
		Map<String, String> map = new HashMap<String, String>();
		/* TODO Complete. */
		return map;
	}
	
	private static Map<String, String> getTourismToMatsimMap(){
		Map<String, String> map = new HashMap<String, String>();
		/* TODO Complete. */
		return map;
	}
	
	private static List<String> getUnmannedList(){
		List<String> list = new ArrayList<>();
		/* TODO Complete. */
		return list;
	}
	
}
