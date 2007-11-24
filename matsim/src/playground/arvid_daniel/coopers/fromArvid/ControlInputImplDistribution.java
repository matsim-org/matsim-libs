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

package playground.arvid_daniel.coopers.fromArvid;

import java.io.IOException;
import java.util.List;

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
import org.matsim.withinday.trafficmanagement.Accident;
import org.matsim.withinday.trafficmanagement.ControlInput;

/**
 * Just like ControlInputSB, this model checks if the agents
 * before the bottleneck will cause a queue or not, and based 
 * on that predicts the nashtime. 
 * However, the prediction is improved by checking the 
 * distribution of the traffic before the bottleneck.
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


public class ControlInputImplDistribution extends AbstractControlInputImpl 
implements EventHandlerLinkLeaveI, EventHandlerLinkEnterI, 
EventHandlerAgentDepartureI, EventHandlerAgentArrivalI, ControlInput {

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
	public void handleEvent(final EventLinkEnter event) {
		super.handleEvent(event);
	}

	@Override
	public void handleEvent(final EventLinkLeave event) {
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

		String accidentLinkId = this.accidents.get(0).getLinkId();
		Link bottleNeckLinkRoute1 = searchAccidentsOnRoutes(accidentLinkId);
		
		// first link on route used as default -- should be the bottleneck specific to the route
		Link bottleNeckLinkRoute2 = this.alternativeRoute.getLinkRoute()[0]; 
		this.predTTRoute1 = getPredictedTravelTime(this.mainRoute,
				bottleNeckLinkRoute1);
		this.predTTRoute2 = getPredictedTravelTime(this.alternativeRoute,
				bottleNeckLinkRoute2);

		return this.predTTRoute1 - this.predTTRoute2;
	}

	private double getPredictedTravelTime(final Route route,
			final Link bottleNeckLink) {
		Link[] routeLinks = route.getLinkRoute();
		double bottleNeckCapacity = bottleNeckLink.getCapacity() / 3600;
	
		// get the array index of the bottleneck link
		int bottleNeckLinkNumber = 0;
		for (int i = 0; i < routeLinks.length; i++) {
			if (bottleNeckLink.equals(routeLinks[i])) {
				bottleNeckLinkNumber = i;
				break;
			}
		}

		/*Checking which links on the route that have agents few enough 
		 * not to build a queue at the bottleneck (non-critical links).
		 */ 
		double ttFreeSpeedPart = 0.0;
		// start counting either from last link on route or from the bottleneck link
//		int j = routeLinks.length - 1; 
		int j = bottleNeckLinkNumber;
		

		if ( j != routeLinks.length - 1 ) {
			for (int i = j; i <= routeLinks.length - 1; i++) 
			ttFreeSpeedPart += this.ttFreeSpeeds.get(routeLinks[j].getId().toString());
		}
		
		while ( j >= 0 && 
				(this.numberOfAgents.get(routeLinks[j].getId().toString()) / bottleNeckCapacity) 
				<= this.ttFreeSpeeds.get(routeLinks[j].getId().toString()) ) {
			
			ttFreeSpeedPart += this.ttFreeSpeeds.get(routeLinks[j].getId().toString());
			j--;
		}
				
		// count agents on congested part of the route 
		int agentsToQueueAtBottleNeck = 0;
		for (int i = 0; i <= j; i++) 
			agentsToQueueAtBottleNeck += 
				this.numberOfAgents.get(routeLinks[i].getId().toString());
		
		double predictedTT = 
			(agentsToQueueAtBottleNeck / bottleNeckCapacity) + ttFreeSpeedPart;
		return predictedTT;
	}
	

	private Link searchAccidentsOnRoutes(final String accidentLinkId) {
		Route r = this.mainRoute;
		for (int j = 0; j < 2; j++) {
			Link[] links = r.getLinkRoute();
			for (int i = 0; i < links.length; i++) {
				if (links[i].getId().toString()
						.equalsIgnoreCase(accidentLinkId)) {
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
			this.writer.writeTravelTimesMainRoute(this.lastTime1,
					this.predTTRoute1);
			this.writer.writeTravelTimesAlternativeRoute(this.lastTime2,
					this.predTTRoute2);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return getPredictedNashTime();
	}

	public void setAccidents(final List<Accident> accidents) {
		this.accidents = accidents;
	}
}
