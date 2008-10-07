/* *********************************************************************** *
 * project: org.matsim.*
 * PersonEntersVehicleEvent.java
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

package playground.marcel.pt.events;

import java.util.Map;

import org.matsim.events.BasicEvent;
import org.matsim.mobsim.queuesim.Vehicle;
import org.matsim.population.Person;

public class PersonLeavesVehicleEvent extends BasicEvent {

	final private Vehicle vehicle;
	
	public PersonLeavesVehicleEvent(final double time, final Person person, final Vehicle vehicle) {
		super(time, person);
		this.vehicle = vehicle;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attrs = super.getAttributes();
		attrs.put("vehicle", vehicle.getId().toString());
		attrs.put("type", "PersonLeavesVehicleEvent");
		return attrs;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}
	
	@Override
	public String toString() {
		return "[PersonLeavesVehicle: agent: " + this.agentId + "; vehicle: " + vehicle.getId() + "]";
	}

}
