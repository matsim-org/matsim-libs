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

package org.matsim.events.algorithms;

import java.util.HashMap;

import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.router.util.TravelTimeI;

public class TravelTimeCalculator implements EventHandlerLinkEnterI, EventHandlerLinkLeaveI, EventHandlerAgentArrivalI, TravelTimeI {

	// EnterEvent implements Comparable based on linkId and vehId. This means that the key-pair <linkId, vehId> must always be unique!
	private final HashMap<String, EnterEvent> enterEvents = new HashMap<String, EnterEvent>();
	private NetworkLayer network = null;
	final int roleIndex;
	private final int timeslice;
	private final int numSlots;

	static private class EnterEvent {

		public final String linkId;
		public final double time;

		public EnterEvent(final String linkId, final double time) {
			this.linkId = linkId;
			this.time = time;
		}

	};

	private class TravelTimeRole {
		private final double[] timeSum;
		private final int[] timeCnt;
		private final double[] travelTimes;
		private final double freetraveltime;

		public TravelTimeRole(final Link link, final int numSlots) {
			this.timeSum = new double[numSlots];
			this.timeCnt = new int[numSlots];
			this.travelTimes = new double[numSlots];
			this.freetraveltime = link.getLength() / link.getFreespeed();
			resetTravelTimes();
		}

		public void resetTravelTimes() {
			for (int i = 0; i < this.timeSum.length; i++) {
				this.timeSum[i] = 0.0;
				this.timeCnt[i] = 0;
				this.travelTimes[i] = -1.0;
			}
		}

		public void addTravelTime(final double now, final double traveltime) {
			int index = getTimeSlotIndex(now);
			double sum = this.timeSum[index];
			int cnt = this.timeCnt[index];
			sum += traveltime;
			cnt++;
			this.timeSum[index] = sum;
			this.timeCnt[index] = cnt;
			this.travelTimes[index] = -1.0; // initialize with negative value
		}

		public double getTravelTime(final double now) {
			int index = getTimeSlotIndex(now);
			double ttime = this.travelTimes[index];
			if (ttime >= 0.0) return ttime; // negative values are invalid.

			int cnt = this.timeCnt[index];
			if (cnt == 0) {
				this.travelTimes[index] = this.freetraveltime;
				return this.freetraveltime;
			}

			double sum = this.timeSum[index];
			this.travelTimes[index] = sum / cnt;
			return this.travelTimes[index];
		}

	};

	public TravelTimeCalculator(final NetworkLayer network) {
		this(network, 15*60, 30*3600);	// default timeslot-duration: 15 minutes
	}
	
	public TravelTimeCalculator(final NetworkLayer network, final int timeslice) {
		this(network, timeslice, 30*3600); // default: 30 hours at most
	}
	
	public TravelTimeCalculator(final NetworkLayer network, final int timeslice, final int maxTime) {
		this.network = network;
		this.timeslice = timeslice;
		this.numSlots = (maxTime / this.timeslice) + 1; // TODO hard-coded max-time
		this.roleIndex = network.requestLinkRole();
		resetTravelTimes();
	}

	public void resetTravelTimes() {
		for (Object link : this.network.getLinks()) {
			TravelTimeRole r = getTravelTimeRole((Link) link);
			r.resetTravelTimes();
		}
		this.enterEvents.clear();
	}

	public void reset(final int iteration) {
		resetTravelTimes();
	}

	//////////////////////////////////////////////////////////////////////
	// Implementation of EventAlgorithmI
	//////////////////////////////////////////////////////////////////////

	public void handleEvent(final EventLinkEnter event) {
		EnterEvent e = new EnterEvent(event.linkId, event.time);
		this.enterEvents.put(event.agentId, e);
	}

	public void handleEvent(final EventLinkLeave event) {
		EnterEvent e = this.enterEvents.remove(event.agentId);
		if (e != null && e.linkId.equals(event.linkId)) {
			double timediff = event.time - e.time;
			if (event.link == null) event.link = (Link)this.network.getLocation(event.linkId);
			if (event.link != null) {
				getTravelTimeRole(event.link).addTravelTime(e.time, timediff);
			}
		}
	}

	public void handleEvent(final EventAgentArrival event) {
		// remove EnterEvents from list when an agent arrives.
		// otherwise, the activity duration would counted as travel time, when the
		// agent departs again and leaves the link!
		this.enterEvents.remove(event.agentId);
	}

	private TravelTimeRole getTravelTimeRole(final Link link) {
		TravelTimeRole r = (TravelTimeRole) link.getRole(this.roleIndex);
		if (null == r) {
			r = new TravelTimeRole(link, this.numSlots);
			link.setRole(this.roleIndex, r);
		}
		return r;
	}

	private int getTimeSlotIndex(final double time) {
		int slice = ((int) time)/this.timeslice;
		if (slice >= this.numSlots) slice = this.numSlots - 1;
		return slice;
	}


	//////////////////////////////////////////////////////////////////////
	// Implementation of TravelTimeI
	//////////////////////////////////////////////////////////////////////

	/* (non-Javadoc)
	 * @see org.matsim.network.TravelCostI#getLinkTravelTime(org.matsim.network.Link, int)
	 */
	public double getLinkTravelTime(final Link link, final double time) {
		return getTravelTimeRole(link).getTravelTime(time);
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return this.getClass().getSimpleName();
	}
	
	
}
