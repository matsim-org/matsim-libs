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
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.router.util.TravelTime;

public class TravelTimeCalculator implements TravelTime, LinkEnterEventHandler, LinkLeaveEventHandler, 
AgentArrivalEventHandler, AgentStuckEventHandler {

	// EnterEvent implements Comparable based on linkId and vehId. This means that the key-pair <linkId, vehId> must always be unique!
	private final HashMap<String, EnterEvent> enterEvents = new HashMap<String, EnterEvent>();
	private NetworkLayer network = null;
	private final HashMap<Link, TravelTimeData> linkData;
	private final int timeslice;
	private final int numSlots;
	private final TravelTimeAggregatorFactory factory;
	private final AbstractTravelTimeAggregator aggregator;

	
	
	public TravelTimeCalculator(final NetworkLayer network) {
		this(network, 15*60, 30*3600);	// default timeslot-duration: 15 minutes
	}

	public TravelTimeCalculator(final NetworkLayer network, final int timeslice) {
		this(network, timeslice, 30*3600); // default: 30 hours at most
	}

	public TravelTimeCalculator(NetworkLayer network, int timeslice,	int maxTime) {
		this(network, timeslice, maxTime, new TravelTimeAggregatorFactory());
	}
	
	public TravelTimeCalculator(final NetworkLayer network, final int timeslice, final int maxTime, TravelTimeAggregatorFactory factory) {
		this.factory = factory;
		this.network = network;
		this.timeslice = timeslice;
		this.numSlots = (maxTime / this.timeslice) + 1;
		this.linkData = new HashMap<Link, TravelTimeData>((int) (this.network.getLinks().size() * 1.4));
		this.aggregator = this.factory.createTravelTimeAggregator(this.numSlots, this.timeslice);
		
		resetTravelTimes();
	}

	
	


	public void resetTravelTimes() {
		for (Link link : this.network.getLinks().values()) {
			TravelTimeData r = getTravelTimeRole(link);
			r.resetTravelTimes();
			/*
			 * 	lazy initialization of travel time data would be:
			 *	if (r != null) {
			 *		r.resetTravelTimes();
			 *	}
			 *	(konrad/25feb2009)
			 */
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

	public void handleEvent(final LinkEnterEvent event) {
		EnterEvent e = new EnterEvent(event.linkId, event.time);
		this.enterEvents.put(event.agentId, e);
	}

	public void handleEvent(final LinkLeaveEvent event) {
		EnterEvent e = this.enterEvents.remove(event.agentId);
		if ((e != null) && e.linkId.equals(event.linkId)) {
			if (event.link == null) event.link = this.network.getLink(event.linkId);
			if (event.link != null) {
				this.aggregator.addTravelTime(getTravelTimeRole(event.link),e.time,event.time);
				/*
				* 	lazy initialization of travel time data would be:
				* 	TravelTimeRole r = getTravelTimeRole(event.link);
				* 	if (r == null) {
				*		r = this.factory.createTravelTimeRole(event.link, this.numSlots);
				*		this.linkData.put(event.link, r);
				*	}
				*	this.aggregator.addTravelTime(r,e.time,event.time);
				*	(konrad/25feb2009)
				*/

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
			if (event.link == null) event.link = this.network.getLink(event.linkId);
			if (event.link != null) {
				this.aggregator.addStuckEventTravelTime(getTravelTimeRole(event.link),e.time,event.time);
				/*
				* 	lazy initialization of travel time data would be:
				* 	TravelTimeRole r = getTravelTimeRole(event.link);
				* 	if (r == null) {
				*		r = this.factory.createTravelTimeRole(event.link, this.numSlots);
				*		this.linkData.put(event.link, r);
				*	}
				*	this.aggregator.addStuckEventTravelTime(r,e.time,event.time);
				*	(konrad/25feb2009)
				*/
			}
		}
	}
	
	private TravelTimeData getTravelTimeRole(final Link link) {
		TravelTimeData r = this.linkData.get(link);
		if (null == r) {
			r = this.factory.createTravelTimeRole(link, this.numSlots);
			this.linkData.put(link, r);
		}
		return r;
		/*
		 * lazy initialization of travel time data would be:
		 * return this.linkData.get(link);
		 *	(konrad/25feb2009)
		 */
		
	}

	public double getLinkTravelTime(final Link link, final double time) {
		return this.aggregator.getTravelTime(getTravelTimeRole(link), time); 
		/*
		 * lazy initialization of travel time data would be:
		 * 		TravelTimeRole r = getTravelTimeRole(link);
		 * 		if (r == null) {
		 *		r = this.factory.createTravelTimeRole(link, this.numSlots);
		 *			this.linkData.put(link, r);
		 *		}
		 *		return this.aggregator.getTravelTime(r, time); 
		 *	(konrad/25feb2009)
 		 */
	}

	private static class EnterEvent {

		public final String linkId;
		public final double time;

		public EnterEvent(final String linkId, final double time) {
			this.linkId = linkId;
			this.time = time;
		}

	}
}
