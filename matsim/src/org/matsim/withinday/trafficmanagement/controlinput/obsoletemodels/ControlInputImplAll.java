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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.LinkEnterEnter;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.SimulationTimer;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.population.Route;
import org.matsim.withinday.trafficmanagement.AbstractControlInputImpl;
import org.matsim.withinday.trafficmanagement.ControlInput;
import org.matsim.withinday.trafficmanagement.controlinput.ControlInputWriter;

/**
 *
 * @author abergsten and dzetterberg
 */

/* User parameters are:
 * DISTRIBUTIONCHECK	True means that model checks traffic distribution before bottle
 * 						neck.
 * NUMBEROFFLOWEVENTS	The flow calculations are based on the last NUMBEROFFLOWEVENTS
 * 						agents. A higher value means better predictions if congestion.
 * IGNOREDQUEUINGIME	Additional link travel times up to IGNOREDQUEUINGIME will not be
 * 						considered a sign of temporary capacity reduction.
 * FLOWUPDATETIME		determines how often to measure additional flows from in- and
 * 						outlinks. Default is to update every 60 seconds.
 *
 */


public class ControlInputImplAll extends AbstractControlInputImpl
implements LinkLeaveEventHandler, LinkEnterEventHandler,
AgentDepartureEventHandler, AgentArrivalEventHandler, ControlInput {

//	User parameters:
	private static final boolean DISTRIBUTIONCHECKACTIVATED = true;

		private static final int NUMBEROFFLOWEVENTS = 10;

		private static final double IGNOREDQUEUINGIME = 5;

	private static final boolean DISTURBANCECHECKACTIVATED = true;

		private static final double FLOWUPDATETIME = 100;


	private static final Logger log = Logger.getLogger(ControlInputImplAll.class);

	private double predTTMainRoute;

	private double predTTAlternativeRoute;

	private ControlInputWriter writer;

	private Map<String, Double> ttMeasured = new HashMap<String, Double> ();


//	For distribution heterogenity check:
	private Map<String, Double> enterLinkEvents = new HashMap<String, Double>();

	private Map <String, Double> linkFlows = new HashMap<String, Double>();

	private Map <String, List<Double>> enterLinkEventTimes = new HashMap<String, List<Double>>();

	private Map<String, Double> capacities = new HashMap<String, Double> ();


//	For in/outlinks disturbance check:
	private List<Link> inLinksMainRoute = new ArrayList<Link>();

	private List<Link> outLinksMainRoute = new ArrayList<Link>();

	private ArrayList<Node> nodesMainRoute = new ArrayList<Node>();

	private List<Link> inLinksAlternativeRoute = new ArrayList<Link>();

	private List<Link> outLinksAlternativeRoute = new ArrayList<Link>();

	private ArrayList<Node> nodesAlternativeRoute = new ArrayList<Node>();

	private Map<String, Double> inFlowDistances = new HashMap<String, Double>();

	private Map<String, Double> outFlowDistances = new HashMap<String, Double>();

	private	Map<String, Double> inFlows = new HashMap<String, Double>();

	private	Map<String, Double> outFlows = new HashMap<String, Double>();

	private Map<String, Integer> numbersPassedOnInAndOutLinks = new HashMap<String, Integer>();

	private double ttFreeSpeedBeforeBottleNeck = 0.0;



	public ControlInputImplAll() {
		super();
		this.writer = new ControlInputWriter();
	}

	@Override
	public void init() {
		super.init();
		this.writer.open();

//		Initialize ttMeasured with ttFreeSpeeds and linkFlows with zero.
//		Main route
		Link [] linksMainRoute = this.mainRoute.getLinkRoute();
		for (Link l : linksMainRoute) {
			if (!this.linkFlows.containsKey(l.getId().toString()))  {
				this.linkFlows.put(l.getId().toString(), 0.0);
			}

			if (!this.ttMeasured.containsKey(l.getId().toString()))  {
				this.ttMeasured.put(l.getId().toString(),
						this.ttFreeSpeeds.get(l.getId().toString()));
			}

			if (!this.capacities.containsKey(l.getId().toString()))  {
				double capacity = ((QueueLink)l).getSimulatedFlowCapacity() / SimulationTimer.getSimTickTime();
				this.capacities.put(l.getId().toString(), capacity);
			}

			if (!this.enterLinkEventTimes.containsKey(l.getId().toString()))  {
				List<Double> list = new LinkedList<Double>();
				this.enterLinkEventTimes.put(l.getId().toString(), list );
			}
		}
		this.nodesMainRoute = this.mainRoute.getRoute();
		List<Link> linksMainRouteList = Arrays.asList(linksMainRoute);
		for (int i = 1; i < this.nodesMainRoute.size() -1; i++ ) {
			Node n = this.nodesMainRoute.get(i);
			for (Link inLink : n.getInLinks().values()) {
				if(!linksMainRouteList.contains(inLink)){
					double d = getDistanceFromFirstNode(inLink.getToNode(), this.mainRoute);
					this.inLinksMainRoute.add(inLink);
					this.inFlowDistances.put(inLink.getId().toString(), d);
					this.inFlows.put(inLink.getId().toString(), 0.0);
					this.numbersPassedOnInAndOutLinks.put(inLink.getId().toString(), 0);
				}
			}
			for (Link outLink : n.getOutLinks().values()) {
				if(!linksMainRouteList.contains(outLink)){
					double d = getDistanceFromFirstNode(outLink.getFromNode(), this.mainRoute);
					this.outLinksMainRoute.add(outLink);
					this.outFlowDistances.put(outLink.getId().toString(), d);
					this.outFlows.put(outLink.getId().toString(), 0.0);
					this.numbersPassedOnInAndOutLinks.put(outLink.getId().toString(), 0);
				}
			}
		}

//		Alt Route
		Link [] linksAlternativeRoute = this.alternativeRoute.getLinkRoute();
		for (Link l : linksAlternativeRoute) {
			if (!this.linkFlows.containsKey(l.getId().toString()))  {
				this.linkFlows.put(l.getId().toString(), 0.0);
			}

			if (!this.ttMeasured.containsKey(l.getId().toString()))  {
				this.ttMeasured.put(l.getId().toString(),
						this.ttFreeSpeeds.get(l.getId().toString()));
			}

			if (!this.capacities.containsKey(l.getId().toString()))  {
				double capacity = ((QueueLink)l).getSimulatedFlowCapacity() / SimulationTimer.getSimTickTime();
				this.capacities.put(l.getId().toString(), capacity);			}

			if (!this.enterLinkEventTimes.containsKey(l.getId().toString()))  {
				List<Double> list = new LinkedList<Double>();
				this.enterLinkEventTimes.put(l.getId().toString(), list );
			}
		}

		this.nodesAlternativeRoute = this.alternativeRoute.getRoute();
		List<Link> linksAlternativeRouteList = Arrays.asList(linksAlternativeRoute);
		for (int i = 1; i < this.nodesAlternativeRoute.size() -1; i++ ) {
			Node n = this.nodesAlternativeRoute.get(i);
			for (Link inLink : n.getInLinks().values()) {
				if(!linksAlternativeRouteList.contains(inLink)){
					double d = getDistanceFromFirstNode(inLink.getToNode(), this.alternativeRoute);
					this.inLinksAlternativeRoute.add(inLink);
					this.inFlowDistances.put(inLink.getId().toString(), d);
					this.outFlows.put(inLink.getId().toString(), 0.0);
					this.numbersPassedOnInAndOutLinks.put(inLink.getId().toString(), 0);
//				} else{
//					System.out.println("No additional inLinks");
				}
			}
			for (Link outLink : n.getOutLinks().values()) {
				if(!linksAlternativeRouteList.contains(outLink)){
					double d = getDistanceFromFirstNode(outLink.getFromNode(), this.alternativeRoute);
					this.outLinksAlternativeRoute.add(outLink);
					this.outFlowDistances.put(outLink.getId().toString(), d);
					this.outFlows.put(outLink.getId().toString(), 0.0);
					this.numbersPassedOnInAndOutLinks.put(outLink.getId().toString(), 0);
//				} else{
//					System.out.println("No additional outLinks");
				}
			}
		}
	}

	private double getDistanceFromFirstNode(Node node, Route route) {
		Link[] routeLinks = route.getLinkRoute();
		double distance = 0;
		int i=0;
		while(!routeLinks[i].getToNode().equals(node)){
			distance += routeLinks[i].getLength();
			i++;
		}
		distance += routeLinks[i].getLength();
		return distance;
	}

	@Override
	public void handleEvent(final LinkEnterEnter event) {

//		Must be done before super.handleEvent as that removes entries
		if ( this.ttMeasured.containsKey(event.linkId) ) {
			this.enterLinkEvents.put(event.agentId, event.time);
		}

		//handle flows on outLinks
		if (this.outLinksMainRoute.contains(event.link)){
			int numbersPassed = this.numbersPassedOnInAndOutLinks.get(event.linkId) + 1;
			this.numbersPassedOnInAndOutLinks.put(event.linkId, numbersPassed);
		}
		else if (this.outLinksAlternativeRoute.contains(event.linkId)){
			int numbersPassed = this.numbersPassedOnInAndOutLinks.get(event.linkId) + 1;
			this.numbersPassedOnInAndOutLinks.put(event.linkId, numbersPassed);
		}

		super.handleEvent(event);
	}


	@Override
	public void handleEvent(final LinkLeaveEvent event) {

//		Must be done before super.handleEvent as that removes entries
		if (this.ttMeasured.containsKey(event.linkId)
				&& (this.enterLinkEvents.get(event.agentId) != null)) {
			Double enterTime = this.enterLinkEvents.remove(event.agentId);
			Double travelTime = event.time - enterTime;
			this.ttMeasured.put(event.linkId, travelTime);
		}

//		Stores [NUMBEROFFLOWEVENTS] last events and calculates flow
		if (this.linkFlows.containsKey(event.linkId)) {
			LinkedList<Double> list = (LinkedList<Double>) this.enterLinkEventTimes.get(event.linkId);
			if ( list.size() == NUMBEROFFLOWEVENTS ) {
			list.removeFirst();
			list.add(event.time);
			}
			else if ((1 < list.size()) || (list.size() < NUMBEROFFLOWEVENTS)) {
				list.add(event.time);
			} else if ( list.size() == 0 ) {
				list.add(event.time - 1);
				list.add(event.time);
			}
			else {
				System.err.println("Error: number of enter event times stored exceeds numberofflowevents!");
			}

//			Flow = agents / seconds:
			double flow = (list.size() - 1) / (list.getLast() - list.getFirst());
			this.linkFlows.put(event.linkId, flow);
		}

		//handle flows on inLinks
		if (this.inLinksMainRoute.contains(event.link)) {
			int numbersPassed = this.numbersPassedOnInAndOutLinks.get(event.linkId) + 1;
			this.numbersPassedOnInAndOutLinks.put(event.linkId, numbersPassed);
		}
		else if (this.inLinksAlternativeRoute.contains(event.linkId)){
			int numbersPassed = this.numbersPassedOnInAndOutLinks.get(event.linkId) + 1;
			this.numbersPassedOnInAndOutLinks.put(event.linkId, numbersPassed);
		}

		super.handleEvent(event);
	}

	public void reset(int iteration) {
		//nothing need to be done here anymore cause everything is done in the finishIteration().
	}

	public void finishIteration() {
		BufferedWriter w1 = null;
		BufferedWriter w2 = null;
		try{
			w1 = new BufferedWriter(new FileWriter("../studies/arvidDaniel/output/ttMeasuredMainRoute.txt"));
			w2 = new BufferedWriter(new FileWriter("../studies/arvidDaniel/output/ttMeasuredAlternativeRoute.txt"));
		}catch(IOException e){
			e.printStackTrace();
		}

		Iterator<Double> it1 = this.ttMeasuredMainRoute.iterator();
		try{
			while(it1.hasNext()){
				double measuredTimeMainRoute = it1.next();
				w1.write(Double.toString(measuredTimeMainRoute));
				w1.write("\n");
				w1.flush();
			}
		}catch (IOException e){
			e.printStackTrace();
		}

		Iterator<Double> it2 = this.ttMeasuredAlternativeRoute.iterator();
		try{
			while(it2.hasNext()){
				double measuredTimeAlternativeRoute = it2.next();
				w2.write(Double.toString(measuredTimeAlternativeRoute));
				w2.write("\n");
				w2.flush();
			}
		}catch (IOException e){
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
					this.predTTMainRoute);
			this.writer.writeTravelTimesAlternativeRoute(this.lastTimeAlternativeRoute,
					this.predTTAlternativeRoute);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if( SimulationTimer.getTime()%FLOWUPDATETIME == 0 ){
			calculateInAndOutFlows();
		}

		return getPredictedNashTime();

	}

	// calculates the predictive time difference
	public double getPredictedNashTime() {

		this.predTTMainRoute =
			getPredictedTravelTime(this.mainRoute, this.mainRouteNaturalBottleNeck);

		this.predTTAlternativeRoute =
			getPredictedTravelTime(this.alternativeRoute, this.altRouteNaturalBottleNeck);

		return this.predTTMainRoute - this.predTTAlternativeRoute;
	}


	private double getPredictedTravelTime(final Route route,
			final Link naturalBottleNeck) {

		Link bottleNeck = naturalBottleNeck;

		double bottleNeckCapacity = getCapacity(bottleNeck);
		double currentBottleNeckFlow = getFlow(bottleNeck);
		Link[] routeLinks = route.getLinkRoute();

		boolean queueOnRoute = false;
		boolean bottleNeckCongested = false;

		double ttFreeSpeedPart = 0.0;
		int agentsToQueueAtBottleNeck = 0;

		boolean guidanceObjectWillQueue = false;

		double predictedTT;


			if (getMeasuredRouteTravelTime(route) > getFreeSpeedRouteTravelTime(route) ) {
			queueOnRoute = true;

			for ( int i = routeLinks.length - 1; i >= 0; i-- ) {
				String linkId = routeLinks[i].getId().toString();

//				The difference has to be at least [IGNOREDQUEUINGIME] seconds to avoid using incorrect flows
				if ( this.ttMeasured.get(linkId) > this.ttFreeSpeeds.get(linkId) + IGNOREDQUEUINGIME)  {
					bottleNeckCongested = true;
					bottleNeck = routeLinks[i];
					currentBottleNeckFlow = getFlow(bottleNeck);

//					If measured flow for some reason (e.g. inexact measuring) is higher than
//					the links capacity, use the normal capacity
					if ( currentBottleNeckFlow	< bottleNeckCapacity) {
						log.debug("The bottleneck capacity is changed from " + bottleNeckCapacity + ", to the measured flow: " + currentBottleNeckFlow);

						bottleNeckCapacity = currentBottleNeckFlow;

					}
					else  // measuring is inexact
						log.debug("Measured tt is longer than ttfreespeed, but measured flow is not reduced.");

//				do not check links before current bottleneck
				break;
				}
			}
		}

		if ( !queueOnRoute )
			log.debug("No queue on route (no capacity reduction was found).");
		else if (queueOnRoute && !bottleNeckCongested)
			log.debug("There is queue on the route, but it is ignored because no individual link has a significant queue.");


		// get the array index of the bottleneck link
		int bottleNeckIndex = 0;
		for (int i = 0; i < routeLinks.length; i++) {
			if (bottleNeck.equals(routeLinks[i])) {
				bottleNeckIndex = i;
				break;
			}
		}


		log.debug("The bottleneck link is " + bottleNeck.getId().toString() + " (index " + bottleNeckIndex + ").");

//		Agents after bottleneck drive free speed (bottle neck index + 1)
		for (int i = bottleNeckIndex + 1; i < routeLinks.length; i++) {
			double f = this.ttFreeSpeeds.get(routeLinks[i].getId().toString());
//			log.debug("Added to ttFreeSpeed: link " + routeLinks[i].getId().toString() + " (index " + i + ") after bottleneck to ttFreeSpeedPart: + " + f);
			ttFreeSpeedPart += f;
		}
		log.debug("After the bottleneck, the ttFreeSpeed is " + ttFreeSpeedPart);

//		Link criticalCongestedLink = currentBottleNeck;
//		int criticalCongestedLinkIndex = 0;
		if (DISTRIBUTIONCHECKACTIVATED) {

			for (int r = bottleNeckIndex; r >= 0; r--) {
				Link link = routeLinks[r];
				double linkAgents = this.numberOfAgents.get(link.getId().toString());
				double linkFreeSpeedTT = this.ttFreeSpeeds.get(link.getId().toString());

				if ( (linkAgents / bottleNeckCapacity) <= linkFreeSpeedTT ) {
					ttFreeSpeedPart += linkFreeSpeedTT;
					log.debug("Distribution check: Link " + link.getId().toString() + " is not congested and is added to freeSpeedPart.");
				}

				else {

					int agentsUpToLink = 0;
					double freeSpeedUpToLink = 0;
					for (int p = 0; p <= r; p++ ) {
						agentsUpToLink += this.numberOfAgents.get(routeLinks[p].getId().toString());
						freeSpeedUpToLink += this.ttFreeSpeeds.get(routeLinks[p].getId().toString());
						this.ttFreeSpeedBeforeBottleNeck = freeSpeedUpToLink;
					}
					if (DISTURBANCECHECKACTIVATED) {
						agentsUpToLink += getAdditionalAgents(route, r);
					}
					if ( (agentsUpToLink / bottleNeckCapacity) >= freeSpeedUpToLink ) {
						guidanceObjectWillQueue = true;

						bottleNeck = link;
//						criticalCongestedLink = link; //we only care about agents up to and including

						bottleNeckIndex = r;
//						criticalCongestedLinkIndex = r;
						agentsToQueueAtBottleNeck = agentsUpToLink;

//						log.debug("Distribution check: Critical link. All agents on link " + criticalCongestedLink.getId().toString() + " will NOT pass the bottleneck before the guidance object arrive.");
						break;
					}
					else {
						ttFreeSpeedPart += linkFreeSpeedTT;
//						log.debug("Distribution check: Non-critical link. All agents on link " + criticalCongestedLink.getId().toString() + " will pass the bottle neck when before the guidancde object arrive." );
					}
				}
			}
			if (guidanceObjectWillQueue) {
				log.debug("The guidance object will queue with agents ahead.");
			}
			else {
				log.debug("The guidance object will not queue at the bottleneck. No critical congestend link was found.");
			}
//			log.debug("Distribution check performed: " + agentsToQueueAtBottleNeck + " will queue at link " + criticalCongestedLink.getId().toString());
		}




//		Run without distribution check
		else if (!DISTRIBUTIONCHECKACTIVATED){
//			criticalCongestedLinkIndex = bottleNeckIndex;
			// count agents on congested part of the route
			this.ttFreeSpeedBeforeBottleNeck = 0;
			for (int i = 0; i <= bottleNeckIndex; i++) {
				agentsToQueueAtBottleNeck +=
					this.numberOfAgents.get(routeLinks[i].getId().toString());
				this.ttFreeSpeedBeforeBottleNeck += this.ttFreeSpeeds.get(routeLinks[i].getId().toString());
			}
			if (DISTURBANCECHECKACTIVATED) {
				agentsToQueueAtBottleNeck += getAdditionalAgents(route, bottleNeckIndex);
			}
			log.debug("Distribution check inactivated: " + agentsToQueueAtBottleNeck + " agents before bottle neck link " + bottleNeck.getId().toString());
		}



		predictedTT = (agentsToQueueAtBottleNeck / bottleNeckCapacity) + ttFreeSpeedPart;


//		Check route criteria if distribution check is deactivated
		if ( !DISTRIBUTIONCHECKACTIVATED &&
				!(agentsToQueueAtBottleNeck / bottleNeckCapacity > this.ttFreeSpeedBeforeBottleNeck) ) {
			predictedTT = getFreeSpeedRouteTravelTime(route);
			log.debug("Distribution check inactivated: Predicted tt is " + predictedTT);
		}

		log.debug("Predicted tt is " + predictedTT /* + " = " +
				agentsToQueueAtBottleNeck +" / "+currentBottleNeckCapacity + " + " + ttFreeSpeedPart */ );
		log.debug("(Route freespeed tt is " + this.getFreeSpeedRouteTravelTime(route) + " .)");

		return predictedTT;
	}


	private int getAdditionalAgents(final Route route, final int lastCriticalLinkIndex){
		double weightedNetFlow = 0.0;
		double netFlow = 0.0;
		double distanceToBottleNeck = 0.0;
//		double ttToBottleNeck = 0.0;

		//check distance and free speed travel time from start node to bottleneck
		Link [] routeLinks = route.getLinkRoute();
		distanceToBottleNeck =
			getDistanceFromFirstNode(routeLinks[lastCriticalLinkIndex].getToNode(), route);
//		for (int i = 0; i <= lastCriticalLinkIndex; i++) {
//			String linkId = routeLinks[lastCriticalLinkIndex].getId().toString();
//			ttToBottleNeck += this.ttFreeSpeeds.get(linkId);
//		}

		//Sum up INFLOWS and weigh it corresponding to their distance to the start node.
		Iterator<Link> itInlinks = this.getInlinks(route).iterator();
		while(itInlinks.hasNext()){
			Link inLink = itInlinks.next();
			double inFlow = getInFlow(inLink, route);
			double weightedInFlow = 0;
			//don't include Links after the bottleNeck. Also takes care of null-case
			if((this.inFlowDistances.get(inLink.getId().toString()) == null)
					|| (this.inFlowDistances.get(inLink.getId().toString()) > distanceToBottleNeck)){
				weightedInFlow = 0;
			}
			else{
				weightedInFlow = inFlow * this.inFlowDistances.get(inLink.getId().toString());				}
			weightedNetFlow += weightedInFlow;
		}

		//Sum up OUTFLOWS and weigh it corresponding to their distance to the start node.
		Iterator<Link> itOutlinks = this.getOutlinks(route).iterator();
		while(itOutlinks.hasNext()){
			Link outLink = itOutlinks.next();
			double outFlow = getOutFlow(outLink, route);
			double weightedOutFlow = 0;
			if( (this.outFlowDistances.get(outLink.getId().toString()) == null)
					|| (this.outFlowDistances.get(outLink.getId().toString()) > distanceToBottleNeck)){
				weightedOutFlow = 0;
			}
			else{
				weightedOutFlow = outFlow * this.outFlowDistances.get(outLink.getId().toString());
			}
			weightedNetFlow -= weightedOutFlow;
		}

		netFlow = weightedNetFlow / distanceToBottleNeck;

//		return (int)(ttToBottleNeck * netFlow);
		return (int)(this.ttFreeSpeedBeforeBottleNeck * netFlow);
	}


	//set new in and outflows and reset numbersPassedOnInAndOutLinks every [FLOWUPDATETIME]
	private void calculateInAndOutFlows() {
		//inLinksMainRoute
		Iterator<Link> itInlinksMain = this.inLinksMainRoute.iterator();
		while(itInlinksMain.hasNext()){
			Link inLink = itInlinksMain.next();
			double flow = (double)this.numbersPassedOnInAndOutLinks.get(inLink.getId().toString())/FLOWUPDATETIME;
			this.inFlows.put(inLink.getId().toString(), flow);
			this.numbersPassedOnInAndOutLinks.put(inLink.getId().toString(), 0);
		}

		//outLinksMainRoute
		Iterator<Link> itOutlinksMain = this.outLinksMainRoute.iterator();
		while(itOutlinksMain.hasNext()){
			Link outLink = itOutlinksMain.next();
			double flow = (double)this.numbersPassedOnInAndOutLinks.get(outLink.getId().toString())/FLOWUPDATETIME;
			this.outFlows.put(outLink.getId().toString(), flow);
			this.numbersPassedOnInAndOutLinks.put(outLink.getId().toString(), 0);
		}
		//inLinksAlternativeRoute
		Iterator<Link> itInlinksAlt = this.inLinksAlternativeRoute.iterator();
		while(itInlinksAlt.hasNext()){
			Link inLink = itInlinksAlt.next();
			double flow = (double)this.numbersPassedOnInAndOutLinks.get(inLink.getId().toString())/FLOWUPDATETIME;
			this.inFlows.put(inLink.getId().toString(), flow);
			this.numbersPassedOnInAndOutLinks.put(inLink.getId().toString(), 0);
		}

		//outLinksAlternativeRoute
		Iterator<Link> itOutlinksAlt = this.outLinksAlternativeRoute.iterator();
		while(itOutlinksAlt.hasNext()){
			Link outLink = itOutlinksAlt.next();
			double flow = (double)this.numbersPassedOnInAndOutLinks.get(outLink.getId().toString())/FLOWUPDATETIME;
			this.outFlows.put(outLink.getId().toString(), flow);
			this.numbersPassedOnInAndOutLinks.put(outLink.getId().toString(), 0);
		}
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

	private double getOutFlow(Link outLink, Route route) {
		double flow;
		if(route == this.mainRoute){
			flow = this.outFlows.get(outLink.getId().toString());
		}
		else if(route == this.alternativeRoute){
			flow = this.outFlows.get(outLink.getId().toString());
		}
		else{
			flow = 0;
			System.err.println("Something is wrong, this shouldn't happen!");
		}
		return flow;
	}


	private double getInFlow(Link inLink, Route route) {
		double flow;
		if(route == this.mainRoute){
			flow = this.inFlows.get(inLink.getId().toString());
		}
		else if(route == this.alternativeRoute){
			flow = this.inFlows.get(inLink.getId().toString());
		}
		else{
			flow = 0;
			System.err.println("Something is wrong, this shouldn't happen!");
		}
		return flow;
	}

	public double getFlow(Link link) {
		double flow = this.linkFlows.get(link.getId().toString());
		return flow;
	}

	public double getCapacity(Link link) {
		double capacity = this.capacities.get(link.getId().toString());
		return capacity;
	}

}

