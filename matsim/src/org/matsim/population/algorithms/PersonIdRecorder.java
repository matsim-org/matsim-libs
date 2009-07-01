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

package org.matsim.population.algorithms;

import java.util.HashSet;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

public class PersonIdRecorder extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private HashSet<Id> ids = new HashSet<Id>();

	public HashSet<Id> getIds() {
		return ids;
	}

	@Override
	public void run(PersonImpl person) {
		ids.add(person.getId());	
}

	public void run(PlanImpl plan) {
		this.run(plan.getPerson());
	}

}
