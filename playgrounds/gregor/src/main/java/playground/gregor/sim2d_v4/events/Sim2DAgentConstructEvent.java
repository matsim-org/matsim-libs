/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DAgentConstuctEvent.java
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

package playground.gregor.sim2d_v4.events;

import java.util.Map;

import org.matsim.api.core.v01.events.Event;

import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;

public class Sim2DAgentConstructEvent extends Event {

	public static final String EVENT_TYPE = "Sim2DAgentConstructEvent";
	public static final String ATTRIBUTE_PERSON = "person";
	private final Sim2DAgent agent;
	
	public Sim2DAgentConstructEvent(double time, Sim2DAgent agent) {
		super(time);
		this.agent = agent;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_PERSON, this.agent.getId().toString());
		return attr;
	}
	
	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Sim2DAgent getSim2DAgent() {
		return this.agent;
	}
}
