/* *********************************************************************** *
 * project: org.matsim.*
 * ControlInputImplSBNoise.java
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.AgentWait2LinkEvent;
import org.matsim.events.LinkEnterEnter;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.EventHandlerActivityEndI;
import org.matsim.events.handler.EventHandlerActivityStartI;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.events.handler.EventHandlerAgentStuckI;
import org.matsim.events.handler.EventHandlerAgentWait2LinkI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.SimulationTimer;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.population.Route;
import org.matsim.withinday.trafficmanagement.AbstractControlInputImpl;
import org.matsim.withinday.trafficmanagement.Accident;
import org.matsim.withinday.trafficmanagement.ControlInput;
import org.matsim.withinday.trafficmanagement.controlinput.ControlInputWriter;

/**
 * Does the same as ControlInputImplSB.java, but also: takes in- and outflows
 * from other roads into the travel time calculations. The additional flows are
 * measured and are assumed to be constant during the simulation time for the
 * prediction. The additional agents are then included in the travel time
 * calculation
 */

/*
 * USER PARAMETERS ARE: FLOWUPDATETIME, which determines how often to measure
 * the additional flows from in- and outlinks. Default is to update every
 * minute.
 */

public class ControlInputImplSBNoise extends AbstractControlInputImpl implements
		EventHandlerLinkLeaveI, EventHandlerLinkEnterI,
		EventHandlerAgentWait2LinkI, EventHandlerAgentStuckI,
		EventHandlerAgentDepartureI, EventHandlerAgentArrivalI,
		EventHandlerActivityStartI, EventHandlerActivityEndI, ControlInput {

	private final List<Link> inLinksMainRoute = new ArrayList<Link>();

	private final Map<String, Double> inFlowDistancesMainRoute = new HashMap<String, Double>();

	private final Map<String, Double> inFlowsMainRoute = new HashMap<String, Double>();

	private final Map<String, Integer> numbersPassedOnInAndOutLinks = new HashMap<String, Integer>();

	private final List<Link> outLinksMainRoute = new ArrayList<Link>();

	private final Map<String, Double> outFlowDistancesMainRoute = new HashMap<String, Double>();

	private final Map<String, Double> outFlowsMainRoute = new HashMap<String, Double>();

	private final List<Link> inLinksAlternativeRoute = new ArrayList<Link>();

	private final Map<String, Double> inFlowDistancesAlternativeRoute = new HashMap<String, Double>();

	private final Map<String, Double> inFlowsAlternativeRoute = new HashMap<String, Double>();

	private final List<Link> outLinksAlternativeRoute = new ArrayList<Link>();

	private final Map<String, Double> outFlowDistancesAlternativeRoute = new HashMap<String, Double>();

	private final Map<String, Double> outFlowsAlternativeRoute = new HashMap<String, Double>();

	private double predTTMainRoute;

	private double predTTAlternativeRoute;

	private final ControlInputWriter writer;

	private List<Accident> accidents;

	private Link bottleNeckLinkMainRoute;

	private Link bottleNeckLinkAlternativeRoute;

	// private double ttFreeSpeedAfterBottleNeckMainRoute;

	// private double ttFreeSpeedAfterBottleNeckAlternativeRoute;

	private int additionalAgentsMainRoute = 0;

	private int additionalAgentsAlternativeRoute = 0;

	private Link[] routeLinksMainRoute;

	private Link[] routeLinksAlternativeRoute;

	private ArrayList<Node> routeNodesMainRoute = new ArrayList<Node>();

	private ArrayList<Node> routeNodesAlternativeRoute = new ArrayList<Node>();

	private List<Link> routeLinksListMainRoute = new ArrayList<Link>();

	private List<Link> routeLinksListAlternativeRoute = new ArrayList<Link>();

	private double ttFreeSpeedBeforeBottleNeckMainRoute = 0;

	private double ttFreeSpeedBeforeBottleNeckAlternativeRoute = 0;

	private static final double FLOWUPDATETIME = 30;

	public ControlInputImplSBNoise() {
		super();
		this.writer = new ControlInputWriter();
	}

	@Override
	public void init() {
		super.init();

		// init the mainRoute
		this.routeLinksMainRoute = this.mainRoute.getLinkRoute();
		this.routeLinksListMainRoute = Arrays.asList(this.routeLinksMainRoute);
		this.routeNodesMainRoute = this.mainRoute.getRoute();
		this.routeNodesMainRoute.remove(0); // remove first Node since its inLinks
																				// shouldn't be included
		this.routeNodesMainRoute.remove(this.routeNodesMainRoute.size() - 1);
		// all distances to in- and outLinks are stored in Maps <link id, distance>
		for (Node n : this.routeNodesMainRoute) {
			for (Link inLink : n.getInLinks().values()) {
				if (!this.routeLinksListMainRoute.contains(inLink)) {
					double d = getDistanceFromFirstNode(inLink.getToNode(),
							this.routeLinksMainRoute);
					this.inLinksMainRoute.add(inLink);
					this.inFlowDistancesMainRoute.put(inLink.getId().toString(), d);
					this.inFlowsMainRoute.put(inLink.getId().toString(), 0.0);
					this.numbersPassedOnInAndOutLinks.put(inLink.getId().toString(), 0);
				}
			}
			for (Link outLink : n.getOutLinks().values()) {
				if (!this.routeLinksListMainRoute.contains(outLink)) {
					double d = getDistanceFromFirstNode(outLink.getFromNode(),
							this.routeLinksMainRoute);
					this.outLinksMainRoute.add(outLink);
					this.outFlowDistancesMainRoute.put(outLink.getId().toString(), d);
					this.outFlowsMainRoute.put(outLink.getId().toString(), 0.0);
					this.numbersPassedOnInAndOutLinks.put(outLink.getId().toString(), 0);
				}
			}
		}

		// init the alternativeRoute
		this.routeLinksAlternativeRoute = this.alternativeRoute.getLinkRoute();
		this.routeNodesAlternativeRoute = this.alternativeRoute.getRoute();
		this.routeNodesAlternativeRoute.remove(0); // first node's inLinks
																								// shouldn't be included in the
																								// model
		this.routeNodesAlternativeRoute.remove(this.routeNodesAlternativeRoute
				.size() - 1);
		this.routeLinksListAlternativeRoute = Arrays
				.asList(this.routeLinksAlternativeRoute);
		// all distances to in- and outlinks are stored in Maps <link id, distance>
		for (Node n : this.routeNodesAlternativeRoute) {
			for (Link inLink : n.getInLinks().values()) {
				if (!this.routeLinksListAlternativeRoute.contains(inLink)) {
					double d = getDistanceFromFirstNode(inLink.getToNode(),
							this.routeLinksAlternativeRoute);
					this.inLinksAlternativeRoute.add(inLink);
					this.inFlowDistancesAlternativeRoute
							.put(inLink.getId().toString(), d);
					this.inFlowsAlternativeRoute.put(inLink.getId().toString(), 0.0);
					this.numbersPassedOnInAndOutLinks.put(inLink.getId().toString(), 0);
				}
				else {
					System.out.println("No additional inLinks");
				}
			}
			for (Link outLink : n.getOutLinks().values()) {
				if (!this.routeLinksListAlternativeRoute.contains(outLink)) {
					double d = getDistanceFromFirstNode(outLink.getFromNode(),
							this.routeLinksAlternativeRoute);
					this.outLinksAlternativeRoute.add(outLink);
					this.outFlowDistancesAlternativeRoute.put(outLink.getId().toString(),
							d);
					this.outFlowsAlternativeRoute.put(outLink.getId().toString(), 0.0);
					this.numbersPassedOnInAndOutLinks.put(outLink.getId().toString(), 0);
				}
				else {
					System.out.println("No additional outLinks");
				}
			}
		}

		this.writer.open();
	}

	private double getDistanceFromFirstNode(final Node node,
			final Link[] routeLinks) {
		double distance = 0;
		int i = 0;
		while (!routeLinks[i].getToNode().equals(node)) {
			distance += routeLinks[i].getLength();
			i++;
		}
		distance += routeLinks[i].getLength();
		return distance;
	}

	// memorize linkEnterEvents on the first links of the two alternative routes:
	@Override
	public void handleEvent(final LinkEnterEnter event) {
		super.handleEvent(event);

		// handle flows on outLinks
		if (this.outLinksMainRoute.contains(event.link)) {

			// increase numbersPassed
			int numbersPassed = this.numbersPassedOnInAndOutLinks.get(event.linkId) + 1;
			this.numbersPassedOnInAndOutLinks.put(event.linkId, numbersPassed);
		}
		else if (this.outLinksAlternativeRoute.contains(event.linkId)) {

			// increase numbersPassed
			int numbersPassed = this.numbersPassedOnInAndOutLinks.get(event.linkId) + 1;
			this.numbersPassedOnInAndOutLinks.put(event.linkId, numbersPassed);
		}
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		super.handleEvent(event);

		// handle flows on inLinks
		if (this.inLinksMainRoute.contains(event.link)) {

			// increase numbersPassed
			int numbersPassed = this.numbersPassedOnInAndOutLinks.get(event.linkId) + 1;
			this.numbersPassedOnInAndOutLinks.put(event.linkId, numbersPassed);
		}
		else if (this.inLinksAlternativeRoute.contains(event.linkId)) {

			// increase numbersPassed
			int numbersPassed = this.numbersPassedOnInAndOutLinks.get(event.linkId) + 1;
			this.numbersPassedOnInAndOutLinks.put(event.linkId, numbersPassed);
		}
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

	public void handleEvent(final AgentWait2LinkEvent event) {
		// TODO Auto-generated method stub
	}

	public void handleEvent(final AgentStuckEvent event) {
		// TODO Auto-generated method stub
	}

	public void handleEvent(final ActStartEvent event) {
		// TODO Auto-generated method stub
	}

	public void handleEvent(final ActEndEvent event) {
		// TODO Auto-generated method stub
	}

	// calculates the predictive NashTime with a single-bottle-neck-model.
	public double getPredictedNashTime() {

		String accidentLinkId = this.accidents.get(0).getLinkId();
		this.bottleNeckLinkMainRoute = searchAccidentsOnRoutes(accidentLinkId);
		this.bottleNeckLinkAlternativeRoute = this.altRouteNaturalBottleNeck;
		this.additionalAgentsMainRoute = getAdditionalAgents(this.mainRoute);
		this.additionalAgentsAlternativeRoute = getAdditionalAgents(this.alternativeRoute);

		this.predTTMainRoute = getPredictedTravelTime(this.mainRoute,
				this.bottleNeckLinkMainRoute);
		this.predTTAlternativeRoute = getPredictedTravelTime(this.alternativeRoute,
				this.bottleNeckLinkAlternativeRoute);

		return this.predTTMainRoute - this.predTTAlternativeRoute;
	}

	private double getPredictedTravelTime(final Route route,
			final Link bottleNeckLink) {
		double ttFreeSpeedBeforeBottleNeck = 0.0;
		double ttFreeSpeedAfterBottleNeck;
		double bottleNeckCapacity = ((QueueLink) bottleNeckLink)
				.getSimulatedFlowCapacity()
				/ SimulationTimer.getSimTickTime();
		double predictedTT;
		Link[] routeLinks;
		if (route == this.mainRoute) {
			routeLinks = this.routeLinksMainRoute;
		}
		else {
			routeLinks = this.routeLinksAlternativeRoute;
		}

		// get the array index of the bottleneck link
		int bottleNeckLinkNumber = 0;
		for (int i = 0; i < routeLinks.length; i++) {
			if (bottleNeckLink.equals(routeLinks[i])) {
				bottleNeckLinkNumber = i;
				break;
			}
		}

		// count agents and free speed travel time BEFORE bottleneck
		double agentsBeforeBottleNeck;
		if (route == this.mainRoute) {
			agentsBeforeBottleNeck = this.additionalAgentsMainRoute;
		}
		else {
			agentsBeforeBottleNeck = this.additionalAgentsAlternativeRoute;
		}

		for (int i = 0; i <= bottleNeckLinkNumber; i++) {
			agentsBeforeBottleNeck += this.numberOfAgents.get(routeLinks[i].getId()
					.toString());

			ttFreeSpeedBeforeBottleNeck += this.ttFreeSpeeds.get(routeLinks[i]
					.getId().toString());
		}

		// sum up free speed travel time AFTER bottleneck
		ttFreeSpeedAfterBottleNeck = 0;
		for (int i = bottleNeckLinkNumber + 1; i < routeLinks.length; i++) {
			ttFreeSpeedAfterBottleNeck += this.ttFreeSpeeds.get(routeLinks[i].getId()
					.toString());
		}

		if (agentsBeforeBottleNeck / bottleNeckCapacity > ttFreeSpeedBeforeBottleNeck) {
			predictedTT = agentsBeforeBottleNeck / bottleNeckCapacity
					+ ttFreeSpeedAfterBottleNeck;
		}
		else {
			predictedTT = ttFreeSpeedBeforeBottleNeck + ttFreeSpeedAfterBottleNeck;
		}

		if (route == this.mainRoute) {
			// ttFreeSpeedAfterBottleNeckMainRoute = ttFreeSpeedAfterBottleNeck;
			this.ttFreeSpeedBeforeBottleNeckMainRoute = ttFreeSpeedBeforeBottleNeck;
		}
		else {
			// ttFreeSpeedAfterBottleNeckAlternativeRoute =
			// ttFreeSpeedAfterBottleNeck;
			this.ttFreeSpeedBeforeBottleNeckAlternativeRoute = ttFreeSpeedBeforeBottleNeck;
		}

		return predictedTT;
	}

	private int getAdditionalAgents(final Route route) {
		double weightedNetFlow = 0;
		double netFlow = 0;
		double distanceToBottleNeck = 0;
		double ttToBottleNeck = 0;

		// mainRoute
		if (route == this.mainRoute) {
			// check distance from startNode to bottleneck
			distanceToBottleNeck = getDistanceFromFirstNode(
					this.bottleNeckLinkMainRoute.getToNode(), this.routeLinksMainRoute);

			// Sum up inflows, weighted with their distance to the start node.
			Iterator<Link> it1 = this.inLinksMainRoute.iterator();
			while (it1.hasNext()) {
				Link inLink = it1.next();
				double inFlow = getInFlow(inLink, this.mainRoute);
				double weightedInFlow = 0;
				// don't include Links after the bottleNeck. Also takes care of
				// null-case
				if ((this.inFlowDistancesMainRoute.get(inLink.getId().toString()) == null)
						|| (this.inFlowDistancesMainRoute.get(inLink.getId().toString()) > distanceToBottleNeck)) {
					weightedInFlow = 0;
				}
				else {
					weightedInFlow = inFlow
							* this.inFlowDistancesMainRoute.get(inLink.getId().toString());
				}
				weightedNetFlow += weightedInFlow;

			}
			Iterator<Link> it2 = this.outLinksMainRoute.iterator();
			while (it2.hasNext()) {
				Link outLink = it2.next();
				double outFlow = getOutFlow(outLink, this.mainRoute);
				double weightedOutFlow = 0;
				if ((this.outFlowDistancesMainRoute.get(outLink.getId().toString()) == null)
						|| (this.outFlowDistancesMainRoute.get(outLink.getId().toString()) > distanceToBottleNeck)) {
					weightedOutFlow = 0;
				}
				else {
					weightedOutFlow = outFlow
							* this.outFlowDistancesMainRoute.get(outLink.getId().toString());
				}
				weightedNetFlow -= weightedOutFlow;
			}
			netFlow = weightedNetFlow / distanceToBottleNeck;
			// ttToBottleNeck = getPredictedTravelTime(mainRoute,
			// bottleNeckLinkMainRoute) - ttFreeSpeedAfterBottleNeckMainRoute;
			ttToBottleNeck = this.ttFreeSpeedBeforeBottleNeckMainRoute;
		}

		// AlternativeRoute
		else {
			distanceToBottleNeck = getDistanceFromFirstNode(
					this.bottleNeckLinkAlternativeRoute.getToNode(),
					this.routeLinksAlternativeRoute);
			Iterator<Link> it3 = this.inLinksAlternativeRoute.iterator();
			while (it3.hasNext()) {
				Link inLink = it3.next();
				double inFlow = getInFlow(inLink, this.alternativeRoute);
				double weightedInFlow = 0;
				if ((this.inFlowDistancesAlternativeRoute
						.get(inLink.getId().toString()) == null)
						|| (this.inFlowDistancesAlternativeRoute.get(inLink.getId()
								.toString()) > distanceToBottleNeck)) {
					weightedInFlow = 0;
				}
				else {
					weightedInFlow = inFlow
							* this.inFlowDistancesAlternativeRoute.get(inLink.getId()
									.toString());
				}
				weightedNetFlow += weightedInFlow;
			}
			Iterator<Link> it4 = this.outLinksAlternativeRoute.iterator();
			while (it4.hasNext()) {
				Link outLink = it4.next();
				double outFlow = getOutFlow(outLink, this.alternativeRoute);
				double weightedOutFlow = 0;
				if ((this.outFlowDistancesAlternativeRoute.get(outLink.getId()
						.toString()) == null)
						|| (this.outFlowDistancesAlternativeRoute.get(outLink.getId()
								.toString()) > distanceToBottleNeck)) {
					weightedOutFlow = 0;
				}
				else {
					weightedOutFlow = outFlow
							* this.outFlowDistancesAlternativeRoute.get(outLink.getId()
									.toString());
				}
				weightedNetFlow -= weightedOutFlow;
			}
			netFlow = (weightedNetFlow / distanceToBottleNeck);
			ttToBottleNeck = this.ttFreeSpeedBeforeBottleNeckAlternativeRoute;
			// ttToBottleNeck = getPredictedTravelTime(alternativeRoute,
			// bottleNeckLinkAlternativeRoute) -
			// ttFreeSpeedAfterBottleNeckAlternativeRoute;
		}
		return (int) (ttToBottleNeck * netFlow);
	}

	private double getOutFlow(final Link outLink, final Route route) {
		double flow;
		if (route == this.mainRoute) {
			flow = this.outFlowsMainRoute.get(outLink.getId().toString());
		}
		else if (route == this.alternativeRoute) {
			flow = this.outFlowsAlternativeRoute.get(outLink.getId().toString());
		}
		else {
			flow = 0;
			System.err.println("Something is wrong, this shouldn't happen!");
		}
		return flow;
	}

	private double getInFlow(final Link inLink, final Route route) {
		double flow;
		if (route == this.mainRoute) {
			flow = this.inFlowsMainRoute.get(inLink.getId().toString());
		}
		else if (route == this.alternativeRoute) {
			flow = this.inFlowsAlternativeRoute.get(inLink.getId().toString());
		}
		else {
			flow = 0;
			System.err.println("Something is wrong, this shouldn't happen!");
		}
		return flow;
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
			r = this.alternativeRoute;
		}
		throw new IllegalArgumentException(
				"The set Accident has to be on one of the routes if using this implementation of ControlInput!");
	}

	// ContolInputI interface methods:

	public double getNashTime() {
		try {
			this.writer.writeAgentsOnLinks(this.numberOfAgents);
			this.writer.writeTravelTimesMainRoute(this.lastTimeMainRoute,
					this.predTTMainRoute);
			this.writer.writeTravelTimesAlternativeRoute(
					this.lastTimeAlternativeRoute, this.predTTAlternativeRoute);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// set new in and outflows and reset numbersPassedOnInAndOutLinks every five
		// minutes
		double simTime = SimulationTimer.getTime();

		// reset the additionalAgents ...
		this.additionalAgentsMainRoute = 0;
		this.additionalAgentsAlternativeRoute = 0;

		if (simTime % FLOWUPDATETIME == 0) {
			// inLinksMainRoute
			Iterator<Link> iter1 = this.inLinksMainRoute.iterator();
			while (iter1.hasNext()) {
				Link inLink = iter1.next();
				double flow = (double) this.numbersPassedOnInAndOutLinks.get(inLink
						.getId().toString())
						/ FLOWUPDATETIME;
				this.inFlowsMainRoute.put(inLink.getId().toString(), flow);
				this.numbersPassedOnInAndOutLinks.put(inLink.getId().toString(), 0);
			}

			// outLinksMainRoute
			Iterator<Link> iter2 = this.outLinksMainRoute.iterator();
			while (iter2.hasNext()) {
				Link outLink = iter2.next();
				double flow = (double) this.numbersPassedOnInAndOutLinks.get(outLink
						.getId().toString())
						/ FLOWUPDATETIME;
				this.outFlowsMainRoute.put(outLink.getId().toString(), flow);
				this.numbersPassedOnInAndOutLinks.put(outLink.getId().toString(), 0);
			}
			// inLinksAlternativeRoute
			Iterator<Link> iter3 = this.inLinksAlternativeRoute.iterator();
			while (iter3.hasNext()) {
				Link inLink = iter3.next();
				double flow = (double) this.numbersPassedOnInAndOutLinks.get(inLink
						.getId().toString())
						/ FLOWUPDATETIME;
				this.inFlowsAlternativeRoute.put(inLink.getId().toString(), flow);
				this.numbersPassedOnInAndOutLinks.put(inLink.getId().toString(), 0);
			}

			// outLinksAlternativeRoute
			Iterator<Link> iter4 = this.inLinksAlternativeRoute.iterator();
			while (iter4.hasNext()) {
				Link outLink = iter4.next();
				double flow = (double) this.numbersPassedOnInAndOutLinks.get(outLink
						.getId().toString())
						/ FLOWUPDATETIME;
				this.outFlowsAlternativeRoute.put(outLink.getId().toString(), flow);
				this.numbersPassedOnInAndOutLinks.put(outLink.getId().toString(), 0);
			}
		}

		// return timeDiffernce;
		return getPredictedNashTime();

	}

	public void setAccidents(final List<Accident> accidents) {
		this.accidents = accidents;
	}

}
