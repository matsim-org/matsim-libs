/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.api.experimental.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.events.HasPersonId;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Event thrown when agent is added to stop, i.e. starts waiting.
 * 
 * @author mrieser / senozon
 */
public final class AgentWaitingForPtEvent extends Event implements HasPersonId {

	public static final String EVENT_TYPE = "waitingForPt";
	public static final String ATTRIBUTE_AGENT = "agent";
	public static final String ATTRIBUTE_WAITSTOP = "atStop";
	public static final String ATTRIBUTE_DESTINATIONSTOP = "destinationStop";
	public final Id<Person> agentId;
	public final Id<TransitStopFacility> waitingAtStopId;
	public final Id<TransitStopFacility> destinationStopId;
	
	public AgentWaitingForPtEvent(final double now, final Id<Person> agentId,
			final Id<TransitStopFacility> waitingAtStopId, final Id<TransitStopFacility> destinationStopId) {
		super(now);
		this.agentId = agentId;
		this.waitingAtStopId = waitingAtStopId;
		this.destinationStopId = destinationStopId;
	}
	
	public Id<Person> getPersonId() {
		return this.agentId;
	}
	
	public Id<TransitStopFacility> getWaitingAtStopId() {
		return this.waitingAtStopId;
	}
	
	public Id<TransitStopFacility> getDestinationStopId() {
		return this.destinationStopId;
	}
	
	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		
		attr.put(ATTRIBUTE_AGENT, this.agentId.toString());
		attr.put(ATTRIBUTE_WAITSTOP, this.waitingAtStopId.toString());
		attr.put(ATTRIBUTE_DESTINATIONSTOP, this.destinationStopId.toString());
		
		return attr;
	}
}
