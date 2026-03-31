/* *********************************************************************** *
 * project: org.matsim.*
 * SimulationConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;

import static org.matsim.core.utils.io.XmlUtils.writeEncodedAttributeKeyValue;

/**
 * The idea of this event is to have a list of all Persons in the beginning of the events file
 * 
 * @author tkohl / senozon
 * @author sebhoerl / IRT SystemX
 */
public class PersonInitializedEvent extends Event implements HasPersonId, HasLinkId, HasFacilityId, BasicLocation {

	public static final String EVENT_TYPE = "personInitialized";
	public static final String ATTRIBUTE_ACTIVITY_TYPE = ActivityEndEvent.ATTRIBUTE_ACTTYPE;
	
	private final Id<Person> personId;
	private final Id<Link> linkId;
	private final Id<ActivityFacility> facilityId;
	private final String activityType;
	private final Coord coord;
	
	public PersonInitializedEvent(double time, Id<Person> personId, Id<Link> linkId, 
		Id<ActivityFacility> facilityId, String activityType) {
		this(time, personId, linkId, facilityId, activityType, null);
	}
	
	public PersonInitializedEvent(double time, Id<Person> personId, Id<Link> linkId, 
		Id<ActivityFacility> facilityId, String activityType, Coord coord) {
		super(time);
		this.personId = personId;
		this.linkId = linkId;
		this.facilityId = facilityId;
		this.activityType = activityType;
		this.coord = coord;
	}

	@Override
	public Id<Person> getPersonId() {
		return this.personId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Coord getCoord() {
		return this.coord;
	}

	public String getActivityType() {
		return this.activityType;
	}

	@Override public Id<Link> getLinkId() {
		return this.linkId;
	}

	@Override public Id<ActivityFacility> getFacilityId() {
		return this.facilityId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_ACTIVITY_TYPE, this.activityType);
		return attr;
	}

	@Override
	public void writeAsXML(StringBuilder out) {
		writeXMLStart(out);
		writeEncodedAttributeKeyValue(out, ATTRIBUTE_ACTIVITY_TYPE, this.activityType);
		writeXMLEnd(out);
	}
}
