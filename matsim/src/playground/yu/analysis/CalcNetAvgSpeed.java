/* *********************************************************************** *
 * project: org.matsim.*
 * CalcAvgSpeed.java
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

/**
 *
 */
package playground.yu.analysis;

import java.util.TreeMap;

import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.handler.LinkLeaveEventHandler;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.yu.utils.TollTools;

/**
 * Calculates the average travel speed
 * 
 * @author ychen
 * 
 */
public class CalcNetAvgSpeed implements LinkEnterEventHandler,
		LinkLeaveEventHandler, AgentArrivalEventHandler {
	/**
	 * @param lengthSum
	 *            - the sum of all the covered distance [km].
	 * @param timeSum
	 *            - the sum of all the drivingtime [h]
	 */
	private double lengthSum, timeSum;
	protected NetworkLayer network = null;
	/*
	 * enterTimes<String agentId, Double enteringtime>
	 */
	private TreeMap<String, Double> enterTimes = null;
	private RoadPricingScheme toll = null;

	// --------------------------CONSTRUCTOR------------------
	/**
	 *
	 */
	public CalcNetAvgSpeed(final NetworkLayer network) {
		this.network = network;
		this.enterTimes = new TreeMap<String, Double>();
	}

	public CalcNetAvgSpeed(NetworkLayer network, RoadPricingScheme toll) {
		this(network);
		this.toll = toll;
	}

	public void handleEvent(LinkEnterEventImpl enter) {
		if (toll == null)
			this.enterTimes
					.put(enter.getPersonId().toString(), enter.getTime());
		else {
			LinkImpl link = enter.getLink();
			if (link == null)
				link = network.getLink(enter.getLinkId().toString());
			if (link != null)
				if (TollTools.isInRange(link, toll))
					this.enterTimes.put(enter.getPersonId().toString(), enter
							.getTime());
		}
	}

	public void reset(int iteration) {
		this.enterTimes.clear();
		this.lengthSum = 0;
		this.timeSum = 0;
	}

	public void handleEvent(LinkLeaveEventImpl leave) {
		Double enterTime = this.enterTimes.remove(leave.getPersonId()
				.toString());
		if (enterTime != null) {
			LinkImpl l = leave.getLink();
			if (l == null) {
				l = this.network.getLink(leave.getLinkId().toString());
			}
			if (l != null) {
				this.lengthSum += l.getLength() / 1000.0;
				this.timeSum += (leave.getTime() - enterTime.doubleValue()) / 3600.0;
			}
		}
	}

	public double getNetAvgSpeed() {
		return ((this.timeSum != 0.0) ? this.lengthSum / this.timeSum : 0.0);
	}

	public void handleEvent(AgentArrivalEventImpl arrival) {
		String id = arrival.getPersonId().toString();
		if (this.enterTimes.containsKey(id)) {
			this.enterTimes.remove(id);
		}
	}
}
