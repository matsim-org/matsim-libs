/* *********************************************************************** *
 * project: org.matsim.*
 * Emme2CountsFileParser.java
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

/**
 * <p>
 * Class to parse results from the EMME/2 model containing data for AM, OP and PM time periods.
 * </p>
 * <p>
 * The following columns are expected in the file:<br>
 * 0 ... i-node<br>
 * 1 ... j-node<br>
 * 2 ... length<br>
 * 3 ... modes<br>
 * 4 ... type<br>
 * 5 ... lanes<br>
 * 6 ... VDF<br>
 * 7 ... AM_volume<br>
 * 8 ... OP_volume<br>
 * 9 ... PM_volume<br>
 * </p>
 * 
 * @author cdobler
 */
public class Emme2CountsFileParser {

	private final String separator;
	
	public Emme2CountsFileParser(String separator) {
		this.separator = separator;
	}
	
	public List<Emme2Count> readFile(String inFile) throws Exception {
		List<Emme2Count> counts = new ArrayList<Emme2Count>();
	    
		BufferedReader br = IOUtils.getBufferedReader(inFile);
		
	    // skip first Line
	    br.readLine();
	    
	    String line;
	    while((line = br.readLine()) != null) {
	    	Emme2Count emme2Count = new Emme2Count();
	    	
	    	String[] cols = line.split(separator);
	    	
	    	emme2Count.inode = parseInteger(cols[0]);
	    	emme2Count.jnode = parseInteger(cols[1]);
	    	emme2Count.amVolume = parseInteger(cols[7]);
	    	emme2Count.opVolume = parseInteger(cols[8]);
	    	emme2Count.pmVolume = parseInteger(cols[9]);
	    	
	    	counts.add(emme2Count);
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