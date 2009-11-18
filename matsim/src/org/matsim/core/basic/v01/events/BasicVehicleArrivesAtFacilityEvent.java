/* *********************************************************************** *
 * project: org.matsim.*
 * BasicVehicleArrivesAtFacilityEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.basic.v01.events;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.experimental.events.Event;

/**
 * @author mrieser
 */
public interface BasicVehicleArrivesAtFacilityEvent extends Event {

	public static final String EVENT_TYPE = "VehicleArrivesAtFacility";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	public static final String ATTRIBUTE_FACILITY = "facility";

	public Id getVehicleId();
	
	public Id getFacilityId();
	
}
