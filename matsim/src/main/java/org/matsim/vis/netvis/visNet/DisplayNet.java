/* *********************************************************************** *
 * project: org.matsim.*
 * DisplayNet.java
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

package org.matsim.vis.netvis.visNet;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.netvis.DisplayableLinkI;
import org.matsim.vis.netvis.DisplayableNetI;

/**
 * @author gunnar
 *
 */
public class DisplayNet implements Network, DisplayableNetI {

	// -------------------- MEMBER VARIABLES --------------------

	private double minEasting;
	private double maxEasting;
	private double minNorthing;
	private double maxNorthing;

	private final Map<Id, DisplayNode> nodes = new TreeMap<Id, DisplayNode>();
	private final Map<Id, DisplayLink> links = new TreeMap<Id, DisplayLink>();
	private final double capacityPeriod;

	// -------------------- CONSTRUCTION --------------------

	public DisplayNet(final NetworkLayer layer) {
		this.capacityPeriod = layer.getCapacityPeriod();
		// first create nodes
		for (Node node : layer.getNodes().values()) {
			DisplayNode node2 = new DisplayNode(node.getId(), this);
			node2.setCoord(((NodeImpl) node).getCoord());
			this.nodes.put(node2.getId(), node2);
		}

		// second, create links
		for (Link link : layer.getLinks().values()) {
			DisplayLink link2 = new DisplayLink(link.getId(), this);

			Node from = this.getNodes().get(link.getFromNode().getId());
			from.addOutLink(link2);
			link2.setFromNode(from);

			Node to = this.getNodes().get(link.getToNode().getId());
			to.addInLink(link2);
			link2.setToNode(to);

			link2.setLength_m(((LinkImpl) link).getLength());
			link2.setNumberOfLanes(NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, link));

			this.links.put(link2.getId(), link2);
		}

		// third, build/complete the network
		this.build();
	}

	@Override
	public double getCapacityPeriod() {
		return this.capacityPeriod;
	}

	// -------------------- IMPLEMENTATION OF BasicNetworkI --------------------

	@Override
	public Map<Id, ? extends DisplayNode> getNodes() {
		return this.nodes;
	}

	@Override
	public Map<Id, ? extends DisplayableLinkI> getLinks() {
		return this.links;
	}

	// -------------------- OVERRIDING OF TrafficNet --------------------

	public void build() {
		for (DisplayableLinkI link : getLinks().values()) {
			link.build();
		}

		this.minEasting = Double.POSITIVE_INFINITY;
		this.maxEasting = Double.NEGATIVE_INFINITY;
		this.minNorthing = Double.POSITIVE_INFINITY;
		this.maxNorthing = Double.NEGATIVE_INFINITY;

		for (DisplayNode node : getNodes().values()) {
			this.minEasting = Math.min(this.minEasting, node.getEasting());
			this.maxEasting = Math.max(this.maxEasting, node.getEasting());
			this.minNorthing = Math.min(this.minNorthing, node.getNorthing());
			this.maxNorthing = Math.max(this.maxNorthing, node.getNorthing());
		}
	}

	@Override
	public double minEasting() {
		return this.minEasting;
	}

	@Override
	public double maxEasting() {
		return this.maxEasting;
	}

	@Override
	public double minNorthing() {
		return this.minNorthing;
	}

	@Override
	public double maxNorthing() {
		return this.maxNorthing;
	}

	@Override
	public NetworkFactory getFactory() {
		throw new UnsupportedOperationException("Not available in this class");
	}

	@Override
	public double getEffectiveLaneWidth() {
		throw new UnsupportedOperationException("Not available in this class");
	}

	@Override
	public void addLink(Link ll) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addNode(Node nn) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Link removeLink(Id linkId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node removeNode(Id nodeId) {
		throw new UnsupportedOperationException();
	}

}