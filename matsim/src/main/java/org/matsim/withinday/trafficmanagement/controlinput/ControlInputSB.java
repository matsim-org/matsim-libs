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
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.ptproject.qsim.SimulationTimer;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.misc.Time;

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

public class ControlInputSB extends AbstractControlInputImpl {

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

	private final Map<Id, Double> ttMeasured = new HashMap<Id, Double>();

	// For distribution heterogenity check:
	private final Map<Id, Double> enterLinkEvents = new HashMap<Id, Double>();

	private final Map<Id, Double> capacities = new HashMap<Id, Double>();

	private final Map<Id, Double> ttFreeSpeedUpToAndIncludingLink = new HashMap<Id, Double>();

	private final Map<Id, Integer> numbersPassedOnInAndOutLinks = new HashMap<Id, Integer>();

	// For Accident detection:
	private Link currentBottleNeckMainRoute;

	private Double currentBNCapacityMainRoute;

	private Link currentBottleNeckAlternativeRoute;

	private Double currentBNCapacityAlternativeRoute;

	private Collection<NetworkChangeEvent> accidents;

	private final SimulationConfigGroup simulationConfig;

	public ControlInputSB(final SimulationConfigGroup simulationConfigGroup) {
		this.simulationConfig = simulationConfigGroup;
	}

	@Override
	public void init() {
		super.init();
		// Initialize ttMeasured with ttFreeSpeeds and linkFlows with zero.
		// Main route
		List<Link> linksMainRoute = this.mainRoute.getLinks();
		for (Link l : linksMainRoute) {
			if (!this.intraFlows.containsKey(l.getId())) {
				this.intraFlows.put(l.getId(), 0.0);
			}

			if (!this.ttMeasured.containsKey(l.getId())) {
				this.ttMeasured.put(l.getId(), this.ttFreeSpeeds.get(l
						.getId()));
			}

			if (!this.capacities.containsKey(l.getId())) {
				double capacity = ((LinkImpl)l).getFlowCapacity(Time.UNDEFINED_TIME) * this.simulationConfig.getFlowCapFactor()
						/ SimulationTimer.getSimTickTime();
				this.capacities.put(l.getId(), capacity);
			}

			if (!this.enterLinkEventTimes.containsKey(l.getId())) {
				List<Double> list = new LinkedList<Double>();
				this.enterLinkEventTimes.put(l.getId(), list);
			}

			if (!this.ttFreeSpeedUpToAndIncludingLink.containsKey(l.getId())) {
				double tt = sumUpTTFreeSpeed(l.getToNode(), this.mainRoute);
				this.ttFreeSpeedUpToAndIncludingLink.put(l.getId(), tt);
			}
		}
		this.currentBottleNeckMainRoute = this.mainRouteNaturalBottleNeck;
		this.currentBNCapacityMainRoute = getCapacity(this.mainRouteNaturalBottleNeck.getId());
		List<Node> nodesMainRoute = this.mainRoute.getNodes();
		for (int i = 1; i < nodesMainRoute.size() - 1; i++) {
			Node n = nodesMainRoute.get(i);
			for (Link inLink : n.getInLinks().values()) {
				if (!linksMainRoute.contains(inLink)) {
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
				if (!linksMainRoute.contains(outLink)) {
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

		// Alt Route
		List<Link> linksAlternativeRoute = this.alternativeRoute.getLinks();
		for (Link l : linksAlternativeRoute) {
			if (!this.intraFlows.containsKey(l.getId())) {
				this.intraFlows.put(l.getId(), 0.0);
			}

			if (!this.ttMeasured.containsKey(l.getId())) {
				this.ttMeasured.put(l.getId(), this.ttFreeSpeeds.get(l
						.getId()));
			}

			if (!this.capacities.containsKey(l.getId())) {
				double capacity = ((LinkImpl) l).getFlowCapacity(Time.UNDEFINED_TIME) * this.simulationConfig.getFlowCapFactor()
						/ SimulationTimer.getSimTickTime();
				this.capacities.put(l.getId(), capacity);
			}

			if (!this.enterLinkEventTimes.containsKey(l.getId())) {
				List<Double> list = new LinkedList<Double>();
				this.enterLinkEventTimes.put(l.getId(), list);
			}
			if (!this.ttFreeSpeedUpToAndIncludingLink.containsKey(l.getId())) {
				double tt = sumUpTTFreeSpeed(l.getToNode(), this.mainRoute);
				this.ttFreeSpeedUpToAndIncludingLink.put(l.getId(), tt);
			}
		}
		this.currentBottleNeckAlternativeRoute = this.altRouteNaturalBottleNeck;
		this.currentBNCapacityAlternativeRoute = getCapacity(this.altRouteNaturalBottleNeck.getId());

		List<Node> nodesAlternativeRoute = this.alternativeRoute.getNodes();
		for (int i = 1; i < nodesAlternativeRoute.size() - 1; i++) {
			Node n = nodesAlternativeRoute.get(i);
			for (Link inLink : n.getInLinks().values()) {
				if (!linksAlternativeRoute.contains(inLink)) {
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
				if (!linksAlternativeRoute.contains(outLink)) {
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

		this.UPDATETIMEINOUTFLOW = getFreeSpeedRouteTravelTime(this.mainRoute);

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
			updateFlow(NUMBEROFEVENTSDETECTION, event);
		}
		if (this.inLinksAlternativeRoute.contains(event.getLinkId())
				|| this.outLinksAlternativeRoute.contains(event.getLinkId())
				|| this.inLinksMainRoute.contains(event.getLinkId())
				|| this.outLinksMainRoute.contains(event.getLinkId())) {
			// updateFlow(NUMBEROFEVENTSINOUTFLOW, event);
			updateFlow(this.UPDATETIMEINOUTFLOW, event);
		}

		super.handleEvent(event);
	}

	@Override
	public double getPredictedNashTime(final NetworkRouteWRefs route) {
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
					this.mainRouteNaturalBottleNeck);
		}
		else {
			Id accidentLinkId =  this.accidents.iterator().next().getLinks().iterator().next().getId();
			Link accidentLinkMainRoute = searchAccidentsOnRoutes(accidentLinkId);
			this.predTTMainRoute = getPredictedTravelTime(this.mainRoute,
					accidentLinkMainRoute);
		}

		this.predTTAlternativeRoute = getPredictedTravelTime(this.alternativeRoute,
				this.altRouteNaturalBottleNeck);

		return this.predTTMainRoute - this.predTTAlternativeRoute;
	}

	private double getPredictedTravelTime(final NetworkRouteWRefs route, final Link bottleNeck) {

		double predictedTT;
		List<Link> routeLinks = route.getLinks();
		double ttFreeSpeedPart = 0.0;
		int agentsToQueueAtBottleNeck = 0;
//		boolean guidanceObjectWillQueue = false;
		Link currentBottleNeck = bottleNeck;
		double currentBottleNeckCapacity = 0;
		double ttFreeSpeedBeforeBottleNeck = 0;

		if (this.incidentDetectionActive) {
			currentBottleNeck = getDetectedBottleNeck(route);
			currentBottleNeckCapacity = getIncidentCapacity(route);
			for (int i = routeLinks.size() - 1; i >= 0; i--) {
				Id id = routeLinks.get(i).getId();

				if (this.ttMeasured.get(id) > this.ttFreeSpeeds.get(id)
						+ this.ignoredQueuingTime) {
					currentBottleNeck = routeLinks.get(i);
					setIncidentLink(currentBottleNeck, route);
					currentBottleNeckCapacity = getFlow(currentBottleNeck.getId());
					setIncidentCapacity(currentBottleNeckCapacity, route);

					// do not check links before current bottleneck
					break;
				}
				else if (SimulationTimer.getTime() % RESETBOTTLENECKINTERVALL == 0) {
					this.currentBNCapacityAlternativeRoute = getCapacity(this.altRouteNaturalBottleNeck.getId());
					this.currentBNCapacityMainRoute = getCapacity(this.mainRouteNaturalBottleNeck.getId());
					this.currentBottleNeckAlternativeRoute = this.altRouteNaturalBottleNeck;
					this.currentBottleNeckMainRoute = this.mainRouteNaturalBottleNeck;
				}
			}
		}

		else if (!this.incidentDetectionActive) {
			currentBottleNeck = bottleNeck;
			currentBottleNeckCapacity = ((LinkImpl)currentBottleNeck).getFlowCapacity(SimulationTimer.getTime()) * this.simulationConfig.getFlowCapFactor()
					/ SimulationTimer.getSimTickTime();

		}

		// get the array index of the bottleneck link
		int bottleNeckIndex = 0;
		for (int i = 0; i < routeLinks.size(); i++) {
			if (currentBottleNeck.equals(routeLinks.get(i))) {
				bottleNeckIndex = i;
				break;
			}
		}

		// Agents after bottleneck drive free speed (bottle neck index + 1)
		for (int i = bottleNeckIndex + 1; i < routeLinks.size(); i++) {
			ttFreeSpeedPart += this.ttFreeSpeeds.get(routeLinks.get(i).getId());
		}

		if (this.distributioncheckActive) {

			for (int r = bottleNeckIndex; r >= 0; r--) {
				Link link = routeLinks.get(r);
				double linkAgents = this.numberOfAgents.get(link.getId());
				double linkFreeSpeedTT = this.ttFreeSpeeds.get(link.getId());

				if ((linkAgents / currentBottleNeckCapacity) <= linkFreeSpeedTT) {
					ttFreeSpeedPart += linkFreeSpeedTT;
//					log.debug("Distribution check: Link " + link.getId().toString()
//							+ " is added to freeSpeedPart.");
				}
				else {
					int agentsUpToLink = 0;
					double freeSpeedUpToLink = 0;
					for (int p = 0; p <= r; p++) {
						agentsUpToLink += this.numberOfAgents.get(routeLinks.get(p).getId());
						freeSpeedUpToLink += this.ttFreeSpeeds.get(routeLinks.get(p).getId());
						ttFreeSpeedBeforeBottleNeck = freeSpeedUpToLink;
					}

					if (this.backgroundnoiseDetectionActive) {
						agentsUpToLink += getAdditionalAgents(route, r);
					}

					if ((agentsUpToLink / currentBottleNeckCapacity) >= freeSpeedUpToLink) {
//						guidanceObjectWillQueue = true;
						currentBottleNeck = link;
						agentsToQueueAtBottleNeck = agentsUpToLink;
						// log.debug("Distribution check: Critical link. All agents on link
						// " + criticalCongestedLink.getId().toString() + " will NOT pass
						// the bottleneck before the guidance object arrive.");
						break;
					}
//					else {
						ttFreeSpeedPart += linkFreeSpeedTT;
						// log.debug("Distribution check: Non-critical link. All agents on
						// link " + criticalCongestedLink.getId().toString() + " will pass
						// the bottle neck when before the guidancde object arrive." );
//					}
				}
			}
//			if (guidanceObjectWillQueue) {
//				log.debug("The guidance object will queue with agents ahead.");
//			}
//			else {
//						.debug("The guidance object will not queue at the bottleneck. No critical congested link was found.");
//			}
			// log.debug("Distribution check performed: " + agentsToQueueAtBottleNeck
			// + " will queue at link " + criticalCongestedLink.getId().toString());
		}

		// Run without distribution check
		else if (!this.distributioncheckActive) {

			// count agents on congested part of the route
			ttFreeSpeedBeforeBottleNeck = 0;
			for (int i = 0; i <= bottleNeckIndex; i++) {
				agentsToQueueAtBottleNeck += this.numberOfAgents.get(routeLinks.get(i).getId());
				ttFreeSpeedBeforeBottleNeck += this.ttFreeSpeeds.get(routeLinks.get(i).getId());
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

	private int getAdditionalAgents(final NetworkRouteWRefs route, final int linkIndex) {
		double totalExtraAgents = 0.0;

		// check distance and free speed travel time from start node to bottleneck
		Id linkId1 = route.getLinks().get(linkIndex).getId();
		double ttToLink = this.ttFreeSpeedUpToAndIncludingLink.get(linkId1);

		List<Id> inAndOutLinks = new ArrayList<Id>();
		inAndOutLinks.addAll(this.getOutlinks(route));
		inAndOutLinks.addAll(this.getInlinks(route));
		for (Id id : inAndOutLinks) {
			double extraAgents = 0.0;
			double flow = getInOutFlow(id, route);
			if ((this.ttFreeSpeedUpToAndIncludingLink.get(id) > ttToLink)
					|| (this.ttFreeSpeedUpToAndIncludingLink == null)) {
				extraAgents = 0.0;
			}
			else {
				extraAgents = flow * this.ttFreeSpeedUpToAndIncludingLink.get(id);
				// System.out.println("Extra agents = " + flow + " * " +
				// this.ttFreeSpeedUpToAndIncludingLink.get(linkId) + " (link" + linkId
				// + " )." );
			}
			totalExtraAgents += extraAgents;
		}
		return (int) (totalExtraAgents);
	}

	private double getInOutFlow(final Id inLinkId, final NetworkRouteWRefs route) {
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

	public double getFlow(final Id linkId) {
		return this.intraFlows.get(linkId);
	}

	public double getCapacity(final Id linkId) {
		double capacity = this.capacities.get(linkId);
		return capacity;
	}

	public Link getDetectedBottleNeck(final NetworkRouteWRefs route) {
		Link l;
		if (route == this.mainRoute) {
			l = this.currentBottleNeckMainRoute;
		}
		else {
			l = this.currentBottleNeckAlternativeRoute;
		}
		return l;
	}

	public void setIncidentLink(final Link link, final NetworkRouteWRefs route) {
		if (route == this.mainRoute) {
			this.currentBottleNeckMainRoute = link;
		}
		else {
			this.currentBottleNeckAlternativeRoute = link;
		}
	}

	private void setIncidentCapacity(final Double currentBottleNeckCapacity, final NetworkRouteWRefs route) {
		if (route == this.mainRoute) {
			this.currentBNCapacityMainRoute = currentBottleNeckCapacity;
		}
		else {
			this.currentBNCapacityAlternativeRoute = currentBottleNeckCapacity;
		}
	}

	private Double getIncidentCapacity(final NetworkRouteWRefs route) {
		double cap;
		if (route == this.mainRoute) {
			cap = this.currentBNCapacityMainRoute;
		}
		else {
			cap = this.currentBNCapacityAlternativeRoute;
		}
		return cap;
	}

	private Link searchAccidentsOnRoutes(final Id accidentLinkId) {
		NetworkRouteWRefs r = this.mainRoute;
		for (int j = 0; j < 2; j++) {
			for (Link link : r.getLinks()) {
				if (link.getId().equals(accidentLinkId)) {
					return link;
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

	public void setDistributionCheckActive(final boolean b) {
		log.debug("distribution check active: " + b);
		this.distributioncheckActive = b;
	}

	public void setBackgroundnoiseCompensationActive(final boolean b) {
		log.debug("background noise compensation active: " + b);
		this.backgroundnoiseDetectionActive = b;
	}

	public void setIncidentDetectionActive(final boolean b) {
		log.debug("Incident detection active: " + b);
		this.incidentDetectionActive = b;
	}


}
