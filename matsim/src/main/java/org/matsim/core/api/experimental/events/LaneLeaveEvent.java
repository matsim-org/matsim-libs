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
package org.matsim.core.api.experimental.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class LaneLeaveEvent extends Event {
	
	public static final String EVENT_TYPE = "left lane";
	
	public LaneLeaveEvent(double time, Id agentId, Id linkId, Id laneId) {
		super(time);
		this.laneId = laneId;
		this.personId = agentId;
		this.linkId = linkId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	public static final String ATTRIBUTE_LANE = "lane";
	public static final String ATTRIBUTE_LINK = "link";

	private final Id linkId;

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_PERSON, this.personId.toString());
		attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		attr.put(ATTRIBUTE_LANE, this.laneId.toString());
		return attr;
	}

	public Id getLinkId() {
		return this.linkId;
	}

	public static final String ATTRIBUTE_PERSON = "person";

	private final Id personId;


	public Id getPersonId() {
		return this.personId;
	}
	
	
	private final Id laneId;

	public Id getLaneId() {
		return this.laneId;
	}


}
