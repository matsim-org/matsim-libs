/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PersonAlgorithm;

import playground.mmoyo.io.PopSecReader;

/**
 * Converts each plan into a new person with a consecutive suffix. 
 * The difference with planFragmenter is that here the whole plan, not only a pt leg is converted into a new person
 */
public class Plan2Person implements PersonAlgorithm{ 
	final String SEP = "_";
	private Population newPopulation;

    {
        ScenarioImpl sc = (ScenarioImpl) new DataLoader().createScenario();
        newPopulation = PopulationUtils.createPopulation(sc.getConfig(), sc.getNetwork());
    }

    @Override
	public void run(Person person) {
		char suffix = 'a';
		for (Plan plan: person.getPlans()){
			Id<Person> newId = Id.create (person.getId().toString() + SEP + (suffix++), Person.class);
			Person newPerson = new PersonImpl(newId);
			newPerson.addPlan(plan);
			newPopulation.addPerson(newPerson);
		}
	}
	
	public Population getNewPopulation(){
		return this.newPopulation;
	}
	
	public static void main(String[] args) {
		String popFile = "../../";
		String netFile = "../../";
		String newPopFile = "../../";
		
		DataLoader dLoader = new DataLoader();
		Plan2Person plan2Person = new Plan2Person();
		ScenarioImpl scn = (ScenarioImpl) dLoader.createScenario();
		PopSecReader popSecReader = new PopSecReader (scn, plan2Person);
		popSecReader.readFile(popFile);
		
		Network net = dLoader.readNetwork(netFile);
		PopulationWriter popwriter = new PopulationWriter(plan2Person.getNewPopulation(), net);
		popwriter.write(newPopFile) ;
		System.out.println("done");
	}

}
