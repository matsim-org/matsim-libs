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
package playground.vsp.parkAndRide;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * @author ikaddoura
 *
 */
public class PRFileReader {
	private static final Logger log = Logger.getLogger(PRFileReader.class);
	private Map<Id<PRFacility>, PRFacility> id2prFacility = new HashMap<>();
	private String prFacilityFile;

	public PRFileReader(String prFacilityFile) {
		this.prFacilityFile = prFacilityFile;
	}

	public Map<Id<PRFacility>, PRFacility> getId2prFacility() {
		
		log.info("Reading Facilities from file " + this.prFacilityFile + "...");
		
		 BufferedReader br = null;
	        try {
	            br = new BufferedReader(new FileReader(new File(this.prFacilityFile)));
	            String line = null;
	            int lineCounter = 0;
	            while((line = br.readLine()) != null) {
	                if (lineCounter > 0) {
	                	String[] parts = line.split(";"); 
	                	PRFacility prFacility = new PRFacility();
	                	prFacility.setId(Id.create(parts[0], PRFacility.class));
	                	prFacility.setPrLink1in(Id.create(parts[1], Link.class));
	                	prFacility.setPrLink1out(Id.create(parts[2], Link.class));
	                	prFacility.setPrLink2in(Id.create(parts[3], Link.class));
	                	prFacility.setPrLink2out(Id.create(parts[4], Link.class));
	                	prFacility.setPrLink3in(Id.create(parts[5], Link.class));
	                	prFacility.setPrLink3out(Id.create(parts[6], Link.class));
	                	prFacility.setStopFacilityName(parts[7]);
	                	prFacility.setCapacity(Integer.valueOf(parts[8]));
	                	this.id2prFacility.put(prFacility.getId(), prFacility);
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
	        log.info("Reading Facilities from file " + this.prFacilityFile + "... Done.");
	        return this.id2prFacility;
	}
}
