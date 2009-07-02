/* *********************************************************************** *
 * project: org.matsim.*
 * LinkRoute.java
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

package org.matsim.core.population.routes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;

/**
 * Implementation of {@link NetworkRoute} which internally stores the route as a series of {@link LinkImpl}s.
 *
 * @author mrieser
 */
public class LinkNetworkRoute extends AbstractRoute implements NetworkRoute {

	private final ArrayList<LinkImpl> route = new ArrayList<LinkImpl>();
	private double travelCost = Double.NaN;

	public LinkNetworkRoute(LinkImpl startLink, LinkImpl endLink){
		super(startLink, endLink);
	}
	
	@Override
	public double getDistance() {
		double dist = super.getDistance();
		if (Double.isNaN(dist)) {
			dist = 0;
			for (LinkImpl link : this.route) {
				dist += link.getLength();
			}
			this.setDistance(dist);
		}
		return dist;
	}

	@Override
	public void setLinkIds(final List<Id> linkIds) {
		NetworkLayer network = (NetworkLayer)Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE);
		if (network == null) {
			throw new RuntimeException("NetworkLayer does not exist in world.");
		}

		this.route.clear();
		for (Id id : linkIds) {
			this.route.add(network.getLink(id));
		}
		this.route.trimToSize();
	}

	public List<Id> getLinkIds() {
		ArrayList<Id> ids = new ArrayList<Id>(this.route.size());
		for (LinkImpl link : this.route) {
			ids.add(link.getId());
		}
		ids.trimToSize();
		return ids;
	}

	public List<LinkImpl> getLinks() {
		return Collections.unmodifiableList(this.route);
	}

	public List<NodeImpl> getNodes() {
		ArrayList<NodeImpl> nodes = new ArrayList<NodeImpl>(this.route.size() + 1);
		if (this.route.size() > 0) {
			nodes.add(this.route.get(0).getFromNode());
			for (LinkImpl link : this.route) {
				nodes.add(link.getToNode());
			}
		} else if (this.getStartLink() != this.getEndLink()) {
			nodes.add(getStartLink().getToNode());
		}
		nodes.trimToSize();
		return nodes;
	}

	public NetworkRoute getSubRoute(final NodeImpl fromNode, final NodeImpl toNode) {
		LinkImpl fromLink = getStartLink();
		LinkImpl toLink = getEndLink();
		int fromIndex = -1;
		int toIndex = -1;
		int max = this.route.size();
		if (fromNode == toNode) {
			boolean found = false;
			for (int i = 0; i < max; i++) {
				LinkImpl link = this.route.get(i);
				if (found) {
					toLink = link;
					break;
				}
				NodeImpl node = link.getToNode();
				if (node.equals(fromNode)) {
					found = true;
					fromIndex = 0; // value doesn't really matter, just >= 0
					fromLink = link;
				}
			}
			if (fromIndex == -1) {
				if (fromNode.equals(getStartLink().getToNode())) {
					fromIndex = 0;
					fromLink = getStartLink();
					if (this.route.size() > 0) {
						toLink = this.route.get(0);
					}
				} else {
					throw new IllegalArgumentException("Can't create subroute because fromNode is not in the original Route");
				}
			}
		} else {
			for (int i = 0; i < max; i++) {
				LinkImpl link = this.route.get(i);
				NodeImpl node = link.getFromNode();
				if (node.equals(fromNode)) {
					fromIndex = i;
					break;
				}
				fromLink = link;
			}
			if (fromIndex == -1) {
				throw new IllegalArgumentException("Can't create subroute because fromNode is not in the original Route");
			}
			for (int i = fromIndex; i < max; i++) {
				LinkImpl link = this.route.get(i);
				if (toIndex >= 0) {
					toLink = link;
					break;
				}
				NodeImpl node = link.getToNode();
				if (node.equals(toNode)) {
					toIndex = i;
				}
			}
			if (toIndex == -1) {
				throw new IllegalArgumentException("Can't create subroute because toNode is not in the original Route");
			}
		}
		LinkNetworkRoute ret = new LinkNetworkRoute(fromLink, toLink);
		if (toIndex >= fromIndex) {
			ret.setLinks(fromLink, this.route.subList(fromIndex, toIndex + 1), toLink);
		} else {
			ret.setLinks(fromLink, null, toLink);
		}
		return ret;
	}

	public double getTravelCost() {
		return this.travelCost;
	}

	public void setTravelCost(final double travelCost) {
		this.travelCost = travelCost;
	}

	public void setLinks(final LinkImpl startLink, final List<LinkImpl> srcRoute, final LinkImpl endLink) {
		this.route.clear();
		setStartLink(startLink);
		setEndLink(endLink);
		if (srcRoute != null) {
			this.route.addAll(srcRoute);
		}
		this.route.trimToSize();
	}

	public void setNodes(final LinkImpl startLink, final List<NodeImpl> srcRoute, final LinkImpl endLink) {
		setStartLink(startLink);
		setEndLink(endLink);
		setNodes(srcRoute);
	}

	public void setNodes(final List<NodeImpl> srcRoute) {
		this.route.clear();
		NodeImpl prevNode = null;
		for (NodeImpl node : srcRoute) {
			if (prevNode != null) {
				// find link from prevNode to node
				for (LinkImpl link : prevNode.getOutLinks().values()) {
					if (link.getToNode().equals(node)) {
						this.route.add(link);
						break;
					}
				}
			}
			prevNode = node;
		}
		this.route.trimToSize();
	}

}
