/* *********************************************************************** *
 * project: org.matsim.*
 * Emme2ExternalTripFileParser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.telaviv.population;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class Emme2ExternalTripFileParser {

	private String inFile;
	private String separator = ",";
	private Charset charset = Charset.forName("UTF-8");
	
	public Emme2ExternalTripFileParser(String inFile) {
		this.inFile = inFile;
	}
	
	public List<Emme2ExternalTrip> readFile() {
		List<Emme2ExternalTrip> externalTrips = new ArrayList<Emme2ExternalTrip>();
		
		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;
	       
    	try {
    		fis = new FileInputStream(inFile);
    		isr = new InputStreamReader(fis, charset);
			br = new BufferedReader(isr);
			
			// skip first Line
			br.readLine();
			 
			String line;
			while((line = br.readLine()) != null) {
				Emme2ExternalTrip externalTrip = new Emme2ExternalTrip();
				
				String[] cols = line.split(separator);
				
				externalTrip.originNodeId = parseInteger(cols[0]);
				externalTrip.destinationNodeId = parseInteger(cols[1]);
				externalTrip.numOfTrips = parseInteger(cols[2]);
								
				externalTrips.add(externalTrip);
			}
			
			br.close();
			isr.close();
			fis.close();
    	} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return externalTrips;
	}
	
	private int parseInteger(String string) {
		if (string == null) return 0;
		else if (string.trim().equals("")) return 0;
		else return Integer.valueOf(string);
	}
	
	private double parseDouble(String string) {
		if (string == null) return 0.0;
		else if (string.trim().equals("")) return 0.0;
		else return Double.valueOf(string);
	}
}