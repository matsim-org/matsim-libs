/* *********************************************************************** *
 * project: org.matsim.*
 * CircleEvent.java
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

public class CircleEvent  extends Event{

	private static final String TYPE = "CIRCLE_EVENT";
	private final double x;
	private final double y;
	
	public CircleEvent(double time,double x, double y) {
		super(time);
		this.x = x;
		this.y = y;
	}

	@Override
	public String getEventType() {
		return CircleEvent.TYPE;
	}
	
	public double getX() {
		return this.x;
	}
	
	public double getY() {
		return this.y;
	}

}
