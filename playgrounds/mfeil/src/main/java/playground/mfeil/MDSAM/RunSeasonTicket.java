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
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;

import playground.mfeil.AgentsAttributesAdder;

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

		final String input1 = "/home/baug/mfeil/data/Zurich10/MobTSet_1.txt";
		final String input2 = "/home/baug/mfeil/data/Zurich10/Zurich10_ids.dat";
	//	final String output1 = "D:/Documents and Settings/Matthias Feil/Desktop/workspace/MATSim/plans/choiceSet_1.dat";
	//	final String output2 = "D:/Documents and Settings/Matthias Feil/Desktop/workspace/MATSim/plans/modFile_1.mod";
		final String output3 = "/home/baug/mfeil/data/Zurich10/probabilities_10_rec.xls";

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
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		new MatsimFacilitiesReader(scenario).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);
		// load income information
		adder.loadIncomeData(scenario);

		// go through every agent and assign seasonticket according to given probability or randomly if no info available
		int count =0;
		int nothing=0;
		int ht = 0;
		int ga = 0;
		int nokey=0;
		int withkey=0;
		for (Iterator<? extends Person> iterator = scenario.getPopulation().getPersons().values().iterator(); iterator.hasNext();){
			PersonImpl person = (PersonImpl) iterator.next();
			count++;
			if (person.getTravelcards()!=null) person.getTravelcards().clear();
			double gen = MatsimRandom.getRandom().nextDouble();
			if (count<10) log.info("personId = "+person.getId()+ " und gen = "+gen);

			// transform key
			// Age
			int age = -1;
			if (person.getAge()<10) age=0;
			else if (person.getAge()<20) age=10;
			else if (person.getAge()<30) age=20;
			else if (person.getAge()<40) age=30;
			else if (person.getAge()<50) age=40;
			else if (person.getAge()<60) age=50;
			else if (person.getAge()<70) age=60;
			else age = 70;

			//Gender
			int gender = -1;
			if (person.getSex().equals("m")) gender=1;
			else gender=0;

			// License
			int license = -1;
			if (person.getLicense().equals("yes")) license=1;
			else license=0;

			// Income
			int income = -1;
			if (Double.parseDouble(person.getCustomAttributes().get("income").toString())/1000<4) income=0;
			else if (Double.parseDouble(person.getCustomAttributes().get("income").toString())/1000<8) income=4;
			else if (Double.parseDouble(person.getCustomAttributes().get("income").toString())/1000<30) income=8;
			else income=12;
			if (count<10) log.info("income = "+income);

			// CarAvail
			int carAvail = -1;
			if (person.getCarAvail().equals("never")) carAvail=3;
			else if (person.getCarAvail().equals("always")) carAvail=1;
			else carAvail=2;

			String key = age+"_"+gender+"_"+license+"_"+income+"_"+carAvail;
			if (count<10) log.info("key = "+key);
			if (probabilities.containsKey(key) && probabilities.get(key)[3]>1.0){ // assign according to information, add nothing if no ticket to assign
				withkey++;
				if (gen>=probabilities.get(key)[0] && gen<probabilities.get(key)[0]+probabilities.get(key)[1]){
					if (count<10) log.info("adding ht with key");
					person.addTravelcard("ch-HT");
					ht++;
				}
				else if (gen>=probabilities.get(key)[0]){
					if (count<10) log.info("adding ga with key");
					person.addTravelcard("ch-GA");
					ga++;
				}
				else {
					if (count<10) log.info("adding nothing with key");
					nothing++;
				}
			}
			else { // randomly chosen since no info available, add nothing if no ticket to assign
				nokey++;
				if (gen>=probabilities.get("total")[0] && gen<probabilities.get("total")[0]+probabilities.get("total")[1]){
					if (count<10) log.info("adding ht without key");
					person.addTravelcard("ch-HT");
					ht++;
				}
				else if (gen>=probabilities.get("total")[0]){
					if (count<10) log.info("adding ga without key");
					person.addTravelcard("ch-GA");
					ga++;
				}
				else {
					if (count<10) log.info("adding nothing without key");
					nothing++;
				}
			}
		}
		log.info("No of assignments: "+nothing+" agents without any card, "+ht+" agents with Halbtax or similar, and "+ga+" agents with GA.");
		log.info("No of assignments with key = "+withkey+", and without key "+nokey+".");

		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(), scenario.getKnowledges()).write(output_populationFilename);

		log.info("Process finished.");
	}
}


