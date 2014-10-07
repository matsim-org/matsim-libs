/* *********************************************************************** *
 * project: org.matsim.*
 * PlanCounter.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PersonAlgorithm;

import playground.mmoyo.io.PopSecReader;

public class PlanCounter2 implements PersonAlgorithm {
	private final String SEP = " ";
	private final Id personId;
	
	public PlanCounter2(Id personId){
		this.personId = personId;
	}
	
	@Override
	public void run(Person person) {
		if (person.getPlans().size() != 20){
			System.out.println(person.getId() + SEP + person.getPlans().size());
		}
	}
	
	public static void main(String[] args) {
		String popFilePath;
		if(args.length==0){
			popFilePath = "../../input/deleteme2/output_plans.xml.gz";
		}else{
			popFilePath = args[0];
		}
			
		DataLoader dataLoader = new DataLoader();
		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario();
		
		Id<Person> personId = Id.create("11140292", Person.class);
		PlanCounter2 planCounter2 = new PlanCounter2(personId);
		PopSecReader popSecReader = new PopSecReader (scn, planCounter2);
		popSecReader.readFile(popFilePath);

	}



}
