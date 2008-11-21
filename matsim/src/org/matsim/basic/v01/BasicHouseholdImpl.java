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

package org.matsim.basic.v01;

import java.util.List;

import org.matsim.interfaces.basic.v01.BasicHousehold;
import org.matsim.interfaces.basic.v01.BasicIncome;
import org.matsim.interfaces.basic.v01.BasicLocation;

/**
 * @author dgrether
 */
public class BasicHouseholdImpl implements BasicHousehold {

	private Id id;
	private List<Id> memberIds = null;
	private BasicLocation location;
	private List<Id> vehicleDefinitionIds = null;
	private BasicIncome income;
	private String language;
	
	public BasicHouseholdImpl(Id id) {
		this.id = id;
	}

	public Id getId() {
		return this.id;
	}

	public BasicIncome getIncome() {
		return this.income;
	}

	public String getLanguage() {
		return this.language;
	}

	public BasicLocation getBasicLocation() {
		return this.location;
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

	public void setLocation(BasicLocation locationImpl) {
		this.location = locationImpl;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public void setIncome(BasicIncome income) {
		this.income = income;
	}

	public void setVehicleIds(List<Id> vehicleIds) {
		this.vehicleDefinitionIds = vehicleIds;
	}

}
