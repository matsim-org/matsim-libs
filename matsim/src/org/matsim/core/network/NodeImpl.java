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

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.BasicNodeImpl;

public class NodeImpl extends BasicNodeImpl implements Node {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private String type = null;
	private String origid = null;

	private final static Logger log = Logger.getLogger(NodeImpl.class);

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	protected NodeImpl(final Id id, final Coord coord, final String type) {
		super(id, coord);
		this.type = type == null ? null : type.intern();
	}

	//////////////////////////////////////////////////////////////////////
	// interface methods
	//////////////////////////////////////////////////////////////////////

	public NodeImpl(Id id) {
		super(id);
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

	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////

	// normally, the removed object should be passed back (like in other utils) balmermi
	// the new collections convention seems to be that the return type is boolean, and "true" is returned when
	// the collection is modified, and "false" else.  kai, dec06
	public final void removeInLink(final LinkImpl inlink) {
		this.inlinks.remove(inlink.getId());
	}

	// normally, the removed object should be passed back (like in other utils) balmermi
	// see above (removeInLink).  kai, dec06
	public final void removeOutLink(final LinkImpl outlink) {
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

	public final Map<Id, ? extends LinkImpl> getIncidentLinks() {
		Map<Id, LinkImpl> links = new TreeMap<Id, LinkImpl>(getInLinks());
		links.putAll(getOutLinks());
		return links;
	}

	public final Map<Id, ? extends NodeImpl> getInNodes() {
		Map<Id, NodeImpl> nodes = new TreeMap<Id, NodeImpl>();
		for (LinkImpl link : getInLinks().values()) {
			NodeImpl node = link.getFromNode();
			nodes.put(node.getId(), node);
		}
		return nodes;
	}

	public final Map<Id, ? extends NodeImpl> getOutNodes() {
		Map<Id, NodeImpl> nodes = new TreeMap<Id, NodeImpl>();
		for (LinkImpl link : getOutLinks().values()) {
			NodeImpl node = link.getToNode();
			nodes.put(node.getId(), node);
		}
		return nodes;
	}

	public final Map<Id, ? extends NodeImpl> getIncidentNodes() {
		Map<Id, NodeImpl> nodes = new TreeMap<Id, NodeImpl>(getInNodes());
		nodes.putAll(getOutNodes());
		return nodes;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Id, ? extends LinkImpl> getInLinks() {
		return (Map<Id, LinkImpl>) super.getInLinks();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Id, ? extends LinkImpl> getOutLinks() {
		return (Map<Id, LinkImpl>)super.getOutLinks();
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
