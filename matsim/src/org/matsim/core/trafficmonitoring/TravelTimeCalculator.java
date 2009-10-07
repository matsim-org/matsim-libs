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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicAgentArrivalEvent;
import org.matsim.api.basic.v01.events.BasicAgentStuckEvent;
import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.BasicLinkLeaveEvent;
import org.matsim.api.basic.v01.events.handler.BasicAgentArrivalEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentStuckEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkEnterEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;


/**
 * Calculates actual travel times on link from events and optionally also the link-to-link 
 * travel times, e.g. if signaled nodes are used and thus turns in different directions
 * at a node may take a different amount of time.
 * <br>
 * Travel times on links are collected and averaged in bins/slots with a specified size
 * (<code>binSize</code>, in seconds, default 900 seconds = 15 minutes). The data for the travel times per link
 * is stored in {@link TravelTimeData}-objects. If a short binSize is used, it is useful to
 * use {@link TravelTimeDataHashMap} (see {@link #setTravelTimeDataFactory(TravelTimeDataFactory)}
 * as that one does not use any memory to time bins where no traffic occurred. By default,
 * {@link TravelTimeDataArray} is used.
 * 
 * 
 * @author dgrether
 * @author mrieser
 */
public class TravelTimeCalculator
		implements TravelTime, LinkToLinkTravelTime, BasicLinkEnterEventHandler, BasicLinkLeaveEventHandler, 
		BasicAgentArrivalEventHandler, BasicAgentStuckEventHandler {
	
	private static final String ERROR_STUCK_AND_LINKTOLINK = "Using the stuck feature with turning move travel times is not available. As the next link of a stucked" +
	"agent is not known the turning move travel time cannot be calculated!";
	
	/*package*/ final int timeslice;
	/*package*/ final int numSlots;
	private AbstractTravelTimeAggregator aggregator;
	
	private static final Logger log = Logger.getLogger(TravelTimeCalculator.class);
	
	private Map<Id, DataContainer> linkData;
	
	private Map<Tuple<Id, Id>, DataContainer> linkToLinkData;
	
	private final Map<Id, BasicLinkEnterEvent> linkEnterEvents;

	private final boolean calculateLinkTravelTimes;

	private final boolean calculateLinkToLinkTravelTimes;
	
	private TravelTimeDataFactory ttDataFactory = null; 
	
	public TravelTimeCalculator(final Network network, TravelTimeCalculatorConfigGroup ttconfigGroup) {
		this(network, ttconfigGroup.getTraveltimeBinSize(), 30*3600, ttconfigGroup); // default: 30 hours at most
	}

	public TravelTimeCalculator(final Network network, final int timeslice, final int maxTime,
			TravelTimeCalculatorConfigGroup ttconfigGroup) {
		this.timeslice = timeslice;
		this.numSlots = (maxTime / this.timeslice) + 1;
		this.aggregator = new OptimisticTravelTimeAggregator(this.numSlots, this.timeslice);
		this.ttDataFactory = new TravelTimeDataArrayFactory(network, this.numSlots);
		this.calculateLinkTravelTimes = ttconfigGroup.isCalculateLinkTravelTimes();
		this.calculateLinkToLinkTravelTimes = ttconfigGroup.isCalculateLinkToLinkTravelTimes();
		if (this.calculateLinkTravelTimes){
			this.linkData = new ConcurrentHashMap<Id, DataContainer>((int) (network.getLinks().size() * 1.4));
		}
		if (this.calculateLinkToLinkTravelTimes){
			// assume that every link has 2 outgoing links as default
			this.linkToLinkData = new ConcurrentHashMap<Tuple<Id, Id>, DataContainer>((int) (network.getLinks().size() * 1.4 * 2));
		}
		this.linkEnterEvents = new ConcurrentHashMap<Id, BasicLinkEnterEvent>();
		
		this.reset(0);
	}

	public void handleEvent(final BasicLinkEnterEvent e) {
		BasicLinkEnterEvent oldEvent = this.linkEnterEvents.remove(e.getPersonId());
		if ((oldEvent != null) && this.calculateLinkToLinkTravelTimes) {
			Tuple<Id, Id> fromToLink = new Tuple<Id, Id>(oldEvent.getLinkId(), e.getLinkId());
			DataContainer data = getLinkToLinkTravelTimeData(fromToLink, true);
			this.aggregator.addTravelTime(data.ttData, oldEvent.getTime(), e.getTime());
			data.needsConsolidation = true;
		}
		this.linkEnterEvents.put(e.getPersonId(), e);
	}

	public void handleEvent(final BasicLinkLeaveEvent e) {
		if (this.calculateLinkTravelTimes) {
			BasicLinkEnterEvent oldEvent = this.linkEnterEvents.get(e.getPersonId());
  		if (oldEvent != null) {
  			DataContainer data = getTravelTimeData(e.getLinkId(), true);
  			this.aggregator.addTravelTime(data.ttData, oldEvent.getTime(), e.getTime());
  			data.needsConsolidation = true;
  		}
		}
	}

	public void handleEvent(final BasicAgentArrivalEvent event) {
		/* remove EnterEvents from list when an agent arrives.
		 * otherwise, the activity duration would counted as travel time, when the
		 * agent departs again and leaves the link! */
		this.linkEnterEvents.remove(event.getPersonId());
	}

	public void handleEvent(BasicAgentStuckEvent event) {
		BasicLinkEnterEvent e = this.linkEnterEvents.remove(event.getPersonId());
		if (e != null) {
			DataContainer data = getTravelTimeData(e.getLinkId(), true);
			data.needsConsolidation = true;
			this.aggregator.addStuckEventTravelTime(data.ttData, e.getTime(), event.getTime());
			if (this.calculateLinkToLinkTravelTimes){
				log.error(ERROR_STUCK_AND_LINKTOLINK);
				throw new IllegalStateException(ERROR_STUCK_AND_LINKTOLINK);
			}
		}
	}
	
	private DataContainer getLinkToLinkTravelTimeData(Tuple<Id, Id> fromLinkToLink, final boolean createIfMissing) {
		DataContainer data = this.linkToLinkData.get(fromLinkToLink);
		if ((null == data) && createIfMissing) {
			data = new DataContainer(this.ttDataFactory.createTravelTimeData(fromLinkToLink.getFirst()));
			this.linkToLinkData.put(fromLinkToLink, data);
		}
		return data;
	}

	private DataContainer getTravelTimeData(final Id linkId, final boolean createIfMissing) {
		DataContainer data = this.linkData.get(linkId);
		if ((null == data) && createIfMissing) {
			data = new DataContainer(this.ttDataFactory.createTravelTimeData(linkId));
			this.linkData.put(linkId, data);
		}
		return data;
	}

	public double getLinkTravelTime(final Link link, final double time) {
		if (this.calculateLinkTravelTimes) {
			DataContainer data = getTravelTimeData(link.getId(), true);
			if (data.needsConsolidation) {
				consolidateData(data);
			}
			return this.aggregator.getTravelTime(data.ttData, time); 
		}
		throw new IllegalStateException("No link travel time is available " +
				"if calculation is switched off by config option!");
	}
	
	public double getLinkToLinkTravelTime(Link fromLink, Link toLink, double time) {
		if (!this.calculateLinkToLinkTravelTimes) {
			throw new IllegalStateException("No link to link travel time is available " +
			"if calculation is switched off by config option!");			
		}
		DataContainer data = this.getLinkToLinkTravelTimeData(new Tuple<Id, Id>(fromLink.getId(), toLink.getId()), true);
		if (data.needsConsolidation) {
			consolidateData(data);
		}
		return this.aggregator.getTravelTime(data.ttData, time);
	}

	public void reset(int iteration) {
		if (this.calculateLinkTravelTimes) {
			for (DataContainer data : this.linkData.values()){
				data.ttData.resetTravelTimes();
				data.needsConsolidation = false;
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
	
	public void setTravelTimeDataFactory(final TravelTimeDataFactory factory) {
		this.ttDataFactory = factory;
	}
	
	public void setTravelTimeAggregator(final AbstractTravelTimeAggregator aggregator) {
		this.aggregator = aggregator;
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
