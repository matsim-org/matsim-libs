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

import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.PersonAlgorithm;

/**
 * Interface for filtering persons.
 *
 * @author ychen
 */
public interface PersonFilter extends Filter, PersonAlgorithm {
	/**
	 * judges whether the Person will be selected or not
	 *
	 * @param person person being judged
	 * @return true if the Person meets the criterion of the filter
	 */
	boolean judge(Person person);

	/**
	 * sends the person to the next PersonAlgorithm, which could be another filter.
	 *
	 * @param person person to be handled
	 */
	void run(Person person);

}
