/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.analysis.facilities.facilityLoad;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;


public class FacilityLoadReader {
	
	TreeMap<Id<ActivityFacility>, FacilityLoad> facilityLoads = new TreeMap<Id<ActivityFacility>, FacilityLoad>();
	private final static Logger log = Logger.getLogger(FacilityLoadReader.class);
	
	public void readFiles() {
		this.readFacilityLoadFile("input/postprocessing/loads0.txt", 0);
		this.readFacilityLoadFile("input/postprocessing/loads1.txt", 1);
	}
		
	private void readFacilityLoadFile(String file, int state) {
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			
			String curr_line = bufferedReader.readLine(); // Skip header				
			while ((curr_line = bufferedReader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				
				Id<ActivityFacility> facilityId = Id.create(entries[0].trim(), ActivityFacility.class);
				double x = Double.parseDouble(entries[1].trim());
				double y = Double.parseDouble(entries[2].trim());
				Coord coord = new Coord(x, y);
				
				double load = Double.parseDouble(entries[4].trim());
				String facilityType = entries[7].trim();
				
				if (!facilityType.equals("shop")) {
					continue;
				}
				
				FacilityLoad facilityLoad;
				if (state == 0) {
					facilityLoad = new FacilityLoad();
					facilityLoad.setLoad0(load);
					facilityLoads.put(facilityId, facilityLoad);	
				}
				else {
					facilityLoad = facilityLoads.get(facilityId);
					facilityLoad.setLoad1(load);
				}
				facilityLoad.setFacilityId(facilityId);	
				facilityLoad.setCoord(coord);
			}	
			bufferedReader.close();
			fileReader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		log.info("Number of facility loads: " + this.facilityLoads.size());
	}

	public TreeMap<Id<ActivityFacility>, FacilityLoad> getFacilityLoads() {
		return facilityLoads;
	}

	public void setFacilityLoads(TreeMap<Id<ActivityFacility>, FacilityLoad> facilityLoads) {
		this.facilityLoads = facilityLoads;
	}
}