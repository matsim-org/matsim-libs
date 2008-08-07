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

import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.LinkEnterEnter;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;

/**
 * Calculates the average travel speed
 *
 * @author ychen
 *
 */
public class CalcNetAvgSpeed implements EventHandlerLinkEnterI,
		EventHandlerLinkLeaveI, EventHandlerAgentArrivalI {
	/**
	 * @param lengthSum -
	 *            the sum of all the covered distance [km].
	 * @param timeSum -
	 *            the sum of all the drivingtime [h]
	 */
	private double lengthSum, timeSum;
	protected NetworkLayer network = null;
	/*
	 * enterTimes<String agentId, Double enteringtime>
	 */
	private TreeMap<String, Double> enterTimes = null;

	// --------------------------CONSTRUCTOR------------------
	/**
	 *
	 */
	public CalcNetAvgSpeed(final NetworkLayer network) {
		this.network = network;
		this.enterTimes = new TreeMap<String, Double>();
	}

	public void handleEvent(LinkEnterEnter enter) {
		// if (enter.agent.getSelectedPlan().getType().equals("car")) {
		this.enterTimes.put(enter.agentId, enter.time);
		// }
	}

	public void reset(int iteration) {
		this.enterTimes.clear();
		this.lengthSum = 0;
		this.timeSum = 0;
	}

	public void handleEvent(LinkLeaveEvent leave) {
		Double enterTime = this.enterTimes.get(leave.agentId);
		if (enterTime != null) {
			Link l = leave.link;
			if (l == null) {
				l = this.network.getLink(leave.linkId);
			}
			if (l != null) {
				this.lengthSum += l.getLength() / 1000.0;
				this.timeSum += (leave.time - enterTime.doubleValue()) / 3600.0;
			}
		}
	}

	public double getNetAvgSpeed() {
		return ((this.timeSum != 0.0) ? this.lengthSum / this.timeSum : 0.0);
	}

	public void handleEvent(AgentArrivalEvent arrival) {
		String id = arrival.agentId;
		if (this.enterTimes.containsKey(id)) {
			this.enterTimes.remove(id);
		}
	}
}
