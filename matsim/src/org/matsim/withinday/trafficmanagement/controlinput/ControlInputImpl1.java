/* *********************************************************************** *
 * project: org.matsim.*
 * ControlInputImpl1.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.withinday.trafficmanagement.controlinput;

import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.population.routes.CarRoute;
import org.matsim.withinday.trafficmanagement.ControlInput;

/**
 * @author a.bergsten, d.zetterberg
 *
 */

/*
 *
 * Measures the travel time difference between route 1 and 2 and returns that as
 * the control signal. ("Reactive control")
 */

public class ControlInputImpl1 extends AbstractControlInputImpl implements
		LinkLeaveEventHandler, LinkEnterEventHandler,
		AgentDepartureEventHandler, AgentArrivalEventHandler, ControlInput {


	public ControlInputImpl1() {}

	@Override
	public double getPredictedNashTime(CarRoute route) {
		if (route.equals(this.mainRoute)) {
			return this.lastTimeMainRoute;
		}
		return this.lastTimeAlternativeRoute;
	}


	@Override
	public double getNashTime() {
		super.getNashTime();
		return this.timeDifference;
	}

	public void reset(int iteration) {}



}
