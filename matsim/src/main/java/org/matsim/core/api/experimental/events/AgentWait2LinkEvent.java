/* *********************************************************************** *
 * project: org.matsim.*
 * AgentWait2LinkEvent.java
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
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.events.EventImpl;

public class AgentWait2LinkEvent extends EventImpl {

	public static final String EVENT_TYPE = "wait2link";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	private final Id vehicleId;

	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_LEGMODE = "legMode";
	public static final String ATTRIBUTE_PERSON = "person";

	private final Id personId;
	private final Id linkId;

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_PERSON, this.personId.toString());
		attr.put(ATTRIBUTE_LINK, (this.linkId == null ? null : this.linkId.toString()));
		if (this.vehicleId != null) {
			attr.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		}
		return attr;
	}

	public Id getLinkId() {
		return this.linkId;
	}


	public Id getPersonId() {
		return this.personId;
	}
	
	
	public AgentWait2LinkEvent(final double time, final Id agentId, final Id linkId, Id vehicleId) {
		super(time);
		this.personId = agentId;
		this.linkId = linkId;
		this.vehicleId = vehicleId;
	}

	public String getEventType() {
		return EVENT_TYPE;
	}

	public Id getVehicleId() {
		return vehicleId;
	}

	public String getLegMode() {
		return null;
	}

}
