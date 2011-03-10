/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsAttributesAdder.java
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

package playground.mfeil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;

import playground.mfeil.attributes.AgentsHighestEducationAdder;



/**
 * Reads agents attributes from a given *.txt file.
 *
 * @author mfeil
 */
public class AgentsAttributesAdder {

	private static final Logger log = Logger.getLogger(AgentsHighestEducationAdder.class);
	private Map<Id, Double> income;
	private Map<Id, Integer> carAvail;
	private Map<Id, Integer> seasonTicket;	
	private Map<Id, Double> agentsWeight;
	private Map<Id, Integer> munType;
	public static double AVERAGE_INCOME;
	


	public AgentsAttributesAdder() {
		this.income = new TreeMap<Id, Double>();
		this.carAvail = new TreeMap<Id, Integer>();
		this.seasonTicket = new TreeMap<Id, Integer>();
		this.agentsWeight = new TreeMap<Id, Double> ();
		this.munType = new TreeMap<Id, Integer> ();
	}
	
	public static void main (String[]args){
		final String input1 = "D:/Documents and Settings/Matthias Feil/Desktop/workspace/MATSim/plans/MobTSet_1.txt";
		final String input2 = "D:/Documents and Settings/Matthias Feil/Desktop/workspace/MATSim/plans/plans.dat";
		final String output = "D:/Documents and Settings/Matthias Feil/Desktop/workspace/MATSim/plans/output.txt";
		
		ArrayList<String> ids = new AgentsAttributesAdder().readPlans (input2);
		new AgentsAttributesAdder().runMZZurich10(input1, output, ids);
	}
	
	
	/** Reads the Ids of the Zurich10 agents from a Biogeme estimation data file
	 * @param input2
	 * @return
	 */
	public ArrayList<String> readPlans (final String input2){
		log.info("Reading input2 file...");
		ArrayList<String> ids = new ArrayList<String>();
		try {

			FileReader fr = new FileReader(input2);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			StringTokenizer tokenizer = null;
			line = br.readLine(); // do not parse first line which just
									// contains column headers
			line = br.readLine();
			String tokenId = null;
			while (line != null) {		
				
				tokenizer = new StringTokenizer(line);
				
				tokenId = tokenizer.nextToken();
				ids.add(tokenId);
				
				line = br.readLine();
			}		
		} catch (Exception ex) {
			System.out.println(ex);
		}
		log.info("done...");
		return ids;
	}
	
	
	/** Reads agents' attributes for the selected Ids of readPlans() from the MobTSet_1 file so that further 
	 * analysis can be conducted manually from the resulting excel file 
	 * @param inputFile
	 * @param outputFile
	 * @param ids
	 */
	public void runMZZurich10 (final String inputFile, final String outputFile, final ArrayList<String> ids){
		
		log.info("Reading input1 file...");
		
		String outputfile = outputFile;
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputfile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		stream.println("Agent_id\tweight\tage\tgender\tlicense\tincome_simulated\tinc_4\tinc_4_8\tincome_8_12\tincome_12_on\tincome_clustered\tcar_avail");					
		try {

			FileReader fr = new FileReader(inputFile);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			StringTokenizer tokenizer = null;
			line = br.readLine(); // do not parse first line which just
									// contains column headers
			line = br.readLine();
			String tokenId = null;
			String token1 = null;
			String token2 = null;
			String token3 = null;
			String token4 = null;
			while (line != null) {
				tokenizer = new StringTokenizer(line);
				
				tokenId = tokenizer.nextToken();
				if (!ids.contains(tokenId)) {
					line = br.readLine();
					continue;
				}
								
				// Watch out that the order is equal to the order in the file!
				stream.print(tokenId+"\t"+tokenizer.nextToken()+"\t"+tokenizer.nextToken()+"\t"+tokenizer.nextToken()+"\t"+tokenizer.nextToken()+"\t");					
				tokenizer.nextToken();				
				stream.print(tokenizer.nextToken()+"\t");
				
				int income = 0;
				token1 = tokenizer.nextToken();
				token2 = tokenizer.nextToken();
				token3 = tokenizer.nextToken();
				token4 = tokenizer.nextToken();
				if (token1.equals("1")) income = 2000;
				else if (token2.equals("1")) income = 6000;
				else if (token3.equals("1")) income = 10000;
				else if (token4.equals("1")) income = 16000;
				else log.warn("For agent "+tokenId+", no valid income could be detected!");
				stream.print(token1+"\t"+token2+"\t"+token3+"\t"+token4+"\t"+income+"\t");		
				
				for (int i=0;i<3;i++) tokenizer.nextToken();		
				stream.println(tokenizer.nextToken());
				
				line = br.readLine();
			}		
		} catch (Exception ex) {
			System.out.println(ex);
		}
		log.info("done...");
	}	
	
	/** Reads some Biogeme-estimation attributes for the selected agent ids from the MobTSet_1 file and writes the
	 * Biogeme-compatible choice set file
	 * @param inputFile
	 * @param outputFile
	 * @param ids
	 */
	public void runMZZurich10ForBiogeme (final String inputFile, final String outputFile, final ArrayList<String> ids){
		
		log.info("Starting Biogeme compilation...");
		
		String outputfile = outputFile;
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputfile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		stream.println("Id\tChoice\tAge\tGender\tLicense\tIncome\tCar_always\tCar_sometimes\tav1\tav2\tav3");					
		try {

			FileReader fr = new FileReader(inputFile);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			StringTokenizer tokenizer = null;
			line = br.readLine(); // do not parse first line which just
									// contains column headers
			line = br.readLine();
			String tokenId = null;
			
			while (line != null) {
				tokenizer = new StringTokenizer(line);
				
				tokenId = tokenizer.nextToken();
				if (!ids.contains(tokenId)) {
					line = br.readLine();
					continue;
				}
								
				// Id
				stream.print(tokenId+"\t");
				tokenizer.nextToken();
				
				// Age, gender, license
				String age = tokenizer.nextToken();
				String gender = tokenizer.nextToken();
				String license = tokenizer.nextToken();	
				tokenizer.nextToken();
				
				// Income	
				String income = tokenizer.nextToken();
				for (int i=0;i<7;i++) tokenizer.nextToken();
				
				// Car Avail	
				int carAlways = 0;
				int carSometimes = 0;
				String carAvail = tokenizer.nextToken();
				if (carAvail.equals("1")) carAlways = 1;
				else if (carAvail.equals("2")) carSometimes = 1;
				for (int i=0;i<11;i++) tokenizer.nextToken();
				
				String ticket = tokenizer.nextToken();
				int choice = 0;
				if (ticket.equals("2") || ticket.equals("3")) choice = 3;
				else if (ticket.equals("11")) choice = 1;
				else choice = 2;
				
				stream.println(choice+"\t"+age+"\t"+gender+"\t"+license+"\t"+income+"\t"+carAlways+"\t"+carSometimes+"\t1\t1\t1");		
				
				line = br.readLine();
			}		
		} catch (Exception ex) {
			System.out.println(ex);
		}
		log.info("done...");
	}	
	
	/** Calculates the probabilities for the seasonticket attribute according to other agent attributes
	 * @param inputFile
	 * @param outputFile
	 * @param ids
	 * @return
	 */
	public Map<String,double[]> runMZZurich10ForProbabilities (final String inputFile, final String outputFile, final ArrayList<String> ids){
		
		log.info("Starting probabilities calculation...");
		
		String outputfile = outputFile;
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputfile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		stream.println("Age\tGender\tLicense\tIncome\tCarAvail\tNothing\tHalbtax\tGA");		
		
		Map<String,double[]> probabilities = new TreeMap<String,double[]>();
		int count=0;
		try {

			FileReader fr = new FileReader(inputFile);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			StringTokenizer tokenizer = null;
			line = br.readLine(); // do not parse first line which just
									// contains column headers
			line = br.readLine();
			String tokenId = null;
			
			while (line != null) {
				count++;
				tokenizer = new StringTokenizer(line);
				
				tokenId = tokenizer.nextToken();
				if (!ids.contains(tokenId)) {
					line = br.readLine();
					continue;
				}
								
				int[] key = new int[5];
				// Weight
				double weight = Double.parseDouble(tokenizer.nextToken());
				
				// Age, 
				int ageIn = Integer.parseInt(tokenizer.nextToken());
				if (ageIn<10) key[0]=0;
				else if (ageIn<20) key[0]=10;
				else if (ageIn<30) key[0]=20;
				else if (ageIn<40) key[0]=30;
				else if (ageIn<50) key[0]=40;
				else if (ageIn<60) key[0]=50;
				else if (ageIn<70) key[0]=60;
				else key[0] = 70;
				
				// Gender, license
				key[1] = Integer.parseInt(tokenizer.nextToken());
				key[2] = Integer.parseInt(tokenizer.nextToken());
				tokenizer.nextToken();
				
				// Income	
				double incomeIn = Double.parseDouble(tokenizer.nextToken());
				if (incomeIn<4) key[3]=0;
				else if (incomeIn<8) key[3]=4;
				else if (incomeIn<12) key[3]=8;
				else key[3]=12;
				
				for (int i=0;i<7;i++) tokenizer.nextToken();
				
				// Car Avail	
				key[4] = Integer.parseInt(tokenizer.nextToken());
				for (int i=0;i<11;i++) tokenizer.nextToken();
				
				// Ticket
				int ticketIn = Integer.parseInt(tokenizer.nextToken());
				int ticket = -1;
				if (ticketIn==2 || ticketIn==3) ticket = 3;
				else if (ticketIn==11) ticket = 1;
				else ticket = 2;
			
				// Create classes of same agent types
				String index = key[0]+"_"+key[1]+"_"+key[2]+"_"+key[3]+"_"+key[4];
				if (probabilities.containsKey(index)){
					if (ticket==1) probabilities.get(index)[0]+=weight;
					else if (ticket==2) probabilities.get(index)[1]+=weight;
					else if (ticket==3) probabilities.get(index)[2]+=weight;
					else log.warn("Something going wrong for the ticket documentation!");
					probabilities.get(index)[3]+=1.0;
				}
				else {
					if (ticket==1) probabilities.put(index,new double[]{weight,0,0,1.0});
					else if (ticket==2) probabilities.put(index,new double[]{0,weight,0,1.0});
					else if (ticket==3) probabilities.put(index,new double[]{0,0,weight,1.0});
					else log.warn("Something going wrong for the ticket documentation!");
				}	
				
				line = br.readLine();
			}		
			
			// now go through all agent types and calculate their seasonticket probabilities
			double nothingTotal = 0;
			double htTotal = 0;
			double gaTotal = 0;
			double total = 0;
			for (Iterator<String> iterator = probabilities.keySet().iterator(); iterator.hasNext();){
				String id = iterator.next();
				String[] entries = id.split("_", -1);
				
				stream.println(entries[0]+"\t"+entries[1]+"\t"+entries[2]+"\t"+entries[3]+"\t"+entries[4]+"\t"+probabilities.get(id)[0]+"\t"+probabilities.get(id)[1]+"\t"+probabilities.get(id)[2]+"\t");
				double nothing = probabilities.get(id)[0];
				nothingTotal +=nothing;
				double ht = probabilities.get(id)[1];
				htTotal += ht; 
				double ga = probabilities.get(id)[2];
				gaTotal += ga;
				double sum = nothing + ht + ga;
				total += sum;
				
				probabilities.get(id)[0]=nothing/sum;
				probabilities.get(id)[1]=ht/sum;
				probabilities.get(id)[2]=ga/sum;
			}
		probabilities.put("total", new double[]{nothingTotal/total, htTotal/total, gaTotal/total});
						
		} catch (Exception ex) {
			System.out.println(ex+" at count "+count);
		}
		log.info("done...");
		return probabilities;
	}	
	
	/** Reads the agent attributes from attributs_MZ2005.txt (from PlansConstructor) as requested by PlansConstructor
	 * @param inputFile
	 */
	public void runMZ (final String inputFile){
		
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
				this.income.put(new IdImpl(tokenId), Double.parseDouble(token)*1000);
				
				token = tokenizer.nextToken();				
				this.carAvail.put(new IdImpl(tokenId), Integer.parseInt(token));
				
				token = tokenizer.nextToken();				
				this.seasonTicket.put(new IdImpl(tokenId), Integer.parseInt(token));
				
				token = tokenizer.nextToken();				
				this.agentsWeight.put(new IdImpl(tokenId), Double.parseDouble(token));
				
				token = tokenizer.nextToken();				
				this.munType.put(new IdImpl(tokenId), Integer.parseInt(token));
				
				line = br.readLine();
			}		
		} catch (Exception ex) {
			System.out.println(ex);
		}
		log.info("done...");
	}	
	
	
	/** Adds income information to all agents of a scenario
	 * 
	 * @param scenario
	 */
		
	public void loadIncomeData(ScenarioImpl scenario){
		log.info("   adding agents income data...");
		
		this.runZurich10("/home/baug/mfeil/data/Zurich10/agents_income_MZoverall_weighted_wo_sums.txt");
		if (this.income.isEmpty()) {
			log.warn("No income loaded!");
			return; // No income loaded
		}
		double averageIncome = 0;
		int personCounter = 0;
		for (Iterator<? extends Person> iterator = scenario.getPopulation().getPersons().values().iterator(); iterator.hasNext();){
			PersonImpl person = (PersonImpl) iterator.next();
			try{
				double income = this.getIncome().get(person.getId());
				averageIncome+=income;
				personCounter++;
				person.getCustomAttributes().put("income", income);
			} catch (Exception e) {
				log.warn("No income information found for agent "+person.getId());
			}
			try{
				person.getCustomAttributes().put("municipality", this.getMunType().get(person.getId()));
			} catch (Exception e) {
				log.warn("No munType information found for agent "+person.getId());
			}
		}	
		if (personCounter!=0) {
			AgentsAttributesAdder.AVERAGE_INCOME = averageIncome/personCounter;
			log.info("   ... done. Average income is "+AgentsAttributesAdder.AVERAGE_INCOME+".");
		}
		else log.warn("... done, but could not calculate average income!");
	}
	
	
	/** Conducts the income data collection from a corresponding file for the method loadIncomeData()
	 * 
	 * @param inputFile
	 */
	public void runZurich10 (final String inputFile){
		
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
				tokenizer.nextToken(); // jump over irrelevant information
				token = tokenizer.nextToken();		
				this.munType.put(new IdImpl(tokenId), Integer.parseInt(token));
				
				for (int i=0;i<4;i++) tokenizer.nextToken(); // jump over irrelevant information
				token = tokenizer.nextToken();		
				this.income.put(new IdImpl(tokenId), Double.parseDouble(token));
				
				line = br.readLine();
			}		
		} catch (Exception ex) {
			log.warn(ex+". No income will be loaded.");
		}
		log.info("done...");
	}	
	
	
	// get methods
	public Map<Id, Double> getIncome (){
		return this.income;
	}
	
	public Map<Id, Integer> getCarAvail (){
		return this.carAvail;
	}
	
	public Map<Id, Integer> getSeasonTicket (){
		return this.seasonTicket;
	}
	public Map<Id, Integer> getMunType (){
		return this.munType;
	}
	public Map<Id, Double> getAgentsWeight (){
		return this.agentsWeight;
	}
}

