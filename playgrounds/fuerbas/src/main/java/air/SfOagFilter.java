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

public class SfOagFilter {

	/** @author fuerbas
	 * @throws IOException 
	 * Filters the OAG Schedule Data to include intra-European flights only.
	 * Work in progress...
	 */
	public static void main(String[] args) throws IOException {
		
		SfOagFilter oag = new SfOagFilter();
		oag.filterEurope(args[0], args[1]);

	}
	
	
	public void filterEurope(String input, String output) throws IOException {
		
		String[] euroCountries = {"AD","AL","AM","AT","AX","AZ","BA","BE","BG","BY","CH","CY","CZ",
				"DE","DK","EE","ES","FI","FO","FR","GB","GE","GG","GR","HR","HU","IE","IM","IS","IT",
				"JE","KZ","LI","LT","LU","LV","MC","MD","ME","MK","MT","NL","NO","PL","PT","RO","RS",
				"RU","SE","SI","SJ","SK","SM","TK","UA","VA"
		};
		
		BufferedReader br = new BufferedReader(new FileReader(new File(input)));
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(output)));
		br.ready();
		while (br.readLine()!=null) {
			String oneLine = br.readLine();
			String[] lineEntries = new String[81];
			lineEntries = oneLine.split(",");

			String orig = lineEntries[6];
			String dest = lineEntries[9];
			boolean origin = false; boolean destination = false;
			
			for (int ii=0; ii<euroCountries.length; ii++) {
				if (orig.contains(euroCountries[ii])) origin=true;
				if (dest.contains(euroCountries[ii])) destination=true;
			}
			
			
			if (origin && destination) {
			if (lineEntries[47].contains("O")) {
			
				bw.write(lineEntries[0]+"\t"+lineEntries[1]+"\t"+lineEntries[4]+"\t"
						+lineEntries[7]+"\t"+lineEntries[10]+"\t"+lineEntries[11]+"\t"
						+lineEntries[13]+"\t"+lineEntries[14]+"\t"+lineEntries[20]+"\t"
						+lineEntries[23]+"\t"+lineEntries[42]);
				bw.newLine();
				}
			}
		}
		br.close();
		bw.close();
		
	}

}
