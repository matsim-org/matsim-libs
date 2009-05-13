/* *********************************************************************** *
 * project: org.matsim.*
 * ArrivalAtFacilityEvent.java
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

import org.matsim.core.api.facilities.ActivityFacility;

import playground.marcel.pt.interfaces.TransitVehicle;

public class ArrivalAtFacilityEvent extends VehicleEvent {

	public static final String EVENT_TYPE = "arrivalAtFacility";

	public final ActivityFacility facility;

	public ArrivalAtFacilityEvent(final double time, final TransitVehicle vehicle, final ActivityFacility facility) {
		super(time, vehicle);
		this.facility = facility;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public String getTextRepresentation() {
		return "[ArrivalAtFacilityEvent: ]";
	}

}
