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

/**
 * This class is an example to select a person with a special person-id
 * 
 * @author ychen
 */
public class PersonIDFilter extends PersonFilterA {
	private final int criterion;

	@Override
	public boolean judge(Person person) {
		return Integer.parseInt(person.getId().toString()) % criterion == 0;
	}

	/*-------------------------CONSTRUCTOR----------------------*/
	/**
	 * @param criterion
	 */
	public PersonIDFilter(int criterion) {
		this.criterion = criterion;
	}

}
