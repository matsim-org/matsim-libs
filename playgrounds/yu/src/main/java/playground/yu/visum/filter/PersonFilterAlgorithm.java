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
package playground.yu.visum.filter;

import org.matsim.api.core.v01.population.Person;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * @author yu
 */
public class PersonFilterAlgorithm extends AbstractPersonAlgorithm implements
		PersonFilterI {
	private PersonFilterI nextFilter = null;

	private int count = 0;

	/**
	 * @return the count.
	 */
	@Override
	public int getCount() {
		return count;
	}

	@Override
	public void run(Person person) {
		count();
		nextFilter.run(person);
	}

	/**
	 * it's a virtual judge-function, all persons shall be allowed to pass or
	 * leave
	 * 
	 * @param person
	 *            - a person to be judge
	 * @return true if the Person meets the criterion
	 */
	@Override
	public boolean judge(Person person) {
		return true;
	}

	@Override
	public void count() {
		count++;
	}

	/**
	 * @param nextFilter
	 *            - The nextFilter to set.
	 */
	@Override
	public void setNextFilter(PersonFilterI nextFilter) {
		this.nextFilter = nextFilter;
	}
}
