/* *********************************************************************** *
 * project: org.matsim.*
 * CalcODMatrices.java
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

package org.matsim.core.events.algorithms;

import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicAgentArrivalEvent;
import org.matsim.api.basic.v01.events.BasicAgentDepartureEvent;
import org.matsim.api.basic.v01.events.handler.BasicAgentArrivalEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentDepartureEventHandler;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.utils.misc.Time;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.world.Location;
import org.matsim.world.ZoneLayer;

public class CalcODMatrices implements BasicAgentArrivalEventHandler, BasicAgentDepartureEventHandler {

	private final Network network;
	private final ZoneLayer tvzLayer;
	private final TreeMap<Id, Location> agents = new TreeMap<Id, Location>(); // <AgentID, StartLoc>
	private final TreeMap<Id, Double> agentsTime = new TreeMap<Id, Double>();
	private final Matrix matrix;
	private double minTime = Time.UNDEFINED_TIME; //Integer.MIN_VALUE;
	private double maxTime = Double.POSITIVE_INFINITY;//Integer.MAX_VALUE;
	public int counter = 0;

	public CalcODMatrices(final Network network, final ZoneLayer tvzLayer, final Matrix matrix) {
		this.network = network;
		this.tvzLayer = tvzLayer;
		this.matrix = matrix;
	}

	public void handleEvent(final BasicAgentDepartureEvent event) {
		double time = event.getTime();
		if ((time < this.minTime) || (time >= this.maxTime)) {
			return;
		}
		Location fromLoc = getLocation(event.getLinkId());
		if (fromLoc != null) {
			Id agentId = event.getPersonId();
			this.agents.put(agentId, fromLoc);
			this.agentsTime.put(agentId, time);
		}
	}

	public void handleEvent(final BasicAgentArrivalEvent event) {
		double time = event.getTime();
		if (time < this.minTime) {
			return;
		}
		Id agentId = event.getPersonId();
		Location fromLoc = this.agents.remove(agentId); // use remove instead of get to make sure one arrival event is not used for multiple departure events
		if (fromLoc == null) {
			// we have no information on where the agent started
			return;
		}
		Location toLoc = getLocation(event.getLinkId());
		if (toLoc != null) {
			this.agents.remove(agentId);
			Entry entry = this.matrix.getEntry(fromLoc, toLoc);
			this.counter++;
			if (entry == null) {
				this.matrix.setEntry(fromLoc, toLoc, 1);
			} else {
				this.matrix.setEntry(fromLoc, toLoc, entry.getValue() + 1);
			}
		}
	}

	/**
	 * sets the time range in which events are counted. only events in the
	 * range [minTime, maxTime[ are counted, with maxTime excluded
	 *
	 * @param minTime events with a smaller time will not be counted
	 * @param maxTime events with this time or higher will not be counted
	 */
	public void setTimeRange(final double minTime, final double maxTime) {
		this.minTime = minTime;
		this.maxTime = maxTime;
	}

	public void reset(final int iteration) {
		// nothing to do
	}

	private Location getLocation(final Id linkId) {
		Link link = this.network.getLinks().get(linkId);

		ArrayList<Location> locs = this.tvzLayer.getNearestLocations(link.getCoord(), null);
		if (locs.size() > 0) {
			return locs.get(0);
		}
		return null;
	}

}
