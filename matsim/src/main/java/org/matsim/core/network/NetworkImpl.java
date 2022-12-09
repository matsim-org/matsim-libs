/* *********************************************************************** *
 * project: org.matsim.*
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.scenario.Lockable;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.util.*;

/**
 * Design thoughts:<ul>
 * <li> This class is final, since it is sitting behind an interface, and thus delegation can be used for 
 * implementation modifications.  Access to the quad tree might be justified in some cases, but should then be realized
 * by specific methods and not via inheritance of the field (I would think).

 </ul>
 * 
 * @author nagel
 * @author mrieser
 */
/*deliberately package*/ final class NetworkImpl implements Network, Lockable, TimeDependentNetwork, SearchableNetwork {

	private final static Logger log = LogManager.getLogger(NetworkImpl.class);

	private double capacityPeriod = 3600.0 ;

	private final IdMap<Node, Node> nodes = new IdMap<>(Node.class);

	private final IdMap<Link, Link> links = new IdMap<>(Link.class);

	private QuadTree<Node> nodeQuadTree = null;

	private LinkQuadTree linkQuadTree = null;

	private static final double DEFAULT_EFFECTIVE_CELL_SIZE = 7.5;

	private double effectiveCellSize = DEFAULT_EFFECTIVE_CELL_SIZE;

	private double effectiveLaneWidth = 3.75;

	private NetworkFactory factory;

//	private final Collection<NetworkChangeEvent> networkChangeEvents = new ArrayList<>();
	
	private final Queue<NetworkChangeEvent> networkChangeEvents
//			= new PriorityQueue<>(11, new Comparator<NetworkChangeEvent>() {
//		@Override
//		public int compare(NetworkChangeEvent arg0, NetworkChangeEvent arg1) {
//			return Double.compare(arg0.getStartTime(), arg1.getStartTime()) ;
//		}
//	});
			= new PriorityQueue<>(11, new NetworkChangeEvent.StartTimeComparator() ) ;
	
	private String name = null;

	private int counter=0;

	private int nextMsg=1;

	private int counter2=0;

	private int nextMsg2=1;

	private boolean locked = false ;
	private final Attributes attributes = new AttributesImpl();

	NetworkImpl(LinkFactory linkFactory) {
		this.factory = new NetworkFactoryImpl(this, linkFactory);
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
        Node fromNode = nodes.get(link.getFromNode().getId());
        if (fromNode == null) {
            throw new IllegalArgumentException("Trying to add link = " + link.getId() + ", but its fromNode = " + link.getFromNode().getId() + " has not been added to the network.");
        }
        Node toNode = nodes.get(link.getToNode().getId());
        if (toNode == null) {
            throw new IllegalArgumentException("Trying to add link = " + link.getId() + ", but its toNode = " + link.getToNode().getId() + " has not been added to the network.");
        }

        if (!fromNode.getOutLinks().containsKey(link.getId()))
            fromNode.addOutLink(link);
        if (!toNode.getInLinks().containsKey(link.getId()))
            toNode.addInLink(link);

        link.setFromNode(fromNode);
        link.setToNode(toNode);

        links.put(link.getId(), link);

        if (this.linkQuadTree != null) {
            double linkMinX = Math.min(link.getFromNode().getCoord().getX(), link.getToNode().getCoord().getX());
            double linkMaxX = Math.max(link.getFromNode().getCoord().getX(), link.getToNode().getCoord().getX());
            double linkMinY = Math.min(link.getFromNode().getCoord().getY(), link.getToNode().getCoord().getY());
			double linkMaxY = Math.max(link.getFromNode().getCoord().getY(), link.getToNode().getCoord().getY());
			if (Double.isInfinite(this.linkQuadTree.getMinEasting())) {
				// looks like the quad tree was initialized with infinite bounds, see MATSIM-278.
				this.linkQuadTree = null;
			} else if (this.linkQuadTree.getMinEasting() <= linkMinX && this.linkQuadTree.getMaxEasting() > linkMaxX
					&& this.linkQuadTree.getMinNorthing() <= linkMinY && this.linkQuadTree.getMaxNorthing() > linkMaxY) {
				this.linkQuadTree.put(link);
			} else {
				// we add a link outside the current bounds, invalidate it
				this.linkQuadTree = null;
			}
		}


		// show counter
		this.counter++;
		if (this.counter % this.nextMsg == 0) {
			this.nextMsg *= 4;
			printLinksCount();
		}
		if ( this.locked && link instanceof Lockable ) {
			((Lockable)link).setLocked() ;
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
			this.nextMsg2 *= 4;
			printNodesCount();
		}

		if ( this.locked && nn instanceof Lockable ) {
			((Lockable)nn).setLocked() ;
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
		l.getFromNode().removeOutLink(l.getId()) ;
		l.getToNode().removeInLink(l.getId()) ;

		if (this.linkQuadTree != null) {
			this.linkQuadTree.remove(l);
		}

		return l;
	}

	// ////////////////////////////////////////////////////////////////////
	// set methods
	// ////////////////////////////////////////////////////////////////////

	/**
	 * @param capPeriod the capacity-period in seconds
	 */
	@Override
	public void setCapacityPeriod(final double capPeriod) {
		testForLocked() ;
		this.capacityPeriod = (int) capPeriod;
	}
	@Override
	public void setEffectiveCellSize(final double effectiveCellSize) {
		testForLocked() ;
		if (this.effectiveCellSize != effectiveCellSize) {
			if (effectiveCellSize != DEFAULT_EFFECTIVE_CELL_SIZE) {
				log.warn("Setting effectiveCellSize to a non-default value of " + effectiveCellSize);
			} else {
				log.info("Setting effectiveCellSize to " + effectiveCellSize);
			}
			this.effectiveCellSize = effectiveCellSize;
		}
	}
	@Override
	public void setEffectiveLaneWidth(final double effectiveLaneWidth) {
		testForLocked() ;
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
	@Override public void setNetworkChangeEvents(final List<NetworkChangeEvent> events) {
		this.networkChangeEvents.clear();
		for(Link link : getLinks().values()) {
			if (link instanceof TimeVariantLinkImpl) {
				((TimeVariantLinkImpl)link).clearEvents();
			}
			// Presumably, there is no exception here if this fails because it can be interpreted: maybe only some links are time-dependent
			// and others are not, and it is sufficient if the time-dependent ones can be configured by the addNetworkChangeEvent method.
			// kai, jul'16
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
	@Override
	public void addNetworkChangeEvent(final NetworkChangeEvent event) {
		this.networkChangeEvents.add(event);
		for (Link link : event.getLinks()) {
			if (link instanceof TimeVariantLinkImpl) {
				((TimeVariantLinkImpl)link).applyEvent(event);
			} else {
				throw new IllegalArgumentException("Link " + link.getId().toString() + " is not timeVariant. "
						+ "Did you make the network factory time variant?  The easiest way to achieve this is "
						+ "either in the config file, or syntax of the type\n"
						+ "config.network().setTimeVariantNetwork(true);\n"
						+ "Scenario scenario = ScenarioUtils.load/createScenario(config);\n"
						+ "Note that the scenario needs to be created _after_ the config option is set, otherwise"
						+ "the factory will already be there.");
			}
		}
	}

	@Override
	public double getCapacityPeriod() {
		return this.capacityPeriod;
	}
	@Override
	public double getEffectiveCellSize() {
		return this.effectiveCellSize;
	}

	@Override
	public double getEffectiveLaneWidth() {
		return this.effectiveLaneWidth;
	}

	@Override
	public Map<Id<Node>, Node> getNodes() {
		return Collections.unmodifiableMap(this.nodes);
	}

	@Override public Link getNearestLinkExactly(final Coord coord) {
		return this.getLinkQuadTree().getNearest(coord.getX(), coord.getY());
	}

	/**
	 * finds the node nearest to <code>coord</code>
	 *
	 * @param coord the coordinate to which the closest node should be found
	 * @return the closest node found, null if none
	 */
	@Override public Node getNearestNode(final Coord coord) {
		return this.getNodeQuadTree().getClosest(coord.getX(), coord.getY());
	}

	/**
	 * finds the nodes within distance to <code>coord</code>
	 *
	 * @param coord the coordinate around which nodes should be located
	 * @param distance the maximum distance a node can have to <code>coord</code> to be found
	 * @return all nodes within distance to <code>coord</code>
	 */
	@Override public Collection<Node> getNearestNodes(final Coord coord, final double distance) {
		return this.getNodeQuadTree().getDisk(coord.getX(), coord.getY(), distance);
	}

	@Override
	public Queue<NetworkChangeEvent> getNetworkChangeEvents() {
		return this.networkChangeEvents;
	}
	@Override
	public NetworkFactory getFactory() {
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

	//	public void connect() {
	//		buildQuadTree();
	//	}
	// it is safer if all functionality that could be done here is either done lazily or directly when nodes/links are added.  kai, jul'16

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

	void setFactory(final NetworkFactory networkFactory) {
		this.factory = networkFactory;
	}

	@Override
	public String getName() {
		return this.name;
	}
	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setLocked() {
		this.locked = true ;
		for ( Link link : this.links.values() ) {
			if ( link instanceof Lockable ) {
				((Lockable) link).setLocked();
			}
		}
		for ( Node node : this.nodes.values() ) {
			if ( node instanceof Lockable ) {
				((Lockable) node).setLocked();
			}
		}
	}
	@SuppressWarnings("unused")
	private void testForLocked() {
		if ( locked ) {
			throw new RuntimeException( "Network is locked; too late to do this.  See comments in code.") ;
		}
	}
	@Override public Attributes getAttributes() {
		return attributes;
	}
	@Override public LinkQuadTree getLinkQuadTree() {
		if (this.linkQuadTree == null) buildLinkQuadTree();
		return this.linkQuadTree ;
	}
	@Override public QuadTree<Node> getNodeQuadTree() {
		if (this.nodeQuadTree == null) buildQuadTree();
		return this.nodeQuadTree ;
	}
}
