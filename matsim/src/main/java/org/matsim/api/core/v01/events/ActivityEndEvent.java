/* *********************************************************************** *
 * project: org.matsim.*
 * ActEndEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

public final class ActivityEndEvent extends Event implements HasPersonId, HasLinkId, HasFacilityId, BasicLocation {

	public static final String EVENT_TYPE = "actend";
	public static final String ATTRIBUTE_ACTTYPE = "actType";

	private final Id<Person> personId;
	private Coord coord;
	private final Id<Link> linkId;
	private final Id<ActivityFacility> facilityId;
	private final String acttype;

	/**
	 * @deprecated -- add Coord as argument
	 */
	@Deprecated // add Coord as argument
	public ActivityEndEvent( final double time, final Id<Person> agentId, final Id<Link> linkId,
							 final Id<ActivityFacility> facilityId, final String acttype ){
		this( time, agentId, linkId, facilityId, acttype, null);
	}
	// this is the new constructor:
	public ActivityEndEvent(final double time, final Id<Person> agentId, final Id<Link> linkId,
			final Id<ActivityFacility> facilityId, final String acttype, final Coord coord) {
		super(time);
		this.linkId = linkId;
		this.facilityId = facilityId;
		this.acttype = acttype == null ? "" : acttype;
		this.personId = agentId;
		this.coord = coord;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public String getActType() {
		return this.acttype;
	}

	@Override public Id<Link> getLinkId() {
		return this.linkId;
	}

	@Override public Id<ActivityFacility> getFacilityId() {
		return this.facilityId;
	}

	@Override public Id<Person> getPersonId() {
		return this.personId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		// person, link, facility done by superclass
		attr.put(ATTRIBUTE_ACTTYPE, this.acttype);
		return attr;
	}

	@Override public Coord getCoord(){
		return coord;
	}
	public void setCoord( Coord coord ) {
		// yy  this is to retrofit the coordinate into existing events that don't have it.
		this.coord = coord;
	}

	@Override
	public void writeAsXML(StringBuilder out) {
		// Writes common attributes
		writeXMLStart(out);
		writeEncodedAttributeKeyValue(out, ATTRIBUTE_ACTTYPE, this.acttype);
		writeXMLEnd(out);
	}
}
