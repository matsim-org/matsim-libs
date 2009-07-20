/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterNetworkWrapper.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.marcel.pt.router;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.network.BasicLink;
import org.matsim.api.basic.v01.network.BasicNode;
import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.api.experimental.network.Network;
import org.matsim.core.api.experimental.network.NetworkBuilder;
import org.matsim.core.api.experimental.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.world.Layer;

/**
 * @author mrieser
 */
public class TransitRouterNetworkWrapper implements Network {

	/*package*/static final Set<TransportMode> allowedModes = EnumSet.of(TransportMode.pt);

	private final TransitRouterNetwork transitNetwork;
	private final Map<TransitRouterNetwork.TransitRouterNetworkNode, NodeWrapper> nodesLookup =
		new HashMap<TransitRouterNetwork.TransitRouterNetworkNode, NodeWrapper>();
	private final Map<TransitRouterNetwork.TransitRouterNetworkLink, LinkWrapper> linksLookup =
		new HashMap<TransitRouterNetwork.TransitRouterNetworkLink, LinkWrapper>();
	private final Map<Id, TransitRouterNetworkWrapper.NodeWrapper> nodes = new LinkedHashMap<Id, TransitRouterNetworkWrapper.NodeWrapper>();
	private final Map<Id, TransitRouterNetworkWrapper.LinkWrapper> links = new LinkedHashMap<Id, TransitRouterNetworkWrapper.LinkWrapper>();

	private long nextNodeId = 0;
	private long nextLinkId = 0;

	public TransitRouterNetworkWrapper(final TransitRouterNetwork transitNetwork) {
		this.transitNetwork = transitNetwork;
		for (TransitRouterNetwork.TransitRouterNetworkNode node : this.transitNetwork.getNodes()) {
			NodeWrapper node2 = new NodeWrapper(node, new IdImpl(this.nextNodeId++));
			this.nodesLookup.put(node, node2);
			this.nodes.put(node2.getId(), node2);
		}
		for (TransitRouterNetwork.TransitRouterNetworkLink link : this.transitNetwork.getLinks()) {
			LinkWrapper link2 = new LinkWrapper(link, new IdImpl(this.nextLinkId++));
			this.linksLookup.put(link, link2);
			this.links.put(link2.getId(), link2);
		}
	}

	public Map<Id, Link> getLinks() {
		return (Map) this.links;
	}

	public Map<Id, Node> getNodes() {
		return (Map) this.nodes;
	}

	/*package*/ NodeWrapper getWrappedNode(final TransitRouterNetwork.TransitRouterNetworkNode node) {
		return this.nodesLookup.get(node);
	}

	/*package*/ LinkWrapper getWrappedLink(final TransitRouterNetwork.TransitRouterNetworkLink link) {
		return this.linksLookup.get(link);
	}

	public double getCapacityPeriod() {
		return 3600.0;
	}

	public NetworkBuilder getBuilder() {
		return null;
	}

	public double getEffectiveLaneWidth() {
		return 3;
	}

	/*package*/ class NodeWrapper implements Node {
		final TransitRouterNetwork.TransitRouterNetworkNode node;
		final Id id;

		public NodeWrapper(final TransitRouterNetwork.TransitRouterNetworkNode node, final Id id) {
			this.node = node;
			this.id = id;
		}

		public Map<Id, ? extends LinkImpl> getInLinks() {
			throw new UnsupportedOperationException();
		}

		public Map<Id, ? extends Link> getOutLinks() {
			Map<Id, Link> links = new LinkedHashMap<Id, Link>(this.node.outgoingLinks.size());
			for (TransitRouterNetwork.TransitRouterNetworkLink link : this.node.outgoingLinks) {
				LinkWrapper wrapped = getWrappedLink(link);
				links.put(wrapped.id, wrapped);
			}
			return links;
		}

		public boolean addInLink(final BasicLink link) {
			throw new UnsupportedOperationException();
		}

		public boolean addOutLink(final BasicLink link) {
			throw new UnsupportedOperationException();
		}

		public Coord getCoord() {
			return this.node.stop.getStopFacility().getCoord();
		}

		public Id getId() {
			return this.id;
		}

	}

	/*package*/ class LinkWrapper implements Link {

		final TransitRouterNetwork.TransitRouterNetworkLink link;
		final Id id;

		public LinkWrapper(final TransitRouterNetwork.TransitRouterNetworkLink link, final Id id) {
			this.link = link;
			this.id = id;
		}

		public Node getFromNode() {
			return getWrappedNode(this.link.fromNode);
		}

		public Node getToNode() {
			return getWrappedNode(this.link.toNode);
		}

		public double getCapacity(final double time) {
			return 9999;
		}

		public double getFreespeed(final double time) {
			return 10;
		}

		public Id getId() {
			return this.id;
		}

		public double getNumberOfLanes(final double time) {
			return 1;
		}

		public double getLength() {
			return CoordUtils.calcDistance(this.link.toNode.stop.getStopFacility().getCoord(), this.link.fromNode.stop.getStopFacility().getCoord());
		}

		public void setCapacity(final double capacity) {
			throw new UnsupportedOperationException();
		}

		public void setFreespeed(final double freespeed) {
			throw new UnsupportedOperationException();
		}

		public boolean setFromNode(final BasicNode node) {
			throw new UnsupportedOperationException();
		}

		public void setNumberOfLanes(final double lanes) {
			throw new UnsupportedOperationException();
		}

		public void setLength(final double length) {
			throw new UnsupportedOperationException();
		}

		public boolean setToNode(final BasicNode node) {
			throw new UnsupportedOperationException();
		}

		public Layer getLayer() {
			throw new UnsupportedOperationException();
		}

		public Coord getCoord() {
			throw new UnsupportedOperationException();
		}

		public Set<TransportMode> getAllowedModes() {
			return allowedModes;
		}

		public void setAllowedModes(final Set<TransportMode> modes) {
			throw new UnsupportedOperationException();
		}
	}

}
