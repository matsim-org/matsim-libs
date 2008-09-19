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

public class ControlInputMB extends AbstractControlInputImpl implements
		LinkLeaveEventHandler, LinkEnterEventHandler,
		AgentDepartureEventHandler, AgentArrivalEventHandler, ControlInput {

	// User parameters:

	private int numberofeventsdetection = 10;

	private double ignoredQueuingTime = 30; // seconds

	private double updatetimeinoutflow = Double.NaN;

	// private static final int NUMBEROFEVENTSINOUTFLOW = 20;

	private double resetbottleneckintervall = 1;

	//end of user parameters

	private static final Logger log = Logger.getLogger(ControlInputMB.class);

	private double predTTMainRoute;

	private double predTTAlternativeRoute;

	private Map<String, Double> ttMeasured = new HashMap<String, Double>();

	private LinkedList<Link> bottleNeckListMain = new LinkedList<Link>();

	private LinkedList<Link> bottleNeckListAlt = new LinkedList<Link>();

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

	private Link currentBottleNeckAlternativeRoute;

	private Collection<NetworkChangeEvent> accidents;

	private SimulationConfigGroup simulationConfig;

	public ControlInputMB(SimulationConfigGroup simulationConfigGroup) {
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
		// this.bottleNecksMain = null;

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
		if (Double.isNaN(this.updatetimeinoutflow)) {
			this.updatetimeinoutflow = getFreeSpeedRouteTravelTime(this.mainRoute);
		}
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
		else if (this.outLinksAlternativeRoute.contains(event.link)) {
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
			updateFlow(this.numberofeventsdetection, event);
		}
		if (this.inLinksAlternativeRoute.contains(event.link)
				|| this.outLinksAlternativeRoute.contains(event.link)
				|| this.inLinksMainRoute.contains(event.link)
				|| this.outLinksMainRoute.contains(event.link)) {
			// updateFlow(NUMBEROFEVENTSINOUTFLOW, event);
			updateFlow(this.updatetimeinoutflow, event);

		}

		super.handleEvent(event);
	}

	private void updateFlow(int flowResolution, LinkLeaveEvent event) {

		LinkedList<Double> list = (LinkedList<Double>) this.enterLinkEventTimes
				.get(event.linkId);
		if (list.size() == flowResolution) {
			list.removeFirst();
			list.add(event.time);
		}
		else if ((1 < list.size()) || (list.size() < flowResolution)) {
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

	public void reset(int iteration) {	}

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
			String accidentLinkId = this.accidents.iterator().next().getLinks().iterator().next().getId().toString();
			Link accidentLinkMainRoute = searchAccidentsOnRoutes(accidentLinkId);
			this.predTTMainRoute = getPredictedTravelTime(this.mainRoute,
					accidentLinkMainRoute);
		}

		this.predTTAlternativeRoute = getPredictedTravelTime(this.alternativeRoute,
				this.altRouteNaturalBottleNeck);

		return this.predTTMainRoute - this.predTTAlternativeRoute;
	}

	private double getPredictedTravelTime(final Route route, final Link bottleNeck) {

		log.trace("");
		log.trace("Sim time: " + SimulationTimer.getTime());
		double predictedTT;
		Link[] routeLinks = route.getLinkRoute();
		boolean searchForBottleNecks = true;
		LinkedList<Link> bottleNeckList = new LinkedList<Link>();

		if (route == this.mainRoute) {
			bottleNeckList = this.bottleNeckListMain;
		}
		if (route == this.alternativeRoute) {
			bottleNeckList = this.bottleNeckListAlt;
		}

		// boolean queueFound = true;
		if (SimulationTimer.getTime() % this.resetbottleneckintervall == 0) {
			bottleNeckList.clear();

			for (int i = routeLinks.length - 1; i >= 0; i--) {
				Link link = routeLinks[i];
				String linkId = link.getId().toString();

				if ((this.ttMeasured.get(linkId) > (this.ttFreeSpeeds.get(linkId) + this.ignoredQueuingTime))
						&& searchForBottleNecks) {
					bottleNeckList.addFirst(link);
					searchForBottleNecks = false;
					// queueFound = true;
					log.trace("Link " + linkId + " was detected as a bottleneck.");
				}
				else if (this.ttMeasured.get(linkId) < this.ttFreeSpeeds.get(linkId)
						+ this.ignoredQueuingTime) {
					searchForBottleNecks = true;
				}
			}
		}

		if (route == this.mainRoute) {
			this.bottleNeckListMain = bottleNeckList;
		}
		else if (route == this.alternativeRoute) {
			this.bottleNeckListAlt = bottleNeckList;
		}

		if (bottleNeckList.isEmpty()) {
			double agentsOnRoute = getAgents(route);
			double currentBottleNeckCapacity = bottleNeck.getFlowCapacity(SimulationTimer.getTime()) * this.simulationConfig.getFlowCapFactor()
			/ SimulationTimer.getSimTickTime();
			double ttQueue = agentsOnRoute / currentBottleNeckCapacity;
			double ttFreeSpeed = getFreeSpeedRouteTravelTime(route);
			if (ttQueue > ttFreeSpeed) {
				return ttQueue;
			}
			return ttFreeSpeed;
		}

		// Sum up free speed after the last queue (last route segment)
		double ttAfterLastQueue;
		Link endLinkLastRS = bottleNeckList.getLast();
		Node rsFromNode = endLinkLastRS.getToNode(); // the last queue ends at this
																									// node
		Node rsToNode = routeLinks[routeLinks.length - 1].getToNode();
		if (rsFromNode == rsToNode) { // if the last queue is on the last link
			ttAfterLastQueue = 0.0;
		}
		else {
			Route lastRouteSegment = route.getSubRoute(rsFromNode, rsToNode);
			ttAfterLastQueue = getFreeSpeed(lastRouteSegment);
		}
		Link lastBottleNeckFound = bottleNeckList.getLast();
		predictedTT = getTTincludingThisRouteSegment(route, /*
																												 * bottleNeckIndexArray,
																												 * bottleNeckIndexArray.length -
																												 * 1,
																												 */
				bottleNeckList, lastBottleNeckFound)
				+ ttAfterLastQueue; // the last link on the route
		log.trace("Route: predicted tt = " + predictedTT);

		return predictedTT;
	}

	private double getTTincludingThisRouteSegment(Route wholeRoute, /*
																																	 * Integer []
																																	 * bnIndexArray,
																																	 * int
																																	 * bnNumber
																																	 */
			LinkedList<Link> bottleNeckList, Link bottleNeck) {

		double agentsAdditional = 0;
		double ttThisRouteSegment;
		double ttToThisRouteSegment;
		Link firstLinkInRS;
		Link[] routeLinks = wholeRoute.getLinkRoute();
		Route routeSegment = null;

		double additionalAgentsOnRoute = 0;
		int previousBottleNeckIndex;

		log.trace("bottleNeckLink: " + bottleNeck.getId().toString());

		// If it is the first queue on the route...
		if (bottleNeckList.indexOf(bottleNeck) == 0) {
			ttToThisRouteSegment = 0;
			firstLinkInRS = routeLinks[0];
			previousBottleNeckIndex = -1;
		}
		else {

			Link previousBottleNeck = bottleNeckList.get((bottleNeckList
					.indexOf(bottleNeck) - 1));
			previousBottleNeckIndex = 0;
			for (int i = 0; i < routeLinks.length; i++) {
				if (previousBottleNeck.equals(routeLinks[i])) {
					previousBottleNeckIndex = i;
					break;
				}
			}
			firstLinkInRS = routeLinks[previousBottleNeckIndex + 1];
			ttToThisRouteSegment = getTTincludingThisRouteSegment(wholeRoute, /*
																																				 * bnIndexArray,
																																				 * bnNumber -
																																				 * 1
																																				 */
					bottleNeckList, previousBottleNeck); // calls the tt calc recursively

		}

		routeSegment = wholeRoute.getSubRoute(firstLinkInRS.getFromNode(),
				bottleNeck.getToNode());
		double netFlowOnRoute = getIntraFlow(firstLinkInRS)
				- getIntraFlow(bottleNeck);

		// Calculate additional agents that enter or leaves route
		List<Node> routeList = new ArrayList<Node>(routeSegment.getRoute());
		List<Link> inAndOutLinks = new ArrayList<Link>();
		inAndOutLinks.addAll(this.getOutlinks(wholeRoute));
		inAndOutLinks.addAll(this.getInlinks(wholeRoute));
		double flowOnThisLink = 0;
		double ttFreeSpeedOnRouteSegment = 0;
		double additionalAgentsInOutLinks = 0;
		for (Link link : inAndOutLinks) {
			if (routeList.contains(link.getToNode())
					|| routeList.contains(link.getFromNode())) {
				flowOnThisLink = getInOutFlow(link, wholeRoute);
				ttFreeSpeedOnRouteSegment = getTTFreeSpeedOnRouteSegmentToLink(
						wholeRoute, previousBottleNeckIndex, link);
			}
			additionalAgentsInOutLinks += flowOnThisLink
					* (ttToThisRouteSegment + ttFreeSpeedOnRouteSegment);
		}

		additionalAgentsOnRoute = netFlowOnRoute * ttToThisRouteSegment;
		log.trace("additionalAgentsInOutLinks: " + additionalAgentsInOutLinks
				+ ". additionalAgentsOnRoute: " + additionalAgentsOnRoute);
		agentsAdditional = additionalAgentsOnRoute + additionalAgentsInOutLinks;

		double agents_t0 = getAgents(routeSegment);
		double agentsPredicted = agents_t0 + agentsAdditional;
		log.trace("agents at t0:  " + agents_t0 + ". Simulated: "
				+ agentsAdditional);

		// Do the queuecheck
		double ttFreeSpeedRS = getFreeSpeed(routeSegment);
		double ttQueue = agentsPredicted / getIntraFlow(bottleNeck);
		log.trace("RS ttFreeSpeed: " + ttFreeSpeedRS);
		log.trace("RS ttQueue: " + ttQueue);
		if (ttQueue > ttFreeSpeedRS) {
			ttThisRouteSegment = ttQueue;
		}
		else {
			ttThisRouteSegment = ttFreeSpeedRS;
		}
		log.trace(" This rs (bnlink: " + bottleNeck.getId().toString()
				+ ") + previous rs = " + ttThisRouteSegment + " + "
				+ ttToThisRouteSegment);
		return ttToThisRouteSegment + ttThisRouteSegment;
	}

	private double getTTFreeSpeedOnRouteSegmentToLink(Route wholeRoute,
			int previousBottleNeckIndex, Link inOutLink) {

		Link[] wholeRouteLinks = wholeRoute.getLinkRoute();
		Link firstLinkOnRouteSegment = wholeRouteLinks[previousBottleNeckIndex + 1];

		double ttFreeSpeedToThisRouteSegment = 0.0;

		if (previousBottleNeckIndex == -1) {
			ttFreeSpeedToThisRouteSegment = 0.0;
		}
		else {
			for (int i = 0; i < wholeRouteLinks.length; i++) {
				Link l = wholeRouteLinks[i];
				ttFreeSpeedToThisRouteSegment += this.ttFreeSpeeds.get(l.getId()
						.toString());
				if (l.getToNode() == firstLinkOnRouteSegment.getFromNode()) {
					break;
				}
			}
		}

		double ttFreeSpeedOnRouteInlcudingThisLink = 0;
		ttFreeSpeedOnRouteInlcudingThisLink = this.ttFreeSpeedUpToAndIncludingLink
				.get(inOutLink.getId().toString());

		return ttFreeSpeedOnRouteInlcudingThisLink - ttFreeSpeedToThisRouteSegment;
	}

	private double getFreeSpeed(Route routeSegment) {
		Link[] routeLinks = routeSegment.getLinkRoute();
		double ttFS = 0;
		for (Link l : routeLinks) {
			ttFS += this.ttFreeSpeeds.get(l.getId().toString());
		}
		return ttFS;
	}

	private double getAgents(Route routeSegment) {
		Link[] routeLinks = routeSegment.getLinkRoute();
		double agents = 0;
		for (Link l : routeLinks) {
			agents += this.numberOfAgents.get(l.getId().toString());
		}
		return agents;
	}

	/*
	 * abergsten: the methods getAdditionalAgents and getExtraFlow are not used
	 * but might come handy someday... private int getAdditionalAgents(final Route
	 * route, final int linkIndex){ double totalExtraAgents = 0.0;
	 *
	 * //check distance and free speed travel time from start node to bottleneck
	 * Link [] routeLinks = route.getLinkRoute(); String linkId1 =
	 * routeLinks[linkIndex].getId().toString(); double ttToLink =
	 * ttFreeSpeedUpToAndIncludingLink.get(linkId1);
	 *
	 * List<Link> inAndOutLinks = new ArrayList<Link>();
	 * inAndOutLinks.addAll(this.getOutlinks(route));
	 * inAndOutLinks.addAll(this.getInlinks(route)); Iterator<Link> it =
	 * inAndOutLinks.iterator(); while (it.hasNext()) { Link link = it.next();
	 * String linkId = link.getId().toString(); double extraAgents = 0.0; double
	 * flow = getInOutFlow(link, route); if (
	 * this.ttFreeSpeedUpToAndIncludingLink.get(linkId) > ttToLink ||
	 * this.ttFreeSpeedUpToAndIncludingLink == null) { extraAgents = 0.0; } else {
	 * extraAgents = flow * this.ttFreeSpeedUpToAndIncludingLink.get(linkId);
	 * log.debug("Extra agents = " + extraAgents + " = " + flow + " * " +
	 * this.ttFreeSpeedUpToAndIncludingLink.get(linkId) + " (link" + linkId + "
	 * )." ); } totalExtraAgents += extraAgents; } return (int)(totalExtraAgents); }
	 *
	 * private double getExtraFlow(Route wholeRoute, Route routeSegment){
	 *
	 * double flow; double netFlow = 0; List<Node> routeList = new ArrayList<Node>(routeSegment.getRoute());
	 * List<Link> inAndOutLinks = new ArrayList<Link>();
	 * inAndOutLinks.addAll(this.getOutlinks(wholeRoute));
	 * inAndOutLinks.addAll(this.getInlinks(wholeRoute));
	 *
	 * for (Link link : inAndOutLinks) { if ( routeList.contains(link.getToNode()) ||
	 * routeList.contains(link.getFromNode()) ) { flow = getInOutFlow(link,
	 * wholeRoute); netFlow += flow; } } return netFlow; }
	 *
	 *
	 *
	 * private void setIncidentCapacity(Double currentBottleNeckCapacity, Route
	 * route) { if(route == mainRoute){ currentBNCapacityMainRoute =
	 * currentBottleNeckCapacity; } else{ currentBNCapacityAlternativeRoute =
	 * currentBottleNeckCapacity; } }
	 *
	 * private Double getIncidentCapacity(Route route) { double cap; if(route ==
	 * mainRoute){ cap = currentBNCapacityMainRoute; } else{ cap =
	 * currentBNCapacityAlternativeRoute; } return cap; }
	 */

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

	public double getIntraFlow(Link link) {
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

	public void setUpdateTimeInOutFlow(double sec) {
		this.updatetimeinoutflow = sec;
		log.debug("Set update time in and outflow to: " + sec);
	}

	public void setResetBottleNeckIntervall(double time) {
		this.resetbottleneckintervall = time;
		log.debug("Set reset bottle neck intervall time to: " + this.resetbottleneckintervall);
	}

	public void setNumberOfEventsDetection(int events) {
		this.numberofeventsdetection = events;
		log.debug("Set number of events detection: " + this.numberofeventsdetection);
	}

}
