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

import java.util.Map;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.facilities.ActivityFacility;

public class ActivityStartEvent extends Event implements HasPersonId, BasicLocation{

	public static final String EVENT_TYPE = "actstart";
	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_FACILITY = "facility";
	public static final String ATTRIBUTE_ACTTYPE = "actType";


	private final Id<Person> personId;
	private final Coord coord;
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
	public ActivityStartEvent( final double time, final Id<Person> agentId, final Id<Link> linkId,
		final Id<ActivityFacility> facilityId, final String acttype, Coord coord ) {
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

	public Id<Link> getLinkId() {
		return this.linkId;
	}

	public Id<ActivityFacility> getFacilityId() {
		return this.facilityId;
	}
	
	public Id<Person> getPersonId() {
		return this.personId;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		// personId is automatic in superclass
		// coord is automatic in superclass
		if (this.linkId != null) {
			attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		}
		if (this.facilityId != null) {
			attr.put(ATTRIBUTE_FACILITY, this.facilityId.toString());
		}
		attr.put(ATTRIBUTE_ACTTYPE, this.acttype);
		return attr;
	}
	@Override public Coord getCoord(){
		return coord;
	}
}
