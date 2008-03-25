/* *********************************************************************** *
 * project: org.matsim.*
 * CalcPaidToll.java
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

package org.matsim.roadpricing;

import java.util.TreeMap;

import org.matsim.events.BasicEvent;
import org.matsim.events.EventAgentWait2Link;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.handler.EventHandlerAgentWait2LinkI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.roadpricing.RoadPricingScheme.Cost;

/**
 * Calculates the toll agents pay during a simulation by analyzing events. To
 * fully function, add an instance of this class as EventHandler to your Events
 * object.
 *
 * @author mrieser
 */
public class CalcPaidToll implements EventHandlerLinkEnterI,
		EventHandlerAgentWait2LinkI {

	static class AgentInfo {
		public double toll = 0.0;
		public boolean insideCordonArea = true;
	}

	final RoadPricingScheme scheme;
	final TreeMap<String, AgentInfo> agents = new TreeMap<String, AgentInfo>();
	private final NetworkLayer network;

	private TollBehaviourI handler = null;

	public CalcPaidToll(final NetworkLayer network,
			final RoadPricingScheme scheme) {
		super();
		this.network = network;
		this.scheme = scheme;
		if ("distance".equals(scheme.getType()))
			this.handler = new DistanceTollBehaviour();
		else if ("area".equals(scheme.getType()))
			this.handler = new AreaTollBehaviour();
		else if ("cordon".equals(scheme.getType()))
			this.handler = new CordonTollBehaviour();
		else
			throw new IllegalArgumentException("RoadPricingScheme of type \""
					+ scheme.getType() + "\" is not supported.");
	}

	public void handleEvent(final EventLinkEnter event) {
		Link link = event.link;
		if (link == null) {
			link = (Link) this.network.getLocation(event.linkId);
		}
		this.handler.handleEvent(event, link);
	}

	public void handleEvent(final EventAgentWait2Link event) {
		Link link = event.link;
		if (link == null) {
			link = (Link) this.network.getLocation(event.linkId);
		}
		this.handler.handleEvent(event, link);
	}

	public void reset(final int iteration) {
		this.agents.clear();
	}

	/**
	 * Returns the toll the specified agent has paid in the course of the
	 * simulation so far.
	 *
	 * @param agentId
	 * @return The toll paid by the specified agent, 0.0 if no toll was paid.
	 */
	public double getAgentToll(final String agentId) {
		AgentInfo info = this.agents.get(agentId);
		if (info == null) {
			return 0.0;
		}
		return info.toll;
	}

	/**
	 * @return The toll paid by all the agents.
	 */
	public double getAllAgentsToll() {
		double tolls = 0;
		for (AgentInfo ai : this.agents.values()) {
			tolls += (ai == null) ? 0.0 : ai.toll;
		}
		return tolls;
	}

	/**
	 * @return The Number of all the Drawees.
	 */
	public int getDraweesNr() {
		int dwCnt = 0;
		for (AgentInfo ai : this.agents.values()) {
			if (ai != null)
				if (ai.toll > 0.0)
					dwCnt++;
		}
		return dwCnt;
	}

	/**
	 * A simple interface to implement different toll schemes.
	 */
	private interface TollBehaviourI {
		public void handleEvent(BasicEvent event, Link link);
	}

	/**
	 * Handles the calculation of the distance toll. If an agent enters a link at
	 * a time the toll has to be paid, the toll amount is added to the agent. The
	 * agent does not have to pay the toll for a link if it starts on the link,
	 * as it may have paid already when arriving on the link.
	 */
	class DistanceTollBehaviour implements TollBehaviourI {
		public void handleEvent(final BasicEvent event, final Link link) {
			if (event instanceof EventAgentWait2Link) {
				/* we do not handle wait2link-events for distance toll, because the agent
				 * must not pay twice for this link, and he (likely) paid already when
				 * arriving at this link.  */
				return;
			}
			Cost cost = CalcPaidToll.this.scheme.getLinkCost(link.getId(),
					event.time);
			if (cost != null) {
				double newToll = link.getLength() * cost.amount;
				AgentInfo info = CalcPaidToll.this.agents.get(event.agentId);
				if (info == null) {
					info = new AgentInfo();
					CalcPaidToll.this.agents.put(event.agentId, info);
				}
				info.toll += newToll;
			}
		}
	}

	/** Handles the calculation of the area toll. Whenever the agent is seen on
	 * one of the tolled link, the constant toll amount has to be paid once. */
	class AreaTollBehaviour implements TollBehaviourI {
		public void handleEvent(final BasicEvent event, final Link link) {
			Cost cost = CalcPaidToll.this.scheme.getLinkCost(link.getId(), event.time);
			if (cost != null) {
				AgentInfo info = CalcPaidToll.this.agents.get(event.agentId);
				if (info == null) {
					info = new AgentInfo();
					CalcPaidToll.this.agents.put(event.agentId, info);
					info.toll = cost.amount;
				}
			}
		}
	}

	/**
	 * Handles the calculation of the cordon toll. An agent has only to pay if he
	 * crosses the cordon from the outside to the inside.
	 */
	class CordonTollBehaviour implements TollBehaviourI {
		public void handleEvent(final BasicEvent event, final Link link) {
			Cost cost = CalcPaidToll.this.scheme.getLinkCost(link.getId(), event.time);
			if (cost != null) {
				// this is a link inside the toll area.
				AgentInfo info = CalcPaidToll.this.agents.get(event.agentId);
				if (info == null) {
					// no information about this agent, so it did not yet pay the toll
					info = new AgentInfo();
					CalcPaidToll.this.agents.put(event.agentId, info);
					info.toll = 0.0; // we start in the area, do not toll
				} else if (!info.insideCordonArea) {
					// agent was outside before, now inside the toll area --> agent has to pay
					info.insideCordonArea = true;
					info.toll += cost.amount;
				}
			} else {
				// this is a link outside the toll area.
				AgentInfo info = CalcPaidToll.this.agents.get(event.agentId);
				if (info == null) {
					info = new AgentInfo();
					CalcPaidToll.this.agents.put(event.agentId, info);
				}
				info.insideCordonArea = false;
			}
		}
	}

}
