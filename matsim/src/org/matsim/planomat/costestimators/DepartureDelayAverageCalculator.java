/* *********************************************************************** *
 * project: org.matsim.*
 * DepartureDelayAverageCalculator.java
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

package org.matsim.planomat.costestimators;

import java.util.HashMap;

import org.matsim.basic.v01.Id;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;

public class DepartureDelayAverageCalculator implements EventHandlerAgentDepartureI, EventHandlerLinkLeaveI {

	private NetworkLayer network;
	private int timeBinSize;
	private HashMap<DepartureEvent, Double> departureEventsTimes = new HashMap<DepartureEvent, Double>();

	private int roleIndex;

	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////

	public DepartureDelayAverageCalculator(NetworkLayer network, int timeBinSize) {
		super();
		this.network = network;
		this.timeBinSize = timeBinSize;
		this.roleIndex = network.requestLinkRole();
		this.resetDepartureDelays();
	}

	//////////////////////////////////////////////////////////////////////
	// get the departure delay estimation for a given departure time HERE
	//////////////////////////////////////////////////////////////////////

	public double getLinkDepartureDelay(Link link, double departureTime) {

		return this.getDepartureDelayRole(link).getDepartureDelay(departureTime);

	}

	//////////////////////////////////////////////////////////////////////
	// Implementation of link role
	//////////////////////////////////////////////////////////////////////

	private class DepartureDelayRole {
		private HashMap<Integer, Double> timeSum = null;
		private HashMap<Integer, Integer> timeCnt = null;

		public DepartureDelayRole() {
			super();
		}

		private int getTimeSlotIndex(double time) {
			int slice = (int)(time/DepartureDelayAverageCalculator.this.timeBinSize);
			return slice;
		}

		public void addDepartureDelay(double departureTime, double departureDelay) {
			Integer index = Integer.valueOf(getTimeSlotIndex(departureTime));
			Double sum = this.timeSum.get(index);
			Integer cnt = this.timeCnt.get(index);
			if (null == sum) {
				sum = Double.valueOf(departureDelay);
				cnt = Integer.valueOf(1);
			} else {
				sum += departureDelay;
				cnt = Integer.valueOf(cnt.intValue() + 1);
			}
			this.timeSum.put(index, sum);
			this.timeCnt.put(index, cnt);
		}

		public double getDepartureDelay(double time) {

			double departureDelay = 0.0;

			Integer index = Integer.valueOf(getTimeSlotIndex(time));
			Double sum = this.timeSum.get(index);

			if (sum != null) {

				Integer cnt = this.timeCnt.get(index);
				if (cnt != null) {
					double cnt2 = cnt.doubleValue();
					departureDelay = sum.doubleValue() / cnt2;
				}

			}
			// else
			// return 0.0 if there were no departures in this time range

			return departureDelay;

		}

		public void resetDepartureDelays() {
			int nofSlots = ((27*3600)/DepartureDelayAverageCalculator.this.timeBinSize);	// default number of slots
			this.timeSum = new HashMap<Integer, Double>(nofSlots);
			this.timeCnt = new HashMap<Integer, Integer>(nofSlots);
		}

	}

	private DepartureDelayRole getDepartureDelayRole(Link l) {
		DepartureDelayRole r = (DepartureDelayRole)l.getRole(this.roleIndex);
		if (null == r) {
			r = new DepartureDelayRole();
			l.setRole(this.roleIndex, r);
		}
		return r;
	}

	//////////////////////////////////////////////////////////////////////
	// Implementation of EventAlgorithmI
	//////////////////////////////////////////////////////////////////////

	public void handleEvent(EventAgentDeparture event) {
		DepartureEvent depEvent = new DepartureEvent(new Id(event.agentId), event.getAttributes().getValue("leg"));
		this.departureEventsTimes.put(depEvent, event.time);
	}

	public void handleEvent(EventLinkLeave event) {
		DepartureEvent removeMe = new DepartureEvent(new Id(event.agentId), event.getAttributes().getValue("leg"));
		Double departureTime = departureEventsTimes.remove(removeMe);
		if (departureTime != null) {
			double departureDelay = event.time - departureTime.intValue();
			if (departureDelay < 0) {
				Gbl.errorMsg("");
			}
			Link link = event.link;
			if (null == link) link = (Link)this.network.getLocation(event.linkId);
			if (null != link) {
				this.getDepartureDelayRole(link).addDepartureDelay(departureTime, departureDelay);
			}
		}
	}

	public void resetDepartureDelays() {
		/* WARNING: this method iterates over the entire network
		 * and eventually creates the roles. This might become very
		 * resource intensive and should be avoided!
		 * it's fine for the equil-net, but not for bigger networks */
		for (Link link : this.network.getLinks().values()) {
			getDepartureDelayRole(link).resetDepartureDelays();
		}
		this.departureEventsTimes.clear();
	}

	public void reset(int iteration) {
		resetDepartureDelays();
	}

	//////////////////////////////////////////////////////////////////////
	// Overriding Object
	//////////////////////////////////////////////////////////////////////

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}
