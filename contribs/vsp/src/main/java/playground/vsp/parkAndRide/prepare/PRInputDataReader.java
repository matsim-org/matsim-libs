/* *********************************************************************** *
 * project: org.matsim.*
 * PRfileReader.java
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import playground.vsp.parkAndRide.PRFacility;

/**
 * @author Ihab
 *
 * input File format: id;name;xCoord;yCoord;capacity
 * 					
 *
 */
public class PRInputDataReader {
	private static final Logger log = Logger.getLogger(PRInputDataReader.class);
	
	private Map<Id<PRFacility>, PRInputData> id2PRInputData = new HashMap<>();
	
	public Map<Id<PRFacility>, PRInputData> getId2prInputData(String prInputDataFile) {
		BufferedReader br = null;
	    try {
	        br = new BufferedReader(new FileReader(new File(prInputDataFile)));
	        String line = null;
	        int lineCounter = 0;
	        while((line = br.readLine()) != null) {
	            if (lineCounter > 0) {	            	
	            	String[] parts = line.split(";");
	            	
	            	for (int i = 0 ; i <= parts.length; i++){
	            		if (parts[i].isEmpty()){
	        				throw new RuntimeException("The prInputFile " + prInputDataFile + " is not complete. Aborting...");
	            		}
	            	}
	            	
	            	PRInputData prInputData = new PRInputData();
	            	
	            	Id<PRFacility> id = Id.create(parts[0], PRFacility.class);
	            	String name = parts[1];
	            	double xCoord = Double.valueOf(parts[2]);
	            	double yCoord = Double.valueOf(parts[3]);
	            	int capacity = Integer.parseInt(parts[4]);
	            	
	            	prInputData.setId(id);
		            prInputData.setStopName(name);
					prInputData.setCoord(new Coord(xCoord, yCoord));
	            	prInputData.setCapacity(capacity);
	            	this.id2PRInputData.put(prInputData.getId(), prInputData);
	            	}  	
	            lineCounter++;
	        }
	    } catch(FileNotFoundException e) {
	        e.printStackTrace();
	    } catch(IOException e) {
	        e.printStackTrace();
	    } finally {
	        if(br != null) {
	            try {
	                br.close();
	            } catch(IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }
	    log.info("Done reading " + prInputDataFile);
		return this.id2PRInputData;
	}
}
