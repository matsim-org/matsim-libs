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

package org.matsim.core.network;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.IdentifiableArrayMap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class NodeImpl implements Node {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private String type = null;
	private String origid = null;

	private transient  Map<Id<Link>, Link> inlinks  = new IdentifiableArrayMap<>();
	private transient  Map<Id<Link>, Link> outlinks = new IdentifiableArrayMap<>();

	private Coord coord;
	private final Id<Node> id;

	private final static Logger log = Logger.getLogger(NodeImpl.class);

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	protected NodeImpl(final Id<Node> id, final Coord coord) {
		this(id, coord, null);
	}

	protected NodeImpl(final Id<Node> id, final Coord coord, final String type) {
		this(id);
		this.coord = coord;
		this.type = type == null ? null : type.intern();
	}

	//////////////////////////////////////////////////////////////////////
	// interface methods
	//////////////////////////////////////////////////////////////////////

	public NodeImpl(Id<Node> id) {
		this.id = id;
		this.coord = null;
	}

	//////////////////////////////////////////////////////////////////////
	// add / set methods
	//////////////////////////////////////////////////////////////////////

	public final void setOrigId(final String id) {
		this.origid = id;
	}

	public final void setType(final String type) {
		this.type = type == null ? null : type.intern();
	}

	private static int cnt2 = 0 ;
	@Override
	public final boolean addInLink(Link inlink) {
		Id<Link> linkid = inlink.getId();
		if (this.inlinks.containsKey(linkid)) {
			throw new IllegalArgumentException(this + "[inlink_id=" + inlink.getId() + " already exists]");
		}
		if (this.outlinks.containsKey(linkid) && (cnt2 < 1)) {
			cnt2++ ;
			log.warn(this + "[inlink_id=" + inlink.getId() + " is now in- and out-link]");
			log.warn(Gbl.ONLYONCE) ;
		}
		this.inlinks.put(linkid, inlink);
		return true; // yy should return true only if collection changed as result of call
	}

	private static int cnt = 0 ;
	@Override
	public final boolean addOutLink(Link outlink) {
		Id<Link> linkid = outlink.getId();
		if (this.outlinks.containsKey(linkid)) {
			throw new IllegalArgumentException(this + "[outlink_id=" + outlink.getId() + " already exists]");
		}
		if (this.inlinks.containsKey(linkid) && (cnt < 1)) {
			cnt++ ;
			log.warn(this.toString() + "[outlink_id=" + outlink + " is now in- and out-link]");
			log.warn(Gbl.ONLYONCE) ;
		}
		this.outlinks.put(linkid, outlink);
		return true ; // yy should return true only if collection changed as result of call
	}

	public void setCoord(final Coord coord){
		this.coord = coord;
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

	public final Map<Id<Link>, ? extends Link> getIncidentLinks() {
		Map<Id<Link>, Link> links = new TreeMap<>(getInLinks());
		links.putAll(getOutLinks());
		return links;
	}

	public final Map<Id<Node>, ? extends Node> getInNodes() {
		Map<Id<Node>, Node> nodes = new TreeMap<>();
		for (Link link : getInLinks().values()) {
			Node node = link.getFromNode();
			nodes.put(node.getId(), node);
		}
		return nodes;
	}

	public final Map<Id<Node>, ? extends Node> getOutNodes() {
		Map<Id<Node>, Node> nodes = new TreeMap<>();
		for (Link link : getOutLinks().values()) {
			Node node = link.getToNode();
			nodes.put(node.getId(), node);
		}
		return nodes;
	}

	public final Map<Id<Node>, ? extends Node> getIncidentNodes() {
		Map<Id<Node>, Node> nodes = new TreeMap<>(getInNodes());
		nodes.putAll(getOutNodes());
		return nodes;
	}

	@Override
	public Map<Id<Link>, ? extends Link> getInLinks() {
		return this.inlinks;
	}

	@Override
	public Map<Id<Link>, ? extends Link> getOutLinks() {
		return this.outlinks;
	}

	@Override
	public Coord getCoord() {
		return this.coord;
	}

	@Override
	public Id<Node> getId() {
		return this.id;
	}


	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		ois.defaultReadObject();

		inlinks = new LinkedHashMap<>(4, 0.95f);
		outlinks = new LinkedHashMap<>(4, 0.95f);

	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public String toString() {
		return "[id=" + this.id + "]" +
				"[coord=" + this.coord + "]" +
				"[type=" + this.type + "]" +
				"[nof_inlinks=" + this.inlinks.size() + "]" +
				"[nof_outlinks=" + this.outlinks.size() + "]";
	}

}
