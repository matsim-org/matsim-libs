/* *********************************************************************** *
 * project: org.matsim.*
 * PersonHouseholdMapping
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.households;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;


/**
 * Tiny helper to get the household associated with a person's id.
 * @author dgrether
 *
 */
public class PersonHouseholdMapping {

	private final Map<Id<Person>, Household> phMap = new IdMap<>(Person.class);
	
	public PersonHouseholdMapping(Households hhs) {
		this.reinitialize(hhs);
	}

	public void reinitialize(Households hhs) {
		this.phMap.clear();
		for (Household h : hhs.getHouseholds().values()){
			for (Id<Person> member : h.getMemberIds()){
				this.phMap.put(member, h);
			}
		}
		
	}
	
	public Household getHousehold(Id<Person> personId) {
		return this.phMap.get(personId);
	}
	
	
}
