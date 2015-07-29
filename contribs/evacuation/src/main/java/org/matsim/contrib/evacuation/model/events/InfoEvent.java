/* *********************************************************************** *
 * project: org.matsim.*
 * InfoEvent.java
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
package org.matsim.contrib.evacuation.model.events;

import org.matsim.api.core.v01.events.Event;

public class InfoEvent extends Event {
	public static final String EVENT_TYPE = "InfoEvent";
	private final String info;
	public InfoEvent(double time, String info) {
		super(time);
		this.info = info;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public String getInfo(){
		return this.info;
	}

}
