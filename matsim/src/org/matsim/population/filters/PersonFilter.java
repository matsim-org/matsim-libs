/* *********************************************************************** *
 * project: org.matsim.*
 * PersonFilter.java
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

import org.matsim.population.Person;
import org.matsim.population.algorithms.PersonAlgorithm;

/**
 * This interface extends interface: org.matsim.playground.filters.filter.FilterI,
 * and offers important functions for
 * org.matsim.playground.filters.filter.PersonFilterA
 *
 * @author ychen
 *
 */
public interface PersonFilter extends Filter, PersonAlgorithm {
	/**
	 * judges whether the Person will be selected or not
	 *
	 * @param person -
	 *            who is being judged
	 * @return true if the Person meets the criterion of the PersonFilterA
	 */
	boolean judge(Person person);

	/**
	 * sends the person to the next PersonFilterA
	 * (org.matsim.playground.filters.filter.PersonFilterA) or other behavior
	 *
	 * @param person -
	 *            a person being run
	 */
	void run(Person person);

}
