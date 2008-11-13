/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.population;

import java.util.List;

import org.matsim.basic.v01.HouseholdBuilder;
import org.matsim.basic.v01.Id;
import org.matsim.interfaces.basic.v01.BasicHousehold;

/**
 * @author dgrether
 */
public class HouseholdBuilderImpl implements HouseholdBuilder {

	private List<Household> households;
	private Population population;

	public HouseholdBuilderImpl(Population pop, List<Household> households) {
		this.households = households;
		this.population = pop;
	}

	public List<BasicHousehold> getHouseholds() {
		return (List)this.households;
	}

	public BasicHousehold createHousehold(Id householdId,
			List<Id> membersPersonIds, List<Id> vehicleIds) {
		HouseholdImpl hh = new HouseholdImpl(householdId);
		Person p;
		for (Id id : membersPersonIds){
			p = this.population.getPerson(id);
			if (p !=  null) {
				hh.addMember(p);
			}
			else {
				throw new IllegalArgumentException("Household member with Id: " + id + " is not part of population!");
			}
		}
		hh.setVehicleIds(vehicleIds);
		this.households.add(hh);
		return hh;
	}

}
