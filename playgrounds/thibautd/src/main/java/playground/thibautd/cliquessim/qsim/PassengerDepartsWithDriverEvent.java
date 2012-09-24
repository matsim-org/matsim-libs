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
package playground.thibautd.cliquessim.qsim;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.EventImpl;
import org.matsim.core.events.LinkEventImpl;

/**
 * @author thibautd
 */
public class PassengerDepartsWithDriverEvent extends EventImpl {
	public static final String EVENT_TYPE = "passengerdepartswithdriver";
	public static final String ATTRIBUTE_DRIVER = "driverId";
	public static final String ATTRIBUTE_PASSENGER = "passengerId";

	private final Id driver;
	private final Id passenger;
	private final Id link;

	public PassengerDepartsWithDriverEvent(
			final double time,
			final Id driver,
			final Id passenger,
			final Id link) {
		super( time );
		this.driver =driver;
		this.passenger = passenger;
		this.link = link;
	}

	/**
	 * Gets the driver for this instance.
	 *
	 * @return The driver.
	 */
	public Id getDriverId()
	{
		return this.driver;
	}

	/**
	 * Gets the passenger for this instance.
	 *
	 * @return The passenger.
	 */
	public Id getPassengerId()
	{
		return this.passenger;
	}

	/**
	 * Gets the link for this instance.
	 *
	 * @return The link.
	 */
	public Id getLinkId()
	{
		return this.link;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> atts = super.getAttributes();
		atts.put( ATTRIBUTE_DRIVER , ""+driver );
		atts.put( ATTRIBUTE_PASSENGER , ""+passenger );
		atts.put( LinkEventImpl.ATTRIBUTE_LINK , ""+link );
		return atts;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
}

