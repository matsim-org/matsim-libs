/* *********************************************************************** *
 * project: org.matsim.*
 * RectEvent.java
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

package matsimConnector.events.debug;

import org.matsim.api.core.v01.events.Event;

public class RectEvent extends Event {

	private final double tx;
	private final double ty;
	private final double sx;
	private final double sy;
	private final boolean fill;
	
	private static final String EVENT_TYPE = "RECT_EVENT";

	public RectEvent(double time, double tx, double ty, double sx, double sy,boolean fill) {
		super(time);
		this.tx = tx;
		this.ty = ty;
		this.sx = sx;
		this.sy = sy;
		this.fill = fill;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	public double getTx() {
		return this.tx;
	}

	public double getTy() {
		return this.ty;
	}

	public double getSx() {
		return this.sx;
	}

	public double getSy() {
		return this.sy;
	}

	public boolean getFill() {
		return this.fill;
	}

}
