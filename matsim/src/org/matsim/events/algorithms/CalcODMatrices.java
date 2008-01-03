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

package org.matsim.events.algorithms;

import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.gbl.Gbl;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.utils.misc.Time;
import org.matsim.world.Location;
import org.matsim.world.ZoneLayer;

public class CalcODMatrices implements EventHandlerAgentArrivalI, EventHandlerAgentDepartureI {

	private final NetworkLayer network;
	private final ZoneLayer tvzLayer;
	private final Plans population;
	private final TreeMap<String, Location> agents = new TreeMap<String, Location>(); // <AgentID, StartLoc>
	private final TreeMap<String, Double> agentstime = new TreeMap<String, Double>();
	private final Matrix<Integer> matrix;
	private double minTime = Time.UNDEFINED_TIME; //Integer.MIN_VALUE;
	private double maxTime = Double.POSITIVE_INFINITY;//Integer.MAX_VALUE;
	public int counter = 0;

	public CalcODMatrices(NetworkLayer network, ZoneLayer tvzLayer, Plans population, String id) {
		this.network = network;
		this.tvzLayer = tvzLayer;
		this.population = population;
		this.matrix = Matrices.getSingleton().<Integer>createMatrix(id, tvzLayer.getType().toString(), "od for miv");
	}

	//////////////////////////////////////////////////////////////////////
	// Implementation of EventAlgorithmI
	//////////////////////////////////////////////////////////////////////

	public void handleEvent(EventAgentDeparture event) {
		double time = event.time;
		if (time < minTime || time >= maxTime) {
			return;
		}
		Location fromLoc = getLocation(event.agentId, event.linkId);
		if (fromLoc != null) {
			String agentId = event.agentId;
			agents.put(agentId, fromLoc);
			agentstime.put(agentId, time);
		}		
	}

	public void handleEvent(EventAgentArrival event) {
		double time = event.time;
		if (time < minTime) {
			return;
		}
		String agentId = event.agentId;
		Location fromLoc = agents.remove(agentId); // use remove instead of get to make sure one arrival event is not used for multiple departure events
		if (fromLoc == null) {
			// we have no information on where the agent started
			return;
		}
		Location toLoc = getLocation(event.agentId, event.linkId);
		if (toLoc != null) {
			agents.remove(agentId);
			Entry<Integer> entry = matrix.getEntry(fromLoc, toLoc);
			counter++;
			if (entry == null) {
				matrix.setEntry(fromLoc, toLoc, 1);
			} else {
				matrix.setEntry(fromLoc, toLoc, entry.getValue() + 1);
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
	public void setTimeRange(double minTime, double maxTime) {
		this.minTime = minTime;
		this.maxTime = maxTime;
	}

	public Matrix<Integer> getMatrix() {
		return matrix;
	}

	public void reset(int iteration) {
	}


	private Location getLocation(String agentId, String linkId) {
		Link link = (Link)network.getLocation(linkId);
		
		if (population != null) {
			// let's try to find the cell from the plans
			Person person = population.getPerson(agentId);
			if (person != null) {
				Plan plan = person.getSelectedPlan();
				for (int i = 0, max = plan.getActsLegs().size(); i < max; i += 2) {
					Act act = (Act)plan.getActsLegs().get(i);
					if (act.getLink().getId().equals(new Id(linkId)) && act.getRefId() != Integer.MIN_VALUE) {
						return tvzLayer.getLocation(act.getRefId());
					}
				}
			}
			Gbl.warningMsg(this.getClass(), "getLocation", "No tvz-cell found for link " + linkId + " and agent " + agentId);
		}
		// okay, we did not find the cell from the plans, so just get the nearest one.
		ArrayList<Location> locs = tvzLayer.getNearestLocations(link.getCenter(), null);
		if (locs.size() > 0) {
			return locs.get(0);
		}
		return null;
	}

}
