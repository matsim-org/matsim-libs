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
public class PRAnalysisWriter {

	public void writeTimes(Map<Id, List<Double>> linkId2prEndTimes, String prTimesFile) {
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
		}
	
	    bw.flush();
	    bw.close();
    
	    } catch (IOException e) {}
	    
	    System.out.println("Park'n'Ride Times written to "+file.toString());		
	}

	public void writePRusers(Map<Id, Integer> linkId2prActs, Map<Id, ParkAndRideFacility> id2prFacility, String prUsersFile) {

		File file = new File(prUsersFile);
		try {

			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			String zeile0 = "prId ; TransitStopName ; Users";
			bw.write(zeile0);
			bw.newLine();
			
			int prUsers = 0;
			for (Id id : linkId2prActs.keySet()){
				String name = "unknown";
				for (ParkAndRideFacility pr : id2prFacility.values()){
					if (pr.getPrLink3in().equals(id)){
						name = pr.getStopFacilityName();
					}
				}
				String zeile = id + " ; " + name + " ; " + (int) (linkId2prActs.get(id)/2.0);
				bw.write(zeile);
				bw.newLine();
				
				prUsers = prUsers + (int) (linkId2prActs.get(id)/2.0);
			}
			
			String zeile1 = "*******************************";
			bw.write(zeile1);
			bw.newLine();
			
			String zeile2 = "Total PR-Users: " + prUsers;
			bw.write(zeile2);
			bw.newLine();
			
		    bw.flush();
		    bw.close();
		
		} catch (IOException e) {}
		
		System.out.println("PR Users written to " + file.toString());
	}

}
