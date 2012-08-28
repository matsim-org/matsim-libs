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
package playground.ikaddoura.parkAndRide.prepare;

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
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * @author Ihab
 *
 */
public class PRInputDataReader {
	private static final Logger log = Logger.getLogger(PRInputDataReader.class);
	
	private Map<Id, PRInputData> id2PRInputData = new HashMap<Id, PRInputData>();

	public Map<Id, PRInputData> getId2prInputData(String prInputDataFile) {
		BufferedReader br = null;
	    try {
	        br = new BufferedReader(new FileReader(new File(prInputDataFile)));
	        String line = null;
	        int lineCounter = 0;
	        while((line = br.readLine()) != null) {
	            if (lineCounter > 0) {	            	
	            	String[] parts = line.split(" ; ");
	            	PRInputData prInputData = new PRInputData();
	            	prInputData.setId(new IdImpl(parts[0]));
	            	prInputData.setStopName(parts[1]);
	            	prInputData.setCoord(new CoordImpl(parts[2], parts[3]));
	            	prInputData.setCapacity(Integer.parseInt(parts[4]));
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
	    log.info("Done reading file " + prInputDataFile + ".");
		return this.id2PRInputData;
	}
}
