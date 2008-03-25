/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractControlInputImpl.java
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

package org.matsim.withinday.trafficmanagement;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.matsim.mobsim.SimulationTimer;
import org.matsim.network.Link;
import org.matsim.plans.Route;



/**
 * @author dgrether
 *
 */
public abstract class AbstractControlInputImpl implements ControlInput, EventHandlerAgentDepartureI, EventHandlerAgentArrivalI, EventHandlerLinkEnterI, EventHandlerLinkLeaveI {

	private static final Logger log = Logger.getLogger(AbstractControlInputImpl.class);

	protected Route mainRoute;

	protected Route alternativeRoute;

	protected Map<String, Integer> numberOfAgents;

	protected Map <String, Double> ttFreeSpeeds;

	protected String firstLinkOnMainRoute;

	protected String firstLinkOnAlternativeRoute;

	protected String lastLinkOnMainRoute;

	protected String lastLinkOnAlternativeRoute;

	protected Map<String, Double> enterEvents1;

	protected Map<String, Double> enterEvents2;

	protected double lastTimeMainRoute;

	protected double lastTimeAlternativeRoute;

	protected double timeDifference;

	protected Link altRouteNaturalBottleNeck;

	protected Link mainRouteNaturalBottleNeck;

	protected double ttFreeSpeedAltRoute;

	protected double ttFreeSpeedMainRoute;

	protected List<Double> ttMeasuredMainRoute  = new ArrayList<Double>();

	protected List<Double> ttMeasuredAlternativeRoute = new ArrayList<Double>();

	public AbstractControlInputImpl() {
		this.numberOfAgents = new HashMap<String, Integer>();
		this.enterEvents1 = new HashMap<String, Double>();
		this.enterEvents2 = new HashMap<String, Double>();
		this.ttFreeSpeeds = new HashMap<String, Double>();

		this.lastTimeMainRoute = 0.0;
		this.lastTimeAlternativeRoute = 0.0;
		this.timeDifference = 0.0;
		this.ttFreeSpeedAltRoute = 0.0;
		this.ttFreeSpeedMainRoute = 0.0;

	}


	public Route getMainRoute() {
		return this.mainRoute;
	}

	public Route getAlternativeRoute() {
		return this.alternativeRoute;
	}

	public void setAlternativeRoute(final Route route) {
		this.alternativeRoute = route;
	}


	public void setMainRoute(final Route route) {
		this.mainRoute = route;
	}



	public int getNumberOfVehiclesOnRoute(final Route route) {
		Link[] links = route.getLinkRoute();
		int ret = 0;
		for (int i = 0; i < links.length; i++) {
			ret += this.numberOfAgents.get(links[i].getId().toString());
		}
		return ret;
	}

	/**
	 * @see org.matsim.withinday.trafficmanagement.ControlInput#init()
	 */

	public void init() {
		Link [] routeLinks;
		routeLinks = this.getAlternativeRoute().getLinkRoute();
		this.firstLinkOnAlternativeRoute = routeLinks[0].getId().toString();
		this.lastLinkOnAlternativeRoute = routeLinks[routeLinks.length-1].getId().toString();
		for (Link l : routeLinks) {
			if (!this.numberOfAgents.containsKey(l.getId().toString()))  {
				this.numberOfAgents.put(l.getId().toString(), Integer.valueOf(0));
			}
			double tt = l.getLength()/l.getFreespeed();
			this.ttFreeSpeeds.put(l.getId().toString(), tt );
			this.ttFreeSpeedAltRoute += tt;
		}
		this.lastTimeAlternativeRoute = this.ttFreeSpeedAltRoute;

//		find the natural bottleneck on the alternative route
		Link[] altRouteLinks = this.getAlternativeRoute().getLinkRoute();
		this.altRouteNaturalBottleNeck = altRouteLinks[0];
		for ( int i = 1; i < altRouteLinks.length; i++ ) {
			if ( altRouteLinks[i].getCapacity() <= this.altRouteNaturalBottleNeck.getCapacity() )
				this.altRouteNaturalBottleNeck = altRouteLinks[i];
		}

		routeLinks = this.getMainRoute().getLinkRoute();
		this.firstLinkOnMainRoute = routeLinks[0].getId().toString();
		this.lastLinkOnMainRoute = routeLinks[routeLinks.length-1].getId().toString();
		double tt;
		for (Link l : routeLinks) {
			if (!this.numberOfAgents.containsKey(l.getId().toString()))  {
				this.numberOfAgents.put(l.getId().toString(), Integer.valueOf(0));
			}
			tt = l.getLength()/l.getFreespeed();
			this.ttFreeSpeeds.put(l.getId().toString(), tt );
			this.ttFreeSpeedMainRoute += tt;
		}
		this.lastTimeMainRoute = this.ttFreeSpeedMainRoute;

//		find the natural bottleneck on the main route
		Link[] mainRouteLinks = this.getMainRoute().getLinkRoute();
		this.mainRouteNaturalBottleNeck = mainRouteLinks[0];
		for ( int i = 1; i < mainRouteLinks.length; i++ ) {
			if ( mainRouteLinks[i].getCapacity() < this.mainRouteNaturalBottleNeck.getCapacity() )
				this.mainRouteNaturalBottleNeck = mainRouteLinks[i];
		}
	}

	// memorize linkEnterEvents on the first links of the two alternative routes:
	public void handleEvent(final EventLinkEnter event) {
		// count the agents on the route links

		if (event.linkId.equals(this.firstLinkOnMainRoute)) {
			this.enterEvents1.put(event.agentId, event.time);
		}
		else if (event.linkId.equals(this.firstLinkOnAlternativeRoute)) {
			this.enterEvents2.put(event.agentId, event.time);
		}

		if (this.numberOfAgents.containsKey(event.linkId)) {
			int number = this.numberOfAgents.get(event.linkId);
			number++;
			this.numberOfAgents.put(event.linkId, Integer.valueOf(number));
		}

	}

	public void handleEvent(final EventLinkLeave event) {
		// decrease current #agents
		if (this.numberOfAgents.containsKey(event.linkId)) {
			int number = this.numberOfAgents.get(event.linkId);
			number--;
			this.numberOfAgents.put(event.linkId, Integer.valueOf(number));
		}


		// if someone leaves one of the last links of the two alternative routes,
		// then
		// - check if that vehicle entered at the beginning
		// - if so, then derive latest measured nashtime input from that
		boolean timeDifferenceHasChanged = false;
		if (event.linkId.equals(this.lastLinkOnMainRoute)) {
			Double t1 = this.enterEvents1.remove(event.agentId);
			if (t1 != null) {
				double deltaT = event.time - t1;
				if(deltaT <= 10*this.lastTimeMainRoute){
					if((SimulationTimer.getTime() >= 55200) && (SimulationTimer.getTime() <= 64000)){
						this.ttMeasuredMainRoute.add(deltaT);
					}
					if (deltaT >= 0) {
						this.lastTimeMainRoute = deltaT;
						timeDifferenceHasChanged = true;
					}

					else {
//						System.err.println("not sure why this could ever happen 2vnowskljdf");
					}
				}
			}
		}
		else if (event.linkId.equals(this.lastLinkOnAlternativeRoute)) {
			Double t1 = this.enterEvents2.remove(event.agentId);
			if (t1 != null) {
				double deltaT = event.time - t1;
				if(deltaT <= 10*this.lastTimeMainRoute){
					if((SimulationTimer.getTime() >= 55200) && (SimulationTimer.getTime() <= 64000)){
						this.ttMeasuredAlternativeRoute.add(deltaT);
					}
					if (deltaT >= 0) {
						this.lastTimeAlternativeRoute = deltaT;
						timeDifferenceHasChanged = true;
					}
					else {
//						System.err.println("not sure why this could ever happen 2vnowfskljdf");
					}
				}
			}
		}

		if ((this.lastTimeMainRoute >= 0.) && (this.lastTimeAlternativeRoute >= 0.)
				&& timeDifferenceHasChanged) {
			this.timeDifference = this.lastTimeMainRoute - this.lastTimeAlternativeRoute;
			if (log.isTraceEnabled()) {
				log.trace("time at main route: " + this.lastTimeMainRoute);
				log.trace("time at alternative route 2: " + this.lastTimeAlternativeRoute);
				log.trace("timeDifference changed: " + this.timeDifference);		}
			}
	}

	public void handleEvent(final EventAgentDeparture event) {
		// increase number of agents on the route links
		if (this.numberOfAgents.containsKey(event.linkId)) {
			int number = this.numberOfAgents.get(event.linkId);
			number++;
			this.numberOfAgents.put(event.linkId, Integer.valueOf(number));
		}
	}

	public void handleEvent(final EventAgentArrival event) {
		// decrease number of agents on the route links
		if (this.numberOfAgents.containsKey(event.linkId)) {
			int number = this.numberOfAgents.get(event.linkId);
			number--;
			this.numberOfAgents.put(event.linkId, Integer.valueOf(number));
		}
	}

	public double getFreeSpeedRouteTravelTime(Route route) {
		if (route == this.mainRoute )
			return this.ttFreeSpeedMainRoute;
		else if ( route == this.alternativeRoute )
			return this.ttFreeSpeedAltRoute;
		else
			throw new IllegalArgumentException(
			"This route object does not exist!");
	}

	public double getMeasuredRouteTravelTime(final Route route) {
		if (route == this.mainRoute )
			return this.lastTimeMainRoute;
		else if ( route == this.alternativeRoute )
			return this.lastTimeAlternativeRoute;
		else
			throw new IllegalArgumentException(
			"This route object does not exist!");
	}

	public Link getNaturalBottleNeck(final Route r){
		Link naturalBottleNeck;
		if(r == this.mainRoute){
			naturalBottleNeck = this.mainRouteNaturalBottleNeck;
		}
		else{
			naturalBottleNeck = this.altRouteNaturalBottleNeck;
		}
		return naturalBottleNeck;
	}

}
