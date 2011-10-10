/* *********************************************************************** *
 * project: org.matsim.*
 * PersonRemovePlans.java
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

package playground.ucsb.debug;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

public class PersonRemovePlans extends AbstractPersonAlgorithm {

	private final Set<Id> personIdsToKeep;
	
	public PersonRemovePlans(Set<Id> personIdsToKeep) {
		super();
		this.personIdsToKeep = personIdsToKeep;
	}

	@Override
	public void run(final Person person) {
		if (!personIdsToKeep.contains(person.getId())) {
			person.getPlans().clear();
		}
	}
}
