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

package org.matsim.households;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.population.Person;
import org.matsim.core.basic.v01.households.BasicHouseholdImpl;
import org.matsim.core.basic.v01.vehicles.BasicVehicle;

/**
 * @author dgrether
 */
public class HouseholdImpl extends BasicHouseholdImpl implements Household {

	private final Map<Id, Person> members;

	private final Map<Id, BasicVehicle> vehicles;

	public HouseholdImpl(final Id id) {
		super(id);
		this.members = new LinkedHashMap<Id, Person>();
		this.vehicles = new LinkedHashMap<Id, BasicVehicle>();
	}

	/**
	 * @see org.matsim.core.basic.v01.households.Household#getMemberIds()
	 */
	@Override
	public List<Id> getMemberIds() {
		return (List<Id>) Collections.unmodifiableList(new ArrayList<Id>(this.members.keySet()));
	}

	@Override
	public void setMemberIds(final List<Id> members) {
		throw new UnsupportedOperationException("Do not set only Ids on this level in inheritance hierarchy!" +
				"Use method addMember(Person p) instead!");
	}

	public Map<Id, Person> getMembers() {
		return this.members;
	}

	@Override
	public void setVehicleIds(final List<Id> vehicleIds) {
		throw new UnsupportedOperationException("Do not set only Ids on this level in inheritance hierarchy!" +
		"Use method addVehicle() instead!");
	}

	/**
	 * @see org.matsim.core.basic.v01.households.Household#getVehicleIds()
	 */
	@Override
	public List<Id> getVehicleIds() {
		return (List<Id>) Collections.unmodifiableList(new ArrayList<Id>(this.vehicles.keySet()));
	}

	public Map<Id, BasicVehicle> getVehicles() {
		return this.vehicles;
	}

}
