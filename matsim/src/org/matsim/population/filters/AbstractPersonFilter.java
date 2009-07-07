/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractPersonFilter.java
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

package org.matsim.population.filters;

import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.PersonAlgorithm;

public abstract class AbstractPersonFilter implements PersonAlgorithm, PersonFilter {

	protected PersonAlgorithm nextAlgorithm = null;
	private int count = 0;
	
	abstract public boolean judge(PersonImpl person);

	public void run(PersonImpl person) {
		if (judge(person)) {
			count();
			this.nextAlgorithm.run(person);
		}	
	}

	public void count() {
		this.count++;
	}

	public int getCount() {
		return this.count;
	}

	public void run(PopulationImpl population) {
		for (PersonImpl person : population.getPersons().values()) {
			run(person);
		}
	}
	
}
