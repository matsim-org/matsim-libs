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

import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import java.util.TreeMap;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
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
	private Map<Id, Integer> zielperson;
	private Map<Id, Integer> haushaltseinkommen;
	private Map<Integer, double[]> averageIncome;
	

	public AgentsHighestEducationAdder() {
		this.education = new TreeMap<Id, Integer>();
		this.zielperson = new TreeMap<Id, Integer>();
		this.haushaltseinkommen = new TreeMap<Id, Integer>();
		this.averageIncome = new TreeMap<Integer, double[]>();
	}
	
	public void runHighestEducation (final String inputFile){
		
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
			log.warn(ex +" at line "+ counter);
		}*/
		try {
			FileReader file_reader = new FileReader(inputFile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// Skip header
			String curr_line = buffered_reader.readLine(); 
		//	log.info("First line = "+curr_line);
			 curr_line = buffered_reader.readLine(); 
		//	log.info("First line = "+curr_line);
			 curr_line = buffered_reader.readLine(); 
		//	log.info("First line = "+curr_line);
			while ((curr_line = buffered_reader.readLine()) != null) {
			//	log.info("Line = "+curr_line);
				String[] entries = curr_line.split(" ", -1);
			//	for (int i=0;i<entries.length;i++) log.info("String is "+entries[i]);
				// Agent_Id	Education_level	
				// 0        1       

				int id = Integer.parseInt(entries[0].trim());
				int edu = Integer.parseInt(entries[1].trim());
				education.put(new IdImpl(id), edu);
			//	if (counter%100==0) log.info("Having read line "+counter+1);
				counter++;
			}
			buffered_reader.close();
		} catch (Exception ex) {
			log.warn(ex +" at line "+ counter);
		}
		log.info("done...");
	}	
	
	public void runIncomePerEducation (final String haushalte, String zielpersonen){
		
		log.info("Reading input file "+zielpersonen+"...");
		int counter =0;
		try {

			FileReader fr = new FileReader(zielpersonen);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			StringTokenizer tokenizer = null;
			line = br.readLine(); // do not parse first line which just
									// contains column headers
			line = br.readLine();
			String tokenId = null;
			String token = null;
			 
			while (line != null) {		
				counter++;
				tokenizer = new StringTokenizer(line);				
				tokenId = tokenizer.nextToken();
				token = tokenizer.nextToken();		
				if (counter<10) log.info("1st tokenId = "+tokenId+" and token = "+token);
				this.zielperson.put(new IdImpl(tokenId), (Integer.parseInt(token)));
				
				line = br.readLine();
			}		
		} catch (Exception ex) {
			log.warn(ex +" at line "+ counter);
		}
		counter = 0;
		try {

			FileReader fr = new FileReader(haushalte);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			StringTokenizer tokenizer = null;
			line = br.readLine(); // do not parse first line which just
									// contains column headers
			line = br.readLine();
			String tokenId = null;
			String token = null;

			while (line != null) {	
				counter++;
				tokenizer = new StringTokenizer(line);				
				tokenId = tokenizer.nextToken();
				token = tokenizer.nextToken();		
				if (counter<10) log.info("2nd tokenId = "+tokenId+" and token = "+token);
				int index = Integer.parseInt(token);
				int income = -1;
				if (index == 1) income = 1000;
				else if (index == 2) income = 3000;
				else if (index == 3) income = 5000;
				else if (index == 4) income = 7000;
				else if (index == 5) income = 9000;
				else if (index == 6) income = 11000;
				else if (index == 7) income = 13000;
				else if (index == 8) income = 15000;
				else if (index == 9) income = 17000;
				else if (index==-97 || index==-99); // do nothing
				else log.warn("No valid income found for household "+tokenId);
				this.haushaltseinkommen.put(new IdImpl(tokenId), income);
				
				line = br.readLine();
			}		
		} catch (Exception ex) {
			log.warn(ex +" at line "+ counter);
		}
		this.averageIncome = new TreeMap<Integer, double[]>();
		int countTotal = 0;
		double inTotal = 0;
		int countFailure = 0;
		for (int i=1;i<10;i++){
			if (i==9){
				this.averageIncome.put(i, new double[]{0,0}); // {education type's average income, difference with overall's average income}
				log.info("No income information available for type "+i);
				continue;
			}
			int count=0;
			double in=0;
			for (Iterator<Id> iterator = this.zielperson.keySet().iterator(); iterator.hasNext();){
				Id id = iterator.next();
				int value = this.zielperson.get(id);
				if (value==i){
					if (this.haushaltseinkommen.containsKey(id)){
						count++;
						countTotal++;
						in+=this.haushaltseinkommen.get(id);
						inTotal+=this.haushaltseinkommen.get(id);
					}
					else countFailure++;
				}
			}
			this.averageIncome.put(i, new double[]{in/count,-99}); // {education type's average income, difference with overall's average income}
			log.info("Average income for type "+i+" is "+this.averageIncome.get(i)[0]);
		}
		this.averageIncome.put(99, new double[]{inTotal/countTotal,0}); // {overall's average income, 0}
		for (int i=1;i<9;i++){
			this.averageIncome.get(i)[1]=this.averageIncome.get(i)[0]-(inTotal/countTotal);
			log.info("Income difference for type "+i+" is "+this.averageIncome.get(i)[1]);
		}
		log.info(countFailure+" failures to match income and education.");
		log.info("done...");
	}	
	
	
	public Map<Id, Integer> getEducation (){
		log.info("Length of education map is = "+this.education.size());
		return this.education;
	}
	public Map<Integer, double[]> getIncomePerEducation (){
		return this.averageIncome;
	}
}

