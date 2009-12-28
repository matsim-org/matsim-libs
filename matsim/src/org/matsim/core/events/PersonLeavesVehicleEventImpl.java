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

package org.matsim.core.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.BasicVehicle;

/**
 *
 * @author mrieser
 */
public class PersonLeavesVehicleEventImpl extends PersonEventImpl implements PersonLeavesVehicleEvent {

	public static final String EVENT_TYPE = "PersonLeavesVehicle";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";

	private final Id vehicleId;
	private Id transitRouteId = null;

	public PersonLeavesVehicleEventImpl(final double time, final Person person, final BasicVehicle vehicle, final Id transitRouteId) {
		super(time, person);
		this.vehicleId = vehicle.getId();
		this.transitRouteId = transitRouteId;
	}
	
	public PersonLeavesVehicleEventImpl(final double time, final Id personId, final Id vehicleId, final Id transitRouteId) {
		super(time, personId);
		this.vehicleId = vehicleId;
		this.transitRouteId = transitRouteId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attrs = super.getAttributes();
		attrs.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		if (this.transitRouteId != null){
			attrs.put(PersonEntersVehicleEventImpl.TRANSIT_ROUTE_ID, this.transitRouteId.toString());
		}
		return attrs;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	public Id getVehicleId() {
		return this.vehicleId;
	}
	
	public Id getTransitRouteId() {
		return transitRouteId;
	}
	

}
