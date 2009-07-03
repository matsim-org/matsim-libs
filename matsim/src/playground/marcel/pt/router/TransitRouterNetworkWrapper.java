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

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.CoordUtils;

public class TransitRouterNetworkWrapper extends NetworkLayer { //implements BasicNetwork<TransitRouterNetworkWrapper.NodeWrapper, TransitRouterNetworkWrapper.LinkWrapper> {

	/*package*/static final Set<TransportMode> allowedModes = EnumSet.of(TransportMode.pt);

	private final TransitRouterNetwork transitNetwork;
	private final Map<TransitRouterNetwork.TransitRouterNetworkNode, NodeWrapper> nodesLookup =
		new HashMap<TransitRouterNetwork.TransitRouterNetworkNode, NodeWrapper>();
	private final Map<TransitRouterNetwork.TransitRouterNetworkLink, LinkWrapper> linksLookup =
		new HashMap<TransitRouterNetwork.TransitRouterNetworkLink, LinkWrapper>();
//	private final Map<Id, TransitRouterNetworkWrapper.NodeWrapper> nodes = new LinkedHashMap<Id, TransitRouterNetworkWrapper.NodeWrapper>();
//	private final Map<Id, TransitRouterNetworkWrapper.LinkWrapper> links = new LinkedHashMap<Id, TransitRouterNetworkWrapper.LinkWrapper>();
	private final Map<Id, NodeImpl> nodes = new LinkedHashMap<Id, NodeImpl>();
	private final Map<Id, LinkImpl> links = new LinkedHashMap<Id, LinkImpl>();

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

	public LinkImpl getLink(final Id linkId) {
		return this.links.get(linkId);
	}

	public NodeImpl getNode(final Id id) {
		return this.nodes.get(id);
	}

	public NodeImpl getNearestNode(final Coord coord) {
		throw new UnsupportedOperationException();
	}

	public Collection<NodeImpl> getNearestNodes(final Coord coord, final double distance) {
		throw new UnsupportedOperationException();
	}

	public Map<Id, LinkImpl> getLinks() {
		return this.links;
	}

	public Map<Id, NodeImpl> getNodes() {
		return this.nodes;
	}

	/*package*/ NodeWrapper getWrappedNode(final TransitRouterNetwork.TransitRouterNetworkNode node) {
		return this.nodesLookup.get(node);
	}

	/*package*/ LinkWrapper getWrappedLink(final TransitRouterNetwork.TransitRouterNetworkLink link) {
		return this.linksLookup.get(link);
	}
//
//	public double getCapacityPeriod() {
//		return 3600.0;
//	}
//
//	public double getEffectiveCellSize() {
//		return 7.5;
//	}
//
//	public double getEffectiveLaneWidth() {
//		return 3;
//	}
//
//	public NetworkFactory getFactory() {
//		throw new UnsupportedOperationException();
//	}
//	
//	public NetworkBuilder getBuilder() {
//		throw new UnsupportedOperationException();
//	}
//
//	public String getName() {
//		return "TransitRouterNetworkWrapper";
//	}
//
//	public boolean removeLink(final LinkImpl link) {
//		throw new UnsupportedOperationException();
//	}
//
//	public boolean removeNode(final NodeImpl node) {
//		throw new UnsupportedOperationException();
//	}
//
//	public void setCapacityPeriod(final double capPeriod) {
//		throw new UnsupportedOperationException();
//	}
//
//	public void setEffectiveCellSize(final double effectiveCellSize) {
//		throw new UnsupportedOperationException();
//	}
//
//	public void setEffectiveLaneWidth(final double effectiveLaneWidth) {
//		throw new UnsupportedOperationException();
//	}
//
//	public void connect() {
//		throw new UnsupportedOperationException();
//	}


	/*package*/ class NodeWrapper extends NodeImpl { //implements Node {
		final TransitRouterNetwork.TransitRouterNetworkNode node;
		final Id id;

		public NodeWrapper(final TransitRouterNetwork.TransitRouterNetworkNode node, final Id id) {
			super(id);
			this.node = node;
			this.id = id;
		}

//		public Map<Id, ? extends LinkImpl> getInLinks() {
//			throw new UnsupportedOperationException();
//		}
//
//		public Map<Id, ? extends NodeImpl> getInNodes() {
//			throw new UnsupportedOperationException();
//		}
//
//		public Map<Id, ? extends LinkImpl> getIncidentLinks() {
//			throw new UnsupportedOperationException();
//		}
//
//		public Map<Id, ? extends NodeImpl> getIncidentNodes() {
//			throw new UnsupportedOperationException();
//		}
//
//		public String getOrigId() {
//			return null;
//		}

		public Map<Id, ? extends LinkImpl> getOutLinks() {
			Map<Id, LinkImpl> links = new LinkedHashMap<Id, LinkImpl>(this.node.outgoingLinks.size());
			for (TransitRouterNetwork.TransitRouterNetworkLink link : this.node.outgoingLinks) {
				LinkWrapper wrapped = getWrappedLink(link);
				links.put(wrapped.id, wrapped);
			}
			return links;
		}

//		public Map<Id, ? extends NodeImpl> getOutNodes() {
//			throw new UnsupportedOperationException();
//		}
//
//		public int getTopoType() {
//			throw new UnsupportedOperationException();
//		}
//
//		public String getType() {
//			return null;
//		}
//
//		public void removeInLink(final LinkImpl inlink) {
//			throw new UnsupportedOperationException();
//		}
//
//		public void removeOutLink(final LinkImpl outlink) {
//			throw new UnsupportedOperationException();
//		}
//
//		public void setOrigId(final String id) {
//			throw new UnsupportedOperationException();
//		}
//
//		public void setTopoType(final int topotype) {
//			throw new UnsupportedOperationException();
//		}
//
//		public void setType(final String type) {
//			throw new UnsupportedOperationException();
//		}
//
//		public boolean addInLink(final BasicLink link) {
//			throw new UnsupportedOperationException();
//		}
//
//		public boolean addOutLink(final BasicLink link) {
//			throw new UnsupportedOperationException();
//		}

		public Coord getCoord() {
			return this.node.stop.getStopFacility().getCoord();
		}
		
		public void setCoord(final Coord coord) {
			throw new UnsupportedOperationException();
		}

		public Id getId() {
			return this.id;
		}

		public int compareTo(final NodeImpl o) {
			return this.id.compareTo(o.getId());
		}
	}

	/*package*/ class LinkWrapper extends LinkImpl { //implements Link {

		final TransitRouterNetwork.TransitRouterNetworkLink link;
		final Id id;

		public LinkWrapper(final TransitRouterNetwork.TransitRouterNetworkLink link, final Id id) {
			super(id, getWrappedNode(link.fromNode), getWrappedNode(link.toNode), TransitRouterNetworkWrapper.this, 10, 10, 9999, 3);
			this.link = link;
			this.id = id;
		}

//		public double calcDistance(final Coord coord) {
//			throw new UnsupportedOperationException();
//		}
//
//		public double getEuklideanDistance() {
//			throw new UnsupportedOperationException();
//		}
//
//		public double getFlowCapacity(final double time) {
//			throw new UnsupportedOperationException();
//		}
//
//		public double getFreespeedTravelTime(final double time) {
//			throw new UnsupportedOperationException();
//		}

		public NodeImpl getFromNode() {
			return getWrappedNode(this.link.fromNode);
		}
//
//		public String getOrigId() {
//			return null;
//		}

		public NodeImpl getToNode() {
			return getWrappedNode(this.link.toNode);
		}
//
//		public String getType() {
//			return null;
//		}
//
//		public void setOrigId(final String origid) {
//			throw new UnsupportedOperationException();
//		}
//
//		public void setType(final String type) {
//			throw new UnsupportedOperationException();
//		}
//
//		public double getCapacity(final double time) {
//			return 9999;
//		}
//
//		public double getFreespeed(final double time) {
//			return 10;
//		}
//
//		public Id getId() {
//			return this.id;
//		}

		public double getNumberOfLanes(final double time) {
			return 1;
		}

		public int getLanesAsInt(final double time) {
			throw new UnsupportedOperationException();
		}

		public double getLength() {
			return CoordUtils.calcDistance(this.link.toNode.stop.getStopFacility().getCoord(), this.link.fromNode.stop.getStopFacility().getCoord());
		}
//
//		public void setCapacity(final double capacity) {
//			throw new UnsupportedOperationException();
//		}
//
//		public void setFreespeed(final double freespeed) {
//			throw new UnsupportedOperationException();
//		}
//
//		public boolean setFromNode(final BasicNode node) {
//			throw new UnsupportedOperationException();
//		}
//
//		public void setNumberOfLanes(final double lanes) {
//			throw new UnsupportedOperationException();
//		}
//
//		public void setLength(final double length) {
//			throw new UnsupportedOperationException();
//		}
//
//		public boolean setToNode(final BasicNode node) {
//			throw new UnsupportedOperationException();
//		}
//
//		public void addDownMapping(final Location other) {
//			throw new UnsupportedOperationException();
//		}
//
//		public void addUpMapping(final Location other) {
//			throw new UnsupportedOperationException();
//		}
//
//		public Location downLocation(final Id id) {
//			throw new UnsupportedOperationException();
//		}
//
//		public TreeMap<Id, Location> getDownMapping() {
//			throw new UnsupportedOperationException();
//		}
//
//		public Layer getLayer() {
//			throw new UnsupportedOperationException();
//		}
//
//		public Location getUpLocation(final Id id) {
//			throw new UnsupportedOperationException();
//		}
//
//		public TreeMap<Id, Location> getUpMapping() {
//			throw new UnsupportedOperationException();
//		}
//
//		public boolean removeAllDownMappings() {
//			throw new UnsupportedOperationException();
//		}
//
//		public boolean removeAllUpMappings() {
//			throw new UnsupportedOperationException();
//		}
//
//		public void setId(final Id id) {
//			throw new UnsupportedOperationException();
//		}
//		
//		public Coord getCoord() {
//			throw new UnsupportedOperationException();
//		}
//
//		public void setCoord(final Coord coord) {
//			throw new UnsupportedOperationException();
//		}
//
//		public Set<TransportMode> getAllowedModes() {
//			return allowedModes;
//		}
//
//		public void setAllowedModes(final Set<TransportMode> modes) {
//			throw new UnsupportedOperationException();
//		}
	}

}
