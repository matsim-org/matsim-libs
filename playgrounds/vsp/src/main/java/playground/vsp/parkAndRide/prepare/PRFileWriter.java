/* *********************************************************************** *
 * project: org.matsim.*
 * PRFacilityWriter.java
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

/**
 * 
 */
package playground.vsp.parkAndRide.prepare;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.vsp.parkAndRide.PRFacility;

/**
 * @author Ihab
 *
 */
public class PRFileWriter {
	private static final Logger log = Logger.getLogger(PRFileWriter.class);

	public void write(List<PRFacility> parkAndRideFacilities, String prFacilitiesFile) {
		File file = new File(prFacilitiesFile);
		
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "Id;Link1in;Link1out;Link2in;Link2out;Link3in;Link3out;TransitStopFacilityName;Capacity";
	    bw.write(zeile1);
	    bw.newLine();
	
	    for (PRFacility pr : parkAndRideFacilities){
	    	Id id = pr.getId();
	    	Id link1in = pr.getPrLink1in();
	    	Id link1out = pr.getPrLink1out();
	    	Id link2in = pr.getPrLink2in();
	    	Id link2out = pr.getPrLink2out();
	    	Id link3in = pr.getPrLink3in();
	    	Id link3out = pr.getPrLink3out();
	    	String name = pr.getStopFacilityName();
	    	int capacity = pr.getCapacity();
	    	
	    	String zeile = id + ";" + link1in + ";" + link1out + ";" + link2in + ";" + link2out + ";" + link3in + ";" + link3out + ";" + name + ";" + String.valueOf(capacity);
	
	    	bw.write(zeile);
	        bw.newLine();
	    }
	
	    bw.flush();
	    bw.close();
    
	    } catch (IOException e) {}
	    log.info("ParkAndRideFacilites written to "+file.toString());		
	}
	
	public void writeInfo (List<TransitStopFacility> stops, String name) {
		File file = new File(name);
		
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	
	    for (TransitStopFacility stop : stops){
	    	String stopName = "";
	    	if (stop.getName() == null) {
	    		stopName = stop.getId().toString();
	    	} else {
	    		stopName = stop.getName().toString();
	    	}
	    	String zeile = stop.getId().toString() + " / " + stopName;
	
	    	bw.write(zeile);
	        bw.newLine();
	    }
	
	    bw.flush();
	    bw.close();
    
	    } catch (IOException e) {}
	    log.info("Info written to "+file.toString());		
	}
	
	public void writeInfoIDs(List<Id> idsNoNodeFound, String name) {
		File file = new File(name);
		
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	
	    for (Id id : idsNoNodeFound){
	    	
	    	String zeile = id.toString();
	
	    	bw.write(zeile);
	        bw.newLine();
	    }
	
	    bw.flush();
	    bw.close();
    
	    } catch (IOException e) {}
	    log.info("Info written to "+file.toString());	
	}

}
