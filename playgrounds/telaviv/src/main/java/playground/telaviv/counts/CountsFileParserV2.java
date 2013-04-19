/* *********************************************************************** *
 * project: org.matsim.*
 * CountsFileParserV2.java
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

public class CountsFileParserV2 {

	private String inFile;
	private String separator = "\t";
	private Charset charset = Charset.forName("UTF-8");
	
	public CountsFileParserV2(String inFile) {
		this.inFile = inFile;
	}
	
	public List<Emme2CountV2> readFile() {
		List<Emme2CountV2> counts = new ArrayList<Emme2CountV2>();
		
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
				Emme2CountV2 emme2Count = new Emme2CountV2();
				
				String[] cols = line.split(separator);
				
				emme2Count.inode = parseInteger(cols[0]);
				emme2Count.jnode = parseInteger(cols[1]);
				emme2Count.hour = parseInteger(cols[2]);
				emme2Count.value = parseInteger(cols[3]);
				
								
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