/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerDepartsWithDriverEvent.java
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
package playground.thibautd.hitchiking.qsim.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * @author thibautd
 */
public class PassengerDepartsWithDriverEvent extends WaitingEvent {
	public final String ATTRIBUTE_DRIVER = "driverId";
	private final Id driverId;

	public PassengerDepartsWithDriverEvent(
			final double time,
			final Id passengerId,
			final Id driverId,
			final Id linkId) {
		super(time, passengerId, linkId);
		this.driverId = driverId;
	}

	@Override
	public String getEventType() {
		return "passengerDepartsWithDriverEvent";
	}

	public Id getDriverId() {
		return driverId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> atts = super.getAttributes();

		atts.put( ATTRIBUTE_DRIVER  , ""+driverId );

		return atts;
	}
}

