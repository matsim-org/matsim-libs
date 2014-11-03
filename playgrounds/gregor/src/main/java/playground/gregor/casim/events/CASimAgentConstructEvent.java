/* *********************************************************************** *
 * project: org.matsim.*
 * CASimAgentConstructEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.gregor.casim.events;

import org.matsim.api.core.v01.events.Event;

import playground.gregor.casim.simulation.physics.CAMoveableEntity;

public class CASimAgentConstructEvent extends Event{

	private final CAMoveableEntity agent;

	public CASimAgentConstructEvent(double time, CAMoveableEntity a) {
		super(time);
		this.agent = a;
	}

	public static final String EVENT_TYP = "CASIM_AGENT_CONSTR";
	
	@Override
	public String getEventType() {
		return EVENT_TYP;
	}
	
	public CAMoveableEntity getCAAgent() {
		return this.agent;
	}

}
