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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.api.basic.v01.population.PlanElement;

import playground.mfeil.analysis.ASPActivityChainsModesAccumulated;
import playground.mfeil.PersonWithIncomeImpl;


/**
 * Adds the agents' hh income to an existing plans file.
 *
 * @author mfeil
 */
public class IncomeAdder {

	protected final PopulationImpl population;
	protected static final Logger log = Logger.getLogger(IncomeAdder.class);
	


	private IncomeAdder(final PopulationImpl population) {
		this.population = population;
	}
	
	private void run (final String inputFile, final String outputFile){
		
		log.info("Reading input file...");
		Map<Id, Integer> income = new TreeMap<Id, Integer>();
		
		try {

			FileReader fr = new FileReader(inputFile);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			StringTokenizer tokenizer = null;
			line = br.readLine(); // do not parse first line which just
									// contains column headers
			line = br.readLine();
			String tokenId = null;
			String tokenIncome = null;
			while (line != null) {		
				
				tokenizer = new StringTokenizer(line);
				
				tokenId = tokenizer.nextToken();
				tokenIncome = tokenizer.nextToken();
				
				income.put(new IdImpl(tokenId), (int)(Double.parseDouble(tokenIncome)));
			}		
		} catch (Exception ex) {
			System.out.println(ex);
		}
		log.info("done...");
		
		log.info("Connecting persons with income...");
		for (Iterator<PersonImpl> iterator = this.population.getPersons().values().iterator(); iterator.hasNext();){
			PersonWithIncomeImpl person = (PersonWithIncomeImpl) iterator.next();
			
			try {
				person.setHhIncome(income.get(person.getId()));
			} catch (Exception e){
				log.warn("Person of plans file not found in input file!");
			}
		
		}		
		log.info("done.");
		
		log.info("Writing plans...");
		new PopulationWriter(this.population, outputFile).write();
		log.info("done.");
	}	
	

	public static void main(final String [] args) {
		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
		final String populationFilename = "/home/baug/mfeil/data/mz/plans.xml";
	
		final String outputFile = "/home/baug/mfeil/data/mz/plans_income.xml.gz";
		final String inputFile = "/home/baug/mfeil/data/mz/MobSet_Income_only.txt";
		
		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
		new MatsimFacilitiesReader(scenario.getActivityFacilities()).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);
		
		IncomeAdder sp = new IncomeAdder(scenario.getPopulation());
		sp.run(inputFile, outputFile);
	}

}

