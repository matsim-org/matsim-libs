/* *********************************************************************** *
 * project: org.matsim.*
 * PersonTypeFilter.java
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

package org.matsim.plans.filters;

import org.matsim.plans.Person;
import org.matsim.plans.algorithms.PersonAlgorithmI;

public class PersonTypeFilter extends AbstractPersonFilter {

	private final String type;
	
	public PersonTypeFilter(PersonAlgorithmI nextAlgo, String type) {
		this.nextAlgorithm = nextAlgo;
		this.type = type;
	}

	@Override
	public boolean judge(Person person) {
		return type.equals(person.getType());
	}

}
