/* *********************************************************************** *
 * project: org.matsim.*
 * MyEventHandler2.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package tryouts.examples;

import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;

/**
 * @author thomas
 *
 */
public class MyEventHandler2 implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler{
	
	private double travelTime = 0.0;
	private int popSize;
	
	public MyEventHandler2(int popSize) {
		this.popSize = popSize;
	}

	public double getTotalTravelTime() {
		return this.travelTime;
	}
	
	public double getAverageTravelTime() {
		return this.travelTime / this.popSize;
	}

	public void reset(int iteration) {
		this.travelTime = 0.0;
	}

	public void handleEvent(LinkEnterEvent event) {
		this.travelTime -= event.getTime();
	}

	public void handleEvent(LinkLeaveEvent event) {
		this.travelTime += event.getTime();
	}

	public void handleEvent(PersonArrivalEvent event) {
		this.travelTime += event.getTime();
	}

	public void handleEvent(PersonDepartureEvent event) {
		this.travelTime -= event.getTime();
	}

}

