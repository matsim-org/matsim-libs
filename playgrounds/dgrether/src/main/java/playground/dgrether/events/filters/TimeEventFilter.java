/* *********************************************************************** *
 * project: org.matsim.*
 * TimeEventFilter
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
package playground.dgrether.events.filters;

import org.matsim.api.core.v01.events.Event;


/**
 * @author dgrether
 *
 */
public class TimeEventFilter implements EventFilter {

	private double startTime;
	private double endTime;
	
	@Override
	public boolean doProcessEvent(Event event) {
		return (startTime <= event.getTime()) && (endTime >= event.getTime());
	}
	
	public double getStartTime() {
		return startTime;
	}

	
	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	
	public double getEndTime() {
		return endTime;
	}

	
	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

}
