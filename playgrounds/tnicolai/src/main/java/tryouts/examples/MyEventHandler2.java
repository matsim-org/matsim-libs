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

import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

/**
 * @author thomas
 *
 */
public class MyEventHandler2 implements LinkEnterEventHandler, LinkLeaveEventHandler, AgentArrivalEventHandler, AgentDepartureEventHandler{
	
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

	public void handleEvent(AgentArrivalEvent event) {
		this.travelTime += event.getTime();
	}

	public void handleEvent(AgentDepartureEvent event) {
		this.travelTime -= event.getTime();
	}

}

