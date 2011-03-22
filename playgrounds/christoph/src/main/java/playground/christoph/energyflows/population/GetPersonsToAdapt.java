/* *********************************************************************** *
 * project: org.matsim.*
 * GetAgentsToAdapt.java
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

package playground.christoph.energyflows.population;

import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.misc.Counter;

public class GetPersonsToAdapt {

	private Counter counter;
	
	public Set<Person> getPersons(Population population, Set<Id> facilities) {
		Set<Person> persons = new TreeSet<Person>();
		
		counter = new Counter("persons to adapt: ");
		for (Person person : population.getPersons().values()) {
			checkPerson(persons, facilities, person);
		}
		counter.printCounter();
		
		return persons;
	}
	
	private void checkPerson(Set<Person> persons, Set<Id> facilities, Person person) {
		for (Plan plan : person.getPlans()) {
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Leg) continue;
				else {
					Activity activity = (Activity) planElement;
					if (facilities.contains(activity.getFacilityId())) {
						persons.add(person);
						counter.incCounter();
						return;
					}
				}
			}
		}
	}
}
