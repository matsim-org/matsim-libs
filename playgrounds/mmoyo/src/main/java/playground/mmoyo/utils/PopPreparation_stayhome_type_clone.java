/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;

import playground.mmoyo.algorithms.PersonClonner;
import playground.mmoyo.utils.calibration.HomePlanCreator;

/**
 * Reads a plans file and set the selected plan as type "normal".
 * Besides it adds an "stay home" plan which type is exactly "stay home"
 * Duplicates the population 
 */

public class PopPreparation_stayhome_type_clone {
	Population pop;
	final String strNormal= "normal";
	final String strStayHome= "stayHome";
	final String strCLON_SUFFIX = "_2";
	
	public PopPreparation_stayhome_type_clone (Population pop){
		this.pop =pop;
	}
	
	private void run (){
		HomePlanCreator homePlanCreator = new HomePlanCreator(pop);
		PersonClonner clonner = new PersonClonner();
		Set<Person> clonsSet = new HashSet<Person>();

		for (Person person: pop.getPersons().values()){
			((PlanImpl)person.getSelectedPlan()).setType(strNormal); //set original plan as "normal"
			
			homePlanCreator.run(person);  //add "stay home" plan 
			((PlanImpl)person.getPlans().get(person.getPlans().size()-1)).setType(strStayHome);   // set type  "stay home"
		
			//create set of clones
			Id<Person> newId = Id.create(person.getId().toString() + strCLON_SUFFIX, Person.class);
			Person clon = clonner.run(person, newId);
			clonsSet.add(clon);
		}

		for (Person clon: clonsSet){
			pop.addPerson(clon);
		}
}
	
	public static void main(String[] args) {
		String plansFile; 
		String netFile;
		
		if(args.length>0){
			plansFile = args[0];
			netFile = args[1];
		}else{
			plansFile = "../../";
			netFile = "../../";
		}

		//read plans file
		DataLoader loader = new DataLoader();
		Population pop = loader.readPopulation(plansFile);
		
		//prepare:  set types, add stayHome plan, clone
		PopPreparation_stayhome_type_clone preparacion = new PopPreparation_stayhome_type_clone (pop);
		preparacion.run();
		
		//write file 
		Network net = new DataLoader().readNetwork(netFile);
		PopulationWriter popWriter = new PopulationWriter(pop, net);
		File file = new File(plansFile);
		String outFile = file.getPath() + ".Wstayhome_typed_cloned.xml.gz";
		popWriter.write(outFile) ;
		
		//read and write only first agents
		pop=null;
		net=null;
		preparacion = null;
		ScenarioImpl scn = (ScenarioImpl) loader.createScenario();
		new MatsimNetworkReader(scn).readFile(netFile);
		new FirstPersonExtractorFromFile(scn).readFile(outFile, 10);
		popWriter.write(outFile + ".smallSample.xml") ;
		System.out.println("done: "  + outFile + ".smallSample.xml");
		
	}
}
