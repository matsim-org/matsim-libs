/* *********************************************************************** *
 * project: org.matsim.*
 * DepartureDelayAverageCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;

/**
 * Computes average departure delay on a link in a given time slot.
 *
 * @author meisterk
 *
 */
public class DepartureDelayAverageCalculator implements AgentDepartureEventHandler, LinkLeaveEventHandler {

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

	/**
	 * get the departure delay estimation for a given departure time HERE
	 *
	 * @param link
	 * @param departureTime
	 * @return departure delay estimation
	 */
	public double getLinkDepartureDelay(Link link, double departureTime) {
		return this.getDepartureDelayRole(link).getDepartureDelay(departureTime);
	}

	//////////////////////////////////////////////////////////////////////
	// Implementation of link role
	//////////////////////////////////////////////////////////////////////

	private class DepartureDelayRole {
		private double[] timeSum = null;
		private int[] timeCnt = null;

		private int getTimeSlotIndex(double time) {
			int slice = (int)(time/DepartureDelayAverageCalculator.this.timeBinSize);
			if (slice >= timeSum.length) {
				slice = timeSum.length - 1;
			}
			return slice;
		}

		public void addDepartureDelay(double departureTime, double departureDelay) {
			int index = getTimeSlotIndex(departureTime);
			this.timeSum[index] += departureDelay;
			this.timeCnt[index]++;
		}

		public double getDepartureDelay(double time) {
			double departureDelay = 0.0;

			int index = getTimeSlotIndex(time);
			double sum = this.timeSum[index];
			if (sum > 0.0) {
				int cnt = this.timeCnt[index];
				if (cnt > 0) {
					departureDelay = sum / cnt;
				}
			}

			return departureDelay;

		}

		public void resetDepartureDelays() {
			int nofSlots = ((27*3600)/DepartureDelayAverageCalculator.this.timeBinSize);	// default number of slots
			this.timeSum = new double[nofSlots];
			this.timeCnt = new int[nofSlots];
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

	public void handleEvent(AgentDepartureEvent event) {
		DepartureEvent depEvent = new DepartureEvent(new IdImpl(event.agentId), event.legId);
		this.departureEventsTimes.put(depEvent, event.time);
	}

	public void handleEvent(LinkLeaveEvent event) {
		DepartureEvent removeMe = new DepartureEvent(new IdImpl(event.agentId), event.legId);
		Double departureTime = departureEventsTimes.remove(removeMe);
		if (departureTime != null) {
			double departureDelay = event.time - departureTime.intValue();
			if (departureDelay < 0) {
				throw new RuntimeException("departureDelay cannot be < 0.");
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
