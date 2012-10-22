/* *********************************************************************** *
 * project: org.matsim.*
 * JointDepartureEvent.java
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;

public class JointDepartureEvent extends Event {

	public static final String EVENT_TYPE = "jointdeparture";

	public static final String ATTRIBUTE_DEPARTURE = "departure";

	private final Id jointDepartureId;

	public JointDepartureEvent(final double time, final Id jointDepartureId) {
		super(time);
		this.jointDepartureId = jointDepartureId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_DEPARTURE, this.jointDepartureId.toString());
		return attr;
	}

	public Id getJointDepartureId() {
		return this.jointDepartureId;
	}

	public String getEventType() {
		return EVENT_TYPE;
	}

}
