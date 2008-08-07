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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.LinkEnterEnter;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.withinday.trafficmanagement.AbstractControlInputImpl;
import org.matsim.withinday.trafficmanagement.ControlInput;

/**
 * @author a.bergsten, d.zetterberg
 *
 */

/*
 *
 * Measures the  travel time difference between route 1 and 2 and returns that as
 * the control signal. ("Reactive control")
 */

public class ControlInputImpl1 extends AbstractControlInputImpl implements
		EventHandlerLinkLeaveI, EventHandlerLinkEnterI,
		EventHandlerAgentDepartureI, EventHandlerAgentArrivalI, ControlInput {

	private ControlInputWriter writer;

	public ControlInputImpl1() {
		this.writer = new ControlInputWriter();
	}

	@Override
	public void init() {
		super.init();
		this.writer.open();

	}

	@Override
	public void handleEvent(final LinkEnterEnter event) {
		super.handleEvent(event);
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		super.handleEvent(event);
	}

	public double getNashTime() {

		try {
			this.writer.writeTravelTimesMainRoute(this.lastTimeMainRoute,
					this.lastTimeMainRoute);
			this.writer.writeTravelTimesAlternativeRoute(
					this.lastTimeAlternativeRoute, this.lastTimeAlternativeRoute);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return this.timeDifference;
	}

	public void reset(int iteration) {
		// nothing need to be done here anymore cause everything is done in the
		// finishIteration().
	}

	public void finishIteration() {
		BufferedWriter w1 = null;
		BufferedWriter w2 = null;
		try {
			w1 = new BufferedWriter(new FileWriter(
					"../studies/arvidDaniel/output/ttMeasuredMainRoute.txt"));
			w2 = new BufferedWriter(new FileWriter(
					"../studies/arvidDaniel/output/ttMeasuredAlternativeRoute.txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		Iterator<Double> it1 = this.ttMeasuredMainRoute.iterator();
		try {
			while (it1.hasNext()) {
				double measuredTimeMainRoute = it1.next();
				w1.write(Double.toString(measuredTimeMainRoute));
				w1.write("\n");
				w1.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		Iterator<Double> it2 = this.ttMeasuredAlternativeRoute.iterator();
		try {
			while (it2.hasNext()) {
				double measuredTimeAlternativeRoute = it2.next();
				w2.write(Double.toString(measuredTimeAlternativeRoute));
				w2.write("\n");
				w2.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			w1.close();
			w2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.writer.close();
	}

	@Override
	public void handleEvent(final AgentDepartureEvent event) {
		super.handleEvent(event);
	}

	@Override
	public void handleEvent(final AgentArrivalEvent event) {
		super.handleEvent(event);
	}

}
