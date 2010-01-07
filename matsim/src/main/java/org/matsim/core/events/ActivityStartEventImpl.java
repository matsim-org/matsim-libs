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

package org.matsim.core.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.api.experimental.events.ActivityStartEvent;

public class ActivityStartEventImpl extends ActivityEventImpl implements ActivityStartEvent {

	public static final String EVENT_TYPE = "actstart";

	public ActivityStartEventImpl(final double time, final Id agentId, final Id linkId, final Activity act) {
		super(time, agentId, linkId, act);
	}

	public ActivityStartEventImpl(final double time, final Id agentId, final Id linkId, final String acttype) {
		super(time, agentId, linkId, acttype);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

}
