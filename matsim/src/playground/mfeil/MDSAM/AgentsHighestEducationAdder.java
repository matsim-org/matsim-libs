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
import java.io.IOException;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.world.Location;
import org.matsim.world.Zone;

import playground.balmermi.census2000.data.Municipality;



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
		int counter =0;
		/*try {

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
				education.put(new IdImpl(tokenId), (Integer.parseInt(token)));
				
				line = br.readLine();
				if (counter%100==0) log.info("Having read line "+counter+1);
				counter++;
			}		
		} catch (Exception ex) {
			log.warn(ex +"at line"+ counter);
		}*/
		try {
			FileReader file_reader = new FileReader(inputFile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// Skip header
			String curr_line = buffered_reader.readLine(); 
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split(" ", -1);

				// Agent_Id	Education_level	
				// 0        1       

				int id = Integer.parseInt(entries[0].trim());
				int edu = Integer.parseInt(entries[1].trim());
				education.put(new IdImpl(id), edu);
				if (counter%100==0) log.info("Having read line "+counter+1);
				counter++;
			}
			buffered_reader.close();
		} catch (Exception ex) {
			log.warn(ex +"at line"+ counter);
		}
		log.info("done...");
	}	
	
	public Map<Id, Integer> getEducation (){
		log.info("Length of education map is = "+this.education.size());
		return this.education;
	}
}

