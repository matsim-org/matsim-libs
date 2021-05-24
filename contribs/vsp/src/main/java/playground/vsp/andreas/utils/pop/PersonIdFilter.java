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

package playground.vsp.andreas.utils.pop;

import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.analysis.filters.population.AbstractPersonFilter;
import org.matsim.core.population.algorithms.PersonAlgorithm;

/**
 * 
 * @author aneumann
 *
 */
public class PersonIdFilter extends AbstractPersonFilter {

	private Set<String> personIds;

	public PersonIdFilter(Set<Id> personIds, PersonAlgorithm nextAlgorithm) {
		super();
		this.nextAlgorithm = nextAlgorithm;
		this.personIds = new TreeSet<String>();
		for (Id id : personIds) {
			this.personIds.add(id.toString());
		}
	}

	@Override
	public boolean judge(Person person) {
		if (this.personIds.contains(person.getId().toString())) {
			return true;
		}

		return false;
	}
}