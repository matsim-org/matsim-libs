/* *********************************************************************** *
 * project: org.matsim.*
 * IncomeAdder.java
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
 * Reads agents attributes from a given *.txt file.
 *
 * @author mfeil
 */
public class AgentsAttributesAdder {

	private static final Logger log = Logger.getLogger(AgentsAttributesAdder.class);
	private Map<Id, Integer> income;
	private Map<Id, Integer> carAvail;
	private Map<Id, Integer> seasonTicket;	


	public AgentsAttributesAdder() {
		this.income = new TreeMap<Id, Integer>();
		this.carAvail = new TreeMap<Id, Integer>();
		this.seasonTicket = new TreeMap<Id, Integer>();
	}
	
	public void run (final String inputFile){
		
		log.info("Reading input file...");
		
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
				income.put(new IdImpl(tokenId), (int)(Double.parseDouble(token)*1000));
				
				token = tokenizer.nextToken();				
				carAvail.put(new IdImpl(tokenId), (int)(Double.parseDouble(token)));
				
				token = tokenizer.nextToken();				
				seasonTicket.put(new IdImpl(tokenId), (int)(Double.parseDouble(token)));
				
				line = br.readLine();
			}		
		} catch (Exception ex) {
			System.out.println(ex);
		}
		log.info("done...");
	}	
	
	public Map<Id, Integer> getIncome (){
		return this.income;
	}
	
	public Map<Id, Integer> getCarAvail (){
		return this.carAvail;
	}
	
	public Map<Id, Integer> getSeasonTicket (){
		return this.seasonTicket;
	}
}

