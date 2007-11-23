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

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;

import org.matsim.basic.v01.BasicLinkSet;
import org.matsim.basic.v01.BasicNode;
import org.matsim.basic.v01.BasicNodeSet;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.interfaces.networks.basicNet.BasicLinkSetI;
import org.matsim.interfaces.networks.basicNet.BasicNodeSetI;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.misc.ResizableArray;
import org.matsim.world.Coord;

public class Node extends BasicNode implements Comparable<Node> {

	//////////////////////////////////////////////////////////////////////
	// constants
	//////////////////////////////////////////////////////////////////////

	// see "http://www.ivt.ethz.ch/vpl/publications/reports/ab283.pdf"
	// for description of node types. It's the graph matching paper.
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

	// the topo_type member should not be here, instead using a role or
	// inheritance would make more sence. topo_type is calculated by
	// org.matsim.demandmodeling.network.algorithms.NetworkCalcTopoType
	private int topoType = Integer.MIN_VALUE;

	private final ResizableArray<Object> roles = new ResizableArray<Object>(5);

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	protected Node(final String id, final String x, final String y, final String type) {
		super(id, new Coord(x, y));
		this.type = type;
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
	public boolean addInLink(final BasicLinkI inlink) {
	//public boolean addInLink(Link inlink) {
		IdI linkid = inlink.getId();
		if (this.inlinks.containsId(linkid)) {
			Gbl.errorMsg(this + "[inlink_id=" + inlink.getId() + " already exists]");
		}
		if (this.outlinks.containsId(linkid)) {
			Gbl.warningMsg(this.getClass(),"addInLink(...)",this + "[inlink_id=" + inlink.getId() + " is now in- and out-link]");
		}
		this.inlinks.add(inlink);
		return true;
	}

	@Override
	public boolean addOutLink(final BasicLinkI outlink) {
	//public boolean addOutLink(Link outlink) {
		IdI linkid = outlink.getId();
		if (this.outlinks.containsId(linkid)) {
			Gbl.errorMsg(this + "[inlink_id=" + outlink.getId() + " already exists]");
		}
		if (this.inlinks.containsId(linkid)) {
			Gbl.warningMsg(this.getClass(),"addOutLink(...)",this.toString() + "[outlink_id=" + outlink + " is now in- and out-link]");
		}
		this.outlinks.add(outlink);
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

//	@Override
//	public final void setId(final Id id) {
//		this.id = id;
//	}

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
		this.inlinks.remove(inlink);
	}

	// normally, the removed object should be passed back (like in other utils) balmermi
	// see above (removeInLink).  kai, dec06
	public final void removeOutLink(final Link outlink) {
		this.outlinks.remove(outlink);
	}

	//////////////////////////////////////////////////////////////////////
	// query methods
	//////////////////////////////////////////////////////////////////////

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

	public final BasicLinkSetI getIncidentLinks() {
		BasicLinkSetI links = new BasicLinkSet();
		links.addAll(this.inlinks);
		links.addAll(this.outlinks);
		return links;
	}

	public final BasicNodeSetI getInNodes() {
		BasicNodeSetI nodes = new BasicNodeSet();
		Iterator it = this.inlinks.iterator();
		while (it.hasNext()) {
			Link l = (Link)it.next();
			nodes.add(l.getFromNode());
		}
		return nodes;
	}

	public final BasicNodeSetI getOutNodes() {
		BasicNodeSetI nodes = new BasicNodeSet();
		Iterator it = this.outlinks.iterator();
		while (it.hasNext()) {
			Link l = (Link)it.next();
			nodes.add(l.getToNode());
		}
		return nodes;
	}

	public final BasicNodeSetI getIncidentNodes() {
		BasicNodeSetI nodes = new BasicNodeSet();
		nodes.addAll(this.getInNodes());
		nodes.addAll(this.getOutNodes());
		return nodes;
	}

	public int getTopoType() {
		return this.topoType;
	}

	public final Object getRole(final int idx) {
		if (idx < this.roles.size() ) return this.roles.get(idx);
		return null;
	}


	public static class IDComparator implements Comparator<Node>, Serializable {
		private static final long serialVersionUID = 1L;

		public int compare(final Node n1, final Node n2) {
			return n1.getId().toString().compareTo(n2.getId().toString());
		}
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
