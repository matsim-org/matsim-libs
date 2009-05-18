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
public class TravelTimeCalculator extends AbstractTravelTimeCalculator
		implements LinkToLinkTravelTime, LinkEnterEventHandler, LinkLeaveEventHandler, 
		AgentArrivalEventHandler, AgentStuckEventHandler {
	
	private final static String WARNING_NEED_LINK_REFERENCE = "This class only works with LinkEvents where the full reference to the link is set!";
	
	public static final String ERROR_STUCK_AND_LINKTOLINK = "Using the stuck feature with turning move travel times is not available. As the next link of a stucked" +
				"agent is not known the turning move travel time cannot be calculated!";
	
	private static final Logger log = Logger.getLogger(TravelTimeCalculator.class);
	
	private HashMap<Link, DataContainer> linkData;
	
	private HashMap<Tuple<Link, Link>, DataContainer> linkToLinkData;
	
	private HashMap<Id, LinkEnterEvent> linkEnterEvents;

	private final boolean calculateLinkTravelTimes;

	private boolean calculateLinkToLinkTravelTimes;
	
	public TravelTimeCalculator(final Network network, TravelTimeCalculatorConfigGroup ttconfigGroup) {
		this(network, 15*60, 30*3600, ttconfigGroup);	// default timeslot-duration: 15 minutes
	}

	public TravelTimeCalculator(final Network network, final int timeslice, TravelTimeCalculatorConfigGroup ttconfigGroup) {
		this(network, timeslice, 30*3600, ttconfigGroup); // default: 30 hours at most
	}

	public TravelTimeCalculator(Network network, int timeslice,	int maxTime, TravelTimeCalculatorConfigGroup ttconfigGroup) {
		this(network, timeslice, maxTime, new TravelTimeAggregatorFactory(), ttconfigGroup);
	}

	
	public TravelTimeCalculator(final Network network, final int timeslice, final int maxTime,
			TravelTimeAggregatorFactory factory, TravelTimeCalculatorConfigGroup ttconfigGroup) {
		super(network, timeslice, maxTime, factory);
		this.calculateLinkTravelTimes = ttconfigGroup.isCalculateLinkTravelTimes();
		this.calculateLinkToLinkTravelTimes = ttconfigGroup.isCalculateLinkToLinkTravelTimes();
		if (this.calculateLinkTravelTimes){
			this.linkData = new HashMap<Link, DataContainer>((int) (network.getLinks().size() * 1.4));
		}
		if (this.calculateLinkToLinkTravelTimes){
			// assume that every link has 2 outgoing links 
			this.linkToLinkData = new HashMap<Tuple<Link, Link>, DataContainer>((int) (network.getLinks().size() * 1.4 * 2));
		}
		this.linkEnterEvents = new HashMap<Id, LinkEnterEvent>();
		
		this.resetTravelTimes();
	}
	
	@Override
	public void resetTravelTimes(){
		if (this.calculateLinkTravelTimes) {
			for (DataContainer data : this.linkData.values()){
				data.ttData.resetTravelTimes();
			}
		}
		if (this.calculateLinkToLinkTravelTimes){
			for (DataContainer data : this.linkToLinkData.values()){
				data.ttData.resetTravelTimes();
				data.needsConsolidation = false;
			}
		}
		this.linkEnterEvents.clear();
	}

	public void handleEvent(final LinkEnterEvent e) {
		if (e.getLink() == null) {
			throw new IllegalArgumentException(WARNING_NEED_LINK_REFERENCE);
		}
		LinkEnterEvent oldEvent = this.linkEnterEvents.remove(e.getPersonId());
		if ((oldEvent != null) && this.calculateLinkToLinkTravelTimes) {
			Tuple<Link, Link> fromToLink = new Tuple<Link, Link>(oldEvent.getLink(), e.getLink());
			DataContainer data = getLinkToLinkTravelTimeData(fromToLink, true);
			this.getTravelTimeAggregator().addTravelTime(data.ttData, oldEvent.getTime(), e.getTime());
			data.needsConsolidation = true;
		}
		this.linkEnterEvents.put(e.getPersonId(), e);
	}

	public void handleEvent(final LinkLeaveEvent e) {
		if (e.getLink() == null) {
			throw new IllegalArgumentException(WARNING_NEED_LINK_REFERENCE);
		}
		if (this.calculateLinkTravelTimes) {
			LinkEnterEvent oldEvent = this.linkEnterEvents.get(e.getPersonId());
  		if (oldEvent != null) {
  			DataContainer data = getTravelTimeData(e.getLink(), true);
  			this.getTravelTimeAggregator().addTravelTime(data.ttData, oldEvent.getTime(), e.getTime());
  			data.needsConsolidation = true;
  		}
		}
	}

	public void handleEvent(final AgentArrivalEvent event) {
		// remove EnterEvents from list when an agent arrives.
		// otherwise, the activity duration would counted as travel time, when the
		// agent departs again and leaves the link!
		this.linkEnterEvents.remove(event.getPersonId());
	}

	public void handleEvent(AgentStuckEvent event) {
		LinkEnterEvent e = this.linkEnterEvents.remove(event.getPersonId());
		if (e != null) {
			Link link = event.getLink();
			if (link == null) {
				throw new IllegalArgumentException(WARNING_NEED_LINK_REFERENCE);
			}
			DataContainer data = getTravelTimeData(link, true);
			data.needsConsolidation = true;
			this.getTravelTimeAggregator().addStuckEventTravelTime(data.ttData, e.getTime(), event.getTime());
			if (this.calculateLinkToLinkTravelTimes){
				log.error(ERROR_STUCK_AND_LINKTOLINK);
				throw new IllegalStateException(ERROR_STUCK_AND_LINKTOLINK);
			}
		}
	}
	
	private DataContainer getLinkToLinkTravelTimeData(Tuple<Link, Link> fromLinkToLink, final boolean createIfMissing) {
		DataContainer data = this.linkToLinkData.get(fromLinkToLink);
		if ((null == data) && createIfMissing) {
			data = new DataContainer(this.getTravelTimeAggregatorFactory().createTravelTimeData(fromLinkToLink.getFirst(), this.getNumSlots()));
			this.linkToLinkData.put(fromLinkToLink, data);
		}
		return data;
	}

	private DataContainer getTravelTimeData(final Link link, final boolean createIfMissing) {
		DataContainer data = this.linkData.get(link);
		if ((null == data) && createIfMissing) {
			data = new DataContainer(this.getTravelTimeAggregatorFactory().createTravelTimeData(link, this.getNumSlots()));
			this.linkData.put(link, data);
		}
		return data;
	}

	@Override
	public double getLinkTravelTime(final Link link, final double time) {
		if (this.calculateLinkTravelTimes) {
			DataContainer data = getTravelTimeData(link, true);
			if (data.needsConsolidation) {
				consolidateData(data);
			}
			return this.getTravelTimeAggregator().getTravelTime(data.ttData, time); 
		}
		throw new IllegalStateException("No link travel time is available " +
				"if calculation is switched off by config option!");
	}
	
	public double getLinkToLinkTravelTime(Link fromLink, Link toLink, double time) {
		if (!this.calculateLinkToLinkTravelTimes) {
			throw new IllegalStateException("No link to link travel time is available " +
			"if calculation is switched off by config option!");			
		}
		DataContainer data = this.getLinkToLinkTravelTimeData(new Tuple<Link, Link>(fromLink, toLink), true);
		if (data.needsConsolidation) {
			consolidateData(data);
		}
		return this.getTravelTimeAggregator().getTravelTime(data.ttData, time);
	}

	public void reset(int iteration) {
		/* DO NOT CALL resetTravelTimes here!
		 * reset(iteration) is called at the beginning of an iteration, but we still
		 * need the travel times from the last iteration for the replanning!
		 * That's why there is a separat method resetTravelTimes() which can
		 * be called after the replanning.      -marcel/20jan2008
		 */
	}
	
	/**
	 * Makes sure that the travel times returned "make sense".
	 * 
	 * Image short bin sizes (e.g. 5min), small links (e.g. 300 veh/hour)
	 * and small sample sizes (e.g. 2%). This would mean, that effectively
	 * in the simulation only 6 vehicles can pass the link in one hour,
	 * every 10min one. So, the travel time in one time slot could be 
	 * >= 10min if two cars enter the link at the same time. If no car
	 * enters in the next time bin, the travel time in that time bin should
	 * still be >=5 minutes (10min - binSize), and not freespeedTraveltime,
	 * because actually every car entering the link in this bin will be behind
	 * the car entered before, which still needs >=5min until it can leave.
	 * This method ensures exactly that, that the travel time in a time bin
	 * cannot be smaller than the travel time in the bin before minus the
	 * bin size.
	 * 
	 * @param data
	 */
	private void consolidateData(final DataContainer data) {
		synchronized(data) {
			if (data.needsConsolidation) {
				TravelTimeData r = data.ttData;
				double prevTravelTime = r.getTravelTime(1, 0.0);
				for (int i = 1; i < this.numSlots; i++) {
					double time = r.getTravelTime(i, i * this.timeslice);
					double minTime = prevTravelTime - this.timeslice;
					if (time < minTime) {
						r.addTravelTime(i, minTime);
						prevTravelTime = minTime;
					} else {
						prevTravelTime = time;
					}
				}
				data.needsConsolidation = false;
			}
		}
	}

	private static class DataContainer {
		/*package*/ final TravelTimeData ttData;
		/*package*/ volatile boolean needsConsolidation = false;
		
		/*package*/ DataContainer(final TravelTimeData data) {
			this.ttData = data;
		}
	}
}
