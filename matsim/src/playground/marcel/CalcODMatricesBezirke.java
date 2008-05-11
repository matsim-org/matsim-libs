/* *********************************************************************** *
 * project: org.matsim.*
 * CalcODMatricesBezirke.java
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

package playground.marcel;

import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.world.Location;
import org.matsim.world.ZoneLayer;

public class CalcODMatricesBezirke implements EventHandlerAgentArrivalI, EventHandlerAgentDepartureI {

	private final NetworkLayer network;
	private final ZoneLayer tvzLayer;
	private final Plans population;
	private final TreeMap<String, Location> agents = new TreeMap<String, Location>(); // <AgentID, StartLoc>
	private final TreeMap<String, Double> agentstime = new TreeMap<String, Double>();
	private final Matrix matrix;
	private int minTime = Integer.MIN_VALUE;
	private int maxTime = Integer.MAX_VALUE;
	public int counter = 0;

	private final static Logger log = Logger.getLogger(CalcODMatricesBezirke.class);

	public CalcODMatricesBezirke(final NetworkLayer network, final ZoneLayer tvzLayer, final Plans population, final String id) {
		this.network = network;
		this.tvzLayer = tvzLayer;
		this.population = population;
		this.matrix = Matrices.getSingleton().createMatrix(id, tvzLayer, "od for miv");
	}

	//////////////////////////////////////////////////////////////////////
	// Implementation of EventAlgorithmI
	//////////////////////////////////////////////////////////////////////

	public void handleEvent(final EventAgentDeparture event) {
		double time = event.time;
		if ((time < this.minTime) || (time >= this.maxTime)) {
			return;
		}
		Location fromLoc = getLocation(event.agentId, event.linkId);
		if (fromLoc != null) {
			fromLoc = mapLocation(fromLoc);
			this.agents.put(event.agentId, fromLoc);
			this.agentstime.put(event.agentId, Double.valueOf(time));
		}
	}

	public void handleEvent(final EventAgentArrival event) {
		double time = event.time;
		if (time < this.minTime) {
			return;
		}
		Location fromLoc = this.agents.remove(event.agentId); // use remove instead of get to make sure one arrival event is not used for multiple departure events
		if (fromLoc == null) {
			// we have no information on where the agent started
			return;
		}
		Location toLoc = getLocation(event.agentId, event.linkId);
		if (toLoc != null) {
			toLoc = mapLocation(toLoc);
			this.agents.remove(event.agentId);
			Entry entry = this.matrix.getEntry(fromLoc, toLoc);
			this.counter++;
			if (entry == null) {
				this.matrix.setEntry(fromLoc, toLoc, 1.0);
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
	public void setTimeRange(final int minTime, final int maxTime) {
		this.minTime = minTime;
		this.maxTime = maxTime;
	}

	public Matrix getMatrix() {
		return this.matrix;
	}

	public void reset(final int iteration) {
	}


	private Location getLocation(final String agentId, final String linkId) {
		Link link = (Link)this.network.getLocation(linkId);

		if (this.population != null) {
			// let's try to find the cell from the plans
			Person person = this.population.getPersons().get(agentId);
			if (person != null) {
				Plan plan = person.getSelectedPlan();
				for (int i = 0, max = plan.getActsLegs().size(); i < max; i += 2) {
					Act act = (Act)plan.getActsLegs().get(i);
					if (act.getLink().getId().toString().equals(linkId) && (act.getRefId() != Integer.MIN_VALUE)) {
						return this.tvzLayer.getLocation(act.getRefId());
					}
				}
			}
			log.warn("No tvz-cell found for link " + linkId + " and agent " + agentId);
		}
		// okay, we did not find the cell from the plans, so just get the nearest one.
		ArrayList<Location> locs = this.tvzLayer.getNearestLocations(link.getCenter(), null);
		if (locs.size() > 0) {
			return locs.get(0);
		}
		return null;
	}

	private Location mapLocation(final Location loc) {
		TreeMap<Id, Location> mapping = loc.getUpMapping();
		if (mapping.size() == 1) {
			return mapping.get(mapping.firstKey());
		}
		return null;
	}

}
