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

package playground.gregor.risk;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.AgentMoneyEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.network.NetworkChangeEvent;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkChangeEvent.ChangeValue;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.utils.geometry.transformations.TransformationFactory;

public class RiskCostCalculator implements LinkEnterEventHandler {

	private final static double BASE_TIME = 3600 * 3;
	private final static double MAX_COST = 3600*30; //strict down hill;

	//TODO which one is better?
	private final Map<Link,Double> linkRiskCost = new HashMap<Link, Double>();
	private final Map<String,Double> linkRiskCostII = new HashMap<String, Double>(); 


	public RiskCostCalculator(final NetworkLayer network) {
		init(network);
	}

	private void init(final NetworkLayer network) {
		Collection<NetworkChangeEvent>  c = network.getNetworkChangeEvents();
		if (c==null) {
			throw new RuntimeException("NetworkLayer needs to be time dependent in order to calculate risk values!");
		}
		Map<Node,Double> nodeMapping = getNodeMapping(c);
		generateLinkMapping(nodeMapping);
		new NodeCostShapeCreator(this.linkRiskCost,MGC.getCRS(TransformationFactory.WGS84_UTM47S));
//		throw new RuntimeException("");
	}

	private void generateLinkMapping(final Map<Node, Double> nodeMapping) {
		for (Entry<Node,Double> e : nodeMapping.entrySet()) {
			Node n = e.getKey();
			Double cost = e.getValue();
			
			for (Link l : n.getOutLinks().values()) {
				Node tmp = l.getToNode();
				Double nTmp = nodeMapping.get(tmp);
				if (nTmp == null || nTmp <= cost) {
					continue;
				}
				//TODO which one is better?
				this.linkRiskCost.put(l, cost);
				this.linkRiskCostII.put(l.getId().toString(), cost);
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
		Double cost = this.linkRiskCostII.get(event.linkId);
		if (cost == null) {
			return;
		}
		Id id = new IdImpl(event.agentId);
		AgentMoneyEvent e = new AgentMoneyEvent(event.getTime(),id,cost/-600);
		QueueSimulation.getEvents().processEvent(e);		
		
	}

	public void reset(final int iteration) {
		// TODO Auto-generated method stub
		
	}
}
