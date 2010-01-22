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

package org.matsim.core.events;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;

public class ActivityEndEventImpl extends ActivityEventImpl implements ActivityEndEvent {

	public static final String EVENT_TYPE = "actend";

	public ActivityEndEventImpl(final double time, final Id agentId, final Id linkId, final Id facilityId, final String acttype) {
		super(time, agentId, linkId, facilityId, acttype);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

}
