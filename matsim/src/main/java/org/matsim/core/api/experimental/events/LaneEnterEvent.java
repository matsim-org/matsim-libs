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
package org.matsim.core.api.experimental.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.lanes.data.v20.Lane;


/**
 * Design considerations: <ul>
 * <li> This class deliberately does <i>not</i> implement HasPersonId.  One reason is that it does not really
 * belong at this level (since it is the vehicle that enters/leaves links); another reason is that this would
 * make an "instanceof HasPersonId" considerably more expensive. kai/dg, dec'12
 * </ul> 
 *
 * @author dgrether
 *
 */
public final class LaneEnterEvent extends Event  {
	
	public static final String EVENT_TYPE = "entered lane";

	public LaneEnterEvent(double time, Id<Person> agentId, Id<Link> linkId, Id<Lane> laneId) {
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

	private final Id<Link> linkId;

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_PERSON, this.personId.toString());
		attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		attr.put(ATTRIBUTE_LANE, this.laneId.toString());
		return attr;
	}

	public Id<Link> getLinkId() {
		return this.linkId;
	}

	public static final String ATTRIBUTE_PERSON = "person";

	private final Id<Person> personId;


	public Id<Person> getPersonId() {
		return this.personId;
	}
	
	
	private final Id<Lane> laneId;

	public Id<Lane> getLaneId() {
		return this.laneId;
	}


}
