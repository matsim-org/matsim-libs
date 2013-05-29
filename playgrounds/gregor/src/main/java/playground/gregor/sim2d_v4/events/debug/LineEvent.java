/* *********************************************************************** *
 * project: org.matsim.*
 * LineEvent.java
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

import org.matsim.core.api.experimental.events.Event;

import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection.Segment;

public class LineEvent extends Event {

	private static final String TYPE = "LINE_EVENT";
	
	private final boolean isStatic;
	private final Segment s;

	public LineEvent(double time,Segment s, boolean isStatic) {
		super(time);
		this.s = s;
		this.isStatic = isStatic;
	}

	@Override
	public String getEventType() {
		return TYPE;
	}
	
	public Segment getSegment() {
		return this.s;
	}
	
	public boolean isStatic() {
		return this.isStatic;
	}

}
