/* *********************************************************************** *
 * project: org.matsim.*
 * RealWorldCountsFileParser.java
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
import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.io.IOUtils;

public class RealWorldCountsFileParser {
	
	private final String separator;
	
	public RealWorldCountsFileParser(String separator) {
		this.separator = separator;
	}
	
	public List<RealWorldCount> readFile(String inFile) throws Exception {
		List<RealWorldCount> counts = new ArrayList<RealWorldCount>();
		
		BufferedReader br = IOUtils.getBufferedReader(inFile);
			
		// skip first Line
		br.readLine();
		
		String line;
		while((line = br.readLine()) != null) {
			RealWorldCount realWorldCount = new RealWorldCount();
			
			String[] cols = line.split(separator);
			
			realWorldCount.inode = parseInteger(cols[0]);
			realWorldCount.jnode = parseInteger(cols[1]);
			realWorldCount.hour = parseInteger(cols[2]);
			realWorldCount.value = parseInteger(cols[3]);
			
			counts.add(realWorldCount);
		}
		
		br.close();
		
		return counts;
	}
	
	private int parseInteger(String string) {
		if (string == null) return 0;
		else if (string.trim().equals("")) return 0;
		else return Integer.valueOf(string);
	}
}