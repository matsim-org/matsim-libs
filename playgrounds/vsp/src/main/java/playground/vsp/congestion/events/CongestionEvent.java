/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.vsp.congestion.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

/**
 * Event to indicate that an agent entering or leaving a link is delaying other agents on that link.
 * 
 * @author ikaddoura
 */
public final class CongestionEvent extends Event {
	
	public static final String EVENT_TYPE = "MarginalCongestionEffect";
	public static final String EVENT_CAPACITY_CONSTRAINT = "capacityConstraint";
	public static final String ATTRIBUTE_PERSON = "causingAgent";
	public static final String ATTRIBUTE_AFFECTED_AGENT = "affectedAgent";
	public static final String ATTRIBUTE_DELAY = "delay";
	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_EMERGENCETIME = "emergenceTime"; // i.e. the time when the causing agent enters the link
	
	private final Id<Person> causingAgentId;
	private final Id<Person> affectedAgentId;
	private final double delay;
	private final Id<Link> linkId;
	private final String capacityConstraint;
	private final double emergenceTime;

	public CongestionEvent(double time, String capacityConstraint, Id<Person> causingAgentId, Id<Person> affectedAgentId, double externalDelay, Id<Link> linkId, double emergenceTime) {
		super(time);
		this.capacityConstraint = capacityConstraint;
		this.causingAgentId = causingAgentId;
		this.affectedAgentId = affectedAgentId;
		this.delay = externalDelay;
		this.linkId = linkId;
		this.emergenceTime = emergenceTime;
	}
	
	public double getEmergenceTime(){
		return emergenceTime;
	}
	
	public double getDelay() {
		return delay;
	}

	public Id<Person> getCausingAgentId() {
		return causingAgentId;
	}

	public Id<Person> getAffectedAgentId() {
		return affectedAgentId;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}
	
	public String getCapacityConstraint() {
		return capacityConstraint;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attrs = super.getAttributes();
		attrs.put(EVENT_CAPACITY_CONSTRAINT, this.capacityConstraint);
		attrs.put(ATTRIBUTE_PERSON, this.causingAgentId.toString());
		attrs.put(ATTRIBUTE_AFFECTED_AGENT, this.affectedAgentId.toString());
		attrs.put(ATTRIBUTE_DELAY, Double.toString(this.delay));
		attrs.put(ATTRIBUTE_LINK, this.linkId.toString());
		attrs.put(ATTRIBUTE_EMERGENCETIME, Double.toString(this.emergenceTime));
		return attrs;
	}

}
