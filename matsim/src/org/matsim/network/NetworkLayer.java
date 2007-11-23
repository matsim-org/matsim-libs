/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkLayer.java
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

package org.matsim.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.matsim.basic.v01.BasicLinkSet;
import org.matsim.basic.v01.BasicNodeSet;
import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.interfaces.networks.basicNet.BasicLinkSetI;
import org.matsim.interfaces.networks.basicNet.BasicNetI;
import org.matsim.interfaces.networks.basicNet.BasicNodeI;
import org.matsim.interfaces.networks.basicNet.BasicNodeSetI;
import org.matsim.network.algorithms.NetworkAlgorithm;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.misc.QuadTree;
import org.matsim.world.Coord;
import org.matsim.world.Layer;

public class NetworkLayer extends Layer implements BasicNetI {

	// ////////////////////////////////////////////////////////////////////
	// member variables
	// ////////////////////////////////////////////////////////////////////

	public static final IdI LAYER_TYPE = new Id("link");
	public static final double CELL_LENGTH = 7.5;

	protected int capperiod = Integer.MIN_VALUE ;

	// TODO [balmermi] I have moved the 'BasicLinkSetI locations' from Layer to here to get
	// rid of it in the Layer. Since 'BasicLinkSetI locations' affects a lot of classes,
	// i have now introduced a doubled data structure for this class:
	// 'BasicLinkSetI links' and 'TreeMap<IdI,Location> locations' (from Layer class)
	// This is very unfortunate, but otherwise I will never get to an end of re-structuring... *gmpf*
	protected BasicLinkSetI links = new BasicLinkSet();
	protected BasicNodeSetI nodes = new BasicNodeSet();

	private final ArrayList<NetworkAlgorithm> algorithms = new ArrayList<NetworkAlgorithm>();

	private QuadTree<Node> nodeQuadTree = null;

	private final ArrayList<Integer> nodeRoles = new ArrayList<Integer>(5);
	private final ArrayList<Integer> linkRoles = new ArrayList<Integer>(5);
	private int maxNodeRoleIndex = 4;
	private int maxLinkRoleIndex = 4;

	// ////////////////////////////////////////////////////////////////////
	// constructor
	// ////////////////////////////////////////////////////////////////////

	public NetworkLayer() {
		super(LAYER_TYPE,null);
	}

	// ////////////////////////////////////////////////////////////////////
	// create methods
	// overload newNode/newLink in your own classes for supplying other nodes
	// ////////////////////////////////////////////////////////////////////

	protected Node newNode(final String id, final String x, final String y, final String type) {
		return new Node(id,x,y,type);
	}

	protected Link newLink(final NetworkLayer network, final String id, final Node from, final Node to,
	                       final String length, final String freespeed, final String capacity, final String permlanes,
	                       final String origid, final String type) {
		return new Link(this,id,from,to,length,freespeed,capacity,permlanes,origid,type);
	}

	public final Node createNode(final String id, final String x, final String y, final String type) {
		IdI i = new Id(id);
		if (this.nodes.containsId(i)) { Gbl.errorMsg(this + "[id=" + id + " already exists]"); }
		Node n = newNode(id, x, y, type);
		this.nodes.add(n);
		if (this.nodeQuadTree != null) {
			// we changed the nodes, invalidate the quadTree
			this.nodeQuadTree.clear();
			this.nodeQuadTree = null;
		}
		return n;
	}

	public final Link createLink(final String id, final String from, final String to, final String length,
	                             final String freespeed, final String capacity, final String permlanes,
	                             final String origid, final String type) {
		Id f = new Id(from);
		Node from_node = (Node)this.nodes.get(f);
		if (from_node == null) { Gbl.errorMsg(this+"[from="+from+" does not exist]"); }

		Id t = new Id(to);
		Node to_node = (Node)this.nodes.get(t);
		if (to_node == null) { Gbl.errorMsg(this+"[to="+to+" does not exist]"); }

		Id l = new Id(id);
		if (this.locations.containsKey(l)) { Gbl.errorMsg("Link id=" + id + " already exists in 'locations'!"); }
		if (this.links.containsId(l)) { Gbl.errorMsg("Link id=" + id + " already exists in 'links'! SERIOUS WARNING: The 'if' statement above already should have produced this error!!!"); }
		Link link = newLink(this,id,from_node,to_node,length,freespeed,capacity,permlanes,origid,type);
		from_node.addOutLink(link);
		to_node.addInLink(link);
		this.locations.put(link.getId(),link);
		this.links.add(link);
		return link;
	}

	// ////////////////////////////////////////////////////////////////////
	// add methods
	// ////////////////////////////////////////////////////////////////////

	public final void addAlgorithm(final NetworkAlgorithm algo) {
		this.algorithms.add(algo);
	}

	public final boolean removeAlgorithm(final NetworkAlgorithm algo) {
		return this.algorithms.remove(algo);
	}

	// ////////////////////////////////////////////////////////////////////
	// run methods
	// ////////////////////////////////////////////////////////////////////

	public final void runAlgorithms() {
		for (int i = 0; i < this.algorithms.size(); i++) {
			NetworkAlgorithm algo = this.algorithms.get(i);
			algo.run(this);
		}
	}

	// ////////////////////////////////////////////////////////////////////
	// set methods
	// ////////////////////////////////////////////////////////////////////

	public final void setCapacityPeriod(final String capperiod) {
		if (this.capperiod != Integer.MIN_VALUE) {
			Gbl.warningMsg(this.getClass(), "setCapperiod(...)", this + "[capperiod=" + capperiod + " already set capperiod will be overwritten]");
		}
		this.capperiod = (int)Gbl.parseTime(capperiod);
	}

	// ////////////////////////////////////////////////////////////////////
	// get methods
	// ////////////////////////////////////////////////////////////////////

	public final int getCapacityPeriod() {
		return this.capperiod;
	}

	public BasicNodeSetI getNodes() {
		return this.nodes;
	}

	public final Node getNode(final String id) {
		return (Node)this.nodes.get(id);
	}

	/**
	 * Finds the (approx.) nearest link to a given point on the map.<br />
	 * It searches first for the nearest node, and then for the nearest link
	 * originating or ending at that node.
	 *
	 * @param coord
	 *          the coordinate for which the closest link should be found
	 * @return the link found closest to coord
	 */
	public Link getNearestLink(final CoordI coord) {
		Link nearestLink = null;
		Node nearestNode = null;
		if (this.nodeQuadTree == null) { buildQuadTree(); }
		nearestNode = this.nodeQuadTree.get(coord.getX(), coord.getY());

		// now find nearest link from the nearest node
		// [balmermi] it checks now ALL incident links, not only the outgoing ones.
		// TODO [balmermi] Now it finds the first of the typically two nearest links (same nodes, other direction)
		// It would be nicer to find the nearest link on the "right" side of the coordinate.
		// (For Great Britain it would be the "left" side. Could be a global config param...)
		double shortestDistance = Double.MAX_VALUE;
		Iterator<Link> l_it = nearestNode.getIncidentLinks().iterator();
		while (l_it.hasNext()) {
			Link link = l_it.next();
			double dist = link.calcDistance(coord);
			if (dist < shortestDistance) {
				shortestDistance = dist;
				nearestLink = link;
			}
		}
		return nearestLink;
	}

	/**
	 * Finds the (approx.) nearest link to a given point on the map,
	 * such that the point lies on the right side of the directed link,
	 * if such a link exists.<br />
	 * It searches first for the nearest node, and then for the nearest link
	 * originating or ending at that node and fullfilling the above constraint.
	 *
	 * @param coord
	 *          the coordinate for which the closest link should be found
	 * @return the link found closest to coord and orientated such that the
	 * point lies on the right of the link.
	 */
	// TODO [balmermi] there should be only one 'getNearestLink' method
	// which returns either the nearest 'left' or 'right' entry link, based on a global
	// config param.
	public Link getNearestRightEntryLink(final CoordI coord) {
		Link nearestRightLink = null;
		Link nearestOverallLink = null;
		Node nearestNode = null;
		if (this.nodeQuadTree == null) { buildQuadTree(); }
		nearestNode = this.nodeQuadTree.get(coord.getX(), coord.getY());

		double[] coordVector = new double[2];
		coordVector[0] = nearestNode.getCoord().getX() - coord.getX();
		coordVector[1] = nearestNode.getCoord().getY() - coord.getY();

		// now find nearest link from the nearest node
		double shortestRightDistance = Double.MAX_VALUE; // reset the value
		double shortestOverallDistance = Double.MAX_VALUE; // reset the value
		Iterator lIter = nearestNode.getIncidentLinks().iterator();
		while (lIter.hasNext()) {
			Link link = (Link) lIter.next();
			double dist = link.calcDistance(coord);
			if (dist < shortestRightDistance) {
				// Generate a vector representing the link
				double[] linkVector = new double[2];
				linkVector[0] = link.getToNode().getCoord().getX()
						- link.getFromNode().getCoord().getX();
				linkVector[1] = link.getToNode().getCoord().getY()
						- link.getFromNode().getCoord().getY();

				// Calculate the z component of cross product of coordVector and the link
				double crossProductZ = coordVector[0]*linkVector[1] - coordVector[1]*linkVector[0];
				// If coord lies to the right of the directed link, i.e. if the z component
				// of the cross product is negative, set it as new nearest link
				if (crossProductZ < 0) {
					shortestRightDistance = dist;
					nearestRightLink = link;
				}
			}
			if (dist < shortestOverallDistance) {
				shortestOverallDistance = dist;
				nearestOverallLink = link;
			}
		}

		// Return the nearest overall link if there is no nearest link
		// such that the given coord is on the right side of it
		if (nearestRightLink == null) {
			return nearestOverallLink;
		}
		return nearestRightLink;
	}

	/**
	 * finds the node nearest to <code>coord</code>
	 *
	 * @param coord the coordinate to which the closest node should be found
	 * @return the closest node found, null if none
	 */
	public Node getNearestNode(final Coord coord) {
		return this.nodeQuadTree.get(coord.getX(), coord.getY());
	}

	/**
	 * finds the nodes within distance to <code>coord</code>
	 *
	 * @param coord the coordinate around which nodes should be located
	 * @param distance the maximum distance a node can have to <code>coord</code> to be found
	 * @return all nodes within distance to <code>coord</code>
	 */
	public Collection<Node> getNearestNodes(final Coord coord, final double distance) {
		return this.nodeQuadTree.get(coord.getX(), coord.getY(), distance);
	}

	// ////////////////////////////////////////////////////////////////////
	// remove methods
	// ////////////////////////////////////////////////////////////////////

	/**
	 * removes a link from the network.<p>
	 *
	 * In case <tt>link</tt> exists, it first unlinks it from the two
	 * incident nodes and then removes it from the link set of the network.
	 *
	 * @param link Link to be removed.
     * @return <tt>true</tt> if the specified link is part of the network and
     * is successfully removed.
	 */
	public boolean removeLink(final Link link) {
		IdI id = link.getId();
		Link l = (Link)this.links.get(id);

		if (l == null) { return false; }

		Node from = l.getFromNode();
		from.removeOutLink(l);
		Node to = l.getToNode();
		to.removeInLink(l);

		this.links.remove(l);
		if (this.locations.remove(l.getId()) == null) { Gbl.errorMsg("Link id=" + l.getId() + " not found in 'locations' even it was found in 'links'"); }
		return true;
	}

	/**
	 * removes a node from the network.<p>
	 *
	 * In case <tt>node</tt> exists, it first removed all incident links of
	 * <tt>node</tt> and then removes <tt>node</tt> from the link set
	 * and from the <tt>nodeQuadTree</tt>---if instantiated---of the network.<p>
	 *
	 * NOTE: if one of the incident links of <tt>node</tt> cannot be removed
	 * properly, the process crashes.
	 *
	 * @param node Node to be removed.
     * @return <tt>true</tt> if the specified node is part of the network and
     * is successfully removed AND all incient links are removed successfully
     *
	 */
	public boolean removeNode(final Node node) {
		IdI id = node.getId();
		Node n = (Node)this.nodes.get(id);

		if (n == null) { return false; }

		Iterator l_it = n.getIncidentLinks().iterator();
		while (l_it.hasNext()) {
			Link l = (Link)l_it.next();
			if (!this.removeLink(l)) { Gbl.errorMsg("Link id=" + l.getId() + " could not be removed while removing Node id=" + n.getId()); }
		}
		if (this.nodeQuadTree != null) { this.nodeQuadTree.remove(n.getCoord().getX(),n.getCoord().getY(),n); }
		return this.nodes.remove(n);
	}

	// ////////////////////////////////////////////////////////////////////
	// print methods
	// ////////////////////////////////////////////////////////////////////

	@Override
	public String toString() {
		return super.toString() +
				"[capperiod=" + this.capperiod + "]" +
				"[nof_nodes=" + this.nodes.size() + "]" +
				"[nof_algorithms=" + this.algorithms.size() + "]";
	}

	// ////////////////////////////////////////////////////////////////////
	// methods from BasicNetI
	// ////////////////////////////////////////////////////////////////////

	/**
	 * yyyy:
	 *
	 * * QueueNetworkLayer needs to be at least a BasicNetworkI, in order to use Gunnar's viewer.
	 *
	 * * Could have QueueNetworkLayer implement the BasicNetworkI.  But if one looks at what that interface
	 *   does, it is in fact better at the level here.  So I leave the dummy implementations and hope
	 *   for better times.
	 *
	 * kai, dec06
	 */
	public BasicNodeI newNode(final String label) {
        throw new UnsupportedOperationException("Do not use this constructor!");
	}

	public BasicLinkI newLink(final String label) {
        throw new UnsupportedOperationException("Do not use this constructor!");
	}

	public boolean add(final BasicNodeI node) {
        throw new UnsupportedOperationException("Do not use this constructor!");
	}

	public boolean add(final BasicLinkI link) {
        throw new UnsupportedOperationException("Do not use this constructor!");
	}

	public void connect() {
		buildQuadTree();
	}

	private void buildQuadTree() {
		Gbl.startMeasurement();
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		Iterator<Node> n_it = this.nodes.iterator();
		while (n_it.hasNext()) {
			Node n = n_it.next();
			if (n.getCoord().getX() < minx) { minx = n.getCoord().getX(); }
			if (n.getCoord().getY() < miny) { miny = n.getCoord().getY(); }
			if (n.getCoord().getX() > maxx) { maxx = n.getCoord().getX(); }
			if (n.getCoord().getY() > maxy) { maxy = n.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("building quad tree: xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.nodeQuadTree = new QuadTree<Node>(minx, miny, maxx, maxy);
		n_it = this.nodes.iterator();
		while (n_it.hasNext()) {
			Node n = n_it.next();
			this.nodeQuadTree.put(n.getCoord().getX(), n.getCoord().getY(), n);
		}
		Gbl.printRoundTime();
	}

	public BasicLinkSetI getLinks() {
		return this.links;
	}

	public Link getLink(final String linkId) {
		Id i = new Id(linkId);
		return (Link) this.links.get(i);
	}

	public Link getLink(final IdI linkId) {
		return (Link) this.links.get(linkId);
	}

	// ////////////////////////////////////////////////////////////////////
	// methods for RoleHandling
	// ////////////////////////////////////////////////////////////////////

	/**
	 * returns the index to access roles on nodes of this network
	 *
	 * @return the index to access set and get a role on nodes of this network
	 */
	public final synchronized int requestNodeRole() {
		// first, check if there is an empty space somewhere
		int index = this.nodeRoles.indexOf(Integer.valueOf(-1));
		if (index == -1) {
			// nope, no empty slot found, so add a new
			index = this.nodeRoles.size();
			this.nodeRoles.add(Integer.valueOf(index));
			if (index > this.maxNodeRoleIndex) {
				int newMax = (int) (1.2 * index) + 1;
				Iterator iter = this.nodes.iterator();
				while (iter.hasNext()) {
					Node node = (Node)iter.next();
					node.setMaxRoleIndex(newMax);
				}
				this.maxNodeRoleIndex = newMax;
			}
		} else {
			this.nodeRoles.set(index, Integer.valueOf(index));
		}
		return index;
	}

	public final synchronized void releaseNodeRole(final int roleIndex) {
		// clear all stored roles
		Iterator iter = this.nodes.iterator();
		while (iter.hasNext()) {
			Node node = (Node)iter.next();
			node.setRole(roleIndex, null);
		}
		// clear the index
		this.nodeRoles.set(roleIndex, Integer.valueOf(-1));
	}

	/**
	 * returns the index to access roles on links of this network
	 *
	 * @return the index to access set and get a role on links of this network
	 */
	public final int requestLinkRole() {
		int index = this.linkRoles.size();
		// if ever a function releaseLinkRole(int) is implemented, we could check here if there
		// is an empty slot available. This would help keeping index-numbers small.
		this.linkRoles.add(Integer.valueOf(index));
		if (index > this.maxLinkRoleIndex) {
			int newMax = (int) (1.2 * index) + 1;
			Iterator iter = this.links.iterator();
			while (iter.hasNext()) {
				Link link = (Link)iter.next();
				link.setMaxRoleIndex(newMax);
			}
			this.maxLinkRoleIndex = newMax;
		}
		return index;
	}

}
