/* *********************************************************************** *
 * project: org.matsim.*
 * JointDeparture.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/*
 * Nothing like departure time is needed since this is defined
 * by the activities performed before departing. 
 */
public class JointDeparture {
	
	private final Id<JointDeparture> id;
	private final Id<Link> linkId;
	private final Id<Vehicle> vehicleId;
	private final Id<Person> driverId;
	private final Set<Id<Person>> passengerIds;
	
	private boolean departed = false;
	
	/*package*/ JointDeparture(Id<JointDeparture> id, Id<Link> linkId, Id<Vehicle> vehicleId, Id<Person> driverId, Set<Id<Person>> passengerIds) {
		this.id = id;
		this.linkId = linkId;
		this.vehicleId = vehicleId;
		this.driverId = driverId;
		this.passengerIds = passengerIds;
		
		if (linkId == null) throw new RuntimeException("linkId is null!");
		if (vehicleId == null) throw new RuntimeException("vehicleId is null!");
		if (driverId == null) throw new RuntimeException("driverId is null!");
		if (passengerIds == null) throw new RuntimeException("passengerIds are null!");
	}
	
	public void setDeparted() {
		this.departed = true;
	}
	
	public boolean isDeparted() {
		return this.departed;
	}
	
	public Id<JointDeparture> getId() {
		return this.id;
	}
	
	public Id<Link> getLinkId() {
		return this.linkId;
	}

	public Id<Vehicle> getVehicleId() {
		return this.vehicleId;
	}
	
	public Id<Person> getDriverId() {
		return this.driverId;
	}
	
	public Set<Id<Person>> getPassengerIds() {
		return this.passengerIds;
	}
	
	@Override
	public final String toString() {
		StringBuilder b = new StringBuilder();
		b.append("[id=").append(this.getId()).append("]");
		b.append("[driverId=").append(this.driverId.toString()).append("]");
		b.append("[vehicleId=").append(this.vehicleId.toString()).append("]");
		b.append("[linkId=").append(this.linkId.toString()).append("]");
		b.append("[departed=").append(this.departed).append("]");
		int i = 0;
		for (Id<Person> passengerId : this.passengerIds) {
			b.append("[passengerId").append(i).append("=").append(passengerId.toString()).append("]");
			i++;
		}
	  return b.toString();
	}
}