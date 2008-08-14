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

package org.matsim.withinday.trafficmanagement.controlinput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.config.groups.SimulationConfigGroup;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.mobsim.queuesim.SimulationTimer;
import org.matsim.network.Link;
import org.matsim.network.NetworkChangeEvent;
import org.matsim.network.Node;
import org.matsim.population.Route;
import org.matsim.utils.misc.Time;
import org.matsim.withinday.trafficmanagement.ControlInput;

/**
 *
 * @author abergsten and dzetterberg
 * @author dgrether
 */

/*
 * User parameters are: DISTRIBUTIONCHECK True means that model checks traffic
 * distribution before bottle neck. NUMBEROFFLOWEVENTS The flow calculations are
 * based on the last NUMBEROFFLOWEVENTS agents. A higher value means better
 * predictions if congestion. IGNOREDQUEUINGIME Additional link travel times up
 * to IGNOREDQUEUINGIME seconds of the links free speed travel time will not be
 * considered as a accident (temporary capacity reduction). Default is that
 * there FLOWUPDATETIME determines how often to measure additional flows from
 * in- and must be at least 20 seconds longer than the link's free speed time.
 * outlinks. Default is to update every 60 seconds. RESETBOTTLENECKINTERVALL The
 * bottleneck flow is used for predictions this many seconds. Then the accident
 * is forgotten and has to be detected again.
 *
 * TODO if no accident detection is activated the model takes the first
 * CapacityChangeEvent of the network as accident. The model
 * will thus not be usable with real time dependent networks.
 */

public class ControlInputSB extends AbstractControlInputImpl implements
		LinkLeaveEventHandler, LinkEnterEventHandler,
		AgentDepartureEventHandler, AgentArrivalEventHandler, ControlInput {

	// User parameters:

	private static final int NUMBEROFEVENTSDETECTION = 20;

	private static final double RESETBOTTLENECKINTERVALL = 60;

	// private static final int NUMBEROFEVENTSINOUTFLOW = 20;

	private double UPDATETIMEINOUTFLOW = 600;


	private boolean distributioncheckActive = false;

	private boolean backgroundnoiseDetectionActive = false;

	private boolean incidentDetectionActive = false;

	private double ignoredQueuingTime = 20; // seconds

	//end of user parameters
	private static final Logger log = Logger.getLogger(ControlInputSB.class);

	private double predTTMainRoute;

	private double predTTAlternativeRoute;

	private Map<String, Double> ttMeasured = new HashMap<String, Double>();

	// For distribution heterogenity check:
	private Map<String, Double> enterLinkEvents = new HashMap<String, Double>();

	private Map<String, Double> intraFlows = new HashMap<String, Double>();

	private Map<String, List<Double>> enterLinkEventTimes = new HashMap<String, List<Double>>();

	private Map<String, Double> capacities = new HashMap<String, Double>();

	// For in/outlinks disturbance check:
	private Map<String, Double> extraFlowsMainRoute = new HashMap<String, Double>();

	private List<Link> inLinksMainRoute = new ArrayList<Link>();

	private List<Link> outLinksMainRoute = new ArrayList<Link>();

	private ArrayList<Node> nodesMainRoute = new ArrayList<Node>();

	private Map<String, Double> extraFlowsAlternativeRoute = new HashMap<String, Double>();

	private List<Link> inLinksAlternativeRoute = new ArrayList<Link>();

	private List<Link> outLinksAlternativeRoute = new ArrayList<Link>();

	private ArrayList<Node> nodesAlternativeRoute = new ArrayList<Node>();

	// private Map<String, Double> flowLinkDistances = new HashMap<String,
	// Double>();

	private Map<String, Double> ttFreeSpeedUpToAndIncludingLink = new HashMap<String, Double>();

	private Map<String, Double> inFlows = new HashMap<String, Double>();

	private Map<String, Double> outFlows = new HashMap<String, Double>();

	private Map<String, Integer> numbersPassedOnInAndOutLinks = new HashMap<String, Integer>();

	// For Accident detection:
	private Link currentBottleNeckMainRoute;

	private Double currentBNCapacityMainRoute;

	private Link currentBottleNeckAlternativeRoute;

	private Double currentBNCapacityAlternativeRoute;

	private Collection<NetworkChangeEvent> accidents;

	private SimulationConfigGroup simulationConfig;

	public ControlInputSB(SimulationConfigGroup simulationConfigGroup) {
		this.simulationConfig = simulationConfigGroup;
	}

	@Override
	public void init() {
		super.init();
		// Initialize ttMeasured with ttFreeSpeeds and linkFlows with zero.
		// Main route
		Link[] linksMainRoute = this.mainRoute.getLinkRoute();
		for (Link l : linksMainRoute) {
			String linkId = l.getId().toString();
			if (!this.intraFlows.containsKey(l.getId().toString())) {
				this.intraFlows.put(l.getId().toString(), 0.0);
			}

			if (!this.ttMeasured.containsKey(l.getId().toString())) {
				this.ttMeasured.put(l.getId().toString(), this.ttFreeSpeeds.get(l
						.getId().toString()));
			}

			if (!this.capacities.containsKey(l.getId().toString())) {
				double capacity = l.getFlowCapacity(Time.UNDEFINED_TIME) * this.simulationConfig.getFlowCapFactor()
						/ SimulationTimer.getSimTickTime();
				this.capacities.put(l.getId().toString(), capacity);
			}

			if (!this.enterLinkEventTimes.containsKey(l.getId().toString())) {
				List<Double> list = new LinkedList<Double>();
				this.enterLinkEventTimes.put(l.getId().toString(), list);
			}

			if (!this.ttFreeSpeedUpToAndIncludingLink.containsKey(l.getId()
					.toString())) {
				double tt = sumUpTTFreeSpeed(l.getToNode(), this.mainRoute);
				this.ttFreeSpeedUpToAndIncludingLink.put(linkId, tt);
			}
		}
		this.currentBottleNeckMainRoute = this.mainRouteNaturalBottleNeck;
		this.currentBNCapacityMainRoute = getCapacity(this.mainRouteNaturalBottleNeck);
		List<Link> linksMainRouteList = Arrays.asList(linksMainRoute);
		this.nodesMainRoute = this.mainRoute.getRoute();
		for (int i = 1; i < this.nodesMainRoute.size() - 1; i++) {
			Node n = this.nodesMainRoute.get(i);
			for (Link inLink : n.getInLinks().values()) {
				String linkId = inLink.getId().toString();
				if (!linksMainRouteList.contains(inLink)) {
					double tt = sumUpTTFreeSpeed(n, this.mainRoute);
					this.ttFreeSpeedUpToAndIncludingLink.put(linkId, tt);
					this.inLinksMainRoute.add(inLink);
					this.inFlows.put(inLink.getId().toString(), 0.0);
					this.numbersPassedOnInAndOutLinks.put(inLink.getId().toString(), 0);
					this.extraFlowsMainRoute.put(linkId, 0.0);
					List<Double> list = new LinkedList<Double>();
					this.enterLinkEventTimes.put(linkId, list);
				}
			}
			for (Link outLink : n.getOutLinks().values()) {
				String linkId = outLink.getId().toString();
				if (!linksMainRouteList.contains(outLink)) {
					double tt = sumUpTTFreeSpeed(n, this.mainRoute);
					this.ttFreeSpeedUpToAndIncludingLink.put(linkId, tt);
					this.outLinksMainRoute.add(outLink);
					this.outFlows.put(outLink.getId().toString(), 0.0);
					this.numbersPassedOnInAndOutLinks.put(outLink.getId().toString(), 0);
					this.extraFlowsMainRoute.put(linkId, 0.0);
					List<Double> list = new LinkedList<Double>();
					this.enterLinkEventTimes.put(linkId, list);
				}
			}
		}

		// Alt Route
		Link[] linksAlternativeRoute = this.alternativeRoute.getLinkRoute();
		for (Link l : linksAlternativeRoute) {
			String linkId = l.getId().toString();
			if (!this.intraFlows.containsKey(l.getId().toString())) {
				this.intraFlows.put(l.getId().toString(), 0.0);
			}

			if (!this.ttMeasured.containsKey(l.getId().toString())) {
				this.ttMeasured.put(l.getId().toString(), this.ttFreeSpeeds.get(l
						.getId().toString()));
			}

			if (!this.capacities.containsKey(l.getId().toString())) {
				double capacity = l.getFlowCapacity(Time.UNDEFINED_TIME) * this.simulationConfig.getFlowCapFactor()
						/ SimulationTimer.getSimTickTime();
				this.capacities.put(l.getId().toString(), capacity);
			}

			if (!this.enterLinkEventTimes.containsKey(l.getId().toString())) {
				List<Double> list = new LinkedList<Double>();
				this.enterLinkEventTimes.put(l.getId().toString(), list);
			}
			if (!this.ttFreeSpeedUpToAndIncludingLink.containsKey(l.getId()
					.toString())) {
				double tt = sumUpTTFreeSpeed(l.getToNode(), this.mainRoute);
				this.ttFreeSpeedUpToAndIncludingLink.put(linkId, tt);
			}
		}
		this.currentBottleNeckAlternativeRoute = this.altRouteNaturalBottleNeck;
		this.currentBNCapacityAlternativeRoute = getCapacity(this.altRouteNaturalBottleNeck);

		this.nodesAlternativeRoute = this.alternativeRoute.getRoute();
		List<Link> linksAlternativeRouteList = Arrays.asList(linksAlternativeRoute);
		for (int i = 1; i < this.nodesAlternativeRoute.size() - 1; i++) {
			Node n = this.nodesAlternativeRoute.get(i);
			for (Link inLink : n.getInLinks().values()) {
				String linkId = inLink.getId().toString();
				if (!linksAlternativeRouteList.contains(inLink)) {
					double tt = sumUpTTFreeSpeed(n, this.alternativeRoute);
					this.ttFreeSpeedUpToAndIncludingLink.put(linkId, tt);
					this.inLinksAlternativeRoute.add(inLink);
					this.outFlows.put(inLink.getId().toString(), 0.0);
					this.numbersPassedOnInAndOutLinks.put(inLink.getId().toString(), 0);
					this.extraFlowsAlternativeRoute.put(linkId, 0.0);
					List<Double> list = new LinkedList<Double>();
					this.enterLinkEventTimes.put(linkId, list);
					// } else{
					// System.out.println("No additional inLinks");
				}
			}
			for (Link outLink : n.getOutLinks().values()) {
				String linkId = outLink.getId().toString();
				if (!linksAlternativeRouteList.contains(outLink)) {
					double tt = sumUpTTFreeSpeed(n, this.alternativeRoute);
					this.ttFreeSpeedUpToAndIncludingLink.put(linkId, tt);
					this.outLinksAlternativeRoute.add(outLink);
					this.outFlows.put(outLink.getId().toString(), 0.0);
					this.numbersPassedOnInAndOutLinks.put(outLink.getId().toString(), 0);
					this.extraFlowsAlternativeRoute.put(linkId, 0.0);
					List<Double> list = new LinkedList<Double>();
					this.enterLinkEventTimes.put(linkId, list);
					// } else{
					// System.out.println("No additional outLinks");
				}
			}
		}

		this.UPDATETIMEINOUTFLOW = getFreeSpeedRouteTravelTime(this.mainRoute);

	}

	private double sumUpTTFreeSpeed(Node node, Route route) {

		double ttFS = 0;
		Link[] routeLinks = route.getLinkRoute();
		for (int i = 0; i < routeLinks.length; i++) {
			Link l = routeLinks[i];
			ttFS += this.ttFreeSpeeds.get(l.getId().toString());
			if (l.getToNode() == node) {
				break;
			}
		}
		return ttFS;
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {

		// Must be done before super.handleEvent as that removes entries
		if (this.ttMeasured.containsKey(event.linkId)) {
			this.enterLinkEvents.put(event.agentId, event.time);
		}

		// handle flows on outLinks
		if (this.outLinksMainRoute.contains(event.link)) {
			int numbersPassed = this.numbersPassedOnInAndOutLinks.get(event.linkId) + 1;
			this.numbersPassedOnInAndOutLinks.put(event.linkId, numbersPassed);
		}
		else if (this.outLinksAlternativeRoute.contains(event.linkId)) {
			int numbersPassed = this.numbersPassedOnInAndOutLinks.get(event.linkId) + 1;
			this.numbersPassedOnInAndOutLinks.put(event.linkId, numbersPassed);
		}

		super.handleEvent(event);
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {

		// Must be done before super.handleEvent as that removes entries
		if (this.ttMeasured.containsKey(event.linkId)
				&& (this.enterLinkEvents.get(event.agentId) != null)) {
			Double enterTime = this.enterLinkEvents.remove(event.agentId);
			Double travelTime = event.time - enterTime;
			this.ttMeasured.put(event.linkId, travelTime);
		}

		// Stores [NUMBEROFFLOWEVENTS] last events and calculates flow for detection
		// of capacity reduction
		if (this.intraFlows.containsKey(event.linkId)) {
			updateFlow(NUMBEROFEVENTSDETECTION, event);
		}
		if (this.inLinksAlternativeRoute.contains(event.link)
				|| this.outLinksAlternativeRoute.contains(event.link)
				|| this.inLinksMainRoute.contains(event.link)
				|| this.outLinksMainRoute.contains(event.link)) {
			// updateFlow(NUMBEROFEVENTSINOUTFLOW, event);
			updateFlow(this.UPDATETIMEINOUTFLOW, event);
		}

		super.handleEvent(event);
	}

	private void updateFlow(int flowResolution, LinkLeaveEvent event) {

		LinkedList<Double> list = (LinkedList<Double>) this.enterLinkEventTimes
				.get(event.linkId);
		if (list.size() == flowResolution) {
			list.removeFirst();
			list.addLast(event.time);
		}
		else if ((1 < list.size()) || (list.size() < flowResolution)) {
			list.add(event.time);
		}
		else if (list.size() == 0) {
			list.addLast(event.time - 1);
			list.addLast(event.time);
		}
		else {
			System.err
					.println("Error: number of enter event times stored exceeds numberofflowevents!");
		}

		// Flow = agents / seconds:
		double flow = (list.size() - 1) / (list.getLast() - list.getFirst());

		if (this.intraFlows.containsKey(event.linkId)) {
			this.intraFlows.put(event.linkId, flow);
		}
		if (this.inLinksMainRoute.contains(event.link)) {
			double inFlow = flow;
			this.extraFlowsMainRoute.put(event.linkId, inFlow);
		}
		if (this.outLinksMainRoute.contains(event.link)) {
			double outFlow = -flow;
			this.extraFlowsMainRoute.put(event.linkId, outFlow);
		}
		if (this.inLinksAlternativeRoute.contains(event.link)) {
			double inFlow = flow;
			this.extraFlowsAlternativeRoute.put(event.linkId, inFlow);
		}
		if (this.outLinksAlternativeRoute.contains(event.link)) {
			double outFlow = -flow;
			this.extraFlowsAlternativeRoute.put(event.linkId, outFlow);
		}
	}

	private void updateFlow(double flowUpdateTime, LinkLeaveEvent event) {

		LinkedList<Double> list = (LinkedList<Double>) this.enterLinkEventTimes
				.get(event.linkId);
		// Remove times older than flowUpdateTime
			while (!list.isEmpty() && ((list.getFirst() + flowUpdateTime) < event.time)) {
				list.removeFirst();
			}
		// Add new values
		list.addLast(event.time);

		// Flow = agents / seconds:
		double flow = (list.size() - 1) / (list.getLast() - list.getFirst());

		if (this.intraFlows.containsKey(event.linkId)) {
			this.intraFlows.put(event.linkId, flow);
		}
		if (this.inLinksMainRoute.contains(event.link)) {
			double inFlow = flow;
			this.extraFlowsMainRoute.put(event.linkId, inFlow);
		}
		if (this.outLinksMainRoute.contains(event.link)) {
			double outFlow = -flow;
			this.extraFlowsMainRoute.put(event.linkId, outFlow);
		}
		if (this.inLinksAlternativeRoute.contains(event.link)) {
			double inFlow = flow;
			this.extraFlowsAlternativeRoute.put(event.linkId, inFlow);
		}
		if (this.outLinksAlternativeRoute.contains(event.link)) {
			double outFlow = -flow;
			this.extraFlowsAlternativeRoute.put(event.linkId, outFlow);
		}
	}

	public void reset(int iteration) {}


	@Override
	public double getPredictedNashTime(Route route) {
		if (route.equals(this.mainRoute)) {
			return this.predTTMainRoute;
		}
		else {
			return this.predTTAlternativeRoute;
		}
	}


	@Override
	public double getNashTime() {
		super.getNashTime();
		return getPredictedNashTime();
	}

	// calculates the predictive time difference
	public double getPredictedNashTime() {

		if (this.accidents.isEmpty()) {
			// throw new UnsupportedOperationException("To use this controler an
			// accident has to be set");
			this.predTTMainRoute = getPredictedTravelTime(this.mainRoute,
					this.mainRouteNaturalBottleNeck);
		}
		else {
			String accidentLinkId =  this.accidents.iterator().next().getLinks().iterator().next().getId().toString();
			Link accidentLinkMainRoute = searchAccidentsOnRoutes(accidentLinkId);
			this.predTTMainRoute = getPredictedTravelTime(this.mainRoute,
					accidentLinkMainRoute);
		}

		this.predTTAlternativeRoute = getPredictedTravelTime(this.alternativeRoute,
				this.altRouteNaturalBottleNeck);

		return this.predTTMainRoute - this.predTTAlternativeRoute;
	}

	private double getPredictedTravelTime(final Route route, final Link bottleNeck) {

		double predictedTT;
		Link[] routeLinks = route.getLinkRoute();
		double ttFreeSpeedPart = 0.0;
		int agentsToQueueAtBottleNeck = 0;
		boolean guidanceObjectWillQueue = false;
		Link currentBottleNeck = bottleNeck;
		double currentBottleNeckCapacity = 0;
		double ttFreeSpeedBeforeBottleNeck = 0;

		if (this.incidentDetectionActive) {
			currentBottleNeck = getDetectedBottleNeck(route);
			currentBottleNeckCapacity = getIncidentCapacity(route);
			for (int i = routeLinks.length - 1; i >= 0; i--) {
				String linkId = routeLinks[i].getId().toString();

				if (this.ttMeasured.get(linkId) > this.ttFreeSpeeds.get(linkId)
						+ this.ignoredQueuingTime) {
					currentBottleNeck = routeLinks[i];
					setIncidentLink(currentBottleNeck, route);
					currentBottleNeckCapacity = getFlow(currentBottleNeck);
					setIncidentCapacity(currentBottleNeckCapacity, route);

					// do not check links before current bottleneck
					break;
				}
				else if (SimulationTimer.getTime() % RESETBOTTLENECKINTERVALL == 0) {
					this.currentBNCapacityAlternativeRoute = getCapacity(this.altRouteNaturalBottleNeck);
					this.currentBNCapacityMainRoute = getCapacity(this.mainRouteNaturalBottleNeck);
					this.currentBottleNeckAlternativeRoute = this.altRouteNaturalBottleNeck;
					this.currentBottleNeckMainRoute = this.mainRouteNaturalBottleNeck;
				}
			}
		}

		else if (!this.incidentDetectionActive) {
			currentBottleNeck = bottleNeck;
			currentBottleNeckCapacity = currentBottleNeck.getFlowCapacity(SimulationTimer.getTime()) * this.simulationConfig.getFlowCapFactor()
					/ SimulationTimer.getSimTickTime();

		}

		// get the array index of the bottleneck link
		int bottleNeckIndex = 0;
		for (int i = 0; i < routeLinks.length; i++) {
			if (currentBottleNeck.equals(routeLinks[i])) {
				bottleNeckIndex = i;
				break;
			}
		}

		// Agents after bottleneck drive free speed (bottle neck index + 1)
		for (int i = bottleNeckIndex + 1; i < routeLinks.length; i++) {
			ttFreeSpeedPart += this.ttFreeSpeeds
					.get(routeLinks[i].getId().toString());
		}

		if (this.distributioncheckActive) {

			for (int r = bottleNeckIndex; r >= 0; r--) {
				Link link = routeLinks[r];
				double linkAgents = this.numberOfAgents.get(link.getId().toString());
				double linkFreeSpeedTT = this.ttFreeSpeeds.get(link.getId().toString());

				if ((linkAgents / currentBottleNeckCapacity) <= linkFreeSpeedTT) {
					ttFreeSpeedPart += linkFreeSpeedTT;
//					log.debug("Distribution check: Link " + link.getId().toString()
//							+ " is added to freeSpeedPart.");
				}
				else {
					int agentsUpToLink = 0;
					double freeSpeedUpToLink = 0;
					for (int p = 0; p <= r; p++) {
						agentsUpToLink += this.numberOfAgents.get(routeLinks[p].getId()
								.toString());
						freeSpeedUpToLink += this.ttFreeSpeeds.get(routeLinks[p].getId()
								.toString());
						ttFreeSpeedBeforeBottleNeck = freeSpeedUpToLink;
					}

					if (this.backgroundnoiseDetectionActive) {
						agentsUpToLink += getAdditionalAgents(route, r);
					}

					if ((agentsUpToLink / currentBottleNeckCapacity) >= freeSpeedUpToLink) {
						guidanceObjectWillQueue = true;
						currentBottleNeck = link;
						agentsToQueueAtBottleNeck = agentsUpToLink;
						// log.debug("Distribution check: Critical link. All agents on link
						// " + criticalCongestedLink.getId().toString() + " will NOT pass
						// the bottleneck before the guidance object arrive.");
						break;
					}
					else {
						ttFreeSpeedPart += linkFreeSpeedTT;
						// log.debug("Distribution check: Non-critical link. All agents on
						// link " + criticalCongestedLink.getId().toString() + " will pass
						// the bottle neck when before the guidancde object arrive." );
					}
				}
			}
			if (guidanceObjectWillQueue) {
//				log.debug("The guidance object will queue with agents ahead.");
			}
			else {
//						.debug("The guidance object will not queue at the bottleneck. No critical congested link was found.");
			}
			// log.debug("Distribution check performed: " + agentsToQueueAtBottleNeck
			// + " will queue at link " + criticalCongestedLink.getId().toString());
		}

		// Run without distribution check
		else if (!this.distributioncheckActive) {

			// count agents on congested part of the route
			ttFreeSpeedBeforeBottleNeck = 0;
			for (int i = 0; i <= bottleNeckIndex; i++) {
				agentsToQueueAtBottleNeck += this.numberOfAgents.get(routeLinks[i]
						.getId().toString());
				ttFreeSpeedBeforeBottleNeck += this.ttFreeSpeeds.get(routeLinks[i]
						.getId().toString());
			}
			if (this.backgroundnoiseDetectionActive) {
				agentsToQueueAtBottleNeck += getAdditionalAgents(route, bottleNeckIndex);
			}
//			log.debug("Distribution check inactivated: " + agentsToQueueAtBottleNeck
//					+ " agents before bottle neck link "
//					+ currentBottleNeck.getId().toString());
		}

		predictedTT = (agentsToQueueAtBottleNeck / currentBottleNeckCapacity)
				+ ttFreeSpeedPart;
		// Check route criteria if distribution check is deactivated
		if (!this.distributioncheckActive
				&& !(agentsToQueueAtBottleNeck / currentBottleNeckCapacity > ttFreeSpeedBeforeBottleNeck)) {
			predictedTT = getFreeSpeedRouteTravelTime(route);
		}

		return predictedTT;
	}

	private int getAdditionalAgents(final Route route, final int linkIndex) {
		double totalExtraAgents = 0.0;

		// check distance and free speed travel time from start node to bottleneck
		Link[] routeLinks = route.getLinkRoute();
		String linkId1 = routeLinks[linkIndex].getId().toString();
		double ttToLink = this.ttFreeSpeedUpToAndIncludingLink.get(linkId1);

		List<Link> inAndOutLinks = new ArrayList<Link>();
		inAndOutLinks.addAll(this.getOutlinks(route));
		inAndOutLinks.addAll(this.getInlinks(route));
		Iterator<Link> it = inAndOutLinks.iterator();
		while (it.hasNext()) {
			Link link = it.next();
			String linkId = link.getId().toString();
			double extraAgents = 0.0;
			double flow = getInOutFlow(link, route);
			if ((this.ttFreeSpeedUpToAndIncludingLink.get(linkId) > ttToLink)
					|| (this.ttFreeSpeedUpToAndIncludingLink == null)) {
				extraAgents = 0.0;
			}
			else {
				extraAgents = flow * this.ttFreeSpeedUpToAndIncludingLink.get(linkId);
				// System.out.println("Extra agents = " + flow + " * " +
				// this.ttFreeSpeedUpToAndIncludingLink.get(linkId) + " (link" + linkId
				// + " )." );
			}
			totalExtraAgents += extraAgents;
		}
		return (int) (totalExtraAgents);
	}

	private List<Link> getOutlinks(Route route) {
		if (route == this.mainRoute) {
			return this.outLinksMainRoute;
		}
		else {
			return this.outLinksAlternativeRoute;
		}
	}

	private List<Link> getInlinks(Route route) {
		if (route == this.mainRoute) {
			return this.inLinksMainRoute;
		}
		else {
			return this.inLinksAlternativeRoute;
		}
	}

	private double getInOutFlow(Link inLink, Route route) {
		double flow;
		String linkId = inLink.getId().toString();
		if (route == this.mainRoute) {
			flow = this.extraFlowsMainRoute.get(linkId);
		}
		else if (route == this.alternativeRoute) {
			flow = this.extraFlowsAlternativeRoute.get(linkId);
		}
		else {
			flow = 0;
			System.err.println("Something is wrong, this shouldn't happen!");
		}
		return flow;
	}

	public double getFlow(Link link) {
		double flow = this.intraFlows.get(link.getId().toString());
		return flow;
	}

	public double getCapacity(Link link) {
		double capacity = this.capacities.get(link.getId().toString());
		return capacity;
	}

	public Link getDetectedBottleNeck(final Route route) {
		Link l;
		if (route == this.mainRoute) {
			l = this.currentBottleNeckMainRoute;
		}
		else {
			l = this.currentBottleNeckAlternativeRoute;
		}
		return l;
	}

	public void setIncidentLink(final Link link, final Route route) {
		if (route == this.mainRoute) {
			this.currentBottleNeckMainRoute = link;
		}
		else {
			this.currentBottleNeckAlternativeRoute = link;
		}
	}

	private void setIncidentCapacity(Double currentBottleNeckCapacity, Route route) {
		if (route == this.mainRoute) {
			this.currentBNCapacityMainRoute = currentBottleNeckCapacity;
		}
		else {
			this.currentBNCapacityAlternativeRoute = currentBottleNeckCapacity;
		}
	}

	private Double getIncidentCapacity(Route route) {
		double cap;
		if (route == this.mainRoute) {
			cap = this.currentBNCapacityMainRoute;
		}
		else {
			cap = this.currentBNCapacityAlternativeRoute;
		}
		return cap;
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

	public void setNetworkChangeEvents(final Collection<NetworkChangeEvent> accidents) {
		this.accidents = accidents;
	}

	public void setIgnoredQueuingTime(double time) {
		log.debug("Set ignored queing time to: " + time);
		this.ignoredQueuingTime = time;
	}

	public void setDistributionCheckActive(boolean b) {
		log.debug("distribution check active: " + b);
		this.distributioncheckActive = b;
	}

	public void setBackgroundnoiseCompensationActive(boolean b) {
		log.debug("backgroun noise compensation active: " + b);
		this.backgroundnoiseDetectionActive = b;
	}

	public void setIncidentDetectionActive(boolean b) {
		log.debug("Incident detection active: " + b);
		this.incidentDetectionActive = b;
	}


}
