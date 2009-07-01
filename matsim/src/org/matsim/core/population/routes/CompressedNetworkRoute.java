/* *********************************************************************** *
 * project: org.matsim.*
 * CompressedRoute.java
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;

/**
 * Implementation of {@link NetworkRoute} that tries to minimize the amount of
 * data needed to be stored for each route. This will give some memory savings,
 * allowing for larger scenarios (=more agents), especially on detailed
 * networks, but is likely a bit slower due to the more complex access of the
 * route information internally.
 *
 * @author mrieser
 */
public class CompressedNetworkRoute extends AbstractRoute implements NetworkRoute {

	private final static Logger log = Logger.getLogger(CompressedNetworkRoute.class);
	
	private final ArrayList<LinkImpl> route = new ArrayList<LinkImpl>(0);
	private final Map<LinkImpl, LinkImpl> subsequentLinks;
	private double travelCost = Double.NaN;
	/** number of links in uncompressed route */
	private int uncompressedLength = -1;
	private int modCount = 0;
	private int routeModCountState = 0;

	public CompressedNetworkRoute(LinkImpl startLink, LinkImpl endLink, final Map<LinkImpl, LinkImpl> subsequentLinks) {
		super(startLink, endLink);
		this.subsequentLinks = subsequentLinks;
	}

	public List<LinkImpl> getLinks() {
		if (this.uncompressedLength < 0) { // it seems the route never got initialized correctly
			return new ArrayList<LinkImpl>(0);
		}
		ArrayList<LinkImpl> links = new ArrayList<LinkImpl>(this.uncompressedLength);
		if (this.modCount != this.routeModCountState) {
			log.error("Route was modified after storing it! modCount=" + this.modCount + " routeModCount=" + this.routeModCountState);
			return links;
		}
		LinkImpl previousLink = getStartLink();
		LinkImpl endLink = getEndLink();
		if (previousLink == endLink) {
			return links;
		}
		for (LinkImpl link : this.route) {
			getLinksTillLink(links, link, previousLink);
			links.add(link);
			previousLink = link;
		}
		getLinksTillLink(links, endLink, previousLink);

		return links;
	}

	private void getLinksTillLink(final List<LinkImpl> links, final LinkImpl nextLink, final LinkImpl startLink) {
		LinkImpl link = startLink;
		while (true) { // loop until we hit "return;"
			for (LinkImpl outLink : link.getToNode().getOutLinks().values()) {
				if (outLink == nextLink) {
					return;
				}
			}
			link = this.subsequentLinks.get(link);
			links.add(link);
		}
	}

	@Override
	public void setEndLink(LinkImpl link) {
		this.modCount++;
		super.setEndLink(link);
	}
	
	@Override
	public void setStartLink(LinkImpl link) {
		this.modCount++;
		super.setStartLink(link);
	}

	public List<Id> getLinkIds() {
		List<LinkImpl> links = getLinks();
		List<Id> ids = new ArrayList<Id>(links.size());
		for (LinkImpl link : links) {
			ids.add(link.getId());
		}
		return ids;
	}

	public List<NodeImpl> getNodes() {
		if (this.uncompressedLength < 0) { // it seems the route never got initialized correctly
			return new ArrayList<NodeImpl>(0);
		}
		ArrayList<NodeImpl> nodes = new ArrayList<NodeImpl>(this.uncompressedLength + 1);
		if (this.modCount != this.routeModCountState) {
			log.error("Route was modified after storing it! modCount=" + this.modCount + " routeModCount=" + this.routeModCountState);
			return nodes;
		}

		LinkImpl startLink = getStartLink();
		LinkImpl endLink = getEndLink();

		if (startLink == endLink) {
			return nodes;
		}

		LinkImpl previousLink = startLink;
		for (LinkImpl link : this.route) {
			getNodesTillLink(nodes, link, previousLink);
			previousLink = link;
		}
		getNodesTillLink(nodes, endLink, previousLink);

		return nodes;
	}

	private void getNodesTillLink(final List<NodeImpl> nodes, final LinkImpl nextLink, final LinkImpl startLink) {
		LinkImpl link = startLink;
		do {
			NodeImpl node = link.getToNode();
			nodes.add(node);
			for (LinkImpl outLink : node.getOutLinks().values()) {
				if (nextLink == outLink) {
					return;
				}
			}
			// nextLink is not an outgoing link, so continue with the subsequent link
			link = this.subsequentLinks.get(link);
		} while (true);

	}

	public NetworkRoute getSubRoute(final NodeImpl fromNode, final NodeImpl toNode) {
		LinkImpl newStartLink = null;
		LinkImpl newEndLink = null;
		List<LinkImpl> newLinks = new ArrayList<LinkImpl>(10);

		LinkImpl startLink = getStartLink();
		if (startLink.getToNode() == fromNode) {
			newStartLink = startLink;
		}
		for (LinkImpl link : getLinks()) {
			if (link.getFromNode() == toNode) {
				newEndLink = link;
				break;
			}
			if (newStartLink != null) {
				newLinks.add(link);
			}
			if (link.getToNode() == fromNode) {
				newStartLink = link;
			}
		}
		if (newStartLink == null) {
			throw new IllegalArgumentException("fromNode is not part of this route.");
		}
		if (newEndLink == null) {
			if (getEndLink().getFromNode() == toNode) {
				newEndLink = getEndLink();
			} else {
				throw new IllegalArgumentException("toNode is not part of this route.");
			}
		}

		NetworkRoute subRoute = new CompressedNetworkRoute(newStartLink, newEndLink, this.subsequentLinks);
		subRoute.setLinks(newStartLink, newLinks, newEndLink);
		return subRoute;
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
		this.routeModCountState = this.modCount;
		if ((srcRoute == null) || (srcRoute.size() == 0)) {
			this.uncompressedLength = 0;
			return;
		}
		LinkImpl previousLink = startLink;
		for (LinkImpl link : srcRoute) {
			if (!this.subsequentLinks.get(previousLink).equals(link)) {
				this.route.add(link);
			}
			previousLink = link;
		}
		this.route.trimToSize();
		this.uncompressedLength = srcRoute.size();
//		System.out.println("uncompressed size: \t" + this.uncompressedLength + "\tcompressed size: \t" + this.route.size());
	}

	@Deprecated
	public void setNodes(final List<NodeImpl> srcRoute) {
		setNodes(null, srcRoute, null);
	}

	public void setNodes(final LinkImpl startLink, final List<NodeImpl> srcRoute, final LinkImpl endLink) {
		this.route.clear();
		if (startLink != null) {
			setStartLink(startLink);
		}
		if (endLink != null) {
			setEndLink(endLink);
		}
		this.routeModCountState = this.modCount;
		LinkImpl previousLink = getStartLink();
		NodeImpl previousNode = previousLink.getToNode();
		Iterator<NodeImpl> iter = srcRoute.iterator();
		if (iter.hasNext()) {
			iter.next(); // ignore the first part, it should be the same as previousNode
		} else {
			// empty srcRoute, nothing else to do than a check
			// check that this route is complete, so it will be possible to decompress it again without problems
			if ((startLink != endLink) && (startLink.getToNode() != endLink.getFromNode())) {
				throw new IllegalArgumentException("The last node must be the fromNode of the endLink. endLink=" + endLink.getId());
			}

			this.uncompressedLength = 0;
			return;
		}
		NodeImpl node = getStartLink().getToNode();
		while (iter.hasNext()) {
			node = iter.next();
			// find link from prevNode to node
			LinkImpl link = null;
			for (LinkImpl tmpLink : previousNode.getOutLinks().values()) {
				if (node == tmpLink.getToNode()) {
					link = tmpLink;
					break;
				}
			}
			if (link == null) {
				throw new IllegalArgumentException("Could not find any link from node " + previousNode.getId() + " to " + node.getId());
			}
			LinkImpl subsequent = this.subsequentLinks.get(previousLink);
			if (subsequent != link) {
				this.route.add(link);
			}
			previousLink = link;
			previousNode = link.getToNode();
		}

		// check that this route is complete, so it will be possible to uncompress it again without problems
		if (node != getEndLink().getFromNode()) {
			throw new IllegalArgumentException("The last node must be the fromNode of the endLink. endLink=" + endLink.getId());
		}
		
		this.route.trimToSize();
		this.uncompressedLength = srcRoute.size() - 1;
//		System.out.println("uncompressed size: \t" + this.uncompressedLength + "\tcompressed size: \t" + this.route.size());
	}

	@Override
	public double getDistance() {
		double dist = super.getDistance();
		if (Double.isNaN(dist)) {
			dist = calcDistance();
		}
		return dist;
	}

	private double calcDistance() {
		if (this.modCount != this.routeModCountState) {
			log.error("Route was modified after storing it! modCount=" + this.modCount + " routeModCount=" + this.routeModCountState);
			return 99999.999;
		}
		double dist = 0;
		for (LinkImpl link : getLinks()) {
			dist += link.getLength();
		}
		setDistance(dist);
		return dist;
	}

}
