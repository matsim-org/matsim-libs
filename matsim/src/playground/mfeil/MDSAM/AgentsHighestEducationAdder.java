/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsHighestEducationAdder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mfeil.MDSAM;

import java.util.Map;
import java.util.TreeMap;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;



/**
 * Reads Zurich 10% diluted agents' highest education from a given *.txt file.
 *
 * @author mfeil
 */
public class AgentsHighestEducationAdder {

	private static final Logger log = Logger.getLogger(AgentsAttributesAdder.class);
	private Map<Id, Integer> education;
	

	public AgentsHighestEducationAdder() {
		this.education = new TreeMap<Id, Integer>();
	}
	
	public void run (final String inputFile){
		
		log.info("Reading input file "+inputFile+"...");
		
		try {

			FileReader fr = new FileReader(inputFile);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			StringTokenizer tokenizer = null;
			line = br.readLine(); // do not parse first line which just
									// contains column headers
			line = br.readLine();
			String tokenId = null;
			String token = null;
			while (line != null) {		
				
				tokenizer = new StringTokenizer(line);
				
				tokenId = tokenizer.nextToken();
				
				// Watch out that the order is equal to the order in the file!
				token = tokenizer.nextToken();				
				education.put(new IdImpl(tokenId), (int)(Double.parseDouble(token)));
				
				line = br.readLine();
			}		
		} catch (Exception ex) {
			log.warn(ex);
		}
		log.info("done...");
	}	
	
	public Map<Id, Integer> getEducation (){
		return this.education;
	}
}

