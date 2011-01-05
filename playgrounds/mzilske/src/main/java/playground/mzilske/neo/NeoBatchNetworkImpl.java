/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mzilske.neo;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteWRefs;
import org.neo4j.index.lucene.LuceneIndexBatchInserter;
import org.neo4j.kernel.impl.batchinsert.BatchInserter;
import org.neo4j.kernel.impl.batchinsert.SimpleRelationship;

public class NeoBatchNetworkImpl implements Network {

	private final class BasicNode implements Node {

		private Coord coord;
		private Id id;

		public BasicNode(Id id, Coord coord) {
			this.id = id;
			this.coord = coord;
		}

		@Override
		public boolean addInLink(Link link) {
			throw new RuntimeException();
		}

		@Override
		public boolean addOutLink(Link link) {
			throw new RuntimeException();
		}

		@Override
		public Map<Id, ? extends Link> getInLinks() {
			throw new RuntimeException();
		}

		@Override
		public Map<Id, ? extends Link> getOutLinks() {
			Map<Id, Link> outLinks = new HashMap<Id, Link>();
			long nodeid = index.getSingleNode(NeoNodeImpl.KEY_ID, id);
			for (SimpleRelationship r : inserter.getRelationships(nodeid)) {
				if (r.getType().name().equals(RelationshipTypes.LINK_TO.name()) && r.getStartNode() == nodeid) {
					Id linkId = new IdImpl((String) inserter.getNodeProperties(r.getEndNode()).get(NeoLinkImpl.KEY_ID));
					for (SimpleRelationship rr : inserter.getRelationships(r.getEndNode())) {
						if (rr.getType().name().equals(RelationshipTypes.LINK_TO.name()) && rr.getStartNode() == r.getEndNode()) {
							Id toId = new IdImpl((String) inserter.getNodeProperties(rr.getEndNode()).get(NeoNodeImpl.KEY_ID));
							BasicLink link = new BasicLink(linkId, id, toId);
							outLinks.put(linkId, link);
						}
					}

				}
			}
			return outLinks;
		}

		@Override
		public Coord getCoord() {
			return this.coord;
		}

		@Override
		public Id getId() {
			return this.id;
		}

		@Override
		public boolean equals(Object obj) {
			return id.equals(((BasicNode) obj).id);
		}



	}

	private final class BasicLink implements Link {

		private Id toNode;
		public BasicLink(Id id, Id fromNodeId, Id toNodeId) {
			super();
			this.toNode = toNodeId;
			this.fromNode = fromNodeId;
			this.id = id;
		}

		private double capacity;
		private double nLanes;
		private double freespeed;
		private Id fromNode;
		private Id id;
		private double length;

		@Override
		public Set<String> getAllowedModes() {
			throw new RuntimeException();
		}

		@Override
		public double getCapacity() {
			return capacity;
		}

		@Override
		public double getCapacity(double time) {
			return capacity;
		}

		@Override
		public double getFreespeed() {
			return freespeed;
		}

		@Override
		public double getFreespeed(double time) {
			return freespeed;
		}

		@Override
		public Node getFromNode() {
			throw new RuntimeException();
		}

		@Override
		public double getLength() {
			return length;
		}

		@Override
		public double getNumberOfLanes() {
			return nLanes;
		}

		@Override
		public double getNumberOfLanes(double time) {
			throw new RuntimeException();
		}

		@Override
		public Node getToNode() {
			return new BasicNode(toNode, null);
		}

		@Override
		public void setAllowedModes(Set<String> modes) {
			// TODO: allowed Modes
		}

		@Override
		public void setCapacity(double capacity) {
			this.capacity = capacity;
		}

		@Override
		public void setFreespeed(double freespeed) {
			this.freespeed = freespeed;
		}

		@Override
		public boolean setFromNode(Node node) {
			throw new RuntimeException();
		}

		@Override
		public void setLength(double length) {
			this.length = length;
		}

		@Override
		public void setNumberOfLanes(double lanes) {
			this.nLanes = lanes;
		}

		@Override
		public boolean setToNode(Node node) {
			throw new RuntimeException();
		}

		@Override
		public Coord getCoord() {
			throw new RuntimeException();
		}

		@Override
		public Id getId() {
			return this.id;
		}

	}

	final class BasicNetworkRoute implements NetworkRoute {

		private double distance;

		private double travelTime;

		String routeDescription;

		private Id startLinkId;

		private Id endLinkId;

		@Override
		public void setEndLinkId(Id linkId) {
			this.endLinkId = linkId;
		}

		@Override
		public void setStartLinkId(Id linkId) {
			this.startLinkId = linkId;
		}

		@Override
		public double getDistance() {
			throw new RuntimeException();
		}

		@Override
		public Id getEndLinkId() {
			return this.endLinkId;
		}

		@Override
		public Id getStartLinkId() {
			return this.startLinkId;
		}

		@Override
		public double getTravelTime() {
			throw new RuntimeException();
		}

		@Override
		public void setDistance(double distance) {
			this.distance = distance;
		}

		@Override
		public void setTravelTime(double travelTime) {
			this.travelTime = travelTime;
		}

		@Override
		public List<Id> getLinkIds() {
			throw new RuntimeException();
		}

		@Override
		public NetworkRoute getSubRoute(Id fromLinkId, Id toLinkId) {
			throw new RuntimeException();
		}

		@Override
		public double getTravelCost() {
			throw new RuntimeException();
		}

		@Override
		public Id getVehicleId() {
			throw new RuntimeException();
		}

		@Override
		public void setLinkIds(Id startLinkId, List<Id> linkIds, Id endLinkId) {
//			this.allLinkIds.clear();
//			this.allLinkIds.add(startLinkId);
//			this.allLinkIds.addAll(linkIds);
//			this.allLinkIds.add(endLinkId);
			throw new RuntimeException();
		}

		@Override
		public void setTravelCost(double travelCost) {
			throw new RuntimeException();
		}

		@Override
		public void setVehicleId(Id vehicleId) {
			throw new RuntimeException();
		}

		@Override
		public RouteWRefs clone() {
			throw new RuntimeException();
		}



	}

	private BatchInserter inserter;

	private Map<String,Object> properties = new HashMap<String,Object>();

	private LuceneIndexBatchInserter index;

	private boolean inLinkMode = false;

	long nodeRoot;
	long linkRoot;

	public NeoBatchNetworkImpl(BatchInserter inserter,
			LuceneIndexBatchInserter index, long nodeRoot, long linkRoot) {
		super();
		this.inserter = inserter;
		this.index = index;
		this.nodeRoot = nodeRoot;
		this.linkRoot = linkRoot;
	}

	@Override
	public void addLink(Link ll) {
		goToLinkMode();
		properties.clear();
		String fromNodeId = ((BasicLink) ll).fromNode.toString();
		String toNodeId = ((BasicLink) ll).toNode.toString();
		long fromNode = index.getSingleNode(NeoNodeImpl.KEY_ID, fromNodeId);
		long toNode = index.getSingleNode(NeoNodeImpl.KEY_ID, toNodeId);
		String idString = ll.getId().toString();
		properties.put(NeoLinkImpl.KEY_ID, idString);
		properties.put(NeoLinkImpl.KEY_LENGTH, ll.getLength());
		properties.put(NeoLinkImpl.KEY_CAPACITY, ll.getCapacity(0.0));
		properties.put(NeoLinkImpl.KEY_FREESPEED, ll.getFreespeed());
		properties.put(NeoLinkImpl.KEY_LANES, ll.getNumberOfLanes());
		long link = inserter.createNode(properties);
		inserter.createRelationship(linkRoot, link, RelationshipTypes.NETWORK_TO_LINK, null);
		index.index(link, NeoLinkImpl.KEY_ID, idString);
		inserter.createRelationship(fromNode, link, RelationshipTypes.LINK_TO, null);
		inserter.createRelationship(link, toNode, RelationshipTypes.LINK_TO, null);

	}

	private void goToLinkMode() {
		if (!inLinkMode) {
			index.optimize();
			inLinkMode = true;
		}
	}

	@Override
	public void addNode(Node nn) {
		assertInNodeMode();
		properties.clear();
		Coord coord = nn.getCoord();
		double x = coord.getX();
		double y = coord.getY();
		String idString = nn.getId().toString();
		properties.put(NeoNodeImpl.KEY_ID, idString);
		properties.put(NeoNodeImpl.KEY_COORD_X, x);
		properties.put(NeoNodeImpl.KEY_COORD_Y, y);
		long nodeId = inserter.createNode(properties);
		inserter.createRelationship(nodeRoot, nodeId, RelationshipTypes.NETWORK_TO_NODE, null);
		index.index(nodeId, NeoNodeImpl.KEY_ID, idString);
	}

	private void assertInNodeMode() {
		if (inLinkMode) {
			throw new RuntimeException();
		}
	}

	@Override
	public double getCapacityPeriod() {
		throw new RuntimeException();
	}

	@Override
	public double getEffectiveLaneWidth() {
		throw new RuntimeException();
	}

	@Override
	public NetworkFactory getFactory() {
		return new RouteSupportingNetworkFactory() {

			@Override
			public Link createLink(Id id, Id fromNodeId, Id toNodeId) {
				return new BasicLink(id, fromNodeId, toNodeId);
			}

			@Override
			public Link createLink(Id id, Node fromNode, Node toNode) {
				return new BasicLink(id, fromNode.getId(), toNode.getId());
			}

			@Override
			public Node createNode(Id id, Coord coord) {
				return new BasicNode(id, coord);
			}

			@Override
			public Route createRoute() {
				return new BasicNetworkRoute();
			}

		};
	}

	@Override
	public Map<Id, ? extends Link> getLinks() {
		throw new RuntimeException();
	}

	@Override
	public Map<Id, ? extends Node> getNodes() {
		return new AbstractMap<Id, Node>() {

			@Override
			public Set<Entry<Id, Node>> entrySet() {
				return Collections.emptySet();
			}

			@Override
			public Node get(Object key) {
				return new BasicNode((Id) key, null);
			}

		};
	}

	@Override
	public Link removeLink(Id linkId) {
		throw new RuntimeException();
	}

	@Override
	public Node removeNode(Id nodeId) {
		throw new RuntimeException();
	}

}
