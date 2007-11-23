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
public class CalcPaidToll implements EventHandlerLinkEnterI, EventHandlerAgentWait2LinkI {

	static private class AgentInfo {
		public double toll = 0.0;
		public boolean insideCordonArea = true;
	}

	private TreeMap<String, AgentInfo> agents = new TreeMap<String, AgentInfo>();
	private NetworkLayer network;

	private RoadPricingScheme scheme = null;
	private TollBehaviourI handler = null;

	public CalcPaidToll(NetworkLayer network, RoadPricingScheme scheme) {
		super();
		this.network = network;
		this.scheme = scheme;
		if (scheme.getType() == "distance") this.handler = new DistanceTollBehaviour();
		else if (scheme.getType() == "area") this.handler = new AreaTollBehaviour();
		else if (scheme.getType() == "cordon") this.handler = new CordonTollBehaviour();
		else throw new IllegalArgumentException("RoadPricingScheme of type \"" + scheme + "\" is not supported.");
	}

	public void handleEvent(EventLinkEnter event) {
		Link link = event.link;
		if (link == null) {
			link = (Link)this.network.getLocation(event.linkId);
		}
		this.handler.handleEvent(event, link);
	}

	public void handleEvent(EventAgentWait2Link event) {
		Link link = event.link;
		if (link == null) {
			link = (Link)this.network.getLocation(event.linkId);
		}
		this.handler.handleEvent(event, link);
	}

	public void reset(int iteration) {
		this.agents.clear();
	}

	/**
	 * Returns the toll the specified agent has paid in the course of the simulation so far.
	 *
	 * @param agentId
	 * @return The toll paid by the specified agent, 0.0 if no toll was paid.
	 */
	public double getAgentToll(String agentId) {
		AgentInfo info = this.agents.get(agentId);
		if (info == null) {
			return 0.0;
		}
		return info.toll;
	}

	/**
	 * A simple interface to implement different toll schemes.
	 */
	private interface TollBehaviourI {
		public void handleEvent(BasicEvent event, Link link);
	}

	private class DistanceTollBehaviour implements TollBehaviourI {
		public void handleEvent(BasicEvent event, Link link) {
			Cost cost = CalcPaidToll.this.scheme.getLinkCost(link.getId(), event.time);
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

	private class AreaTollBehaviour implements TollBehaviourI {
		public void handleEvent(BasicEvent event, Link link) {
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

	private class CordonTollBehaviour implements TollBehaviourI {
		public void handleEvent(BasicEvent event, Link link) {
			Cost cost = CalcPaidToll.this.scheme.getLinkCost(link.getId(), event.time);
			if (cost != null) {
				AgentInfo info = CalcPaidToll.this.agents.get(event.agentId);
				if (info == null) {
					info = new AgentInfo();
					CalcPaidToll.this.agents.put(event.agentId, info);
					info.toll = 0.0; // we start in the area, do not toll
				} else if (!info.insideCordonArea) {
					info.insideCordonArea = true;
					info.toll += cost.amount;
				}
			} else {
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
