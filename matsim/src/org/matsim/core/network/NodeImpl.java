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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public class NodeImpl implements Node {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private String type = null;
	private String origid = null;

	protected transient  Map<Id, Link> inlinks  = new LinkedHashMap<Id, Link>(4, 0.95f);
	protected transient  Map<Id, Link> outlinks = new LinkedHashMap<Id, Link>(4, 0.95f);

	protected Coord coord;
	protected final Id id;
	
	private final static Logger log = Logger.getLogger(NodeImpl.class);

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	protected NodeImpl(final Id id, final Coord coord, final String type) {
		this(id);
		this.coord = coord;
		this.type = type == null ? null : type.intern();
	}

	//////////////////////////////////////////////////////////////////////
	// interface methods
	//////////////////////////////////////////////////////////////////////

	public NodeImpl(Id id) {
		this.id = id;
		this.coord = null;
	}

	public int compareTo(final NodeImpl o) {
		return this.id.toString().compareTo(o.getId().toString());
	}

	@Override
	public boolean equals(final Object other) {
		if (other instanceof NodeImpl) {
			return this.id.equals(((NodeImpl)other).id);
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

	public final void setOrigId(final String id) {
		this.origid = id;
	}

	public final void setType(final String type) {
		this.type = type == null ? null : type.intern();
	}
	
	public final boolean addInLink(Link inlink) {
		Id linkid = inlink.getId();
		if (this.inlinks.containsKey(linkid)) {
			throw new IllegalArgumentException(this + "[inlink_id=" + inlink.getId() + " already exists]");
		}
		if (this.outlinks.containsKey(linkid)) {
			log.warn(this + "[inlink_id=" + inlink.getId() + " is now in- and out-link]");
		}
		this.inlinks.put(linkid, inlink);
		return true; // yy should return true only if collection changed as result of call
	}

	public final boolean addOutLink(Link outlink) {
		Id linkid = outlink.getId();
		if (this.outlinks.containsKey(linkid)) {
			throw new IllegalArgumentException(this + "[inlink_id=" + outlink.getId() + " already exists]");
		}
		if (this.inlinks.containsKey(linkid)) {
			log.warn(this.toString() + "[outlink_id=" + outlink + " is now in- and out-link]");
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

	public Map<Id, ? extends Link> getInLinks() {
		return this.inlinks;
	}

	public Map<Id, ? extends Link> getOutLinks() {
		return this.outlinks;
	}

	public Coord getCoord() {
		return this.coord;
	}

	public Id getId() {
		return this.id;
	}

	
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
	
		inlinks = new LinkedHashMap<Id, Link>(4, 0.95f);
		outlinks = new LinkedHashMap<Id, Link>(4, 0.95f);

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
