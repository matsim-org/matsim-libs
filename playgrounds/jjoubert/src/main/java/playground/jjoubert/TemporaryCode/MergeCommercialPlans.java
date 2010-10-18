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

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.xml.sax.SAXException;

public class MergeCommercialPlans {
	private static Logger log = Logger.getLogger(MergeCommercialPlans.class);

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
		if(args.length != 5){
			throw new IllegalArgumentException("Incorrect number of arguments.");
		} else{
			root = args[0];
			number = args[1];
			firstId = args[2];
			networkFile = args[3];
			output = args[4];
		}
		
		Scenario sNew = new ScenarioImpl();
		long id = Long.parseLong(firstId);
		
		log.info("Reading commercial vehicle plans files from " + root);
		List<Scenario> listSc = new ArrayList<Scenario>(10);
		for(int i = 1; i <= 10; i++){
			String filename = root + "plansGauteng5000_Sample" + i + ".xml";
			Scenario s = new ScenarioImpl();
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
			long rList = (int) Math.round(r.nextDouble()*9);
			long rId = 100000 + (int) Math.round(r.nextDouble()*4999);
			
			Person p = listSc.get((int) rList).getPopulation().getPersons().get(new IdImpl(rId));
			if(p == null){
				log.warn("No person returned from list " + rList + " with Id " + rId);				
			}
			p.setId(new IdImpl(id++));
			sNew.getPopulation().addPerson(p);
			counter++;
		}
		log.info("   ... created " + counter + " (Done)");
		
		
		// Now write the final population.
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
		
		PopulationWriter pw = new PopulationWriter(sNew.getPopulation(), sNew.getNetwork());
		pw.write(output);
		
		log.info("---------------------------------------");
		log.info("              COMPLETED");
		log.info("=======================================");
		
	}

}
