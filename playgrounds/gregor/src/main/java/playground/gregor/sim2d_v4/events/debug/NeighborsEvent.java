/* *********************************************************************** *
 * project: org.matsim.*
 * Neighbors.java
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

package playground.gregor.sim2d_v4.events.debug;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;

import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;

public class NeighborsEvent extends Event {

	public static final String ATTRIBUTE_PERSON = "person";
	public static final String EVENT_TYPE = "SIM2D_DEBUG_Neighbors";
	private final Id id;
	private final List<Sim2DAgent> n;
	private final Sim2DAgent a;
	
	public NeighborsEvent(double time, Id id, List<Sim2DAgent> ret, Sim2DAgent a) {
		super(time);
		this.id = id;
		this.n = ret;
		this.a = a;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_PERSON, this.id.toString());
		return attr;
	}
	
	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	public List<Sim2DAgent> getNeighbors() {
		return this.n;
	}
	
	public Sim2DAgent getAgent() {
		return this.a;
	}

}
