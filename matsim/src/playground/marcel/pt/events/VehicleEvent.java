/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleEvent.java
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

import playground.marcel.pt.interfaces.Vehicle;

public abstract class VehicleEvent extends BasicEvent {

	public Vehicle vehicle;

	public VehicleEvent(final double time, final Vehicle vehicle) {
		super(time);
		this.vehicle = vehicle;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put("type", getEventType());
		attr.put("vehicle", "unknown");// TODO [MR] Vehicles should have an Id on their own...
		return attr;
	}

}
