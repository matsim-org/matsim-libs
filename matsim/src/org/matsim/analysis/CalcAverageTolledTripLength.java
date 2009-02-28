/* *********************************************************************** *
 * project: org.matsim.*
 * CalcAverageTolledTripLength.java
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

package org.matsim.analysis;

import java.util.TreeMap;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Network;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingScheme.Cost;

/**
 * Calculates the distance of a trip which occurred on tolled links.
 * Requires roadpricing to be on.
 *
 * @author mrieser
 */
public class CalcAverageTolledTripLength implements LinkEnterEventHandler, AgentArrivalEventHandler {

	private double sumLength = 0.0;
	private int cntTrips = 0;
	private RoadPricingScheme scheme = null;
	private Network network = null;
	private TreeMap<String, Double> agentDistance = null;

	public CalcAverageTolledTripLength(final Network network, final RoadPricingScheme scheme) {
		this.scheme = scheme;
		this.network = network;
		this.agentDistance = new TreeMap<String, Double>();
	}

	public void handleEvent(final LinkEnterEvent event) {
		Cost cost = this.scheme.getLinkCost(new IdImpl(event.linkId), event.time);
		if (cost != null) {
			Link link = event.link;
			if (link == null) {
				link = this.network.getLink(new IdImpl(event.linkId));
			}
			if (link != null) {
				Double length = this.agentDistance.get(event.agentId);
				if (length == null) {
					length = 0.0;
				}
				length += link.getLength();
				this.agentDistance.put(event.agentId, length);
			}
		}
	}

	public void handleEvent(final AgentArrivalEvent event) {
		Double length = this.agentDistance.get(event.agentId);
		if (length != null) {
			this.sumLength += length;
			this.agentDistance.put(event.agentId, 0.0);
		}
		this.cntTrips++;
	}

	public void reset(final int iteration) {
		this.sumLength = 0.0;
		this.cntTrips = 0;
	}

	public double getAverageTripLength() {
		if (this.cntTrips == 0) return 0;
		return (this.sumLength / this.cntTrips);
	}
}
