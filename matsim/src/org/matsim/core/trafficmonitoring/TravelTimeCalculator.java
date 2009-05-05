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

public class TravelTimeCalculator extends 
AbstractTravelTimeCalculator implements LinkEnterEventHandler, LinkLeaveEventHandler, 
AgentArrivalEventHandler, AgentStuckEventHandler {

	// EnterEvent implements Comparable based on linkId and vehId. This means that the key-pair <linkId, vehId> must always be unique!
	private final HashMap<Id, LinkEnterEvent> enterEvents = new HashMap<Id, LinkEnterEvent>();
	private final HashMap<Link, DataContainer> linkData;

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
		super(network, timeslice, maxTime, factory);
		this.linkData = new HashMap<Link, DataContainer>((int) (this.getNetwork().getLinks().size() * 1.4));
	
		resetTravelTimes();
	}

	public void resetTravelTimes() {
		for (DataContainer data : this.linkData.values()) {
			data.ttData.resetTravelTimes();
			data.needsConsolidation = false;
		}
		this.enterEvents.clear();
	}

	public void reset(final int iteration) {
		/* DO NOT CALL resetTravelTimes here!
		 * reset(iteration) is called at the beginning of an iteration, but we still
		 * need the travel times from the last iteration for the replanning!
		 * That's why there is a separate method resetTravelTimes() which can
		 * be called after the replanning.      -marcel/20jan2008
		 */
	}

	public void handleEvent(final LinkEnterEvent event) {
		this.enterEvents.put(event.getPersonId(), event);
	}

	public void handleEvent(final LinkLeaveEvent event) {
		LinkEnterEvent e = this.enterEvents.remove(event.getPersonId());
		if ((e != null) && e.getLinkId().equals(event.getLinkId())) {
			if (event.getLink() == null) event.setLink(this.getNetwork().getLink(event.getLinkId()));
			if (event.getLink() != null) {
				DataContainer data = getTravelTimeData(event.getLink(), true);
				data.needsConsolidation = true;
				this.getTravelTimeAggregator().addTravelTime(data.ttData, e.getTime(), event.getTime());
			}
		}
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

	public void handleEvent(final AgentArrivalEvent event) {
		// remove EnterEvents from list when an agent arrives.
		// otherwise, the activity duration would counted as travel time, when the
		// agent departs again and leaves the link!
		this.enterEvents.remove(event.getPersonId());
	}

	public void handleEvent(AgentStuckEvent event) {
		LinkEnterEvent e = this.enterEvents.remove(event.getPersonId());
		if ((e != null) && e.getLinkId().equals(event.getLinkId())) {
			Link link = event.getLink();
			if (link == null) {
				link = this.getNetwork().getLink(event.getLinkId());
			}
			if (link != null) {
				DataContainer data = getTravelTimeData(link, true);
				data.needsConsolidation = true;
				this.getTravelTimeAggregator().addStuckEventTravelTime(data.ttData, e.getTime(), event.getTime());
			}
		}
	}

	private DataContainer getTravelTimeData(final Link link, final boolean createIfMissing) {
		DataContainer data = this.linkData.get(link);
		if ((null == data) && createIfMissing) {
			data = new DataContainer(this.getTravelTimeAggregatorFactory().createTravelTimeData(link, this.getNumSlots()));
			this.linkData.put(link, data);
		}
		return data;
	}

	public double getLinkTravelTime(final Link link, final double time) {
		DataContainer data = getTravelTimeData(link, true);
		if (data.needsConsolidation) {
			consolidateData(data);
		}
		return this.getTravelTimeAggregator().getTravelTime(data.ttData, time); 
	}
	
	private static class DataContainer {
		/*package*/ final TravelTimeData ttData;
		/*package*/ boolean needsConsolidation = false;
		
		/*package*/ DataContainer(final TravelTimeData data) {
			this.ttData = data;
		}
	}
}
