/* *********************************************************************** *
 * project: org.matsim.*
 * RiscCostFromChangeEvents.java
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

package playground.gregor.sims.riskaversion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.events.AgentMoneyEvent;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.handler.LinkEnterEventHandler;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;

public class RiskCostFromNetworkChangeEvents implements RiskCostCalculator, LinkEnterEventHandler {

	private final static double BASE_TIME = 3600 * 3;
	private final static double MAX_COST = 3600*30; //strict down hill;

	private final Map<Link,Double> linkRiskCost = new HashMap<Link, Double>();
	private boolean chargeEqualRiskLinks = false;


	public RiskCostFromNetworkChangeEvents(final NetworkLayer network, final boolean chargeEqualRiskLinks) {
		this.chargeEqualRiskLinks = chargeEqualRiskLinks;
		init(network);
	}

	private void init(final NetworkLayer network) {
		Collection<NetworkChangeEvent>  c = network.getNetworkChangeEvents();
		if (c==null) {
			throw new RuntimeException("NetworkLayer needs to be time dependent in order to calculate risk values!");
		}
		Map<Node,Double> nodeMapping = getNodeMapping(c);
		generateLinkMapping(nodeMapping);
	}

	private void generateLinkMapping(final Map<Node, Double> nodeMapping) {
		for (Entry<Node,Double> e : nodeMapping.entrySet()) {
			Node n = e.getKey();
			Double cost = e.getValue();
			
			for (Link l : n.getOutLinks().values()) {
				Node tmp = l.getToNode();
				Double nTmp = nodeMapping.get(tmp);
				if (this.chargeEqualRiskLinks) {
					if (nTmp == null || nTmp < cost) {
						continue;
					}					
				} else {
					if (nTmp == null || nTmp <= cost) {
						continue;
					}
				}

				this.linkRiskCost.put(l, nTmp * l.getLength());
			}
			
		}
		
	}

	private Map<Node,Double> getNodeMapping(final Collection<NetworkChangeEvent> c) {
		Map<Node,Double> nodeMapping = new HashMap<Node, Double>();
		for (NetworkChangeEvent e : c) {

			ChangeValue cv = e.getFreespeedChange();
			if (cv == null || cv.getValue() != 0.0 ){
				continue;
			}

			Double cost = Math.max(0,MAX_COST - (e.getStartTime() - BASE_TIME));

			for (Link link : e.getLinks()) {
				Node from = link.getFromNode();
				Node to = link.getToNode();

				Double fromCost = nodeMapping.get(from);
				if (fromCost == null){
					nodeMapping.put(from, cost);
				} else if (fromCost < cost) {
					nodeMapping.put(from, cost);
				}

				Double toCost = nodeMapping.get(to);
				if (toCost == null){
					nodeMapping.put(to, cost);
				} else if (toCost < cost) {
					nodeMapping.put(to, cost);
				}

			}
		}
		
		Collection<Node> keys = new ArrayList<Node>(nodeMapping.keySet()); 
		for (Node n : keys) {
			for (Link l : n.getInLinks().values()) {
				Node from = l.getFromNode();
				if (!nodeMapping.containsKey(from)) {
					nodeMapping.put(from, 0.);
				}
			}
		}
		
		
		return nodeMapping;
	}

	public double getLinkRisk(final Link link, final double time) {
		Double cost = this.linkRiskCost.get(link);
		if (cost == null) {
			return 0;
		}
		return cost;
	}

	public void handleEvent(final LinkEnterEvent event) {
		Double cost = this.linkRiskCost.get(event.getLink());
		if (cost == null) {
			return;
		}
		AgentMoneyEvent e = new AgentMoneyEvent(event.getTime(),event.getPersonId(),cost/-600);
		QueueSimulation.getEvents().processEvent(e);		
		
	}

	
	public void reset(final int iteration) {
		// TODO Auto-generated method stub
		
	}
}
