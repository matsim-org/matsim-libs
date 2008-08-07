/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCalculator.java
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

package org.matsim.trafficmonitoring;

import java.util.HashMap;

import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.LinkEnterEnter;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.router.util.TravelTimeI;

public class TravelTimeCalculator implements TravelTimeI, LinkEnterEventHandler, LinkLeaveEventHandler, 
AgentArrivalEventHandler, AgentStuckEventHandler {

	// EnterEvent implements Comparable based on linkId and vehId. This means that the key-pair <linkId, vehId> must always be unique!
	private final HashMap<String, EnterEvent> enterEvents = new HashMap<String, EnterEvent>();
	private NetworkLayer network = null;
	final int roleIndex;
	private final int timeslice;
	private final int numSlots;
	private final TravelTimeCalculatorFactory factory;
	private final AbstractTravelTimeAggregator aggregator;

	
	
	public TravelTimeCalculator(final NetworkLayer network) {
		this(network, 15*60, 30*3600);	// default timeslot-duration: 15 minutes
	}

	public TravelTimeCalculator(final NetworkLayer network, final int timeslice) {
		this(network, timeslice, 30*3600); // default: 30 hours at most
	}

	public TravelTimeCalculator(NetworkLayer network, int timeslice,	int maxTime) {
		this(network, timeslice, maxTime, new TravelTimeCalculatorFactory());
	}
	
	public TravelTimeCalculator(final NetworkLayer network, final int timeslice, final int maxTime, TravelTimeCalculatorFactory factory) {
		this.factory = factory;
		this.network = network;
		this.timeslice = timeslice;
		this.numSlots = (maxTime / this.timeslice) + 1;
		this.roleIndex = network.requestLinkRole();
		this.aggregator = this.factory.createTravelTimeAggregator(this.numSlots, this.timeslice);
		
		resetTravelTimes();
	}

	
	


	public void resetTravelTimes() {
		for (Link link : this.network.getLinks().values()) {
			TravelTimeRole r = getTravelTimeRole(link);
			r.resetTravelTimes();
		}
		this.enterEvents.clear();
	}

	public void reset(final int iteration) {
		/* DO NOT CALL resetTravelTimes here!
		 * reset(iteration) is called at the beginning of an iteration, but we still
		 * need the travel times from the last iteration for the replanning!
		 * That's why there is a separat method resetTravelTimes() which can
		 * be called after the replanning.      -marcel/20jan2008
		 */
	}

	//////////////////////////////////////////////////////////////////////
	// Implementation of EventAlgorithmI
	//////////////////////////////////////////////////////////////////////

	public void handleEvent(final LinkEnterEnter event) {
		EnterEvent e = new EnterEvent(event.linkId, event.time);
		this.enterEvents.put(event.agentId, e);
	}

	public void handleEvent(final LinkLeaveEvent event) {
		EnterEvent e = this.enterEvents.remove(event.agentId);
		if ((e != null) && e.linkId.equals(event.linkId)) {
			if (event.link == null) event.link = (Link)this.network.getLocation(event.linkId);
			if (event.link != null) {
				this.aggregator.addTravelTime(getTravelTimeRole(event.link),e.time,event.time);

			}
		}
	}

	public void handleEvent(final AgentArrivalEvent event) {
		// remove EnterEvents from list when an agent arrives.
		// otherwise, the activity duration would counted as travel time, when the
		// agent departs again and leaves the link!
		this.enterEvents.remove(event.agentId);
	}

	public void handleEvent(AgentStuckEvent event) {
		EnterEvent e = this.enterEvents.remove(event.agentId);
		if ((e != null) && e.linkId.equals(event.linkId)) {
			if (event.link == null) event.link = (Link)this.network.getLocation(event.linkId);
			if (event.link != null) {
				this.aggregator.addStuckEventTravelTime(getTravelTimeRole(event.link),e.time,event.time);
			}
		}
	}
	
	private TravelTimeRole getTravelTimeRole(final Link link) {
		TravelTimeRole r = (TravelTimeRole) link.getRole(this.roleIndex);
		if (null == r) {
			r = this.factory.createTravelTimeRole(link, this.numSlots);
			link.setRole(this.roleIndex, r);
		}
		return r;
	}


	//////////////////////////////////////////////////////////////////////
	// Implementation of TravelTimeI
	//////////////////////////////////////////////////////////////////////

	/* (non-Javadoc)
	 * @see org.matsim.network.TravelCostI#getLinkTravelTime(org.matsim.network.Link, int)
	 */
	public double getLinkTravelTime(final Link link, final double time) {
		return this.aggregator.getTravelTime(getTravelTimeRole(link), time); 
	}


	static private class EnterEvent {

		public final String linkId;
		public final double time;

		public EnterEvent(final String linkId, final double time) {
			this.linkId = linkId;
			this.time = time;
		}

	};
}
