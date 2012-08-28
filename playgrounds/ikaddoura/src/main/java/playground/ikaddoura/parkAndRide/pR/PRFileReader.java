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
package playground.ikaddoura.parkAndRide.pR;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author Ihab
 *
 */
public class PRFileReader {
	private static final Logger log = Logger.getLogger(PRFileReader.class);
	private Map<Id, ParkAndRideFacility> id2prFacility = new HashMap<Id, ParkAndRideFacility>();
	private String prFacilityFile;

	public PRFileReader(String prFacilityFile) {
		this.prFacilityFile = prFacilityFile;
	}

	public Map<Id, ParkAndRideFacility> getId2prFacility() {
		
		log.info("Reading Facilities from file " + this.prFacilityFile);
		
		 BufferedReader br = null;
	        try {
	            br = new BufferedReader(new FileReader(new File(this.prFacilityFile)));
	            String line = null;
	            int lineCounter = 0;
	            int prCounter = 0;
	            while((line = br.readLine()) != null) {
	                if (lineCounter > 0) {
	                	String[] parts = line.split(" ; "); 
	                	ParkAndRideFacility prFacility = new ParkAndRideFacility();
	                	prFacility.setId(new IdImpl(parts[0]));
	                	prFacility.setPrLink1in(new IdImpl(parts[1]));
	                	prFacility.setPrLink1out(new IdImpl(parts[2]));
	                	prFacility.setPrLink2in(new IdImpl(parts[3]));
	                	prFacility.setPrLink2out(new IdImpl(parts[4]));
	                	prFacility.setPrLink3in(new IdImpl(parts[5]));
	                	prFacility.setPrLink3out(new IdImpl(parts[6]));
	                	prFacility.setStopFacilityName(parts[7]);
	                	prFacility.setCapacity(Integer.valueOf(parts[8]));
	                	this.id2prFacility.put(prFacility.getId(), prFacility);
	                	prCounter++;
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
	        log.info("Done.");
			return this.id2prFacility;
	}
}
