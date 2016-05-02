/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package org.matsim.core.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.QuadTree;

/**
 * Design thoughts:<ul>
 * <li> This class is final, since it is sitting behind an interface, and thus delegation can be used for 
 * implementation modifications.  Access to the quad tree might be justified in some cases, but should then be realized
 * by specific methods and not via inheritance of the field (I would think

 </ul>
 * 
 * @author nagel
 * @author mrieser
 */
public final class NetworkImpl implements Network {

	private final static Logger log = Logger.getLogger(NetworkImpl.class);

	@Deprecated // use NetworkUtils.createNetwork() as much as possible.  kai, feb'14
	public static NetworkImpl createNetwork() {
		return new NetworkImpl();
	}

	private double capacityPeriod = 3600.0 ;

	private final Map<Id<Node>, Node> nodes = new LinkedHashMap<>();

	private final Map<Id<Link>, Link> links = new LinkedHashMap<>();

	private QuadTree<Node> nodeQuadTree = null;
	
	private LinkQuadTree linkQuadTree = null;

	private static final double DEFAULT_EFFECTIVE_CELL_SIZE = 7.5;

	private double effectiveCellSize = DEFAULT_EFFECTIVE_CELL_SIZE;

	private double effectiveLaneWidth = 3.75;

	private NetworkFactoryImpl factory;

	private final Collection<NetworkChangeEvent> networkChangeEvents = new ArrayList<>();

	private String name = null;

	private int counter=0;

	private int nextMsg=1;

	private int counter2=0;

	private int nextMsg2=1;

	NetworkImpl() {
		this.factory = new NetworkFactoryImpl(this);
	}

	@Override
	public void addLink(final Link link) {
		Link testLink = links.get(link.getId());
		if (testLink != null) {
			if (testLink == link) {
				log.warn("Trying to add a link a second time to the network. link id = " + link.getId().toString());
				return;
			}
			throw new IllegalArgumentException("There exists already a link with id = " + link.getId().toString() +
					".\nExisting link: " + testLink + "\nLink to be added: " + link +
					".\nLink is not added to the network.");
		}
		
		/* Check if the link's nodes are in the network. */
		Node fromNode = nodes.get( link.getFromNode().getId() );
		if(fromNode == null){
			throw new IllegalArgumentException("Trying to add link = " + link.getId() + ", but its fromNode = " + link.getFromNode().getId() + " has not been added to the network.");
		}
		Node toNode = nodes.get( link.getToNode().getId() );
		if(toNode == null){
			throw new IllegalArgumentException("Trying to add link = " + link.getId() + ", but its toNode = " + link.getToNode().getId() + " has not been added to the network.");
		}

		fromNode.addOutLink(link);
		toNode.addInLink(link);

		links.put(link.getId(), link);
		
		// show counter
		this.counter++;
		if (this.counter % this.nextMsg == 0) {
			this.nextMsg *= 2;
			printLinksCount();
		}
	}
	
	private void printLinksCount() {
		log.info(" link # " + this.counter);
	}
	
	private void printNodesCount() {
		log.info(" node # " + this.counter2);
	}
	
	@Override
	public void addNode(final Node nn) {
		Id<Node> id = nn.getId() ;
		Node node = this.nodes.get(id);
		if (node != null) {
			if (node == nn) {
				log.warn("Trying to add a node a second time to the network. node id = " + id.toString());
				return;
			}
			throw new IllegalArgumentException("There exists already a node with id = " + id.toString() +
					".\nExisting node: " + node + "\nNode to be added: " + nn +
					".\nNode is not added to the network.");
		}
		this.nodes.put(id, nn);
		if (this.nodeQuadTree != null) {
			if (Double.isInfinite(this.nodeQuadTree.getMinEasting())) {
				// looks like the quad tree was initialized with infinite bounds, see MATSIM-278.
				this.nodeQuadTree.clear();
				this.nodeQuadTree = null;
			} else if (this.nodeQuadTree.getMinEasting() <= nn.getCoord().getX() && this.nodeQuadTree.getMaxEasting() > nn.getCoord().getX()
					&& this.nodeQuadTree.getMinNorthing() <= nn.getCoord().getY() && this.nodeQuadTree.getMaxNorthing() > nn.getCoord().getY()) {
				this.nodeQuadTree.put(nn.getCoord().getX(), nn.getCoord().getY(), nn);
			} else {
				// we add a node outside the current bounds, invalidate it
				this.nodeQuadTree.clear();
				this.nodeQuadTree = null;
			}
		}

		// show counter
		this.counter2++;
		if (this.counter2 % this.nextMsg2 == 0) {
			this.nextMsg2 *= 2;
			printNodesCount();
		}
	}
	// ////////////////////////////////////////////////////////////////////
	// remove methods
	// ////////////////////////////////////////////////////////////////////

	@Override
	public Node removeNode(final Id<Node> nodeId) {
		Node n = this.nodes.remove(nodeId);
		if (n == null) {
			return null;
		}
		HashSet<Link> links1 = new HashSet<>();
		links1.addAll(n.getInLinks().values());
		links1.addAll(n.getOutLinks().values());
		for (Link l : links1) {
			removeLink(l.getId());
		}
		if (this.nodeQuadTree != null) {
			this.nodeQuadTree.remove(n.getCoord().getX(),n.getCoord().getY(),n);
		}
		return n;
	}

	@Override
	public Link removeLink(final Id<Link> linkId) {
		Link l = this.links.remove(linkId);
		if (l == null) {
			return null;
		}
		l.getFromNode().getOutLinks().remove(l.getId());
		l.getToNode().getInLinks().remove(l.getId());
		return l;
	}

	// ////////////////////////////////////////////////////////////////////
	// set methods
	// ////////////////////////////////////////////////////////////////////

	/**
	 * @param capPeriod the capacity-period in seconds
	 */
	public void setCapacityPeriod(final double capPeriod) {
		this.capacityPeriod = (int) capPeriod;
	}

	public void setEffectiveCellSize(final double effectiveCellSize) {
		if (this.effectiveCellSize != effectiveCellSize) {
			if (effectiveCellSize != DEFAULT_EFFECTIVE_CELL_SIZE) {
				log.warn("Setting effectiveCellSize to a non-default value of " + effectiveCellSize);
			} else {
				log.info("Setting effectiveCellSize to " + effectiveCellSize);
			}
			this.effectiveCellSize = effectiveCellSize;
		}
	}

	public void setEffectiveLaneWidth(final double effectiveLaneWidth) {
		if (!Double.isNaN(this.effectiveLaneWidth) && this.effectiveLaneWidth != effectiveLaneWidth) {
			log.warn(this + "[effectiveLaneWidth=" + this.effectiveLaneWidth + " already set. Will be overwritten with " + effectiveLaneWidth + "]");
		}
		this.effectiveLaneWidth = effectiveLaneWidth;
	}

	/**
	 * Sets the network change events and replaces existing events. Before
	 * events are applied to their corresponding links, all links are reset to
	 * their initial state. Pass an empty event list to reset the complete network.
	 *
	 * @param events a list of events.
	 */
	public void setNetworkChangeEvents(final List<NetworkChangeEvent> events) {
        this.networkChangeEvents.clear();
        for(Link link : getLinks().values()) {
            if (link instanceof TimeVariantLinkImpl) {
                ((TimeVariantLinkImpl)link).clearEvents();
            }
		}
		for (NetworkChangeEvent event : events) {
			this.addNetworkChangeEvent(event);
		}
	}

	/**
	 * Adds a single network change event and applies it to the corresponding
	 * links.
	 *
	 * @param event
	 *            a network change event.
	 */
	public void addNetworkChangeEvent(final NetworkChangeEvent event) {
		this.networkChangeEvents.add(event);
		for (Link link : event.getLinks()) {
			if (link instanceof TimeVariantLinkImpl) {
				((TimeVariantLinkImpl)link).applyEvent(event);
			} else {
				throw new IllegalArgumentException("Link " + link.getId().toString() + " is not TimeVariant.");
			}
		}
	}

	@Override
	public double getCapacityPeriod() {
		return this.capacityPeriod;
	}

	public double getEffectiveCellSize() {
		return this.effectiveCellSize;
	}

	@Override
	public double getEffectiveLaneWidth() {
		return this.effectiveLaneWidth;
	}

	@Override
	public Map<Id<Node>, Node> getNodes() {
		return this.nodes;
	}

    public Link getNearestLinkExactly(final Coord coord) {
		if (this.linkQuadTree == null) {
			buildLinkQuadTree();
		}
		return this.linkQuadTree.getNearest(coord.getX(), coord.getY());
	}

    /**
	 * finds the node nearest to <code>coord</code>
	 *
	 * @param coord the coordinate to which the closest node should be found
	 * @return the closest node found, null if none
	 */
	public Node getNearestNode(final Coord coord) {
		if (this.nodeQuadTree == null) { buildQuadTree(); }
		return this.nodeQuadTree.getClosest(coord.getX(), coord.getY());
	}

	/**
	 * finds the nodes within distance to <code>coord</code>
	 *
	 * @param coord the coordinate around which nodes should be located
	 * @param distance the maximum distance a node can have to <code>coord</code> to be found
	 * @return all nodes within distance to <code>coord</code>
	 */
	public Collection<Node> getNearestNodes(final Coord coord, final double distance) {
		if (this.nodeQuadTree == null) { buildQuadTree(); }
		return this.nodeQuadTree.getDisk(coord.getX(), coord.getY(), distance);
	}


	public Collection<NetworkChangeEvent> getNetworkChangeEvents() {
		return this.networkChangeEvents;
	}

	@Override
	public NetworkFactoryImpl getFactory() {
		return this.factory;
	}

	// ////////////////////////////////////////////////////////////////////
	// print methods
	// ////////////////////////////////////////////////////////////////////

	@Override
	public String toString() {
		return super.toString() +
				"[capperiod=" + this.capacityPeriod + "]" +
				"[nof_nodes=" + this.nodes.size() + "]";
	}

	public void connect() {
		buildQuadTree();
	}

	synchronized private void buildQuadTree() {
		/* the method must be synchronized to ensure we only build one quadTree
		 * in case that multiple threads call a method that requires the quadTree.
		 */
		if (this.nodeQuadTree != null) {
			return;
		}
		double startTime = System.currentTimeMillis();
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (Node n : this.nodes.values()) {
			if (n.getCoord().getX() < minx) { minx = n.getCoord().getX(); }
			if (n.getCoord().getY() < miny) { miny = n.getCoord().getY(); }
			if (n.getCoord().getX() > maxx) { maxx = n.getCoord().getX(); }
			if (n.getCoord().getY() > maxy) { maxy = n.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		// yy the above four lines are problematic if the coordinate values are much smaller than one. kai, oct'15
		
		log.info("building QuadTree for nodes: xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		QuadTree<Node> quadTree = new QuadTree<>(minx, miny, maxx, maxy);
		for (Node n : this.nodes.values()) {
			quadTree.put(n.getCoord().getX(), n.getCoord().getY(), n);
		}
		/* assign the quadTree at the very end, when it is complete.
		 * otherwise, other threads may already start working on an incomplete quadtree
		 */
		this.nodeQuadTree = quadTree;
		log.info("Building QuadTree took " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
	}
	
	synchronized private void buildLinkQuadTree() {
		if (this.linkQuadTree != null) {
			return;
		}
		double startTime = System.currentTimeMillis();
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (Node n : this.nodes.values()) {
			if (n.getCoord().getX() < minx) { minx = n.getCoord().getX(); }
			if (n.getCoord().getY() < miny) { miny = n.getCoord().getY(); }
			if (n.getCoord().getX() > maxx) { maxx = n.getCoord().getX(); }
			if (n.getCoord().getY() > maxy) { maxy = n.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		// yy the above four lines are problematic if the coordinate values are much smaller than one. kai, oct'15
		
		log.info("building LinkQuadTree for nodes: xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		LinkQuadTree qt = new LinkQuadTree(minx, miny, maxx, maxy);
		for (Link l : this.links.values()) {
			qt.put(l);
		}
		this.linkQuadTree = qt;
		log.info("Building LinkQuadTree took " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
	}

	@Override
	public Map<Id<Link>, Link> getLinks() {
		return Collections.unmodifiableMap(links);
	}

	 void setFactory(final NetworkFactoryImpl networkFactory) {
		this.factory = networkFactory;
	}

	public Node createAndAddNode(final Id<Node> id, final Coord coord) {
		if (this.nodes.containsKey(id)) {
			throw new IllegalArgumentException(this + "[id=" + id + " already exists]");
		}
		NodeImpl n = this.factory.createNode(id, coord);
		this.addNode(n) ;
		return n;
	}

	public Node createAndAddNode(final Id<Node> id, final Coord coord, final String nodeType) {
		NodeImpl n = (NodeImpl) createAndAddNode(id, coord);
		n.setType(nodeType);
		return n;
	}

	public Link createAndAddLink(final Id<Link> id, final Node fromNode,
			final Node toNode, final double length, final double freespeed, final double capacity, final double numLanes) {
				return createAndAddLink(id, fromNode, toNode, length, freespeed, capacity, numLanes, null, null);
			}

	public LinkImpl createAndAddLink(final Id<Link> id, final Node fromNode,
			final Node toNode, final double length, final double freespeed, final double capacity, final double numLanes,
			final String origId, final String type) 
	{

				if (this.nodes.get(fromNode.getId()) == null) {
					throw new IllegalArgumentException(this+"[from="+fromNode+" does not exist]");
				}

				if (this.nodes.get(toNode.getId()) == null) {
					throw new IllegalArgumentException(this+"[to="+toNode+" does not exist]");
				}

				LinkImpl link = (LinkImpl) this.factory.createLink(id, fromNode, toNode, this, length, freespeed, capacity, numLanes);
				link.setType(type);
				link.setOrigId(origId);

				this.addLink( link ) ;

				return link;
			}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
