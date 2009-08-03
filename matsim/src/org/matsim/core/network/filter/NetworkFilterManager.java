/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.network.filter;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.matsim.api.basic.v01.network.BasicNetwork;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;

/**
 * Add several filter instances to this class and create a new 
 * network containing only the links and nodes that passed 
 * the filters.
 * 
 * @author dgrether
 */
public class NetworkFilterManager {
	
	private static final Logger log = Logger
			.getLogger(NetworkFilterManager.class);
	
	private final BasicNetwork<NodeImpl, LinkImpl> network;
	
	private List<NetworkLinkFilter> linkFilters;
	
	private List<NetworkNodeFilter> nodeFilters;
	
	public NetworkFilterManager(final BasicNetwork<NodeImpl, LinkImpl> net) {
		this.network = net;
		this.linkFilters = new ArrayList<NetworkLinkFilter>();
		this.nodeFilters = new ArrayList<NetworkNodeFilter>();
	}
	
	public void addLinkFilter(NetworkLinkFilter f) {
		this.linkFilters.add(f);
	}
	
	public void addNodeFilter(NetworkNodeFilter f) {
		this.nodeFilters.add(f);
	}
	
	/**
	 * Call this method to filter the network.
	 * @return
	 */
	public BasicNetwork<Node, Link> applyFilters() {
		log.info("applying filters to network...");
		int nodeCount = 0;
		int linkCount = 0;
		NetworkLayer net = new NetworkLayer();
		if (!this.nodeFilters.isEmpty()) {
			for (NodeImpl n : this.network.getNodes().values()) {
				boolean add = true;
				for (NetworkNodeFilter f : nodeFilters) {
					if (!f.judgeNode(n)) {
						add = false;
						break;
					}
				}
				if (add) {
					net.getNodes().put(n.getId(), n);
					nodeCount++;
				}
			}
		}
		if (!this.linkFilters.isEmpty()) {
			for (LinkImpl l : this.network.getLinks().values()) {
				boolean add = true;
				for (NetworkLinkFilter f : linkFilters) {
					if (!f.judgeLink(l)) {
						add = false;
						break;
					}
				}
				if (add) {
					NodeImpl from = l.getFromNode();
					NodeImpl to = l.getToNode();
					if (!net.getNodes().containsKey(from.getId())) {
						net.getNodes().put(from.getId(), from);
					}
					if (!net.getNodes().containsKey(to.getId())){
						net.getNodes().put(to.getId(), to);
					}
					net.getLinks().put(l.getId(), l);
					linkCount++;
				}
			}			
		}
		log.info("filtered " + nodeCount + " of " + network.getNodes().size() + " nodes...");
		log.info("filtered " + linkCount + " of " + network.getLinks().size() + " links.");
		return net;
	}
	
}
