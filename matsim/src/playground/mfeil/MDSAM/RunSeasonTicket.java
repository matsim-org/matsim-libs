/* *********************************************************************** *
 * project: org.matsim.*
 * RunSeasonTicket.java
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

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;

import java.util.Map;

import playground.mfeil.attributes.AgentsAttributesAdder;

public class RunSeasonTicket {
	
	private static final Logger log = Logger.getLogger(RunSeasonTicket.class);

	/**
	 * Class to run AgentsAttributesAdder for seasonticket MNL
	 * @param args
	 */
	public static void main (String[]args){
		log.info("Process started...");
		
		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
		final String populationFilename = "/home/baug/mfeil/data/Zurich10/plans.xml";
		final String output_populationFilename = "/home/baug/mfeil/data/Zurich10/plans_with_seasonticket.xml.gz";
		
		final String input1 = "D:/Documents and Settings/Matthias Feil/Desktop/workspace/MATSim/plans/MobTSet_1.txt";
		final String input2 = "D:/Documents and Settings/Matthias Feil/Desktop/workspace/MATSim/plans/plans.dat";
	//	final String output1 = "D:/Documents and Settings/Matthias Feil/Desktop/workspace/MATSim/plans/choiceSet_1.dat";
	//	final String output2 = "D:/Documents and Settings/Matthias Feil/Desktop/workspace/MATSim/plans/modFile_1.mod";
		final String output3 = "D:/Documents and Settings/Matthias Feil/Desktop/workspace/MATSim/plans/probabilities.xls";
		
	/*// Biogeme code
		AgentsAttributesAdder choiceSetter = new AgentsAttributesAdder();
		ArrayList<String> ids = choiceSetter.readPlans (input2);
		choiceSetter.runMZZurich10ForBiogeme(input1, output1, ids);
		
		ModFileMaker modFiler = new ModFileMaker();
		modFiler.writeForSeasonTicket(output2);
		*/
		
		
	// Probabilities code
		AgentsAttributesAdder adder = new AgentsAttributesAdder();
		// Load ids 
		ArrayList<String> ids = adder.readPlans (input2);
		// Load probabilities
		Map<String,double[]> probabilities = adder.runMZZurich10ForProbabilities(input1, output3, ids);
		// Load scenario and population
		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
		new MatsimFacilitiesReader(scenario.getActivityFacilities()).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);
		// load income information
		adder.loadIncomeData(scenario);
		// go through every agent and assign seasonticket according to given probability or randomly if no info available
		for (Iterator<? extends Person> iterator = scenario.getPopulation().getPersons().values().iterator(); iterator.hasNext();){
			PersonImpl person = (PersonImpl) iterator.next();
			double gen = MatsimRandom.getRandom().nextDouble();
			// get income
			int income = -1;
			if (Double.parseDouble(person.getCustomAttributes().get("income").toString())/1000<4) income=0;
			else if (Double.parseDouble(person.getCustomAttributes().get("income").toString())/1000<8) income=4;
			else if (Double.parseDouble(person.getCustomAttributes().get("income").toString())/1000<30) income=8;
			else income=12;
			// transform key
			String key = person.getAge()+"_"+person.getSex()+"_"+person.getLicense()+"_"+income+"_"+person.getCarAvail();
			if (probabilities.containsKey(key)){ // assign according to information
				if (gen>=probabilities.get(key)[0] && gen<probabilities.get(key)[0]+probabilities.get(key)[1])person.getTravelcards().add("halbtax");
				else if (gen>=probabilities.get(key)[0])person.getTravelcards().add("ga");
			}
			else { // randomly chosen since no info available
				if (gen>=(1/3) && gen<(2/3))person.getTravelcards().add("halbtax");
				else if (gen>=(1/3))person.getTravelcards().add("ga");
			}
		}
		new PopulationWriter(scenario.getPopulation()).writeFile(output_populationFilename);
		
		log.info("Process finished.");
	}
}


