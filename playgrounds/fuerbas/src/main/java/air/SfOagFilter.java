/* *********************************************************************** *
 * project: org.matsim.*
 * OSMReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package air;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SfOagFilter {

	/** @author fuerbas
	 * @throws IOException 
	 * Filters the OAG Schedule Data to include intra-European flights only.
	 * 
	 * Einbauen in eine Kopie: OsmAerowayParser bzw. SfOsm2Matsim, sodass nur Flughäfen, die in Osm
	 * enthalten sind in die Flugliste übernommen werden.
	 */
	public static void main(String[] args) throws IOException {
		
		SfOagFilter oag = new SfOagFilter();
		oag.filterEurope(args[0], args[1]);

	}
	
	
	public void filterEurope(String input, String output) throws IOException {
		
		String[] euroCountries = {"AD","AL","AM","AT","AX","AZ","BA","BE","BG","BY","CH","CY","CZ",
				"DE","DK","EE","ES","FI","FO","FR","GB","GE","GG","GR","HR","HU","IE","IM","IS","IT",
				"JE","KZ","LI","LT","LU","LV","MC","MD","ME","MK","MT","NL","NO","PL","PT","RO","RS",
				"RU","SE","SI","SJ","SK","SM","TK","UA","VA" };
		
		BufferedReader br = new BufferedReader(new FileReader(new File(input)));
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(output)));
		Map<String, String> flights = new HashMap<String, String>();
		String flight = "";
		int lines = 0;
		
		while (br.ready()) {
			String oneLine = br.readLine();
			String[] lineEntries = new String[81];
			lineEntries = oneLine.split(",");
			
		if (lines>0) {

			for (int jj=0; jj<81; jj++){
				lineEntries[jj]=lineEntries[jj].replaceAll("\"", "");
			}
			
				String orig = lineEntries[6];
				String dest = lineEntries[9];
				String hours = lineEntries[13].substring(1, 3);
				String minutes = lineEntries[13].substring(3);
				System.out.println(hours);
				System.out.println(minutes);
				double durationMinutes = Double.parseDouble(minutes)*60;	//convert flight dur minutes into seconds
				double durationHours = Double.parseDouble(hours)*3600;
				double duration = durationHours+durationMinutes;
				boolean origin = false; boolean destination = false;
			
				for (int ii=0; ii<euroCountries.length; ii++) {
					if (orig.contains(euroCountries[ii])) origin=true;
					if (dest.contains(euroCountries[ii])) destination=true;
				}
			
			
				if (origin && destination) {
					if (lineEntries[47].contains("O")) {
				
						flight=lineEntries[0]+lineEntries[1];
						int seatsAvail = Integer.parseInt(lineEntries[23]);
				
						if (lineEntries[14].contains("2") && !flights.containsKey(flight) && seatsAvail>0) {
							bw.write(
									lineEntries[4]+lineEntries[7]+"\t"+		//TransitRoute
									lineEntries[4]+lineEntries[7]+"_"+lineEntries[0]+"\t"+	//TransitLine
									lineEntries[0]+lineEntries[1]+"\t"+		//vehicleId
									lineEntries[0]+"\t"+	//carrier code
									lineEntries[1]+"\t"+	//flt number
									lineEntries[4]+"\t"+	//departure arpt
									lineEntries[7]+"\t"+	//arrival arpt
									lineEntries[10]+"\t"+	//departure time (24h)
									lineEntries[11]+"\t"+	//arrival time
									duration+"\t"+	//journey time (HHHMM)
//									lineEntries[14]+"\t"+	//ops days
									lineEntries[21]+"\t"+	//aircraft
									lineEntries[23]+"\t"+	//seats avail
									lineEntries[42]);		//distance (statute miles)
							flights.put(flight, "");
							bw.newLine();
						}
					}
				}
			}
		
		lines++;
		
		}
		
		br.close();
		bw.close();
		
	}

}
