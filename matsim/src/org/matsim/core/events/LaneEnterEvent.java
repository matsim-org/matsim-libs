/* *********************************************************************** *
 * project: org.matsim.*
 * LaneEnterEvent
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

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Person;


/**
 * @author dgrether
 *
 */
public class LaneEnterEvent extends LaneEvent {
	
	public static final String EVENT_TYPE = "entered lane";

	/**
	 * @param time
	 * @param agent
	 * @param link
	 * @param laneId 
	 */
	public LaneEnterEvent(double time, Person agent, Link link, Id laneId) {
		super(time, agent, link, laneId);
	}

	/**
	 * @param time
	 * @param agentId
	 * @param linkId
	 */
	public LaneEnterEvent(double time, Id agentId, Id linkId, Id laneId) {
		super(time, agentId, linkId, laneId);
	}

	/**
	 * @see org.matsim.core.events.BasicEventImpl#getEventType()
	 */
	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}


	
	/**
	 * @see org.matsim.core.events.BasicEventImpl#getTextRepresentation()
	 */
	@Override
	public String getTextRepresentation() {
		return asString() + "5\t" + EVENT_TYPE;
	}



}
