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
package playground.ikaddoura.parkAndRide.analyze;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import playground.ikaddoura.parkAndRide.pR.ParkAndRideFacility;

/**
 * @author Ihab
 *
 */
public class PRTimesWriter {

	public void write(Map<Id, List<Double>> linkId2prEndTimes, String prTimesFile) {
		File file = new File(prTimesFile);
		try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));

	    
		for (Id id : linkId2prEndTimes.keySet()){
			String zeile1 = "Park'n'Ride Id: " + id;
			bw.write(zeile1);
			bw.newLine();
			List<Double> endTimes = linkId2prEndTimes.get(id);
			for (Double dbl : endTimes){
				String zeile2 = dbl.toString();
				bw.write(zeile2);
				bw.newLine();
			}
			bw.write("---------------------------");
			bw.newLine();
		}
	
	    bw.flush();
	    bw.close();
    
	    } catch (IOException e) {}
	    
	    System.out.println("Park'n'Ride Times written to "+file.toString());		
	}

}
