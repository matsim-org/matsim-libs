/* *********************************************************************** *
 * project: org.matsim.*
 * LaneExitEvent
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
package org.matsim.core.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;


/**
 * @author dgrether
 *
 */
public class LaneLeaveEventImpl extends LaneEventImpl implements LaneLeaveEvent {
	
	public static final String EVENT_TYPE = "entered lane";
	
	public LaneLeaveEventImpl(double time, Person agent, Link link, Id laneId) {
		super(time, agent, link, laneId);
	}

	public LaneLeaveEventImpl(double time, Id agentId, Id linkId, Id laneId) {
		super(time, agentId, linkId, laneId);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
}
