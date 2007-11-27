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

package playground.arvid_daniel.coopers.fromArvid;

import java.io.IOException;
import java.util.HashMap;
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

/*
 * FIXME [kn] Because this class was build to replace NashWriter, it inherits a serious flaw:
 * This class takes args of type Route in ctor, and returns arguments of
 * type route at getRoute, but these routes are of different type (one with FakeLink, the other
 * with behavioral links).
 */

/* TODO [abergsten] iterate approach to find several "charges" of traffic 
 * with distances between them.
 */




public class ControlInputImplDAccident extends AbstractControlInputImpl 
implements EventHandlerLinkLeaveI, EventHandlerLinkEnterI, 
EventHandlerAgentDepartureI, EventHandlerAgentArrivalI, ControlInput {

	
	private static final Logger log = Logger.getLogger(ControlInputImplDAccident.class);

	double predTTRoute1;

	double predTTRoute2;

	private ControlInputWriter writer;

	private Map<String, Double> ttMeasured = new HashMap<String, Double> ();
	
	private Map<String, Double> enterLinkEvents = new HashMap<String, Double>();
	
	private Map <String, Double> linkFlows = new HashMap<String, Double>();

	private Map <String, List<Double>> enterLinkEventTimes = new HashMap<String, List<Double>>();
	
	private Map<String, Double> capacities = new HashMap<String, Double> ();


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
		for (Link l : routeLinks) {
			if (!this.linkFlows.containsKey(l.getId().toString()))  {
				this.linkFlows.put(l.getId().toString(), 0.0);
			}
			
			if (!this.ttMeasured.containsKey(l.getId().toString()))  {
				this.ttMeasured.put(l.getId().toString(), 
						this.ttFreeSpeeds.get(l.getId().toString()));
			}
			
			if (!this.capacities.containsKey(l.getId().toString()))  {
				this.capacities.put(l.getId().toString(), l.getCapacity() / 3600);
			}
			
			if (!this.enterLinkEventTimes.containsKey(l.getId().toString()))  {
				List<Double> list = new LinkedList<Double>();
				this.enterLinkEventTimes.put(l.getId().toString(), list );
			}
		}
		
//		Alt Route
		routeLinks = this.getAlternativeRoute().getLinkRoute();
		for (Link l : routeLinks) {
			if (!this.linkFlows.containsKey(l.getId().toString()))  {
				this.linkFlows.put(l.getId().toString(), 0.0);
			}
			
			if (!this.ttMeasured.containsKey(l.getId().toString()))  {
				this.ttMeasured.put(l.getId().toString(), 
						this.ttFreeSpeeds.get(l.getId().toString()));
			}
			
			if (!this.capacities.containsKey(l.getId().toString()))  {
				this.capacities.put(l.getId().toString(), l.getCapacity() / 3600);
			}
			
			if (!this.enterLinkEventTimes.containsKey(l.getId().toString()))  {
				List<Double> list = new LinkedList<Double>();
				this.enterLinkEventTimes.put(l.getId().toString(), list );
			}
		}				
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
		
		if (this.ttMeasured.containsKey(event.linkId)) {
			Double enterTime = this.enterLinkEvents.remove(event.agentId);
			Double travelTime = event.time - enterTime;
			this.ttMeasured.put(event.linkId, travelTime);

		}
		
//		Stores 5 last events and calculates flow
		if (this.linkFlows.containsKey(event.linkId)) {
			LinkedList<Double> list = (LinkedList<Double>) this.enterLinkEventTimes.get(event.linkId);
			if ( list.size() == 5 ) {
			list.removeFirst();
			list.add(event.time);
			}
			else if (1 < list.size() || list.size() < 5) {
				list.add(event.time);
			} else if ( list.size() == 0 ) {
				list.add(event.time - 1);
				list.add(event.time);
			}
			else {
				System.err.println("Error: number of enter event times stored exceeds 5!");
			}
			
//			Flow = agents / seconds:
			double flow = (list.size() - 1) / (list.getLast() - list.getFirst());
			this.linkFlows.put(event.linkId, flow);
		}
		
		super.handleEvent(event);
	}
	

	public void reset(final int iteration) {
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
		
		Link currentBottleNeck = bottleNeckLink;
		Link[] routeLinks = route.getLinkRoute();
		boolean isQueueOnRoute = false;
		boolean bottleNeckCongested = false;
		double currentBottleNeckCapacity = getCapacity(currentBottleNeck);
		double currentBottleNeckFlow = getFlow(currentBottleNeck);

			if (getMeasuredRouteTravelTime(route) > getFreeSpeedRouteTravelTime(route) ) {
			isQueueOnRoute = true;
			
			for ( int i = routeLinks.length - 1; i >= 0; i-- ) {
				String linkId = routeLinks[i].getId().toString();

//				To avoid round of errors, the difference has to be at last 1 second
				if ( this.ttMeasured.get(linkId) > this.ttFreeSpeeds.get(linkId) + 10)  {
					bottleNeckCongested = true;
					currentBottleNeck = routeLinks[i];
					currentBottleNeckFlow = getFlow(currentBottleNeck);
					
//					System.out.println("");
					log.debug("Current bottleneck capacity is " + currentBottleNeckCapacity + " and the measured link's flow is " + currentBottleNeckFlow);
					
//					If measured flow for some reason (inexact measuring) is higher than the links capacity, use the capacity 
					if ( currentBottleNeckFlow	< currentBottleNeckCapacity) {
						currentBottleNeckCapacity = currentBottleNeckFlow;
					}
					else  // measuring is inexact
						log.debug("Measured tt is longer than ttfreespeed, but measured flow is not reduced.");
					

//				do not check links before current bottleneck
				break; 
				}
			}
		}
		

		

		if ( !isQueueOnRoute )
			log.debug("No queue on route (no capacity reduction was found)");
		else if (isQueueOnRoute != bottleNeckCongested)
			log.debug("There is queue on the route, but not longer than one second on for any individual link");

		
		// get the array index of the bottleneck link
		int bottleNeckLinkIndexInArray = 0;
		for (int i = 0; i < routeLinks.length; i++) {
			if (currentBottleNeck.equals(routeLinks[i])) {
				bottleNeckLinkIndexInArray = i;
				break;
			}
		}
		
//		int j = bottleNeckLinkNumber;
		double ttFreeSpeedPart = 0.0;

		log.debug("The BN index is " + bottleNeckLinkIndexInArray + " (link " + currentBottleNeck.getId().toString() + ").");
		
//		Agents after bottleneck drive free speed (bottle neck index + 1)
		for (int i = bottleNeckLinkIndexInArray + 1; i <= routeLinks.length - 1; i++) {
			ttFreeSpeedPart += this.ttFreeSpeeds.get(routeLinks[i].getId().toString());
		}
			
		int firstCongestedLink = bottleNeckLinkIndexInArray;
		
/*		if ( !bottleNeckCongested ) {

//			Agents before bottleneck drive free speed if they are on sparsely trafficated links
			int j = bottleNeckLinkIndexInArray;
//			System.out.println("Bottleneck link index: " + j);
			while ( j >= 0 && 
				(this.numberOfAgents.get(routeLinks[j].getId().toString()) / currentBottleNeckCapacity)
					<= this.ttFreeSpeeds.get(routeLinks[j].getId().toString()) ) {	// is this criterium ok?
				
				ttFreeSpeedPart += this.ttFreeSpeeds.get(routeLinks[j].getId().toString());
				System.out.println("Adding to total free speed part before bottle neck; not-congested-link index " + j);
				j--;
				
			}
			firstCongestedLink = j;
		}
		
		else {
			firstCongestedLink = bottleNeckLinkIndexInArray;
		}*/
		  
						
		// count agents on congested part of the route 
		int agentsToQueueAtBottleNeck = 0;
		double ttFreeSpeedBeforeBottleNeck = 0;
		double predictedTT;
		for (int i = 0; i <= firstCongestedLink; i++) {
			agentsToQueueAtBottleNeck += 
				this.numberOfAgents.get(routeLinks[i].getId().toString());
			ttFreeSpeedBeforeBottleNeck += this.ttFreeSpeeds.get(routeLinks[i].getId().toString());

		}
		
		if (agentsToQueueAtBottleNeck / currentBottleNeckCapacity > ttFreeSpeedBeforeBottleNeck) {
			predictedTT = 
			(agentsToQueueAtBottleNeck / currentBottleNeckCapacity) + ttFreeSpeedPart;
		} else {
			predictedTT = getFreeSpeedRouteTravelTime(route);
		}
		
//		predictedTT = (agentsToQueueAtBottleNeck / currentBottleNeckCapacity) + ttFreeSpeedPart;

		
	
		

			log.debug("Predicted travel time = Agents / current capacity + freespeed = " + 
					agentsToQueueAtBottleNeck +" / "+currentBottleNeckCapacity +" + "+ ttFreeSpeedPart);
			log.debug("Predicted route tt is " + predictedTT);
			log.debug("Route freespeed tt is " + this.getFreeSpeedRouteTravelTime(route));

		return predictedTT;
	}
	

	// ContolInputI interface methods:
	public double getNashTime() {

		try {
			this.writer.writeAgentsOnLinks(this.numberOfAgents);
			this.writer.writeTravelTimesMainRoute(this.lastTime1,
					this.predTTRoute1);
			this.writer.writeTravelTimesAlternativeRoute(this.lastTime2,
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

}
