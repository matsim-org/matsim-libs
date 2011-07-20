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
	private Map<Id, Map<String,NetworkImpl>> line2Dir2Network;
	public static final String UP = "up";
	public static final String DOWN = "down";

	/**
	 * 
	 * @param net the origin Network
	 */
	public LineNetworkStore(Network net){
		this.originNet = net;
		this.line2Dir2Network = new HashMap<Id, Map<String, NetworkImpl>>();
	}
	
	public void addWay(Way w, Id line){
		Map<String, NetworkImpl> nets;
		if(line2Dir2Network.containsKey(line)){
			nets = line2Dir2Network.get(line);
		}else{
			nets = new HashMap<String, NetworkImpl>();
			nets.put(this.UP, NetworkImpl.createNetwork());
			nets.put(this.DOWN, NetworkImpl.createNetwork());
			line2Dir2Network.put(line, nets);
		}
		
		NetworkImpl net;
		for (Tag tag : w.getTags()) {
			if (tag.getKey().startsWith("matsim:backward:link-id")) {
				net = nets.get(this.DOWN);
			}else if(tag.getKey().startsWith("matsim:forward:link-id") ) {
				net = nets.get(this.UP);
			}else{
				continue;
			}
//			System.out.println(tag.getKey() + " " + tag.getValue());
			addLink(net, tag.getValue());
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
	
	public Map<Id, Map<String, NetworkImpl>> getLine2Network(){
		return this.line2Dir2Network;
	}

	/**
	 * @param lineId
	 */
	public void clean() {
		for(Map<String, NetworkImpl> nets: this.line2Dir2Network.values()){
			for(NetworkImpl n : nets.values()){
				new NetworkCleaner().run(n);
			}
		}
	}


}
