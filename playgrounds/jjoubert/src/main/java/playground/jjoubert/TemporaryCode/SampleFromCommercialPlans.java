/* *********************************************************************** *
 * project: org.matsim.*
 * MergeCommercialPlans.java
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

package playground.jjoubert.TemporaryCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class SampleFromCommercialPlans {
	private static Logger log = Logger.getLogger(SampleFromCommercialPlans.class);

	/**
	 * Class to read previously generated commercial vehicle plans files, and
	 * create a single new plans file with a given number of agents, starting
	 * their Ids at a given value.
	 * @param args The following arguments are required:
	 * <ol>
	 * <li> the root where the original plans files are; knowing they are of 
	 * 		the form <code>plansGauteng5000_SampleX.xml</code> where <code>
	 * 		X</code> can be any value 1-10.
	 * <li> the number of plans required.
	 * <li> the absolute path of the final plans file.
	 * </ol>
	 */
	public static void main(String[] args) {
		String root = null;
		String number = null;
		String firstId = null;
		String networkFile = null;
		String output = null;
		String carFile = null;
		String finalFile = null;
		if(args.length == 5){
			root = args[0];
			number = args[1];
			firstId = args[2];
			networkFile = args[3];
			output = args[4];
		} else if(args.length == 7){
			root = args[0];
			number = args[1];
			firstId = args[2];
			networkFile = args[3];
			output = args[4];
			carFile = args[5];
			finalFile = args[6];
		} else{
			throw new IllegalArgumentException("Incorrect number of arguments.");
		}
		
		Scenario sNew = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory pf = sNew.getPopulation().getFactory();
		// Network.
		NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(sNew.getNetwork());
		nr.readFile(networkFile);
		XY2Links xy = new XY2Links(sNew.getNetwork(), null);

		int id = Integer.parseInt(firstId);
		
		log.info("Reading commercial vehicle plans files from " + root);
		List<Scenario> listSc = new ArrayList<Scenario>(10);
		for(int i = 1; i <= 10; i++){
			String filename = root + "plansGauteng5000_Sample" + i + ".xml";
			Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			PopulationReader pr = new PopulationReader(s);
			pr.readFile(filename);
			listSc.add(s);
		}
		
		Random r =  MatsimRandom.getRandom();
		
		log.info("Create " + number + " commercial vehicle agents.");
		int counter = 0;
		while(counter < Integer.parseInt(number)){
			// Pick
			int rList = (int) Math.round(r.nextDouble()*9);
			
			List<Id<Person>> agentIds = new ArrayList<>();
			Set<Id<Person>> Ids = listSc.get(rList).getPopulation().getPersons().keySet();
			for (Id<Person> id2 : Ids) {
				agentIds.add(id2);
			}
			int rId = (int) Math.floor(r.nextDouble()*agentIds.size());
			
			Person p = pf.createPerson(Id.create(id, Person.class));
			Plan plan = listSc.get(rList).getPopulation().getPersons().get(agentIds.get(rId)).getSelectedPlan();
			xy.run(plan);
			p.addPlan(plan);
			sNew.getPopulation().addPerson(p);
			counter++;
			id++;
		}
		log.info("Created " + counter + " commercial vehicles (Done)");
		
		// Check that no Ids are duplicated.
		List<Id<Person>> listIds = new ArrayList<>();
		for(Id<Person> i : sNew.getPopulation().getPersons().keySet()){
			if(listIds.contains(i)){
				log.error("The Id " + i.toString() + " already exists!");
			} else{
				listIds.add(i);
			}
		}
		
		
		// Now write the final population.
		
		PopulationWriter pw = new PopulationWriter(sNew.getPopulation(), sNew.getNetwork());
		pw.write(output);
		
		if(carFile != null){
			log.info("Combining car and commercial vehicles.");
			Scenario car = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			PopulationReader pr = new PopulationReader(car);
			pr.readFile(carFile);
			
			for(Id idCar : car.getPopulation().getPersons().keySet()){
				Person carPerson = car.getPopulation().getPersons().get(idCar);
				xy.run(carPerson);
				sNew.getPopulation().addPerson(carPerson);
			}
			PopulationWriter pw2 = new PopulationWriter(sNew.getPopulation(), sNew.getNetwork());
			pw2.write(finalFile);
		}
		
		log.info("---------------------------------------");
		log.info("              COMPLETED");
		log.info("=======================================");
		
	}

}
