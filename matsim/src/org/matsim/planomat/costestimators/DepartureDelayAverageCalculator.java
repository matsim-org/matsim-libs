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
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicAgentDepartureEvent;
import org.matsim.api.basic.v01.events.BasicLinkLeaveEvent;
import org.matsim.api.basic.v01.events.handler.BasicAgentDepartureEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkLeaveEventHandler;
import org.matsim.api.basic.v01.network.BasicLink;
import org.matsim.api.basic.v01.network.BasicNetwork;
import org.matsim.api.basic.v01.network.BasicNode;

/**
 * Computes average departure delay on a link in a given time slot.
 *
 * @author meisterk
 */
public class DepartureDelayAverageCalculator implements BasicAgentDepartureEventHandler, BasicLinkLeaveEventHandler {

	private BasicNetwork<? extends BasicNode, ? extends BasicLink> network;
	private int timeBinSize;
	private HashMap<DepartureEvent, Double> departureEventsTimes = new HashMap<DepartureEvent, Double>();
	private final HashMap<Id, DepartureDelayData> linkData;
	
	private static final Logger log = Logger.getLogger(DepartureDelayAverageCalculator.class);

	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////

	public DepartureDelayAverageCalculator(BasicNetwork<? extends BasicNode, ? extends BasicLink> network, int timeBinSize) {
		super();
		this.network = network;
		this.timeBinSize = timeBinSize;
		this.linkData = new HashMap<Id, DepartureDelayData>(this.network.getLinks().size());
		this.resetDepartureDelays();
	}

	/**
	 * get the departure delay estimation for a given departure time HERE
	 *
	 * @param link
	 * @param departureTime
	 * @return departure delay estimation
	 */
	public double getLinkDepartureDelay(BasicLink link, double departureTime) {
		DepartureDelayData ddd = this.getDepartureDelayRole(link.getId());
		if (ddd == null) {
			return 0.0;
		}
		return ddd.getDepartureDelay(departureTime);
	}

	//////////////////////////////////////////////////////////////////////
	// Implementation of link role
	//////////////////////////////////////////////////////////////////////

	/*package*/ class DepartureDelayData {
		private double[] timeSum = null;
		private int[] timeCnt = null;

		private DepartureDelayData() {
			this.resetDepartureDelays();
		}

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

	private DepartureDelayData getDepartureDelayRole(Id linkId) {
		return this.linkData.get(linkId);
	}

	public void handleEvent(BasicAgentDepartureEvent event) {
		DepartureEvent depEvent = new DepartureEvent(event.getPersonId());
		this.departureEventsTimes.put(depEvent, event.getTime());
	}

	public void handleEvent(BasicLinkLeaveEvent event) {
		DepartureEvent removeMe = new DepartureEvent(event.getPersonId());
		Double departureTime = departureEventsTimes.remove(removeMe);
		if (departureTime != null) {
			double departureDelay = event.getTime() - departureTime.intValue();
			if (departureDelay < 0) {
				throw new RuntimeException("departureDelay cannot be < 0.");
			}
			Id linkId = event.getLinkId();
			DepartureDelayData ddd = this.getDepartureDelayRole(linkId);
			if (ddd == null) {
				ddd = new DepartureDelayData();
				this.linkData.put(linkId, ddd);
			}
			ddd.addDepartureDelay(departureTime, departureDelay);
		}
	}

	public void resetDepartureDelays() {
		this.linkData.clear();
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
