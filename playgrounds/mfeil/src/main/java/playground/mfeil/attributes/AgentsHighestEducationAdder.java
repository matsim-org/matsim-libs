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

package playground.mfeil.attributes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.mfeil.AgentsAttributesAdder;



/**
 * Reads Zurich 10% diluted agents' highest education from a given *.txt file.
 *
 * @author mfeil
 */
public class AgentsHighestEducationAdder {

	private static final Logger log = Logger.getLogger(AgentsAttributesAdder.class);
	private Map<Id, Integer> education;
	private Map<Id, Integer> zielperson; //<Id, Education>
	private Map<Id, double[]> haushaltseinkommen; // <Id, {Income,weight}>
	private Map<Integer, double[]> averageIncome; //
	

	public AgentsHighestEducationAdder() {
		this.education = new TreeMap<Id, Integer>();
		this.zielperson = new TreeMap<Id, Integer>();
		this.haushaltseinkommen = new TreeMap<Id, double[]>();
		this.averageIncome = new TreeMap<Integer, double[]>();
	}
	
	public void runHighestEducation (final String inputFile){
		
		log.info("Reading input file "+inputFile+"...");
		int counter =0;
		
		try {
			FileReader file_reader = new FileReader(inputFile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// Skip header
			String curr_line = buffered_reader.readLine(); 
			curr_line = buffered_reader.readLine(); 
			curr_line = buffered_reader.readLine(); 
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split(" ", -1);
		
				// Agent_Id	Education_level	
				// 0        1       

				int id = Integer.parseInt(entries[0].trim());
				int edu = Integer.parseInt(entries[1].trim());
				education.put(new IdImpl(id), edu);
				counter++;
			}
			buffered_reader.close();
		} catch (Exception ex) {
			log.warn(ex +" at line "+ counter);
		}
		log.info("done...");
	}	
	
	public void runIncomePerEducation (final String haushalte, String zielpersonen){
		
		log.info("Reading input file \"zielpersonen\"...");
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
				this.zielperson.put(new IdImpl(tokenId), (Integer.parseInt(token)));
				
				line = br.readLine();
			}		
		} catch (Exception ex) {
			log.warn(ex +" at line "+ counter);
		}
		log.info("done.");
		log.info("Reading input file \"haushalte\"...");
		counter = 0;
		int citycentrecountIn = 0;
		int citycentrecountOut = 0;
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
			double weight = -1;
			double xcor = -1;
			double ycor = -1;
			
			while (line != null) {	
				counter++;
				tokenizer = new StringTokenizer(line);				
				tokenId = tokenizer.nextToken(); // id
				weight = Double.parseDouble(tokenizer.nextToken()); // weight
				xcor = Double.parseDouble(tokenizer.nextToken()); // xcor
				ycor = Double.parseDouble(tokenizer.nextToken()); // ycor
				
				/*
				// exclude non-Zurich households
				if (CoordUtils.calcDistance(new CoordImpl(xcor, ycor), new CoordImpl(683518.0,246836.0))>30000){
					line = br.readLine();
					citycentrecountOut++;
					continue;
				}
				*/
				citycentrecountIn++;
				
				token = tokenizer.nextToken();	 // income
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
				
				this.haushaltseinkommen.put(new IdImpl(tokenId), new double[]{income,weight});
				line = br.readLine();
			}		
		} catch (Exception ex) {
			log.warn(ex +" at line "+ counter);
		}
		log.info("done.");
		
		log.info("Calculationg average income per education...");
		this.averageIncome = new TreeMap<Integer, double[]>(); // Map with 9 keys (per every education type), and key 99 for overall average income
		double countTotal = 0;
		double inTotal = 0;
		int countFailure = 0;
		
		// Go through all 9 education types
		for (int i=1;i<10;i++){
			if (i==9){
				this.averageIncome.put(i, new double[]{0,0}); // {education type's average income, difference with overall's average income}
				log.info("No income information available for type "+i);
				continue;
			}
			double count=0;
			double in=0;
			// Go through all persons
			for (Iterator<Id> iterator = this.zielperson.keySet().iterator(); iterator.hasNext();){
				Id id = iterator.next();
				int value = this.zielperson.get(id);
				if (value==i){
					if (this.haushaltseinkommen.containsKey(id)){
						count+=this.haushaltseinkommen.get(id)[1];
						countTotal+=this.haushaltseinkommen.get(id)[1];
						in+=this.haushaltseinkommen.get(id)[0];
						inTotal+=this.haushaltseinkommen.get(id)[0];
					}
					else countFailure++;
				}
			}
			this.averageIncome.put(i, new double[]{in/count,-99}); // {education type's average income, difference with overall's average income}
			log.info("Average income for type "+i+" is "+this.averageIncome.get(i)[0]+", s.t. "+count+" MZ persons.");
		}
		this.averageIncome.put(99, new double[]{inTotal/countTotal,0}); // {overall's average income, 0}
		log.info("Overall average income is "+(inTotal/countTotal));
		
		for (int i=1;i<9;i++){
			this.averageIncome.get(i)[1]=this.averageIncome.get(i)[0]-(inTotal/countTotal);
			log.info("Income difference for type "+i+" is "+this.averageIncome.get(i)[1]);
		}
		log.info(this.zielperson.size()+" Zielpersonen and "+(citycentrecountIn+citycentrecountOut)+" Haushalte in the scenario ("+citycentrecountIn+" within 30km circle, and "+citycentrecountOut+" outside). "+countFailure+" failures to match income and education.");
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

