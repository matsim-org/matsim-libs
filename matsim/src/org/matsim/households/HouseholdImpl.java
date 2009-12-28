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

/**
 * @author dgrether
 */
public class HouseholdImpl implements Household {

	private Id id;
	private List<Id> memberIds = null;
	private List<Id> vehicleDefinitionIds = null;
	private Income income;
	
	public HouseholdImpl(Id id) {
		this.id = id;
	}

	public Id getId() {
		return this.id;
	}

	public Income getIncome() {
		return this.income;
	}

	public List<Id> getMemberIds() {
		return this.memberIds;
	}

	public List<Id> getVehicleIds() {
		return this.vehicleDefinitionIds;
	}
	
	public void setMemberIds(List<Id> memberIds) {
		this.memberIds = memberIds;
	}

	public void setIncome(Income income) {
		this.income = income;
	}

	public void setVehicleIds(List<Id> vehicleIds) {
		this.vehicleDefinitionIds = vehicleIds;
	}

}
