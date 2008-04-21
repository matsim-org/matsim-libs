/* *********************************************************************** *
 * project: org.matsim.*
 * PersonIDsExporter.java
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

package playground.marcel.filters.filter.finalFilters;

import java.util.HashSet;
import java.util.Set;

import org.matsim.basic.v01.Id;
import org.matsim.plans.Person;

import playground.marcel.filters.filter.PersonFilterA;

/**
 * A PersonIDsExporter exports a set of Person- IDs, these persons have passed
 * all the PersonFilters. So a PersonIDsExporter should be used as the last
 * PersonFilterA.
 * 
 * @author ychen
 * 
 */
public class PersonIDsExporter extends PersonFilterA {
	private static Set<Id> idSet = new HashSet<Id>();

	/**
	 * only a virtual overriding, the function will never be used
	 */
	// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
	@Override
	public boolean judge(Person person) {
		return true;
	}

	/*--------------------------SETTER----------------------------*/
	/**
	 * Returns a set of Person-IDs
	 * 
	 * @return a Set of Person-IDs
	 */
	public Set<Id> idSet() {
		System.out.println("exporting " + idSet.size() + " person- IDs.");
		return idSet;
	}

	/*--------------------------OVERRIDING METHOD-------------------*/
	/**
	 * When the function is called, the Person- ID of the person is being put
	 * into the idSet.
	 */
	@Override
	public void run(Person person) {
		idSet.add(person.getId());
	}
}
