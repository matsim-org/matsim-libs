/* *********************************************************************** *
 * project: org.matsim.*
 * ActStartEvent.java
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

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;

import java.util.Map;

import static org.matsim.core.utils.io.XmlUtils.writeEncodedAttributeKeyValue;

public class ActivityStartEvent extends Event implements HasFacilityId, HasPersonId, HasLinkId, BasicLocation{

	public static final String EVENT_TYPE = "actstart";
	public static final String ATTRIBUTE_ACTTYPE = "actType";


	private final Id<Person> personId;
	private Coord coord;
	private final Id<Link> linkId;
	private final Id<ActivityFacility> facilityId;
	private final String acttype;

	/*
	Possible transition path to "coordinates in event":
	- invalidate previous constructor so that we see where we have problems.
	- be minimalistic in repairing (e.g. only in matsim core).  As a tendency, when the event constructor already contains null args (e.g. for facility),
	 then we can put null for coord as well.
	- re-instantiate previous constructor.  I find this better than null because one can still set the constructor to deprecated and get compile time
	warnings.  kai, dec'19
	 */

//	public ActivityStartEvent( final double time, final Id<Person> agentId, final Activity  activity ){
//		this( time, agentId, activity.getLinkId(), activity ) ;
//	}
//	public ActivityStartEvent( final double time, final Id<Person> agentId, final Id<Link> linkId, final Activity activity ) {
//		this( time, agentId, linkId, activity.getFacilityId(), activity.getType(), activity.getCoord() ) ;
//	}

	/**
	 * @deprecated -- add Coord as argument
	 */
	@Deprecated // add Coord as argument
	public ActivityStartEvent( final double time, final Id<Person> agentId, final Id<Link> linkId, final Id<ActivityFacility> facilityId, final String acttype ){
		this( time, agentId, linkId, facilityId, acttype, null);
	}
	// this is the new constructor:
	public ActivityStartEvent( final double time, final Id<Person> agentId, final Id<Link> linkId,
				   final Id<ActivityFacility> facilityId, final String acttype, final Coord coord ) {
		super(time);
		this.linkId = linkId;
		this.facilityId = facilityId;
		this.acttype = acttype == null ? "" : acttype;
		this.personId = agentId;
		this.coord = coord;
	}

	@Override public String getEventType() {
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
		// personId is automatic in superclass
		// coord is automatic in superclass
		// linkId is automatic in superclass
		// facilityId is automatic in superclass
		attr.put(ATTRIBUTE_ACTTYPE, this.acttype);
		return attr;
	}
	@Override public Coord getCoord(){
		return coord;
	}
	public void setCoord( Coord coord ) {
		// yy  this is to retrofit the coordinate into existing events that don't have it.  :-(  kai, mar'20
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
