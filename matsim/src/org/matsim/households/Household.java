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
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.population.Person;
import org.matsim.households.basic.BasicHousehold;
import org.matsim.vehicles.BasicVehicle;

/**
 * @author dgrether
 */
public interface Household extends BasicHousehold {
	/**
	 * Returns an unmodifiable List of the vehicle ids.
	 * @see org.matsim.households.basic.BasicHousehold#getVehicleIds()
	 */
	public List<Id> getVehicleIds();
	/**
	 * Returns an unmodifiable List of the member ids.
	 * @see org.matsim.households.basic.BasicHousehold#getMemberIds()
	 */
	public List<Id> getMemberIds();
	
	public Map<Id, Person> getMembers();
	
	public Map<Id, BasicVehicle> getVehicles();
	
	
}
