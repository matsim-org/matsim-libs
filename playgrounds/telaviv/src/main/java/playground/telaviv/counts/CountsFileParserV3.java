/* *********************************************************************** *
 * project: org.matsim.*
 * CountsFileParserV3.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.telaviv.counts;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/*
 * To parse results from the Emme2 model containing data for AM, OP and PM time periods.
 */
public class CountsFileParserV3 {

	private String inFile;
	private String separator = ",";
	private Charset charset = Charset.forName("UTF-8");
	
	public CountsFileParserV3(String inFile) {
		this.inFile = inFile;
	}
	
	public List<Emme2CountV3> readFile() {
		List<Emme2CountV3> counts = new ArrayList<Emme2CountV3>();
		
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
				Emme2CountV3 emme2Count = new Emme2CountV3();
				
				String[] cols = line.split(separator);
				
				/*
				 * Columns
				 * 0 ... i-node
				 * 1 ... j-node
				 * 2 ... length
				 * 3 ... modes
				 * 4 ... type
				 * 5 ... lanes
				 * 6 ... VDF
				 * 7 ... AM_volume
				 * 8 ... OP_volume
				 * 9 ... PM_volume
				 */
				
				emme2Count.inode = parseInteger(cols[0]);
				emme2Count.jnode = parseInteger(cols[1]);
				emme2Count.amVolume = parseInteger(cols[7]);
				emme2Count.opVolume = parseInteger(cols[8]);
				emme2Count.pmVolume = parseInteger(cols[9]);
												
				counts.add(emme2Count);
			}
			
			br.close();
			isr.close();
			fis.close();
    	} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return counts;
	}
	
	private int parseInteger(String string) {
		if (string == null) return 0;
		else if (string.trim().equals("")) return 0;
		else return Integer.valueOf(string);
	}
}