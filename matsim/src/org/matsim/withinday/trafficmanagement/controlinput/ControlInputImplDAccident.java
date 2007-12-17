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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.network.Link;
import org.matsim.plans.Route;
import org.matsim.withinday.trafficmanagement.AbstractControlInputImpl;
import org.matsim.withinday.trafficmanagement.ControlInput;

/**
 * Just like ControlInputSB, this model checks if the agents before 
 * the bottleneck will cause a queue or not, and based on that predicts
 * the time difference between two alternative routes.
 * 
 *  This model automatically and continuosly detects bottlenecks and 
 *  therefore does not use information about the accident.
 * 
 * 
 * @author abergsten and dzetterberg
 */

//TODO: When the queuing check is done, the average measured tt for the last x cars should be considered instead of just the latest car.

/* User parameters are:
 * DISTRIBUTIONCHECK	True means that model checks traffic distribution before bottle
 * 						neck.
 * NUMBEROFFLOWEVENTS	The flow calculations are based on the last NUMBEROFFLOWEVENTS 
 * 						agents. A higher value means better predictions if congestion.
 * IGNOREDQUEUINGIME	Additional link travel times up to IGNOREDQUEUINGIME will not be 
 * 						considered a sign of temporary capacity reduction.
 * 
 */

/* TODO [abergsten] iterate approach to find several "charges" of traffic 
 * with distances between them.
 */


public class ControlInputImplDAccident extends AbstractControlInputImpl
implements EventHandlerLinkLeaveI, EventHandlerLinkEnterI, 
EventHandlerAgentDepartureI, EventHandlerAgentArrivalI, ControlInput {

	
	private static final int NUMBEROFFLOWEVENTS = 20;

	private static final double IGNOREDQUEUINGIME = 20;
	
	private static final boolean DISTRIBUTIONCHECK = false;
	
	private static final Logger log = Logger.getLogger(ControlInputImplDAccident.class);

	double predTTRoute1;

	double predTTRoute2;

	private ControlInputWriter writer;

	private Map<String, Double> ttMeasured = new HashMap<String, Double> ();
	
	private Map<String, Double> enterLinkEvents = new HashMap<String, Double>();
	
	private Map <String, Double> linkFlows = new HashMap<String, Double>();
	
	private Map <String, List<Double>> enterLinkEventTimes = new HashMap<String, List<Double>>();
	
	private Map<String, Double> capacities = new HashMap<String, Double> ();
	
	private Link currentBottleNeckMainRoute;
	
	private Double currentBNCapacityMainRoute;
	
	private Link currentBottleNeckAlternativeRoute;
	
	private Double currentBNCapacityAlternativeRoute;

	

	public ControlInputImplDAccident() {
		super();
		this.writer = new ControlInputWriter();
	}
	
	@Override
	public void init() {
		super.init();
		this.writer.open();
		
				
//		Initialize ttMeasured with ttFreeSpeeds and linkFlows with zero.
//		Main route
		Link[] routeLinks = this.getMainRoute().getLinkRoute();
		currentBottleNeckMainRoute = mainRouteNaturalBottleNeck;
		for (Link l : routeLinks) {
			if (!this.linkFlows.containsKey(l.getId().toString()))  {
				this.linkFlows.put(l.getId().toString(), 0.0);
			}
			
			if (!this.ttMeasured.containsKey(l.getId().toString()))  {
				this.ttMeasured.put(l.getId().toString(), 
						this.ttFreeSpeeds.get(l.getId().toString()));
			}
			
			if (!this.capacities.containsKey(l.getId().toString()))  {
				this.capacities.put(l.getId().toString(), ((QueueLink)l).getSimulatedFlowCapacity()/SimulationTimer.getSimTickTime());
			}
			
			if (!this.enterLinkEventTimes.containsKey(l.getId().toString()))  {
				List<Double> list = new LinkedList<Double>();
				this.enterLinkEventTimes.put(l.getId().toString(), list );
			}
		}
		currentBNCapacityMainRoute = getCapacity(mainRouteNaturalBottleNeck);

		
//		Alt Route
		routeLinks = this.getAlternativeRoute().getLinkRoute();
		currentBottleNeckAlternativeRoute = altRouteNaturalBottleNeck;
		for (Link l : routeLinks) {
			if (!this.linkFlows.containsKey(l.getId().toString()))  {
				this.linkFlows.put(l.getId().toString(), 0.0);
			}
			
			if (!this.ttMeasured.containsKey(l.getId().toString()))  {
				this.ttMeasured.put(l.getId().toString(), 
						this.ttFreeSpeeds.get(l.getId().toString()));
			}
			
			if (!this.capacities.containsKey(l.getId().toString()))  {
				this.capacities.put(l.getId().toString(), ((QueueLink)l).getSimulatedFlowCapacity()/SimulationTimer.getSimTickTime());
			}
			
			if (!this.enterLinkEventTimes.containsKey(l.getId().toString()))  {
				List<Double> list = new LinkedList<Double>();
				this.enterLinkEventTimes.put(l.getId().toString(), list );
			}
		}
		currentBNCapacityAlternativeRoute = getCapacity(altRouteNaturalBottleNeck);

	}

	@Override
	public void handleEvent(final EventLinkEnter event) {
		
		if ( this.ttMeasured.containsKey(event.linkId) ) {
			this.enterLinkEvents.put(event.agentId, event.time);
		}
		
		super.handleEvent(event);	
	}
	

	@Override
	public void handleEvent(final EventLinkLeave event) {
		
		if (this.ttMeasured.containsKey(event.linkId) 
				&& this.enterLinkEvents.get(event.agentId) != null) {
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
			else if (1 < list.size() || list.size() < NUMBEROFFLOWEVENTS) {
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
		
		super.handleEvent(event);
	}
	

	public void reset(final int iteration) {
		BufferedWriter w1 = null;
		BufferedWriter w2 = null;
		try{
			w1 = new BufferedWriter(new FileWriter("../studies/arvidDaniel/output/ttMeasuredMainRoute.txt"));
			w2 = new BufferedWriter(new FileWriter("../studies/arvidDaniel/output/ttMeasuredAlternativeRoute.txt"));
		}catch(IOException e){
			e.printStackTrace();
		}
		
		Iterator<Double> it1 = ttMeasuredMainRoute.iterator();
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
			
		Iterator<Double> it2 = ttMeasuredAlternativeRoute.iterator();
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
	public void handleEvent(final EventAgentDeparture event) {
		super.handleEvent(event);
	}

	@Override
	public void handleEvent(final EventAgentArrival event) {
		super.handleEvent(event);
	}

	// calculates the predictive NashTime with a single-bottle-neck-model.
	public double getPredictedNashTime() {

//		BottleNeck MainRoute
//		String accidentLinkId = this.accidents.get(0).getLinkId();
//		Link bottleNeckLinkRoute1 = searchAccidentsOnRoutes(accidentLinkId);

//		BottleNeck AltRoute
//		first link on route used as default -- should be the bottleneck specific to the route
//		Link bottleNeckLinkRoute2 = this.alternativeRoute.getLinkRoute()[0];
		
		this.predTTRoute1 = getPredictedTravelTime(this.mainRoute,
				this.mainRouteNaturalBottleNeck);
		this.predTTRoute2 = getPredictedTravelTime(this.alternativeRoute,
				this.altRouteNaturalBottleNeck);

		return this.predTTRoute1 - this.predTTRoute2;
	}

	
	private double getPredictedTravelTime(final Route route,
			final Link bottleNeckLink) {
		
		Link currentBottleNeck;
		Double currentBottleNeckCapacity;
		Link[] routeLinks = route.getLinkRoute();
//		boolean isQueueOnRoute = false;
//		boolean bottleNeckCongested = false;

		currentBottleNeck = getCurrentBottleNeck(route);
		currentBottleNeckCapacity = getCurrentBNCapacity(route);

		//		double currentBottleNeckFlow = getFlow(currentBottleNeck);		
		
//		System.out.println();
//		System.out.println("currentBN cap(flow): " +currentBottleNeckCapacity);
//			if (getMeasuredRouteTravelTime(route) > getFreeSpeedRouteTravelTime(route)) {
//			isQueueOnRoute = true;
			
		for ( int i = routeLinks.length - 1; i >= 0; i-- ) {
			String linkId = routeLinks[i].getId().toString();
//			if(SimulationTimer.getTime()%60*100 == 0){
//				currentBottleNeckMainRoute = mainRouteNaturalBottleNeck;
//				currentBottleNeckAlternativeRoute = altRouteNaturalBottleNeck;
//			}
				
//			The difference has to be at least [IGNOREDQUEUINGIME] seconds to avoid using incorrect flows 
			if ( this.ttMeasured.get(linkId) > this.ttFreeSpeeds.get(linkId) +IGNOREDQUEUINGIME)  {
				System.out.println("link som uppfyllde villkoret: " +routeLinks[i].getId().toString());
//				bottleNeckCongested = true;
				currentBottleNeck = routeLinks[i];
				setCurrentBottleNeck(currentBottleNeck, route);
				currentBottleNeckCapacity = getFlow(currentBottleNeck);
				setCurrentBNCapacity(currentBottleNeckCapacity, route);
//				currentBottleNeckFlow = getFlow(currentBottleNeck);
//				currentBottleNeckCapacity = getCapacity(currentBottleNeck);
				
//				System.out.println("");
//				log.debug("Current bottleneck capacity is " + currentBottleNeckCapacity + " and the measured link's flow is " + currentBottleNeckFlow);
				
//				If measured flow for some reason (inexact measuring) is higher than the links capacity, use the capacity 
//				if ( currentBottleNeckFlow	<= currentBottleNeckCapacity) {
//					currentBottleNeckCapacity = currentBottleNeckFlow;
//				}
				
				System.out.println();
				System.out.println("currentBottleNeckCapacity is: " +currentBottleNeckCapacity);
					//log.debug("Measured tt is longer than ttfreespeed, but measured flow is not reduced.");
				
//				do not check links before current bottleneck
			break; 
			}
			else if(SimulationTimer.getTime()%3600 == 0){
				currentBNCapacityAlternativeRoute = getCapacity(altRouteNaturalBottleNeck);
				currentBNCapacityMainRoute = getCapacity(mainRouteNaturalBottleNeck);
				currentBottleNeckAlternativeRoute = altRouteNaturalBottleNeck;
				currentBottleNeckMainRoute = mainRouteNaturalBottleNeck;
			}
		}
						

//		if ( !isQueueOnRoute )
//			log.debug("No queue on route (no capacity reduction was found)");
//		else if (isQueueOnRoute != bottleNeckCongested)
//			log.debug("There is queue on the route, but not longer than one second on for any individual link");

		
		// get the array index of the bottleneck link
		int bottleNeckArrayIndex = 0;
		for (int i = 0; i < routeLinks.length; i++) {
			if (currentBottleNeck.equals(routeLinks[i])) {
				bottleNeckArrayIndex = i;
				break;
			}
		} 
		
		double ttFreeSpeedPart = 0.0;
		double predictedTT;
		int agentsToQueueAtBottleNeck = 0;
		double ttFreeSpeedBeforeBottleNeck = 0;

		log.debug("The BN index is " + bottleNeckArrayIndex + " (link " + currentBottleNeck.getId().toString() + ").");
		
//		Agents after bottleneck drive free speed (bottle neck index + 1)
		for (int i = bottleNeckArrayIndex + 1; i <= routeLinks.length - 1; i++) {
			ttFreeSpeedPart += this.ttFreeSpeeds.get(routeLinks[i].getId().toString());
		}
		

		if (DISTRIBUTIONCHECK) {
			Link criticalCongestedLink = null;
			int arrayIndexCCL = 0;
			
			for (int r = bottleNeckArrayIndex; r >= 0; r--) {
				Link link = routeLinks[r];
				double linkAgents = this.numberOfAgents.get(link.getId().toString());
				double linkFreeSpeedTT = this.ttFreeSpeeds.get(link.getId().toString());
				
				if ( (linkAgents / currentBottleNeckCapacity) <= linkFreeSpeedTT ) {
					ttFreeSpeedPart += linkFreeSpeedTT;
//					System.out.println("Link " + link.getId().toString() + " was not congested. Added to freeSpeedPart.");
				}
				
				else {
					
					int agentsUpToLink = 0;
					double freeSpeedUpToLink = 0;
					for (int p = 0; p <= r; p++ ) {
						agentsUpToLink += this.numberOfAgents.get(routeLinks[p].getId().toString());
						freeSpeedUpToLink += this.ttFreeSpeeds.get(routeLinks[p].getId().toString());					
					}
					if ( (agentsUpToLink / currentBottleNeckCapacity) >= freeSpeedUpToLink ) {
						criticalCongestedLink = link; //we only care about agents up to and including
						agentsToQueueAtBottleNeck = agentsUpToLink;

						
//						System.out.println("Link " + link.getId().toString() + " was congested and all agents before ( " + agentsToQueueAtBottleNeck + " ) will queue." );
						break;
					}				
					else {
						ttFreeSpeedPart += linkFreeSpeedTT;
//						System.out.println("Link " + link.getId().toString() + " was congested but queue will dissolve before you arrive at BN." );
					}
				}
			}
//			if (criticalCongestedLink != null) {
//				System.out.println("You will queue with agents ahead of you up to and including link " + arrayIndexCCL);
//			}
//			else {
//				System.out.println("You will not queue at the bottleneck. There was no critical congestend link.");
//			}				
			
			predictedTT = 
				(agentsToQueueAtBottleNeck / currentBottleNeckCapacity) + ttFreeSpeedPart;
//			System.out.println("predicted tt = agentsToQueueAtBottleNeck / bottleNeckCapacity + ttFreeSpeedPart = " +
//					agentsToQueueAtBottleNeck + " / " + bottleNeckCapacity + " + " + ttFreeSpeedPart + " = " + predictedTT);
		}
		
//		Run without distribution check
		else {
		int firstCongestedLink = bottleNeckArrayIndex;
		
		// count agents on congested part of the route 

		for (int i = 0; i <= firstCongestedLink; i++) {
			agentsToQueueAtBottleNeck += 
				this.numberOfAgents.get(routeLinks[i].getId().toString());
			ttFreeSpeedBeforeBottleNeck += this.ttFreeSpeeds.get(routeLinks[i].getId().toString());
		}
		
		if (agentsToQueueAtBottleNeck / currentBottleNeckCapacity > ttFreeSpeedBeforeBottleNeck) {
			predictedTT = 
			(agentsToQueueAtBottleNeck / currentBottleNeckCapacity) + ttFreeSpeedPart;
			log.debug("Predicted travel time = Agents / current capacity + freespeed = " + 
					agentsToQueueAtBottleNeck +" / "+currentBottleNeckCapacity +" + "+ ttFreeSpeedPart);
		} else {
			predictedTT = getFreeSpeedRouteTravelTime(route);
		}
			log.debug("Predicted route tt is " + predictedTT);
			log.debug("Route freespeed tt is " + this.getFreeSpeedRouteTravelTime(route));
		}
		return predictedTT;
	}
	

	private void setCurrentBNCapacity(Double currentBottleNeckCapacity, Route route) {
		if(route == mainRoute){
			currentBNCapacityMainRoute = currentBottleNeckCapacity;
		}
		else{
			currentBNCapacityAlternativeRoute = currentBottleNeckCapacity;
		}
	}

	private Double getCurrentBNCapacity(Route route) {
		double cap;
		if(route == mainRoute){
			cap = currentBNCapacityMainRoute;
		}
		else{
			cap = currentBNCapacityAlternativeRoute;
		}
		return cap;
	}

	// ContolInputI interface methods:
	public double getNashTime() {

//		if(SimulationTimer.getTime()%(60*60) == 0){
//			currentBNCapacityAlternativeRoute = getCapacity(altRouteNaturalBottleNeck);
//			currentBNCapacityMainRoute = getCapacity(mainRouteNaturalBottleNeck);
//			currentBottleNeckAlternativeRoute = altRouteNaturalBottleNeck;
//			currentBottleNeckMainRoute = mainRouteNaturalBottleNeck;
//		}
		
		try {
			this.writer.writeAgentsOnLinks(this.numberOfAgents);
			this.writer.writeTravelTimesMainRoute(this.lastTimeMainRoute,
					this.predTTRoute1);
			this.writer.writeTravelTimesAlternativeRoute(this.lastTimeAlternativeRoute,
					this.predTTRoute2);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return getPredictedNashTime();
	}
	
	public double getFlow(Link link) {
		double flow = this.linkFlows.get(link.getId().toString());
		return flow;
	}
	
	public double getCapacity(Link link) {
		double capacity = this.capacities.get(link.getId().toString());
		return capacity;
	}

	public Link getCurrentBottleNeck(final Route route){
		Link l;
		if(route == mainRoute){
			l = currentBottleNeckMainRoute;
		}
		else{
			l = currentBottleNeckAlternativeRoute;
		}
		return l;
	}
	
	public void setCurrentBottleNeck(final Link link, final Route route){
		if(route == mainRoute){
			currentBottleNeckMainRoute = link;
		}
		else{
			currentBottleNeckAlternativeRoute = link;
		}
	}
	
}
