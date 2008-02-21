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
import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.networks.basicNet.BasicNetI;
import org.matsim.network.algorithms.NetworkAlgorithm;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.misc.Time;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.world.Layer;
import org.matsim.world.Location;

public class NetworkLayer extends Layer implements BasicNetI {

	// ////////////////////////////////////////////////////////////////////
	// member variables
	// ////////////////////////////////////////////////////////////////////

	public static final IdI LAYER_TYPE = new Id("link");


	protected int capperiod = Integer.MIN_VALUE ;

	protected Map<IdI, Node> nodes = new TreeMap<IdI, Node>();

	private final ArrayList<NetworkAlgorithm> algorithms = new ArrayList<NetworkAlgorithm>();

	private QuadTree<Node> nodeQuadTree = null;

	private final ArrayList<Integer> nodeRoles = new ArrayList<Integer>(5);
	private final ArrayList<Integer> linkRoles = new ArrayList<Integer>(5);
	private int maxNodeRoleIndex = 4;
	private int maxLinkRoleIndex = 4;
	private double effectivecellsize;
	
	

	// ////////////////////////////////////////////////////////////////////
	// constructor
	// ////////////////////////////////////////////////////////////////////

	public NetworkLayer() {
		super(LAYER_TYPE, null);
	}

	// ////////////////////////////////////////////////////////////////////
	// create methods
	// overload newNode/newLink in your own classes for supplying other nodes
	// ////////////////////////////////////////////////////////////////////

	protected Node newNode(final String id, final String x, final String y, final String type) {
		return new Node(id, x, y, type);
	}

	protected Link newLink(final NetworkLayer network, final String id, final Node from, final Node to,
	                       final String length, final String freespeed, final String capacity, final String permlanes,
	                       final String origid, final String type) {
		return new Link(this,id,from,to,length,freespeed,capacity,permlanes,origid,type);
	}

	public final Node createNode(final String id, final String x, final String y, final String type) {
		IdI i = new Id(id);
		if (this.nodes.containsKey(i)) { Gbl.errorMsg(this + "[id=" + id + " already exists]"); }
		Node n = newNode(id, x, y, type);
		this.nodes.put(i, n);
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
		Node from_node = this.nodes.get(f);
		if (from_node == null) { Gbl.errorMsg(this+"[from="+from+" does not exist]"); }

		Id t = new Id(to);
		Node to_node = this.nodes.get(t);
		if (to_node == null) { Gbl.errorMsg(this+"[to="+to+" does not exist]"); }

		Id l = new Id(id);
		if (this.locations.containsKey(l)) { Gbl.errorMsg("Link id=" + id + " already exists in 'locations'!"); }
		Link link = newLink(this,id,from_node,to_node,length,freespeed,capacity,permlanes,origid,type);
		from_node.addOutLink(link);
		to_node.addInLink(link);
		this.locations.put(link.getId(),link);
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
		this.capperiod = (int)Time.parseTime(capperiod);
	}

	public final void setEffectivecellsize(final String effectivecellsize) {
		if (this.effectivecellsize != Double.NaN) {
			Gbl.warningMsg(this.getClass(), "setEffectivecellsize(...)", this + "[effectivecellsize=" + effectivecellsize + " already set effectivecellsize will be overwritten]");
		}
		this.effectivecellsize = Double.parseDouble(effectivecellsize);
	}
	
	
	// ////////////////////////////////////////////////////////////////////
	// get methods
	// ////////////////////////////////////////////////////////////////////

	public final int getCapacityPeriod() {
		return this.capperiod;
	}

	public final double getEffectiveCellSize() {
		return this.effectivecellsize;
	}
	
	public Map<IdI, ? extends Node> getNodes() {
		return this.nodes;
	}

	public final Node getNode(final String id) {
		return this.nodes.get(new Id(id));
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
		for (Link link : nearestNode.getIncidentLinks().values()) {
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
	 * originating or ending at that node and fulfilling the above constraint.
	 * <p>
	 * <b>Special cases:</b> <tt>nodes:o ; links:<-- ; coord:x</tt><br/>
	 * <i>No right entry link exists</i><br/>
	 * <tt>
	 * o<-1--o returning<br/>
	 * | . . ^ nearest left<br/>
	 * |2 . 4| entry link<br/>
	 * v .x. | (link.id=3)<br/>
	 * o--3->o<br/>
	 * </tt>
	 * <br/>
	 * <i>No right entry link exists but more than one nearest left entry link exist</i><br/>
	 * <tt>
	 * o<-1--o returning<br/>
	 * | . . ^ nearest left<br/>
	 * |2 x 4| entry link with the<br/>
	 * v . . | lowest link id<br/>
	 * o--3->o (link.id=1)<br/>
	 * </tt>
	 * <br/>
	 * <i>More than one nearest right entry link exist</i><br/>
	 * <tt>
	 * o--1->o returning<br/>
	 * ^ . . | nearest right<br/>
	 * |2 x 4| entry link with the<br/>
	 * | . . v lowest link id<br/>
	 * o<-3--o (link.id=1)<br/>
	 * <br/>
	 * o<----7&8--x->o (link.id=7)<br/>
	 * </tt>
	 * </p>
	 *
	 * @param coord
	 *          the coordinate for which the closest link should be found
	 * @return the link found closest to <code>coord</code> and oriented such that the
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
		for (Link link : nearestNode.getIncidentLinks().values()) {
			double dist = link.calcDistance(coord);
			if (dist <= shortestRightDistance) {
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
					if (dist < shortestRightDistance) {
						shortestRightDistance = dist;
						nearestRightLink = link;
					}
					else { // dist == shortestRightDistance
						if (link.getId().compareTo(nearestRightLink.getId()) < 0) {
							shortestRightDistance = dist;
							nearestRightLink = link;
						}
					}
				}
			}
			if (dist < shortestOverallDistance) {
				shortestOverallDistance = dist;
				nearestOverallLink = link;
			}
			else if (dist == shortestOverallDistance) {
				if (link.getId().compareTo(nearestOverallLink.getId()) < 0) {
					shortestOverallDistance = dist;
					nearestOverallLink = link;
				}
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
		Link l = (Link)this.locations.get(id);

		if (l == null) { return false; }

		Node from = l.getFromNode();
		from.removeOutLink(l);
		Node to = l.getToNode();
		to.removeInLink(l);

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
		Node n = this.nodes.get(id);

		if (n == null) { return false; }

		for (Link l : n.getIncidentLinks().values()) {
			if (!this.removeLink(l)) {
				Gbl.errorMsg("Link id=" + l.getId() + " could not be removed while removing Node id=" + n.getId());
			}
		}
		if (this.nodeQuadTree != null) {
			this.nodeQuadTree.remove(n.getCoord().getX(),n.getCoord().getY(),n);
		}
		this.nodes.remove(id);
		return true;
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

	public void connect() {
		buildQuadTree();
	}

	private void buildQuadTree() {
		Gbl.startMeasurement();
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
		System.out.println("building quad tree: xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.nodeQuadTree = new QuadTree<Node>(minx, miny, maxx, maxy);
		for (Node n : this.nodes.values()) {
			this.nodeQuadTree.put(n.getCoord().getX(), n.getCoord().getY(), n);
		}
		Gbl.printRoundTime();
	}

	@SuppressWarnings("unchecked")
	public Map<IdI, ? extends Link> getLinks() {
		return (Map<IdI, Link>) this.getLocations();
	}

	public Link getLink(final String linkId) {
		Id i = new Id(linkId);
		return (Link) this.locations.get(i);
	}

	public Link getLink(final IdI linkId) {
		return (Link) this.locations.get(linkId);
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
				for (Node node : this.nodes.values()) {
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
		for (Node node : this.nodes.values()) {
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
			for (Location location : this.locations.values()) {
				((Link) location).setMaxRoleIndex(newMax);
			}
			this.maxLinkRoleIndex = newMax;
		}
		return index;
	}

}
