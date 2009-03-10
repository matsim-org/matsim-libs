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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.LocationType;
import org.matsim.interfaces.basic.v01.BasicLink;
import org.matsim.interfaces.basic.v01.BasicNode;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Network;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.NetworkFactory;
import org.matsim.world.Layer;
import org.matsim.world.Location;

public class TransitRouterNetworkWrapper implements Network {
	
	private final TransitRouterNetwork transitNetwork;
	private final Map<TransitRouterNetwork.TransitRouterNetworkNode, NodeWrapper> nodesLookup =
		new HashMap<TransitRouterNetwork.TransitRouterNetworkNode, NodeWrapper>();
	private final Map<TransitRouterNetwork.TransitRouterNetworkLink, LinkWrapper> linksLookup =
		new HashMap<TransitRouterNetwork.TransitRouterNetworkLink, LinkWrapper>();
	private final Map<Id, Node> nodes = new LinkedHashMap<Id, Node>();
	private final Map<Id, Link> links = new LinkedHashMap<Id, Link>();
	
	private long nextNodeId = 0;
	private long nextLinkId = 0;
	
	public TransitRouterNetworkWrapper(final TransitRouterNetwork transitNetwork) {
		this.transitNetwork = transitNetwork;
		for (TransitRouterNetwork.TransitRouterNetworkNode node : this.transitNetwork.getNodes()) {
			NodeWrapper node2 = new NodeWrapper(node, new IdImpl(nextNodeId++));
			this.nodesLookup.put(node, node2);
			this.nodes.put(node2.getId(), node2);
		}
		for (TransitRouterNetwork.TransitRouterNetworkLink link : this.transitNetwork.getLinks()) {
			LinkWrapper link2 = new LinkWrapper(link, new IdImpl(nextLinkId++));
			this.linksLookup.put(link, link2);
			this.links.put(link2.getId(), link2);
		}
	}	

	public Link getLink(Id linkId) {
		return this.links.get(linkId);
	}
	
	public Node getNode(Id id) {
		return this.nodes.get(id);
	}
	
	public Node getNearestNode(Coord coord) {
		throw new UnsupportedOperationException();
	}
	
	public Collection<Node> getNearestNodes(Coord coord, double distance) {
		throw new UnsupportedOperationException();
	}
	
	public Map<Id, Link> getLinks() {
		return this.links;
	}
	
	public Map<Id, Node> getNodes() {
		return this.nodes;
	}
	
	/*package*/ NodeWrapper getWrappedNode(final TransitRouterNetwork.TransitRouterNetworkNode node) {
		return this.nodesLookup.get(node);
	}
	
	/*package*/ LinkWrapper getWrappedLink(final TransitRouterNetwork.TransitRouterNetworkLink link) {
		return this.linksLookup.get(link);
	}
	
	public int getCapacityPeriod() {
		return 3600;
	}

	public double getEffectiveCellSize() {
		return 7.5;
	}

	public double getEffectiveLaneWidth() {
		return 3;
	}

	public NetworkFactory getFactory() {
		throw new UnsupportedOperationException();
	}

	public String getName() {
		return "TransitRouterNetworkWrapper";
	}

	public boolean removeLink(Link link) {
		throw new UnsupportedOperationException();
	}

	public boolean removeNode(Node node) {
		throw new UnsupportedOperationException();
	}

	public void setCapacityPeriod(double capPeriod) {
		throw new UnsupportedOperationException();
	}

	public void setEffectiveCellSize(double effectiveCellSize) {
		throw new UnsupportedOperationException();
	}

	public void setEffectiveLaneWidth(double effectiveLaneWidth) {
		throw new UnsupportedOperationException();
	}

	public void connect() {
		throw new UnsupportedOperationException();
	}

	
	/*package*/ class NodeWrapper implements Node {
		final TransitRouterNetwork.TransitRouterNetworkNode node;
		final Id id;
		
		public NodeWrapper(final TransitRouterNetwork.TransitRouterNetworkNode node, final Id id) {
			this.node = node;
			this.id = id;
		}
		
		public Map<Id, ? extends Link> getInLinks() {
			throw new UnsupportedOperationException();
		}

		public Map<Id, ? extends Node> getInNodes() {
			throw new UnsupportedOperationException();
		}

		public Map<Id, ? extends Link> getIncidentLinks() {
			throw new UnsupportedOperationException();
		}

		public Map<Id, ? extends Node> getIncidentNodes() {
			throw new UnsupportedOperationException();
		}

		public String getOrigId() {
			return null;
		}

		public Map<Id, ? extends Link> getOutLinks() {
			Map<Id, Link> links = new LinkedHashMap<Id, Link>(this.node.outgoingLinks.size());
			for (TransitRouterNetwork.TransitRouterNetworkLink link : this.node.outgoingLinks) {
				LinkWrapper wrapped = getWrappedLink(link);
				links.put(wrapped.id, wrapped);
			}
			return links;
		}

		public Map<Id, ? extends Node> getOutNodes() {
			throw new UnsupportedOperationException();
		}

		public int getTopoType() {
			throw new UnsupportedOperationException();
		}

		public String getType() {
			return null;
		}

		public void removeInLink(Link inlink) {
			throw new UnsupportedOperationException();
		}

		public void removeOutLink(Link outlink) {
			throw new UnsupportedOperationException();
		}

		public void setOrigId(String id) {
			throw new UnsupportedOperationException();
		}

		public void setTopoType(int topotype) {
			throw new UnsupportedOperationException();
		}

		public void setType(String type) {
			throw new UnsupportedOperationException();
		}

		public boolean addInLink(BasicLink link) {
			throw new UnsupportedOperationException();
		}

		public boolean addOutLink(BasicLink link) {
			throw new UnsupportedOperationException();
		}

		public Coord getCoord() {
			return this.node.stop.getStopFacility().getCenter();
		}

		public Id getId() {
			return this.id;
		}

		public int compareTo(Node o) {
			return this.id.compareTo(o.getId());
		}
	}
	
	/*package*/ class LinkWrapper implements Link {

		final TransitRouterNetwork.TransitRouterNetworkLink link;
		final Id id;
		
		public LinkWrapper(final TransitRouterNetwork.TransitRouterNetworkLink link, final Id id) {
			this.link = link;
			this.id = id;
		}
		
		public double calcDistance(Coord coord) {
			throw new UnsupportedOperationException();
		}

		public double getEuklideanDistance() {
			throw new UnsupportedOperationException();
		}

		public double getFlowCapacity(double time) {
			throw new UnsupportedOperationException();
		}

		public double getFreespeedTravelTime(double time) {
			throw new UnsupportedOperationException();
		}

		public Node getFromNode() {
			return getWrappedNode(this.link.fromNode);
		}

		public String getOrigId() {
			return null;
		}

		public Node getToNode() {
			return getWrappedNode(this.link.toNode);
		}

		public String getType() {
			return null;
		}

		public void setOrigId(String origid) {
			throw new UnsupportedOperationException();
		}

		public void setType(String type) {
			throw new UnsupportedOperationException();
		}

		public double getCapacity(double time) {
			return 9999;
		}

		public double getFreespeed(double time) {
			return 10;
		}

		public Id getId() {
			return this.id;
		}

		public double getLanes(double time) {
			return 1;
		}

		public int getLanesAsInt(double time) {
			throw new UnsupportedOperationException();
		}

		public double getLength() {
			return this.link.toNode.stop.getStopFacility().calcDistance(this.link.fromNode.stop.getStopFacility().getCenter());
		}

		public void setCapacity(double capacity) {
			throw new UnsupportedOperationException();
		}

		public void setFreespeed(double freespeed) {
			throw new UnsupportedOperationException();
		}

		public boolean setFromNode(BasicNode node) {
			throw new UnsupportedOperationException();
		}

		public void setLanes(double lanes) {
			throw new UnsupportedOperationException();
		}

		public void setLength(double length) {
			throw new UnsupportedOperationException();
		}

		public boolean setToNode(BasicNode node) {
			throw new UnsupportedOperationException();
		}

		public void addDownMapping(Location other) {
			throw new UnsupportedOperationException();
		}

		public void addUpMapping(Location other) {
			throw new UnsupportedOperationException();
		}

		public Location downLocation(Id id) {
			throw new UnsupportedOperationException();
		}

		public TreeMap<Id, Location> getDownMapping() {
			throw new UnsupportedOperationException();
		}

		public Layer getLayer() {
			throw new UnsupportedOperationException();
		}

		public Location getUpLocation(Id id) {
			throw new UnsupportedOperationException();
		}

		public TreeMap<Id, Location> getUpMapping() {
			throw new UnsupportedOperationException();
		}

		public boolean removeAllDownMappings() {
			throw new UnsupportedOperationException();
		}

		public boolean removeAllUpMappings() {
			throw new UnsupportedOperationException();
		}

		public void setId(Id id) {
			throw new UnsupportedOperationException();
		}

		public Coord getCenter() {
			throw new UnsupportedOperationException();
		}

		public LocationType getLocationType() {
			throw new UnsupportedOperationException();
		}
	}
	
}
