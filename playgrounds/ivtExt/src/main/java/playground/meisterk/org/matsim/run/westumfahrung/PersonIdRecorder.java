/* *********************************************************************** *
 * project: org.matsim.*
 * PersonIdRecorder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.org.matsim.run.westumfahrung;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import java.util.HashSet;

public class PersonIdRecorder extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private HashSet<Id> ids = new HashSet<Id>();

	public HashSet<Id> getIds() {
		return ids;
	}

	@Override
	public void run(Person person) {
		ids.add(person.getId());	
}

	@Override
	public void run(Plan plan) {
		this.run(plan.getPerson());
	}

}
