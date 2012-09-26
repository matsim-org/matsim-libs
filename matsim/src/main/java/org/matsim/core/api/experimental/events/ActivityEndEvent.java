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

package org.matsim.core.api.experimental.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;

public class ActivityEndEvent extends Event {

	public static final String EVENT_TYPE = "actend";
	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_FACILITY = "facility";
	public static final String ATTRIBUTE_ACTTYPE = "actType";

	private final Id linkId;
	private final Id facilityId;
	private final String acttype;
	
	public static final String ATTRIBUTE_PERSON = "person";

	private final Id personId;
	
	
	public ActivityEndEvent(final double time, final Id agentId, final Id linkId, final Id facilityId, final String acttype) {
		super(time);
		this.linkId = linkId;
		this.facilityId = facilityId;
		this.acttype = acttype == null ? "" : acttype;
		this.personId = agentId;
	}

	public String getEventType() {
		return EVENT_TYPE;
	}

	public String getActType() {
		return this.acttype;
	}

	public Id getLinkId() {
		return this.linkId;
	}

	public Id getFacilityId() {
		return this.facilityId;
	}
	
	public Id getPersonId() {
		return this.personId;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_PERSON, this.personId.toString());
		attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		if (this.facilityId != null) {
			attr.put(ATTRIBUTE_FACILITY, this.facilityId.toString());
		}
		attr.put(ATTRIBUTE_ACTTYPE, this.acttype);
		return attr;
	}

	
}
