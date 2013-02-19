/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mmoyo.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;

public class PopulationFilter2 {
	private final static Logger log = Logger.getLogger(PopulationFilter2.class);
	
	/** returns from the population only the given persons id's**/	
	private void run (Population population, List<Id> rowList){
		//get by exclusion the list of persons to be deleted 
		List<Id> delebPersonList = new ArrayList<Id>();
		for (Person person : population.getPersons().values()){
			if (!rowList.contains(person.getId())){
				delebPersonList.add(person.getId());
			}
		}
		
		//delete persons who are not in the list
		for (Id delebPersonId : delebPersonList){
			population.getPersons().remove(delebPersonId);
		}
	
		delebPersonList = null;
	}


	public List<Id> getRandomAgents(final Population pop, final int agentNum){
		List<Id> idList = new ArrayList<Id>();
	
		Random random = new Random();
		String str_selected = "Selected person: ";
		int popSize = pop.getPersons().size();
		
		Id[]idArray = pop.getPersons().keySet().toArray(new Id[popSize]);
		
		for (int i=0; i<agentNum; i++){
			 int rand_i = random.nextInt(popSize);
			 Id randId = idArray[rand_i];
			 idList.add(randId);
			 log.info(str_selected + randId.toString());
		 }

		random = null;
		str_selected= null;
		idArray = null;
		return idList;
	}
	
	public static void main(String[] args) {
		List<Id> persIds = new ArrayList<Id>();
		String popFile;
		String netFile;
		String txtFile;
		
		if (args.length>0){
			popFile = args[0];
			netFile = args[1];
			txtFile = args[2];
		}else{
			popFile = "../../";
			netFile = "../../";
			txtFile = "../../";
		}
		
		DataLoader dataLoader = new DataLoader();
		Population pop = dataLoader.readPopulation(popFile);
		PopulationFilter2 popFileter2 = new PopulationFilter2(); 
		
		//get random persons
		//persIds = popFileter2.getRandomAgents(pop, 4);
		
		//get persons directly with code
		/*
		persIds.add(new IdImpl("11100482X1"));
		persIds.add(new IdImpl("11100482X1_2"));
		persIds.add(new IdImpl("11100482X1_3"));
		persIds.add(new IdImpl("11100482X1_4"));
		persIds.add(new IdImpl("11100482X1_5"));
		*/

		popFileter2.run(pop, persIds);
		Network net = dataLoader.readNetwork(netFile);		
		new PopulationWriter(pop, net).write(new File(txtFile).getParent()+ "/agents.xml.gz");
	}

}
