/* *********************************************************************** *
 * project: org.matsim.*
 * ControlInputImplDAccident.java
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

package org.matsim.withinday.trafficmanagement.controlinput.obsoletemodels;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.SimulationTimer;
import org.matsim.network.Link;
import org.matsim.population.Route;
import org.matsim.withinday.trafficmanagement.AbstractControlInputImpl;
import org.matsim.withinday.trafficmanagement.ControlInput;
import org.matsim.withinday.trafficmanagement.controlinput.ControlInputWriter;

// import sun.rmi.runtime.GetThreadPoolAction;

/**
 * Just like ControlInputSB, this model checks if the agents before the
 * bottleneck will cause a queue or not, and based on that predicts the time
 * difference between two alternative routes.
 *
 * This model automatically and continuosly detects bottlenecks and therefore
 * does not use information about the accident.
 *
 *
 * @author abergsten and dzetterberg
 */

/*
 * User parameters are:
 *
 * NUMBEROFFLOWEVENTS The flow calculations are based on the last
 * NUMBEROFFLOWEVENTS agents. A higher value means better predictions if
 * congestion. IGNOREDQUEUINGIME Additional link travel times up to
 * IGNOREDQUEUINGIME will not be considered a sign of temporary capacity
 * reduction.
 *
 */

public class ControlInputImplStaticAddition extends AbstractControlInputImpl
		implements LinkLeaveEventHandler, LinkEnterEventHandler,
		AgentDepartureEventHandler, AgentArrivalEventHandler, ControlInput {

	private static final int NUMBEROFFLOWEVENTS = 20;

	private static final double IGNOREDQUEUINGIME = 5;

	// private static final boolean DISTRIBUTIONCHECK = false;

	private static final Logger log = Logger
			.getLogger(ControlInputImplStaticAddition.class);

	double predTTRoute1;

	double predTTRoute2;

	private ControlInputWriter writer;

	private Map<String, Double> ttMeasured = new HashMap<String, Double>();

	private Map<String, Double> enterLinkEvents = new HashMap<String, Double>();

	private Map<String, Double> linkFlows = new HashMap<String, Double>();

	private Map<String, List<Double>> enterLinkEventTimes = new HashMap<String, List<Double>>();

	private Map<String, Double> capacities = new HashMap<String, Double>();

	public ControlInputImplStaticAddition() {
		super();
		this.writer = new ControlInputWriter();
	}

	@Override
	public void init() {
		super.init();
		this.writer.open();

		// Initialize ttMeasured with ttFreeSpeeds and linkFlows with zero.
		// Main route
		Link[] routeLinks = this.getMainRoute().getLinkRoute();
		for (Link l : routeLinks) {
			if (!this.linkFlows.containsKey(l.getId().toString())) {
				this.linkFlows.put(l.getId().toString(), 0.0);
			}

			if (!this.ttMeasured.containsKey(l.getId().toString())) {
				this.ttMeasured.put(l.getId().toString(), this.ttFreeSpeeds.get(l
						.getId().toString()));
			}

			if (!this.capacities.containsKey(l.getId().toString())) {
				this.capacities.put(l.getId().toString(), ((QueueLink) l)
						.getSimulatedFlowCapacity()
						/ SimulationTimer.getSimTickTime());
			}

			if (!this.enterLinkEventTimes.containsKey(l.getId().toString())) {
				List<Double> list = new LinkedList<Double>();
				this.enterLinkEventTimes.put(l.getId().toString(), list);
			}
		}

		// Alt Route
		routeLinks = this.getAlternativeRoute().getLinkRoute();
		for (Link l : routeLinks) {
			if (!this.linkFlows.containsKey(l.getId().toString())) {
				this.linkFlows.put(l.getId().toString(), 0.0);
			}

			if (!this.ttMeasured.containsKey(l.getId().toString())) {
				this.ttMeasured.put(l.getId().toString(), this.ttFreeSpeeds.get(l
						.getId().toString()));
			}

			if (!this.capacities.containsKey(l.getId().toString())) {
				this.capacities.put(l.getId().toString(), ((QueueLink) l)
						.getSimulatedFlowCapacity()
						/ SimulationTimer.getSimTickTime());
			}

			if (!this.enterLinkEventTimes.containsKey(l.getId().toString())) {
				List<Double> list = new LinkedList<Double>();
				this.enterLinkEventTimes.put(l.getId().toString(), list);
			}
		}
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {

		if (this.ttMeasured.containsKey(event.linkId)) {
			this.enterLinkEvents.put(event.agentId, event.time);
		}

		super.handleEvent(event);
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {

		if (this.ttMeasured.containsKey(event.linkId)) {
			Double enterTime = this.enterLinkEvents.remove(event.agentId);
			Double travelTime = event.time - enterTime;
			this.ttMeasured.put(event.linkId, travelTime);

		}

		// Stores [NUMBEROFFLOWEVENTS] last events and calculates flow
		if (this.linkFlows.containsKey(event.linkId)) {
			LinkedList<Double> list = (LinkedList<Double>) this.enterLinkEventTimes
					.get(event.linkId);
			if (list.size() == NUMBEROFFLOWEVENTS) {
				list.removeFirst();
				list.add(event.time);
			}
			else if ((1 < list.size()) || (list.size() < NUMBEROFFLOWEVENTS)) {
				list.add(event.time);
			}
			else if (list.size() == 0) {
				list.add(event.time - 1);
				list.add(event.time);
			}
			else {
				System.err
						.println("Error: number of enter event times stored exceeds numberofflowevents!");
			}

			// Flow = agents / seconds:
			double flow = (list.size() - 1) / (list.getLast() - list.getFirst());
			this.linkFlows.put(event.linkId, flow);
		}

		super.handleEvent(event);
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

	public double getNashTime() {

		try {
			this.writer.writeAgentsOnLinks(this.numberOfAgents);
			this.writer.writeTravelTimesMainRoute(this.lastTimeMainRoute,
					this.predTTRoute1);
			this.writer.writeTravelTimesAlternativeRoute(
					this.lastTimeAlternativeRoute, this.predTTRoute2);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return getPredictedNashTime();
	}

	// calculates the predictive NashTime with a single-bottle-neck-model.
	public double getPredictedNashTime() {

		this.predTTRoute1 = getPredictedTravelTime(this.mainRoute,
				this.mainRouteNaturalBottleNeck);
		this.predTTRoute2 = getPredictedTravelTime(this.alternativeRoute,
				this.altRouteNaturalBottleNeck);
		return this.predTTRoute1 - this.predTTRoute2;
	}

	private double getPredictedTravelTime(final Route route,
			final Link bottleNeckLink) {

		Link[] routeLinks = route.getLinkRoute();
		double predictedTT = 0;

		for (int i = routeLinks.length - 1; i >= 0; i--) {
			Link link = routeLinks[i];
			String linkId = link.getId().toString();
			double linkTT;

			if (this.ttMeasured.get(linkId) > this.ttFreeSpeeds.get(linkId)
					+ IGNOREDQUEUINGIME) {

				if (this.linkFlows.get(linkId) < this.ttFreeSpeeds.get(linkId)) {
					linkTT = this.numberOfAgents.get(linkId) / this.linkFlows.get(linkId);
				}
				else {
					linkTT = this.numberOfAgents.get(linkId)
							/ this.capacities.get(linkId);
				}
			}
			else {
				linkTT = this.ttFreeSpeeds.get(linkId);
			}
			predictedTT += linkTT;
		}
		return predictedTT;

	}
}
