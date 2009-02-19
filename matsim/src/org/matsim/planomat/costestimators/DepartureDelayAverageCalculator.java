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

import org.apache.log4j.Logger;
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
 */
public class DepartureDelayAverageCalculator implements AgentDepartureEventHandler, LinkLeaveEventHandler {

	private NetworkLayer network;
	private int timeBinSize;
	private HashMap<DepartureEvent, Double> departureEventsTimes = new HashMap<DepartureEvent, Double>();
	private final HashMap<Link, DepartureDelayData> linkData;
	
	private static final Logger log = Logger.getLogger(DepartureDelayAverageCalculator.class);

	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////

	public DepartureDelayAverageCalculator(NetworkLayer network, int timeBinSize) {
		super();
		this.network = network;
		this.timeBinSize = timeBinSize;
		this.linkData = new HashMap<Link, DepartureDelayData>(this.network.getLinks().size());
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

	/*package*/ class DepartureDelayData {
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

			try {
				int index = getTimeSlotIndex(time);
				double sum = 0.0;
				sum = this.timeSum[index];
				if (sum > 0.0) {
					int cnt = this.timeCnt[index];
					if (cnt > 0) {
						departureDelay = sum / cnt;
					}
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				log.warn("A departure delay for an invalid value of time was requested. Returning departureDelay = 0.0. time = " + Double.toString(time));
			}

			return departureDelay;

		}

		public void resetDepartureDelays() {
			int nofSlots = ((27*3600)/DepartureDelayAverageCalculator.this.timeBinSize);	// default number of slots
			this.timeSum = new double[nofSlots];
			this.timeCnt = new int[nofSlots];
		}

	}

	private DepartureDelayData getDepartureDelayRole(Link l) {
		DepartureDelayData r = this.linkData.get(l);
		if (null == r) {
			r = new DepartureDelayData();
			this.linkData.put(l, r);
		}
		return r;
	}

	//////////////////////////////////////////////////////////////////////
	// Implementation of EventAlgorithmI
	//////////////////////////////////////////////////////////////////////

	public void handleEvent(AgentDepartureEvent event) {
		DepartureEvent depEvent = new DepartureEvent(new IdImpl(event.agentId));
		this.departureEventsTimes.put(depEvent, event.time);
	}

	public void handleEvent(LinkLeaveEvent event) {
		DepartureEvent removeMe = new DepartureEvent(new IdImpl(event.agentId));
		Double departureTime = departureEventsTimes.remove(removeMe);
		if (departureTime != null) {
			double departureDelay = event.time - departureTime.intValue();
			if (departureDelay < 0) {
				throw new RuntimeException("departureDelay cannot be < 0.");
			}
			Link link = event.link;
			if (null == link) link = this.network.getLink(event.linkId);
			if (null != link) {
				this.getDepartureDelayRole(link).addDepartureDelay(departureTime, departureDelay);
			}
		}
	}

	public void resetDepartureDelays() {
		log.warn("this method iterates over the entire network and eventually creates the roles. This might become very resource intensive and should be avoided! it's fine for the equil-net, but not for bigger networks");
		for (Link link : this.network.getLinks().values()) {
			getDepartureDelayRole(link).resetDepartureDelays();
		}
		this.departureEventsTimes.clear();
	}

	public void reset(int iteration) {
		resetDepartureDelays();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}
