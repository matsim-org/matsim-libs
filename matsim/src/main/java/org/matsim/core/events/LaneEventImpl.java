/* *********************************************************************** *
 * project: org.matsim.*
 * LaneEvent
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

import java.util.Map;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public abstract class LaneEventImpl extends LinkEventImpl {

	public static final String ATTRIBUTE_LANE = "lane";
	public static final String ATTRIBUTE_LINK = "link";

	private final Id linkId;

	LaneEventImpl(final double time, final Id agentId, final Id linkId, final Id laneId) {
		super(time, agentId, linkId);
		this.laneId = laneId;
		this.personId = agentId;
		this.linkId = linkId;
	}

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
