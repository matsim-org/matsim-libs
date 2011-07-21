/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.osm;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math.analysis.solvers.NewtonSolver;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * @author droeder
 *
 */
public class LineNetworkStore {
	private static final Logger log = Logger.getLogger(LineNetworkStore.class);
	
	private Network originNet;
	private HashMap<Id, NetworkImpl> line2Network;
	public static final String UP = "up";
	public static final String DOWN = "down";

	/**
	 * 
	 * @param net the origin Network
	 */
	public LineNetworkStore(Network net){
		this.originNet = net;
		this.line2Network = new HashMap<Id, NetworkImpl>();
	}
	
	public void addWay(Way w, Id line){
		NetworkImpl net;
		if(line2Network.containsKey(line)){
			net = line2Network.get(line);
		}else{
			net = NetworkImpl.createNetwork();
			line2Network.put(line, net);
		}
		
		for (Tag tag : w.getTags()) {
			if (tag.getKey().startsWith("matsim:backward:link-id") || 
					tag.getKey().startsWith("matsim:forward:link-id")) {
				addLink(net, tag.getValue());
			}
				//			System.out.println(tag.getKey() + " " + tag.getValue());
		}
	}

	private void addLink(NetworkImpl net, String value) {
		LinkImpl l = (LinkImpl) this.originNet.getLinks().get(new IdImpl(value));
		Node from = l.getFromNode();
		Node to = l.getToNode();
		
		if(!net.getNodes().containsKey(from.getId())){
			from = net.createAndAddNode(from.getId(), from.getCoord());
		}else{
			from = net.getNodes().get(from.getId());
		}
		if(!net.getNodes().containsKey(to.getId())){
			to = net.createAndAddNode(to.getId(), to.getCoord());
		}else{
			to = net.getNodes().get(to.getId());
		}
		if(!net.getLinks().containsKey(l.getId())){
			net.createAndAddLink(l.getId(), from, to, l.getLength(), l.getFreespeed(), 
					l.getCapacity(), l.getNumberOfLanes(), l.getOrigId(), l.getType());
		}
	}
	
	public Map<Id, NetworkImpl> getLine2Network(){
		return this.line2Network;
	}
}
