/* *********************************************************************** *
 * project: org.matsim.*
 * PersonsWriterTXT.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package teach.multiagent07.util;

import org.matsim.basic.v01.BasicPlan;
import org.matsim.basic.v01.BasicPlanImpl;

import teach.multiagent07.population.Person;
import teach.multiagent07.population.PersonHandler;


public class PersonsWriterTXT extends PersonHandler{

	@Override
	public void handlePerson(Person person) {
		System.out.println("Person: " + person.getId());
		
		for(BasicPlan plan: person.getPlans()) {
			System.out.println("Plan Score: " + plan.getScore());
			BasicPlanImpl.ActLegIterator iter = plan.getIterator();
			System.out.println(iter.nextAct());
			while (iter.hasNextLeg()) {
				System.out.println(iter.nextLeg());
				System.out.println(iter.nextAct());
			}
		}
		System.out.println();
	}
}
