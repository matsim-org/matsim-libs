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
package playground.ikaddoura.parkAndRide.prepare;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.matsim.api.core.v01.Id;

import playground.ikaddoura.parkAndRide.pR.ParkAndRideFacility;

/**
 * @author Ihab
 *
 */
public class PRFacilitiesWriter {

	public void write(List<ParkAndRideFacility> parkAndRideFacilities, String prFacilitiesFile) {
		File file = new File(prFacilitiesFile);
		
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "Id ; Link1in ; Link1out ; Link2in ; Link2out ; Link3in ; Link3out";
	    bw.write(zeile1);
	    bw.newLine();
	
	    for (ParkAndRideFacility pr : parkAndRideFacilities){
	    	Id id = pr.getId();
	    	Id link1in = pr.getPrLink1in();
	    	Id link1out = pr.getPrLink1out();
	    	Id link2in = pr.getPrLink2in();
	    	Id link2out = pr.getPrLink2out();
	    	Id link3in = pr.getPrLink3in();
	    	Id link3out = pr.getPrLink3out();
	    	
	    	String zeile = id + " ; " + link1in + " ; " + link1out + " ; " + link2in + " ; " + link2out + " ; " + link3in + " ; " + link3out;
	
	    	bw.write(zeile);
	        bw.newLine();
	    }
	
	    bw.flush();
	    bw.close();
    
	    } catch (IOException e) {}
	    System.out.println("ParkAndRideFacilites written to "+file.toString());		
	}

}
