/* *********************************************************************** *
 * project: org.matsim.*
 * PersonFilterAlgorithm.java
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

package playground.marcel.filters.filter;

import org.matsim.plans.Person;
import org.matsim.plans.algorithms.PersonAlgorithm;

/**
 * @author yu
 */
public class PersonFilterAlgorithm extends PersonAlgorithm implements
		PersonFilterI {
	private PersonFilterI nextFilter = null;

	private int count = 0;

	/**
	 * @return the count.
	 */
	public int getCount() {
		return this.count;
	}

	@Override
	public void run(Person person) {
		count();
		this.nextFilter.run(person);
	}

	/**
	 * it's a virtual judge-function, all persons shall be allowed to pass or
	 * leave
	 *
	 * @param person -
	 *            a person to be judge
	 * @return true if the Person meets the criterion
	 */
	public boolean judge(Person person) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.matsim.demandmodeling.filters.filter.FilterI#count()
	 */
	public void count() {
		this.count++;
	}

	/**
	 * @param nextFilter -
	 *            The nextFilter to set.
	 */
	public void setNextFilter(PersonFilterI nextFilter) {
		this.nextFilter = nextFilter;
	}
}
