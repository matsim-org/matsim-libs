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

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicle;

/**
 * @author dgrether
 */
public class HouseholdImpl implements Household {

	private Id<Household> id;
	private List<Id<Person>> memberIds = null;
	private List<Id<Vehicle>> vehicleDefinitionIds = null;
	private Income income;
	
	private final Attributes attributes = new Attributes();
	
	public HouseholdImpl(Id<Household> id) {
		this.id = id;
	}

	@Override
	public Id<Household> getId() {
		return this.id;
	}

	@Override
	public Income getIncome() {
		return this.income;
	}

	@Override
	public List<Id<Person>> getMemberIds() {
		return this.memberIds;
	}

	@Override
	public List<Id<Vehicle>> getVehicleIds() {
		return this.vehicleDefinitionIds;
	}
	
	public void setMemberIds(List<Id<Person>> memberIds) {
		this.memberIds = memberIds;
	}

	@Override
	public void setIncome(Income income) {
		this.income = income;
	}

	public void setVehicleIds(List<Id<Vehicle>> vehicleIds) {
		this.vehicleDefinitionIds = vehicleIds;
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}
	
}