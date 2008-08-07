/* *********************************************************************** *
 * project: org.matsim.*
 * ControlInputImplDistribution.java
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
import java.util.Iterator;
import java.util.List;

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
import org.matsim.withinday.trafficmanagement.Accident;
import org.matsim.withinday.trafficmanagement.ControlInput;
import org.matsim.withinday.trafficmanagement.controlinput.ControlInputWriter;

/**
 * Just like ControlInputSB, this model checks if the agents before the
 * bottleneck will cause a queue or not, and based on that predicts the
 * nashtime. However, the prediction is improved by checking the distribution of
 * the traffic before the bottleneck.
 *
 * @author abergsten and dzetterberg
 */

/*
 * FIXME [kn] Because this class was build to replace NashWriter, it inherits a
 * serious flaw: This class takes args of type Route in ctor, and returns
 * arguments of type route at getRoute, but these routes are of different type
 * (one with FakeLink, the other with behavioral links).
 */

/*
 * TODO [abergsten] iterate approach to find several "charges" of traffic with
 * distances between them. Fixed: abergsten
 */

public class ControlInputImplDistribution extends AbstractControlInputImpl
		implements LinkLeaveEventHandler, LinkEnterEventHandler,
		AgentDepartureEventHandler, AgentArrivalEventHandler, ControlInput {

	double predTTRoute1;

	double predTTRoute2;

	private ControlInputWriter writer;

	private List<Accident> accidents;

	public ControlInputImplDistribution() {
		super();
		this.writer = new ControlInputWriter();
	}

	@Override
	public void init() {
		super.init();
		this.writer.open();

	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		super.handleEvent(event);
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
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

	// calculates the predictive NashTime with a single-bottle-neck-model.
	public double getPredictedNashTime() {

		String accidentLinkId = this.accidents.get(0).getLinkId();
		Link bottleNeckLinkRoute1 = searchAccidentsOnRoutes(accidentLinkId);

		// first link on route used as default -- should be the bottleneck specific
		// to the route
		// Link bottleNeckLinkRoute2 = this.alternativeRoute.getLinkRoute()[0];
		this.predTTRoute1 = getPredictedTravelTime(this.mainRoute,
				bottleNeckLinkRoute1);
		this.predTTRoute2 = getPredictedTravelTime(this.alternativeRoute,
				this.altRouteNaturalBottleNeck);

		return this.predTTRoute1 - this.predTTRoute2;
	}

	private double getPredictedTravelTime(final Route route,
			final Link bottleNeckLink) {
		Link[] routeLinks = route.getLinkRoute();
		double bottleNeckCapacity = ((QueueLink) bottleNeckLink)
				.getSimulatedFlowCapacity()
				/ SimulationTimer.getSimTickTime();

		// get the array index of the bottleneck link
		int bottleNeckArrayIndex = 0;
		for (int i = 0; i < routeLinks.length; i++) {
			if (bottleNeckLink.equals(routeLinks[i])) {
				bottleNeckArrayIndex = i;
				break;
			}
		}

		/*
		 * Checking which links on the route that have agents few enough not to
		 * build a queue at the bottleneck (non-critical links).
		 */
		double ttFreeSpeedPart = 0.0;

		// System.out.println("");
		// System.out.println("BN link is " +
		// routeLinks[bottleNeckArrayIndex].getId().toString() + ", index: "+
		// bottleNeckArrayIndex);

		// Sum up free speed links after BN
		for (int i = bottleNeckArrayIndex + 1; i < routeLinks.length; i++) {
			ttFreeSpeedPart += this.ttFreeSpeeds
					.get(routeLinks[i].getId().toString());
			// System.out.println("ttfreespeedpart, added link " +
			// routeLinks[i].getId().toString() + " with index " + i + " after BN. " +
			// ttFreeSpeedPart);
		}

		Link criticalCongestedLink = null;
		int arrayIndexCCL = 0;
		int agentsToQueueAtBottleNeck = 0;

		for (int r = bottleNeckArrayIndex; r >= 0; r--) {
			Link link = routeLinks[r];
			double linkAgents = this.numberOfAgents.get(link.getId().toString());
			double linkFreeSpeedTT = this.ttFreeSpeeds.get(link.getId().toString());

			if ((linkAgents / bottleNeckCapacity) <= linkFreeSpeedTT) {
				ttFreeSpeedPart += linkFreeSpeedTT;
				// System.out.println("Link " + link.getId().toString() + " was not
				// congested. Added to freeSpeedPart.");

			}

			else {

				int agentsUpToLink = 0;
				double freeSpeedUpToLink = 0;
				for (int p = 0; p <= r; p++) {
					agentsUpToLink += this.numberOfAgents.get(routeLinks[p].getId()
							.toString());
					freeSpeedUpToLink += this.ttFreeSpeeds.get(routeLinks[p].getId()
							.toString());
				}
				if ((agentsUpToLink / bottleNeckCapacity) >= freeSpeedUpToLink) {
					criticalCongestedLink = link; // we only care about agents up to and
																				// including
					agentsToQueueAtBottleNeck = agentsUpToLink;

					// System.out.println("Link " + link.getId().toString() + " was
					// congested and all agents before ( " + agentsToQueueAtBottleNeck + "
					// ) will queue." );
					break;
				}

				else {
					ttFreeSpeedPart += linkFreeSpeedTT;
					// System.out.println("Link " + link.getId().toString() + " was
					// congested but queue will dissolve before you arrive at BN." );

				}
			}
		}
		if (criticalCongestedLink != null) {
			// System.out.println("You will queue with agents ahead of you up to and
			// including link " + arrayIndexCCL);
		}
		else {
			// System.out.println("You will not queue at the bottleneck. There was no
			// critical congestend link.");
		}

		double predictedTT = (agentsToQueueAtBottleNeck / bottleNeckCapacity)
				+ ttFreeSpeedPart;
		// System.out.println("predicted tt = agentsToQueueAtBottleNeck /
		// bottleNeckCapacity + ttFreeSpeedPart = " +
		// agentsToQueueAtBottleNeck + " / " + bottleNeckCapacity + " + " +
		// ttFreeSpeedPart + " = " + predictedTT);
		return predictedTT;
	}

	private Link searchAccidentsOnRoutes(final String accidentLinkId) {
		Route r = this.mainRoute;
		for (int j = 0; j < 2; j++) {
			Link[] links = r.getLinkRoute();
			for (int i = 0; i < links.length; i++) {
				if (links[i].getId().toString().equalsIgnoreCase(accidentLinkId)) {
					return links[i];
				}
			}
		}
		throw new IllegalArgumentException(
				"The set Accident has to be on one of the routes if using this implementation of ControlInput!");
	}

	// ContolInputI interface methods:
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

	public void setAccidents(final List<Accident> accidents) {
		this.accidents = accidents;
	}
}
