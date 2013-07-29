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

import java.util.Collection;
import java.util.Set;

import org.matsim.api.core.v01.Id;

/*
 * Nothing like departure time is needed since this is defined
 * by the activities performed before departing. 
 */
public class JointDeparture {
	
	private final Id id;
	private final Id linkId;
	private final Id vehicleId;
	private final Id driverId;
	private final Set<Id> passengerIds;
	
	private boolean departed = false;
	
	/*package*/ JointDeparture(Id id, Id linkId, Id vehicleId, Id driverId, Set<Id> passengerIds) {
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
	
	public Id getId() {
		return this.id;
	}
	
	public Id getLinkId() {
		return this.linkId;
	}

	public Id getVehicleId() {
		return this.vehicleId;
	}
	
	public Id getDriverId() {
		return this.driverId;
	}
	
	public Set<Id> getPassengerIds() {
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
		for (Id passengerId : this.passengerIds) {
			b.append("[passengerId").append(i).append("=").append(passengerId.toString()).append("]");
			i++;
		}
	  return b.toString();
	}
}