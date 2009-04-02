/* *********************************************************************** *
 * project: org.matsim.*
 * LinkToLinkTravelTimeCalculator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentStuckEvent;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.AgentStuckEventHandler;
import org.matsim.core.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.handler.LinkLeaveEventHandler;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.utils.collections.Tuple;


/**
 * @author dgrether
 *
 */
public class LinkToLinkTravelTimeCalculator extends AbstractTravelTimeCalculator
		implements LinkToLinkTravelTime, LinkEnterEventHandler, LinkLeaveEventHandler, 
		AgentArrivalEventHandler, AgentStuckEventHandler {
	
	private static final Logger log = Logger
			.getLogger(LinkToLinkTravelTimeCalculator.class);
	
	private HashMap<Link, TravelTimeData> linkData;
	
	private HashMap<Tuple<Link, Link>, TravelTimeData> linkToLinkData;
	
	private HashMap<Id, LinkEnterEvent> linkEnterEvents;

	private final boolean calculateLinkTravelTimes;
	
	
	public LinkToLinkTravelTimeCalculator(final Network network, final int timeslice, final int maxTime, TravelTimeAggregatorFactory factory, boolean calcLinkTravelTimes) {
		super(network, timeslice, maxTime, factory);
		this.calculateLinkTravelTimes = calcLinkTravelTimes;
		if (this.calculateLinkTravelTimes){
			this.linkData = new HashMap<Link, TravelTimeData>((int) (network.getLinks().size() * 1.4));
		}
		//assume that every link has 2 outgoing links 
		this.linkToLinkData = new HashMap<Tuple<Link, Link>, TravelTimeData>((int) (network.getLinks().size() * 1.4 * 2));
		this.linkEnterEvents = new HashMap<Id, LinkEnterEvent>();
		
		this.resetTravelTimes();
	}
	
	@Override
	public void resetTravelTimes(){
		if (this.calculateLinkTravelTimes) {
			for (TravelTimeData data : this.linkData.values()){
				data.resetTravelTimes();
			}
		}
		for (TravelTimeData data : this.linkToLinkData.values()){
			data.resetTravelTimes();
		}
		this.linkEnterEvents.clear();
	}
		
	
	public void handleEvent(final LinkEnterEvent e) {
		if (e.getLink() == null) {
			throw new IllegalArgumentException("This class only works with LinkEvents where the full reference to the link is set!");
		}
		if (this.linkEnterEvents.containsKey(e.getPerson().getId())){
			LinkEnterEvent oldEvent = this.linkEnterEvents.remove(e.getPerson().getId());
			Tuple<Link, Link> fromToLink = new Tuple<Link, Link>(oldEvent.getLink(), e.getLink());
			TravelTimeData timeData = getLinkToLinkTravelTimeData(fromToLink, true);
			this.getTravelTimeAggregator().addTravelTime(timeData, oldEvent.getTime(), e.getTime());
		}
		this.linkEnterEvents.put(e.getPerson().getId(), e);
	}

	public void handleEvent(final LinkLeaveEvent e) {
		if (e.getLink() == null) {
			throw new IllegalArgumentException("This class only works with LinkEvents where the full reference to the link is set!");
		}
		if (this.calculateLinkTravelTimes) {
  		if (this.linkEnterEvents.containsKey(e.getPerson().getId())){
  			LinkEnterEvent oldEvent = this.linkEnterEvents.get(e.getPerson().getId());
  			TravelTimeData timeData = getTravelTimeData(e.getLink(), true);
  			this.getTravelTimeAggregator().addTravelTime(timeData, oldEvent.getTime(), e.getTime());
  		}
		}
	}

	public void handleEvent(final AgentArrivalEvent event) {
		// remove EnterEvents from list when an agent arrives.
		// otherwise, the activity duration would counted as travel time, when the
		// agent departs again and leaves the link!
		this.linkEnterEvents.remove(event.getPerson().getId());
	}

	//TODO check stuck error
	public void handleEvent(AgentStuckEvent event) {
		LinkEnterEvent e = this.linkEnterEvents.remove(event.getPerson().getId());
		if (e != null) {
			Link link = event.getLink();
			if (link == null) {
				throw new IllegalArgumentException("This class only works with LinkEvents where the full reference to the link is set!");
			}
			this.getTravelTimeAggregator().addStuckEventTravelTime(getTravelTimeData(link, true),e.getTime(), event.getTime());
			log.error("Using the stuck feature with turning move travel times is discouraged. As the next link of a stucked" +
			"agent is not known the turning move travel time cannot be calculated!");
		}
	}
	
	private TravelTimeData getLinkToLinkTravelTimeData(Tuple<Link, Link> fromLinkToLink, final boolean createIfMissing) {
		TravelTimeData data = this.linkToLinkData.get(fromLinkToLink);
		if ((null == data) && createIfMissing) {
			data = this.getTravelTimeAggregatorFactory().createTravelTimeData(fromLinkToLink.getFirst(), this.getNumSlots());
			this.linkToLinkData.put(fromLinkToLink, data);
		}
		return data;
	}

	private TravelTimeData getTravelTimeData(final Link link, final boolean createIfMissing) {
		TravelTimeData r = this.linkData.get(link);
		if ((null == r) && createIfMissing) {
			r = this.getTravelTimeAggregatorFactory().createTravelTimeData(link, this.getNumSlots());
			this.linkData.put(link, r);
		}
		return r;
	}
	
	
	@Override
	public double getLinkTravelTime(final Link link, final double time) {
		if (this.calculateLinkTravelTimes) {
			return this.getTravelTimeAggregator().getTravelTime(getTravelTimeData(link, true), time); 
		}
		throw new IllegalStateException("No link travel time is available " +
				"if calculation is switched off by config option!");
	}
	
	/**
	 * @see org.matsim.core.router.util.LinkToLinkTravelTime#getLinkToLinkTravelTime(org.matsim.core.api.network.Link, org.matsim.core.api.network.Link, double)
	 */
	public double getLinkToLinkTravelTime(Link fromLink, Link toLink, double time) {
		return this.getTravelTimeAggregator().getTravelTime(this.getLinkToLinkTravelTimeData(new Tuple<Link, Link>(fromLink, toLink), true), time);
	}

	public void reset(int iteration) {
		/* DO NOT CALL resetTravelTimes here!
		 * reset(iteration) is called at the beginning of an iteration, but we still
		 * need the travel times from the last iteration for the replanning!
		 * That's why there is a separat method resetTravelTimes() which can
		 * be called after the replanning.      -marcel/20jan2008
		 */
	}
}
