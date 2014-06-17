/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.parking.PC2.simulation;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.parking.lib.DebugLib;

public class ParkingArrivalEvent extends Event {

	private Id parkingId;
	public final static String ATTRIBUTE_PARKING_ID = "parkingId";
	public final static String EVENT_TYPE = "parkingArrivalEvent";

	public ParkingArrivalEvent(double time, Id parkingId) {
		super(time);
		
		if (time>110000){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		
		this.parkingId = parkingId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		final Map<String, String> attributes = super.getAttributes();
		attributes.put(ATTRIBUTE_PARKING_ID, parkingId.toString());
		return attributes;
	}

}
