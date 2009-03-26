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

package org.matsim.core.trafficmonitoring;

import java.util.HashMap;

import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentStuckEvent;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.AgentStuckEventHandler;
import org.matsim.core.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.handler.LinkLeaveEventHandler;
import org.matsim.core.router.util.TravelTime;

public class TravelTimeCalculator implements TravelTime, LinkEnterEventHandler, LinkLeaveEventHandler, 
AgentArrivalEventHandler, AgentStuckEventHandler {

	// EnterEvent implements Comparable based on linkId and vehId. This means that the key-pair <linkId, vehId> must always be unique!
	private final HashMap<String, EnterEvent> enterEvents = new HashMap<String, EnterEvent>();
	private Network network = null;
	private final HashMap<Link, TravelTimeData> linkData;
	private final int timeslice;
	private final int numSlots;
	private final TravelTimeAggregatorFactory factory;
	private final AbstractTravelTimeAggregator aggregator;



	public TravelTimeCalculator(final Network network) {
		this(network, 15*60, 30*3600);	// default timeslot-duration: 15 minutes
	}

	public TravelTimeCalculator(final Network network, final int timeslice) {
		this(network, timeslice, 30*3600); // default: 30 hours at most
	}

	public TravelTimeCalculator(Network network, int timeslice,	int maxTime) {
		this(network, timeslice, maxTime, new TravelTimeAggregatorFactory());
	}

	public TravelTimeCalculator(final Network network, final int timeslice, final int maxTime, TravelTimeAggregatorFactory factory) {
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
			TravelTimeData r = getTravelTimeData(link, false);
			if (r != null) {
				r.resetTravelTimes();
			}
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

	public void handleEvent(final LinkEnterEvent event) {
		EnterEvent e = new EnterEvent(event.getLinkId().toString(), event.getTime());
		this.enterEvents.put(event.getPersonId().toString(), e);
	}

	public void handleEvent(final LinkLeaveEvent event) {
		EnterEvent e = this.enterEvents.remove(event.getPersonId().toString());
		if ((e != null) && e.linkId.equals(event.getLinkId().toString())) {
			if (event.getLink() == null) event.setLink(this.network.getLink(new IdImpl(event.getLinkId().toString())));
			if (event.getLink() != null) {
				this.aggregator.addTravelTime(getTravelTimeData(event.getLink(), true),e.time,event.getTime());
			}
		}
	}

	public void handleEvent(final AgentArrivalEvent event) {
		// remove EnterEvents from list when an agent arrives.
		// otherwise, the activity duration would counted as travel time, when the
		// agent departs again and leaves the link!
		this.enterEvents.remove(event.getPersonId().toString());
	}

	public void handleEvent(AgentStuckEvent event) {
		EnterEvent e = this.enterEvents.remove(event.getPersonId().toString());
		if ((e != null) && e.linkId.equals(event.getLinkId().toString())) {
			Link link = event.getLink();
			if (link == null) {
				link = this.network.getLink(new IdImpl(event.getLinkId().toString()));
			}
			if (link != null) {
				this.aggregator.addStuckEventTravelTime(getTravelTimeData(link, true),e.time,event.getTime());
			}
		}
	}

	private TravelTimeData getTravelTimeData(final Link link, final boolean createIfMissing) {
		TravelTimeData r = this.linkData.get(link);
		if ((null == r) && createIfMissing) {
			r = this.factory.createTravelTimeData(link, this.numSlots);
			this.linkData.put(link, r);
		}
		return r;
	}

	public double getLinkTravelTime(final Link link, final double time) {
		return this.aggregator.getTravelTime(getTravelTimeData(link, true), time); 
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
