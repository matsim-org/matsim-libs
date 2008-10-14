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

import playground.marcel.pt.interfaces.Vehicle;

public class ArrivalAtFacilityEvent extends VehicleEvent {

	public ArrivalAtFacilityEvent(final double time, final Vehicle vehicle) {
		super(time, vehicle);
	}
	
	@Override
	public String toString() {
		return "[ArrivalAtFacilityEvent: ]";
	}

}
