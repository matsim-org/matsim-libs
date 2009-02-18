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
import java.util.Map;

import org.matsim.interfaces.basic.v01.BasicLocation;
import org.matsim.interfaces.basic.v01.Id;

/**
 * @author dgrether
 */
public class BasicHouseholdBuilder implements HouseholdBuilder {

	private Map<Id, BasicHousehold> households;

	public BasicHouseholdBuilder(Map<Id, BasicHousehold> map) {
		this.households = map;
	}

	public Map<Id, BasicHousehold> getHouseholds() {
		return this.households;
	}

	public BasicHouseholdImpl createHousehold(Id householdId,
			List<Id> membersPersonIds, BasicLocation loc, List<Id> vehicleIds) {
		BasicHouseholdImpl hh = new BasicHouseholdImpl(householdId);
		hh.setLocation(loc);
		hh.setMemberIds(membersPersonIds);
		hh.setVehicleIds(vehicleIds);
		this.households.put(householdId, hh);
		return hh;
	}

}
