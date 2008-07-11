/* *********************************************************************** *
 * project: org.matsim.*
 * Node.java
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

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicNodeImpl;
import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.networks.basicNet.BasicLink;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.misc.ResizableArray;

public class Node extends BasicNodeImpl implements Comparable<Node> {

	//////////////////////////////////////////////////////////////////////
	// constants
	//////////////////////////////////////////////////////////////////////

	/* See "http://www.ivt.ethz.ch/vpl/publications/reports/ab283.pdf"
	 * for description of node types. It's the graph matching paper. */
	public final static int EMPTY        = 0;
	public final static int SOURCE       = 1;
	public final static int SINK         = 2;
	public final static int DEADEND      = 3;
	public final static int PASS1WAY     = 4;
	public final static int PASS2WAY     = 5;
	public final static int START1WAY    = 6;
	public final static int END1WAY      = 7;
	public final static int INTERSECTION = 8;

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private String type = null;
	protected String origid = null;

	/* TODO [balmermi] The topo_type member should not be here, instead using a role or
	 * inheritance would make more sense. topo_type is calculated by
	 * org.matsim.network.algorithms.NetworkCalcTopoType */
	private int topoType = Integer.MIN_VALUE;
	private final ResizableArray<Object> roles = new ResizableArray<Object>(5);

	private final static Logger log = Logger.getLogger(Node.class);

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	protected Node(final Id id, final CoordI coord, final String type) {
		super(id, coord);
		this.type = type == null ? null : type.intern();
	}

	/** @deprecated please use the other Constructor which takes a Coord-object instead of x and y */
	@Deprecated
	protected Node(final Id id, final String x, final String y, final String type) {
		this(id, new Coord(x, y), type);
	}

	//////////////////////////////////////////////////////////////////////
	// interface methods
	//////////////////////////////////////////////////////////////////////

	public int compareTo(final Node o) {
		return this.id.toString().compareTo(o.id.toString());
	}

	@Override
	public boolean equals(final Object other) {
		if (other instanceof Node) {
			return this.id.equals(((Node)other).id);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	//////////////////////////////////////////////////////////////////////
	// add / set methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public boolean addInLink(final BasicLink inlink) {
		Id linkid = inlink.getId();
		if (this.inlinks.containsKey(linkid)) {
			Gbl.errorMsg(this + "[inlink_id=" + inlink.getId() + " already exists]");
		}
		if (this.outlinks.containsKey(linkid)) {
			log.warn(this + "[inlink_id=" + inlink.getId() + " is now in- and out-link]");
		}
		this.inlinks.put(linkid, inlink);
		return true;
	}

	@Override
	public boolean addOutLink(final BasicLink outlink) {
		Id linkid = outlink.getId();
		if (this.outlinks.containsKey(linkid)) {
			Gbl.errorMsg(this + "[inlink_id=" + outlink.getId() + " already exists]");
		}
		if (this.inlinks.containsKey(linkid)) {
			log.warn(this.toString() + "[outlink_id=" + outlink + " is now in- and out-link]");
		}
		this.outlinks.put(linkid, outlink);
		return true;
	}

	public final void setRole(final int idx, final Object role) {
		if (idx > this.roles.size()) {
			this.roles.resize(idx+1);
		}
		this.roles.set(idx, role);
	}

	protected final void setMaxRoleIndex(final int index) {
		this.roles.resize(index+1);
	}

	public final void setOrigId(final String id) {
		this.origid = id;
	}

	public final void setTopoType(final int topotype) {
		this.topoType = topotype;
	}

	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////

	// normally, the removed object should be passed back (like in other utils) balmermi
	// the new collections convention seems to be that the return type is boolean, and "true" is returned when
	// the collection is modified, and "false" else.  kai, dec06
	public final void removeInLink(final Link inlink) {
		this.inlinks.remove(inlink.getId());
	}

	// normally, the removed object should be passed back (like in other utils) balmermi
	// see above (removeInLink).  kai, dec06
	public final void removeOutLink(final Link outlink) {
		this.outlinks.remove(outlink.getId());
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final String getOrigId() {
		return this.origid;
	}

	public final String getType() {
		return this.type;
	}

	@Override
	public Coord getCoord() {
		return (Coord) this.coord;
	}

	public final Map<Id, ? extends Link> getIncidentLinks() {
		Map<Id, Link> links = new TreeMap<Id, Link>(getInLinks());
		links.putAll(getOutLinks());
		return links;
	}

	public final Map<Id, ? extends Node> getInNodes() {
		Map<Id, Node> nodes = new TreeMap<Id, Node>();
		for (Link link : getInLinks().values()) {
			Node node = link.getFromNode();
			nodes.put(node.getId(), node);
		}
		return nodes;
	}

	public final Map<Id, ? extends Node> getOutNodes() {
		Map<Id, Node> nodes = new TreeMap<Id, Node>();
		for (Link link : getOutLinks().values()) {
			Node node = link.getToNode();
			nodes.put(node.getId(), node);
		}
		return nodes;
	}

	public final Map<Id, ? extends Node> getIncidentNodes() {
		Map<Id, Node> nodes = new TreeMap<Id, Node>(getInNodes());
		nodes.putAll(getOutNodes());
		return nodes;
	}

	public int getTopoType() {
		return this.topoType;
	}

	public final Object getRole(final int idx) {
		if (idx < this.roles.size() ) return this.roles.get(idx);
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Id, ? extends Link> getInLinks() {
		return (Map<Id, Link>) super.getInLinks();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Id, ? extends Link> getOutLinks() {
		return (Map<Id, Link>)super.getOutLinks();
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public String toString() {
		return "[id=" + this.id + "]" +
				"[coord=" + this.coord + "]" +
				"[type=" + this.type + "]" +
				"[topoType=" + this.topoType + "]" +
				"[nof_inlinks=" + this.inlinks.size() + "]" +
				"[nof_outlinks=" + this.outlinks.size() + "]";
	}

}
