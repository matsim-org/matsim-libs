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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.ptproject.qsim.QSimTimer;

/**
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

public class ControlInputMB extends AbstractControlInputImpl {

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

	private final Map<Id, Double> ttMeasured = new HashMap<Id, Double>();

	private LinkedList<Id> bottleNeckIdListMain = new LinkedList<Id>();

	private LinkedList<Id> bottleNeckIdListAlt = new LinkedList<Id>();

	// For distribution heterogenity check:
	private final Map<Id, Double> enterLinkEvents = new HashMap<Id, Double>();

	private final Map<Id, Double> capacities = new HashMap<Id, Double>();

	private final Map<Id, Double> ttFreeSpeedUpToAndIncludingLink = new HashMap<Id, Double>();

	private final Map<Id, Integer> numbersPassedOnInAndOutLinks = new HashMap<Id, Integer>();

	// For Accident detection:
	private Id currentBottleNeckMainRouteId;

	private Id currentBottleNeckAlternativeRouteId;

	private Collection<NetworkChangeEvent> accidents;

	private final QSimConfigGroup simulationConfig;

	public ControlInputMB(final QSimConfigGroup simulationConfig2, final Network network, final String outputDirectory) {
		super(network, outputDirectory);
		this.simulationConfig = simulationConfig2;
	}

	@Override
	public void init() {
		super.init();

		// Initialize ttMeasured with ttFreeSpeeds and linkFlows with zero.
		// Main route
		List<Id> linkIdsMainRoute = this.mainRoute.getLinkIds();
		linkIdsMainRoute.add(0, this.mainRoute.getStartLinkId());
		linkIdsMainRoute.add(this.mainRoute.getEndLinkId());
		for (Id linkId : linkIdsMainRoute) {
			if (!this.intraFlows.containsKey(linkId)) {
				this.intraFlows.put(linkId, 0.0);
			}

			if (!this.ttMeasured.containsKey(linkId)) {
				this.ttMeasured.put(linkId, this.ttFreeSpeeds.get(linkId));
			}

			Link l = this.network.getLinks().get(linkId);
			if (!this.capacities.containsKey(linkId)) {
				double capacity = ((LinkImpl)l).getFlowCapacity(Time.UNDEFINED_TIME) * this.simulationConfig.getFlowCapFactor()
						/ QSimTimer.getSimTickTime();
				this.capacities.put(l.getId(), capacity);
			}

			if (!this.enterLinkEventTimes.containsKey(linkId)) {
				List<Double> list = new LinkedList<Double>();
				this.enterLinkEventTimes.put(linkId, list);
			}

			if (!this.ttFreeSpeedUpToAndIncludingLink.containsKey(linkId)) {
				double tt = sumUpTTFreeSpeed(l.getToNode(), this.mainRoute);
				this.ttFreeSpeedUpToAndIncludingLink.put(linkId, tt);
			}
		}
		this.currentBottleNeckMainRouteId = this.mainRouteNaturalBottleNeckId;
		List<Node> nodesMainRoute = RouteUtils.getNodes(this.mainRoute, this.network);
		for (int i = 0; i < nodesMainRoute.size(); i++) {
			Node n = nodesMainRoute.get(i);
			for (Link inLink : n.getInLinks().values()) {
				if (!linkIdsMainRoute.contains(inLink.getId())) {
					double tt = sumUpTTFreeSpeed(n, this.mainRoute);
					this.ttFreeSpeedUpToAndIncludingLink.put(inLink.getId(), tt);
					this.inLinksMainRoute.add(inLink.getId());
					this.numbersPassedOnInAndOutLinks.put(inLink.getId(), 0);
					this.extraFlowsMainRoute.put(inLink.getId(), 0.0);
					List<Double> list = new LinkedList<Double>();
					this.enterLinkEventTimes.put(inLink.getId(), list);
				}
			}
			for (Link outLink : n.getOutLinks().values()) {
				if (!linkIdsMainRoute.contains(outLink.getId())) {
					double tt = sumUpTTFreeSpeed(n, this.mainRoute);
					this.ttFreeSpeedUpToAndIncludingLink.put(outLink.getId(), tt);
					this.outLinksMainRoute.add(outLink.getId());
					this.numbersPassedOnInAndOutLinks.put(outLink.getId(), 0);
					this.extraFlowsMainRoute.put(outLink.getId(), 0.0);
					List<Double> list = new LinkedList<Double>();
					this.enterLinkEventTimes.put(outLink.getId(), list);
				}
			}
		}
		// this.bottleNecksMain = null;

		// Alt Route
		List<Id> linkIdsAlternativeRoute = this.alternativeRoute.getLinkIds();
		linkIdsAlternativeRoute.add(0, this.alternativeRoute.getStartLinkId());
		linkIdsAlternativeRoute.add(this.alternativeRoute.getEndLinkId());
		for (Id linkId : linkIdsAlternativeRoute) {
			Link l = this.network.getLinks().get(linkId);
			if (!this.intraFlows.containsKey(linkId)) {
				this.intraFlows.put(linkId, 0.0);
			}

			if (!this.ttMeasured.containsKey(linkId)) {
				this.ttMeasured.put(linkId, this.ttFreeSpeeds.get(linkId));
			}

			if (!this.capacities.containsKey(linkId)) {
				double capacity = ((LinkImpl)l).getFlowCapacity(Time.UNDEFINED_TIME) * this.simulationConfig.getFlowCapFactor()
						/ QSimTimer.getSimTickTime();
				this.capacities.put(linkId, capacity);
			}

			if (!this.enterLinkEventTimes.containsKey(linkId)) {
				List<Double> list = new LinkedList<Double>();
				this.enterLinkEventTimes.put(linkId, list);
			}
			if (!this.ttFreeSpeedUpToAndIncludingLink.containsKey(linkId)) {
				double tt = sumUpTTFreeSpeed(l.getToNode(), this.mainRoute);
				this.ttFreeSpeedUpToAndIncludingLink.put(linkId, tt);
			}
		}
		this.currentBottleNeckAlternativeRouteId = this.altRouteNaturalBottleNeckId;

		List<Node> nodesAlternativeRoute = RouteUtils.getNodes(this.alternativeRoute, this.network);
		for (int i = 0; i < nodesAlternativeRoute.size(); i++) {
			Node n = nodesAlternativeRoute.get(i);
			for (Link inLink : n.getInLinks().values()) {
				if (!linkIdsAlternativeRoute.contains(inLink.getId())) {
					double tt = sumUpTTFreeSpeed(n, this.alternativeRoute);
					this.ttFreeSpeedUpToAndIncludingLink.put(inLink.getId(), tt);
					this.inLinksAlternativeRoute.add(inLink.getId());
					this.numbersPassedOnInAndOutLinks.put(inLink.getId(), 0);
					this.extraFlowsAlternativeRoute.put(inLink.getId(), 0.0);
					List<Double> list = new LinkedList<Double>();
					this.enterLinkEventTimes.put(inLink.getId(), list);
					// } else{
					// System.out.println("No additional inLinks");
				}
			}
			for (Link outLink : n.getOutLinks().values()) {
				if (!linkIdsAlternativeRoute.contains(outLink.getId())) {
					double tt = sumUpTTFreeSpeed(n, this.alternativeRoute);
					this.ttFreeSpeedUpToAndIncludingLink.put(outLink.getId(), tt);
					this.outLinksAlternativeRoute.add(outLink.getId());
					this.numbersPassedOnInAndOutLinks.put(outLink.getId(), 0);
					this.extraFlowsAlternativeRoute.put(outLink.getId(), 0.0);
					List<Double> list = new LinkedList<Double>();
					this.enterLinkEventTimes.put(outLink.getId(), list);
					// } else{
					// System.out.println("No additional outLinks");
				}
			}
		}
		if (Double.isNaN(this.updatetimeinoutflow)) {
			this.updatetimeinoutflow = getFreeSpeedRouteTravelTime(this.mainRoute);
		}
	}



	@Override
	public void handleEvent(final LinkEnterEvent event) {

		// Must be done before super.handleEvent as that removes entries
		if (this.ttMeasured.containsKey(event.getLinkId())) {
			this.enterLinkEvents.put(event.getPersonId(), event.getTime());
		}

		// handle flows on outLinks
		if (this.outLinksMainRoute.contains(event.getLinkId())) {
			int numbersPassed = this.numbersPassedOnInAndOutLinks.get(event.getLinkId()) + 1;
			this.numbersPassedOnInAndOutLinks.put(event.getLinkId(), numbersPassed);
		}
		else if (this.outLinksAlternativeRoute.contains(event.getLinkId())) {
			int numbersPassed = this.numbersPassedOnInAndOutLinks.get(event.getLinkId()) + 1;
			this.numbersPassedOnInAndOutLinks.put(event.getLinkId(), numbersPassed);
		}

		super.handleEvent(event);
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {

		// Must be done before super.handleEvent as that removes entries
		if (this.ttMeasured.containsKey(event.getLinkId())
				&& (this.enterLinkEvents.get(event.getPersonId()) != null)) {
			Double enterTime = this.enterLinkEvents.remove(event.getPersonId());
			Double travelTime = event.getTime() - enterTime;
			this.ttMeasured.put(event.getLinkId(), travelTime);
		}

		// Stores [NUMBEROFFLOWEVENTS] last events and calculates flow for detection
		// of capacity reduction
		if (this.intraFlows.containsKey(event.getLinkId())) {
			updateFlow(this.numberofeventsdetection, event);
		}
		if (this.inLinksAlternativeRoute.contains(event.getLinkId())
				|| this.outLinksAlternativeRoute.contains(event.getLinkId())
				|| this.inLinksMainRoute.contains(event.getLinkId())
				|| this.outLinksMainRoute.contains(event.getLinkId())) {
			// updateFlow(NUMBEROFEVENTSINOUTFLOW, event);
			updateFlow(this.updatetimeinoutflow, event);

		}

		super.handleEvent(event);
	}

	@Override
	public double getPredictedNashTime(final NetworkRoute route) {
		if (route.equals(this.mainRoute)) {
			return this.predTTMainRoute;
		}
		return this.predTTAlternativeRoute;
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
					this.network.getLinks().get(this.mainRouteNaturalBottleNeckId));
		}
		else {
			Id accidentLinkId = this.accidents.iterator().next().getLinks().iterator().next().getId();
			Id accidentLinkIdMainRoute = searchAccidentsOnRoutes(accidentLinkId);
			this.predTTMainRoute = getPredictedTravelTime(this.mainRoute,
					this.network.getLinks().get(accidentLinkIdMainRoute));
		}

		this.predTTAlternativeRoute = getPredictedTravelTime(this.alternativeRoute,
				this.network.getLinks().get(this.altRouteNaturalBottleNeckId));

		return this.predTTMainRoute - this.predTTAlternativeRoute;
	}

	private double getPredictedTravelTime(final NetworkRoute route, final Link bottleNeck) {

		log.trace("");
		log.trace("Sim time: " + QSimTimer.getTime());
		double predictedTT;
		List<Id> routeLinkIds = route.getLinkIds();
		routeLinkIds.add(0, route.getStartLinkId());
		routeLinkIds.add(route.getEndLinkId());
		boolean searchForBottleNecks = true;
		LinkedList<Id> bottleNeckList = new LinkedList<Id>();

		if (route == this.mainRoute) {
			bottleNeckList = this.bottleNeckIdListMain;
		}
		if (route == this.alternativeRoute) {
			bottleNeckList = this.bottleNeckIdListAlt;
		}

		// boolean queueFound = true;
		if (QSimTimer.getTime() % this.resetbottleneckintervall == 0) {
			bottleNeckList.clear();

			for (int i = routeLinkIds.size() - 1; i >= 0; i--) { // 2
				Id id = routeLinkIds.get(i);

				try{
				if ((this.ttMeasured.get(id) > (this.ttFreeSpeeds.get(id) + this.ignoredQueuingTime))
						&& searchForBottleNecks) {
					bottleNeckList.addFirst(id);
					searchForBottleNecks = false;
					// queueFound = true;
					log.trace("Link " + id.toString() + " was detected as a bottleneck.");
				}
				else if (this.ttMeasured.get(id) < this.ttFreeSpeeds.get(id)
						+ this.ignoredQueuingTime) {
					searchForBottleNecks = true;
				}
				} catch (NullPointerException npe) {
					System.out.println("breakpoint");
				}
			}
		}

		if (route == this.mainRoute) {
			this.bottleNeckIdListMain = bottleNeckList;
		}
		else if (route == this.alternativeRoute) {
			this.bottleNeckIdListAlt = bottleNeckList;
		}

		if (bottleNeckList.isEmpty()) {
			double agentsOnRoute = getAgents(route);
			double currentBottleNeckCapacity = ((LinkImpl)bottleNeck).getFlowCapacity(QSimTimer.getTime()) * this.simulationConfig.getFlowCapFactor()
			/ QSimTimer.getSimTickTime();
			double ttQueue = agentsOnRoute / currentBottleNeckCapacity;
			double ttFreeSpeed = getFreeSpeedRouteTravelTime(route);
			if (ttQueue > ttFreeSpeed) {
				return ttQueue;
			}
			return ttFreeSpeed;
		}

		// Sum up free speed after the last queue (last route segment)
		double ttAfterLastQueue;
		Link endLinkLastRS = this.network.getLinks().get(bottleNeckList.getLast());
//		Node rsFromNode = endLinkLastRS.getToNode(); // the last queue ends at this node
		Link endLink = this.network.getLinks().get(route.getEndLinkId());
		if (endLinkLastRS == endLink) { // if the last queue is on the last link
			ttAfterLastQueue = 0.0;
		}
		else {
//			Node rsToNode = endLink.getFromNode();
//			NetworkRouteWRefs lastRouteSegment = route.getSubRoute(rsFromNode, rsToNode);
			NetworkRoute lastRouteSegment = route.getSubRoute(endLinkLastRS.getId(), route.getEndLinkId());
			ttAfterLastQueue = getFreeSpeed(lastRouteSegment);
		}
		Id lastBottleNeckFound = bottleNeckList.getLast();
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

	private double getTTincludingThisRouteSegment(final NetworkRoute wholeRoute,
			final LinkedList<Id> bottleNeckList, final Id bottleNeckId) {

		double agentsAdditional = 0;
		double ttThisRouteSegment;
		double ttToThisRouteSegment;
		Id firstLinkIdInRS;
		List<Id> routeLinkIds = wholeRoute.getLinkIds();
		routeLinkIds.add(0, wholeRoute.getStartLinkId());
		routeLinkIds.add(wholeRoute.getEndLinkId());
		NetworkRoute routeSegment = null;

		double additionalAgentsOnRoute = 0;
		int previousBottleNeckIndex;

		log.trace("bottleNeckLink: " + bottleNeckId.toString());

		// If it is the first queue on the route...
		if (bottleNeckList.indexOf(bottleNeckId) == 0) {
			ttToThisRouteSegment = 0;
			firstLinkIdInRS = routeLinkIds.get(0);
			previousBottleNeckIndex = -1;
		}
		else {

			Id previousBottleNeck = bottleNeckList.get((bottleNeckList.indexOf(bottleNeckId) - 1));
			previousBottleNeckIndex = 0;
			for (int i = 0; i < routeLinkIds.size(); i++) {
				if (previousBottleNeck.equals(routeLinkIds.get(i))) {
					previousBottleNeckIndex = i;
					break;
				}
			}
			firstLinkIdInRS = routeLinkIds.get(previousBottleNeckIndex + 1);
			ttToThisRouteSegment = getTTincludingThisRouteSegment(wholeRoute,
					bottleNeckList, previousBottleNeck); // calls the tt calc recursively

		}

		Link firstLinkInRS = this.network.getLinks().get(firstLinkIdInRS);
		Link bottleNeck = this.network.getLinks().get(bottleNeckId);
		if (firstLinkInRS == bottleNeck) {
			routeSegment = new LinkNetworkRouteImpl(firstLinkIdInRS, bottleNeckId);
		} else {
			routeSegment = wholeRoute.getSubRoute(firstLinkInRS.getId(), bottleNeck.getId());
		}
		double netFlowOnRoute = getIntraFlow(firstLinkInRS.getId())
				- getIntraFlow(bottleNeckId);

		// Calculate additional agents that enter or leaves route
		List<Node> routeList = RouteUtils.getNodes(routeSegment, this.network);
		routeList.add(0, this.network.getLinks().get(routeSegment.getStartLinkId()).getFromNode());
		routeList.add(this.network.getLinks().get(routeSegment.getEndLinkId()).getToNode());
		List<Id> inAndOutLinks = new ArrayList<Id>();
		inAndOutLinks.addAll(this.getOutlinks(wholeRoute));
		inAndOutLinks.addAll(this.getInlinks(wholeRoute));
		double flowOnThisLink = 0;
		double ttFreeSpeedOnRouteSegment = 0;
		double additionalAgentsInOutLinks = 0;
		for (Id id : inAndOutLinks) {
			Link link = this.network.getLinks().get(id);
			if (routeList.contains(link.getToNode())
					|| routeList.contains(link.getFromNode())) {
				flowOnThisLink = getInOutFlow(id, wholeRoute);
				ttFreeSpeedOnRouteSegment = getTTFreeSpeedOnRouteSegmentToLink(
						wholeRoute, previousBottleNeckIndex, id);
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
		double ttQueue = agentsPredicted / getIntraFlow(bottleNeck.getId());
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

	private double getTTFreeSpeedOnRouteSegmentToLink(final NetworkRoute wholeRoute,
			final int previousBottleNeckIndex, final Id inOutLinkId) {

		List<Id> wholeRouteLinkIds = wholeRoute.getLinkIds();
		wholeRouteLinkIds.add(0, wholeRoute.getStartLinkId());
		wholeRouteLinkIds.add(wholeRoute.getEndLinkId());
		Id firstLinkIdOnRouteSegment = wholeRouteLinkIds.get(previousBottleNeckIndex + 1);

		double ttFreeSpeedToThisRouteSegment = 0.0;

		if (previousBottleNeckIndex == -1) {
			ttFreeSpeedToThisRouteSegment = 0.0;
		}
		else {
			Link firstLinkOnRouteSegment = this.network.getLinks().get(firstLinkIdOnRouteSegment);
			for (Id linkId : wholeRouteLinkIds) {
				ttFreeSpeedToThisRouteSegment += this.ttFreeSpeeds.get(linkId);
				Link l = this.network.getLinks().get(linkId);
				if (l.getToNode() == firstLinkOnRouteSegment.getFromNode()) {
					break;
				}
			}
		}

		double ttFreeSpeedOnRouteInlcudingThisLink = 0;
		ttFreeSpeedOnRouteInlcudingThisLink = this.ttFreeSpeedUpToAndIncludingLink.get(inOutLinkId);

		return ttFreeSpeedOnRouteInlcudingThisLink - ttFreeSpeedToThisRouteSegment;
	}

	private double getFreeSpeed(final NetworkRoute routeSegment) {
		double ttFS = 0;
		ttFS += this.ttFreeSpeeds.get(routeSegment.getStartLinkId());
		for (Id linkId : routeSegment.getLinkIds()) {
			ttFS += this.ttFreeSpeeds.get(linkId);
		}
		ttFS += this.ttFreeSpeeds.get(routeSegment.getEndLinkId());
		return ttFS;
	}

	private double getAgents(final NetworkRoute routeSegment) {
		double agents = 0;
		agents += this.numberOfAgents.get(routeSegment.getStartLinkId());
		for (Id linkId : routeSegment.getLinkIds()) {
			agents += this.numberOfAgents.get(linkId);
		}
		agents += this.numberOfAgents.get(routeSegment.getEndLinkId());
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

	private double getInOutFlow(final Id inLinkId, final NetworkRoute route) {
		double flow;
		if (route == this.mainRoute) {
			flow = this.extraFlowsMainRoute.get(inLinkId);
		}
		else if (route == this.alternativeRoute) {
			flow = this.extraFlowsAlternativeRoute.get(inLinkId);
		}
		else {
			flow = 0;
			System.err.println("Something is wrong, this shouldn't happen!");
		}
		return flow;
	}

	public double getIntraFlow(final Id linkId) {
		return this.intraFlows.get(linkId);
	}

	public double getCapacity(final Id linkId) {
		return this.capacities.get(linkId);
	}

	public Id getDetectedBottleNeck(final NetworkRoute route) {
		if (route == this.mainRoute) {
			return this.currentBottleNeckMainRouteId;
		}
		return this.currentBottleNeckAlternativeRouteId;
	}

	public void setIncidentLink(final Id linkId, final NetworkRoute route) {
		if (route == this.mainRoute) {
			this.currentBottleNeckMainRouteId = linkId;
		}
		else {
			this.currentBottleNeckAlternativeRouteId = linkId;
		}
	}

	private Id searchAccidentsOnRoutes(final Id accidentLinkId) {
		NetworkRoute r = this.mainRoute;
		for (int j = 0; j < 2; j++) {
			if (r.getStartLinkId().equals(accidentLinkId)) {
				return accidentLinkId;
			}
			if (r.getEndLinkId().equals(accidentLinkId)) {
				return accidentLinkId;
			}
			for (Id linkId : r.getLinkIds()) {
				if (linkId.equals(accidentLinkId)) {
					return linkId;
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

	public void setIgnoredQueuingTime(final double time) {
		log.debug("Set ignored queing time to: " + time);
		this.ignoredQueuingTime = time;
	}

	public void setUpdateTimeInOutFlow(final double sec) {
		this.updatetimeinoutflow = sec;
		log.debug("Set update time in and outflow to: " + sec);
	}

	public void setResetBottleNeckIntervall(final double time) {
		this.resetbottleneckintervall = time;
		log.debug("Set reset bottle neck intervall time to: " + this.resetbottleneckintervall);
	}

	public void setNumberOfEventsDetection(final int events) {
		this.numberofeventsdetection = events;
		log.debug("Set number of events detection: " + this.numberofeventsdetection);
	}

}
