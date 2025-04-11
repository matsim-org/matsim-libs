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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinksUtils;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;

/**
 * Add several filter instances to this class and create a new
 * network containing only the links and nodes that passed
 * the filters.
 *
 * Note that the Network instance created is connected, i.e. in- and outlink references are set 
 * to links of the filtered network.
 * 
 * @author dgrether
 */
public final class NetworkFilterManager {

	private static final Logger log = LogManager.getLogger(NetworkFilterManager.class);

	private final Network network;

	private final List<NetworkLinkFilter> linkFilters;

	private final List<NetworkNodeFilter> nodeFilters;
	
	private final NetworkConfigGroup networkConfigGroup;

	public NetworkFilterManager(final Network net, NetworkConfigGroup networkConfigGroup) {
		this.network = net;
		this.networkConfigGroup = networkConfigGroup;
		this.linkFilters = new ArrayList<>();
		this.nodeFilters = new ArrayList<>();
	}

	public void addLinkFilter(NetworkLinkFilter f) {
		this.linkFilters.add(f);
	}

	public void addNodeFilter(NetworkNodeFilter f) {
		this.nodeFilters.add(f);
	}

	private Node addNode(Network net, Node n){
		Node newNode = net.getFactory().createNode(n.getId(), n.getCoord());
		net.addNode(newNode);
		return newNode;
	}
	
	private void addLink(Network net, Link l){
		Id<Node> fromId = l.getFromNode().getId();
		Id<Node> toId = l.getToNode().getId();
		Node from;
		Node to;
		Node nn;
		//check if from node already exists
		if (! net.getNodes().containsKey(fromId)) {
			nn = this.network.getNodes().get(fromId);
			from = this.addNode(net, nn);
		}
		else {
			from = net.getNodes().get(fromId);
		}
		//check if to node already exists
		if (! net.getNodes().containsKey(toId)){
			nn = this.network.getNodes().get(toId);
			to = this.addNode(net, nn);
		}
		else {
			to = net.getNodes().get(toId);
		}
		Link ll = net.getFactory().createLink(l.getId(), from, to);
		ll.setAllowedModes(l.getAllowedModes());
		ll.setCapacity(l.getCapacity());
		ll.setFreespeed(l.getFreespeed());
		ll.setLength(l.getLength());
		ll.setNumberOfLanes(l.getNumberOfLanes());
		AttributesUtils.copyAttributesFromTo(l, ll);
		DisallowedNextLinks disallowedNextLinks = NetworkUtils.getDisallowedNextLinks(l);
		if (disallowedNextLinks != null) {
			NetworkUtils.setDisallowedNextLinks(ll, disallowedNextLinks.copy());
		}
		net.addLink(ll);
	}
	
	/**
	 * Call this method to filter the network.
	 * @return
	 */
	public Network applyFilters() {
		log.info("applying filters to network with " + network.getNodes().size() + " nodes and "
				+ network.getLinks().size() + " links...");
		Network net = NetworkUtils.createNetwork(networkConfigGroup);
		if (!this.nodeFilters.isEmpty()) {
			for (Node n : this.network.getNodes().values()) {
				if (nodeFilters.stream().allMatch(f -> f.judgeNode(n))) {
					this.addNode(net, n);
				}
			}
		}
		if (!this.linkFilters.isEmpty()) {
			for (Link l : this.network.getLinks().values()) {
				if (linkFilters.stream().allMatch(f -> f.judgeLink(l))) {
					this.addLink(net, l);
				}
			}
			// if not all links are copied, ensure DisallowedNextLinks are valid
			DisallowedNextLinksUtils.clean(net);
		}
		log.info("filtered network contains " + net.getNodes().size() + " nodes and " + net.getLinks().size() + " links.");
		return net;
	}

}
