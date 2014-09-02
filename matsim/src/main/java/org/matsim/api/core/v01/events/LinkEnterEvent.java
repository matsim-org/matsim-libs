/* *********************************************************************** *
 * project: org.matsim.*
 * LinkEnterEvent.java
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

package org.matsim.api.core.v01.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * Design considerations: <ul>
 * <li> This class deliberately does <i>not</i> implement HasPersonId. One reason is that it does not really
 * belong at this level (since it is the vehicle that enters/leaves links); another reason is that this would
 * make an "instanceof HasPersonId" considerably more expensive. kai/dg, dec'12
 * </ul> 
 *
 */
public class LinkEnterEvent extends Event {

	public static final String EVENT_TYPE = "entered link";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_PERSON = "person";
	
	private final Id<Person> personId;
	private final Id<Link> linkId;
	private final Id<Vehicle> vehicleId;

	public LinkEnterEvent(final double time, final Id<Person> agentId, final Id<Link> linkId, Id<Vehicle> vehicleId) {
		super(time);
		this.personId = agentId;
		this.linkId = linkId;
		this.vehicleId = vehicleId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	/**
	 * Comments:<ul>
	 * <li> This is currently set as deprecated.  However, there are situations where one needs the driver.  I know that one can get the driver
	 * by other means ... but since this method is already here, and we will really save a lot of work by not removing it, we may consider leaving
	 * it in place. kai, jan'14
	 * <li> Should then, obviously, be called "getDriver()".  But even that would probably mean retrofitting, especially for events.xml.  Is that worth it?
	 * kai, jan'14
	 * </ul>
	 * 
	 * @deprecated will be removed soon.
	 */
	@Deprecated
	public Id<Person> getPersonId() {
		return this.personId;
	}	

	public Id<Link> getLinkId() {
		return this.linkId;
	}
	
	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_PERSON, this.personId.toString());
		attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		if (this.vehicleId != null) {
			attr.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		}
		return attr;
	}
}