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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.XY2Links;
import org.xml.sax.SAXException;

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
		
		Scenario sNew = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory pf = sNew.getPopulation().getFactory();
		// Network.
		NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(sNew);
		try {
			nr.parse(networkFile);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		XY2Links xy = new XY2Links((NetworkImpl) sNew.getNetwork());

		int id = Integer.parseInt(firstId);
		
		log.info("Reading commercial vehicle plans files from " + root);
		List<Scenario> listSc = new ArrayList<Scenario>(10);
		for(int i = 1; i <= 10; i++){
			String filename = root + "plansGauteng5000_Sample" + i + ".xml";
			Scenario s = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
			PopulationReaderMatsimV4 pr = new PopulationReaderMatsimV4(s);
			try {
				pr.parse(filename);
				listSc.add(s);
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		Random r =  MatsimRandom.getRandom();
		
		log.info("Create " + number + " commercial vehicle agents.");
		int counter = 0;
		while(counter < Integer.parseInt(number)){
			// Pick
			int rList = (int) Math.round(r.nextDouble()*9);
			
			List<Id> agentIds = new ArrayList<Id>();
			Set<Id> Ids = listSc.get((int) rList).getPopulation().getPersons().keySet();
			for (Id id2 : Ids) {
				agentIds.add(id2);
			}
			int rId = (int) Math.floor(r.nextDouble()*agentIds.size());
			
			Person p = pf.createPerson(new IdImpl(id));
			Plan plan = listSc.get((int) rList).getPopulation().getPersons().get(agentIds.get(rId)).getSelectedPlan();
			xy.run(plan);
			p.addPlan(plan);
			sNew.getPopulation().addPerson(p);
			counter++;
			id++;
		}
		log.info("Created " + counter + " commercial vehicles (Done)");
		
		// Check that no Ids are duplicated.
		List<Id> listIds = new ArrayList<Id>();
		for(Id i : sNew.getPopulation().getPersons().keySet()){
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
			Scenario car = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
			PopulationReaderMatsimV4 pr = new PopulationReaderMatsimV4(car);
			try {
				pr.parse(carFile);
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
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
