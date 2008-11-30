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

package playground.marcel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.routes.AbstractRoute;
import org.matsim.population.routes.CarRoute;

/**
 * Implementation of {@link CarRoute} which internally stores the route as a series of {@link Link}s.
 *
 * @author mrieser
 */
public class LinkCarRoute extends AbstractRoute implements CarRoute {

	private final ArrayList<Link> route = new ArrayList<Link>();
	private double travelCost = Double.NaN;

	@Override
	public double getDist() {
		double dist = super.getDist();
		if (Double.isNaN(dist)) {
			dist = 0;
			for (Link link : this.route) {
				dist += link.getLength();
			}
			this.setDist(dist);
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

	@Override
	public List<Id> getLinkIds() {
		ArrayList<Id> ids = new ArrayList<Id>(this.route.size());
		for (Link link : this.route) {
			ids.add(link.getId());
		}
		ids.trimToSize();
		return ids;
	}

	public List<Link> getLinks() {
		return Collections.unmodifiableList(this.route);
	}

	public List<Node> getNodes() {
		ArrayList<Node> nodes = new ArrayList<Node>(this.route.size() + 1);
		if (this.route.size() > 0) {
			nodes.add(this.route.get(0).getFromNode());
			for (Link link : this.route) {
				nodes.add(link.getToNode());
			}
		} else if (this.getStartLink() != this.getEndLink()) {
			nodes.add(getStartLink().getToNode());
		}
		nodes.trimToSize();
		return nodes;
	}

	public CarRoute getSubRoute(final Node fromNode, final Node toNode) {
		Link fromLink = getStartLink();
		Link toLink = getEndLink();
		int fromIndex = -1;
		int toIndex = -1;
		int max = this.route.size();
		for (int i = 0; i < max; i++) {
			Link link = this.route.get(i);
			Node node = link.getFromNode();
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
			Link link = this.route.get(i);
			if (toIndex >= 0) {
				toLink = link;
				break;
			}
			Node node = link.getToNode();
			if (node.equals(toNode)) {
				toIndex = i;
			}
		}
		if (toIndex == -1) {
			throw new IllegalArgumentException("Can't create subroute because toNode is not in the original Route");
		}
		LinkCarRoute ret = new LinkCarRoute();
		ret.setLinks(fromLink, this.route.subList(fromIndex, toIndex + 1), toLink);
		return ret;
	}

	public double getTravelCost() {
		return this.travelCost;
	}

	public void setLinks(final Link startLink, final List<Link> srcRoute, final Link endLink) {
		this.route.clear();
		setStartLink(startLink);
		setEndLink(endLink);
		if (srcRoute != null) {
			this.route.addAll(srcRoute);
		}
		this.route.trimToSize();
	}

	public void setNodes(final String route) {
		this.route.clear();
		String[] parts = route.trim().split("[ \t\n]+");

		Node prevNode = null;
		for (String id : parts) {
			if (prevNode != null) {
				// find link from prevNode to node
				for (Link link : prevNode.getOutLinks().values()) {
					if (id.equals(link.getToNode().getId().toString())) {
						this.route.add(link);
						prevNode = link.getToNode();
						break;
					}
				}
			} else {
				NetworkLayer network = (NetworkLayer)Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE);
				if (network == null) {
					throw new RuntimeException("NetworkLayer does not exist in world.");
				}
				prevNode = network.getNode(id);
			}
		}
		this.route.trimToSize();
	}
	
	public void setNodes(final Link startLink, final List<Node> srcRoute, final Link endLink) {
		setStartLink(startLink);
		setEndLink(endLink);
		setNodes(srcRoute);
	}
	
	public void setNodes(final List<Node> srcRoute) {
		this.route.clear();
		Node prevNode = null;
		for (Node node : srcRoute) {
			if (prevNode != null) {
				// find link from prevNode to node
				for (Link link : prevNode.getOutLinks().values()) {
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

	public void setNodes(final List<Node> route, final double travelTime, final double travelCost) {
		setNodes(route);
		this.setTravelTime(travelTime);
		this.travelCost = travelCost;
	}
}
