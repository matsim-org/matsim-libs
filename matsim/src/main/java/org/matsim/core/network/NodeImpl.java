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

import java.util.Collections;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.Lockable;
import org.matsim.core.utils.collections.IdentifiableArrayMap;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

/*deliberately package*/ class NodeImpl implements Node, Lockable {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private String type = null;
	private String origid = null;

	private transient  Map<Id<Link>, Link> inlinks  = new IdentifiableArrayMap<>();
	private transient  Map<Id<Link>, Link> outlinks = new IdentifiableArrayMap<>();

	private Coord coord;
	private final Id<Node> id;
	private boolean locked = false ;

	private final static Logger log = LogManager.getLogger(Node.class);
	private final Attributes attributes = new AttributesImpl();

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	NodeImpl(final Id<Node> id, final Coord coord) {
		this(id, coord, null);
	}

	/* package */NodeImpl(final Id<Node> id, final Coord coord, final String type) {
		this(id);
		this.coord = coord;
		NetworkUtils.setType(this,type);
	}

	/* package */NodeImpl(Id<Node> id) {
		this.id = id ;
		this.coord = null ;
	}
	
	/* package */ void  setType( final String type ) {
		this.type = type == null ? null : type.intern();
	}	
	
	private static int cnt2 = 0 ;
	@Override
	public final boolean addInLink(Link inlink) {
		Id<Link> linkid = inlink.getId();
		if (this.inlinks.containsKey(linkid)) {
			throw new IllegalArgumentException(this + ": inlink_id=" + inlink.getId() + " already exists");
		}
//		if (this.outlinks.containsKey(linkid) && (cnt2 < 1)) {
//			cnt2++ ;
//			log.warn(this + ": inlink_id=" + inlink.getId() + " is now in- and out-link");
//			log.warn(Gbl.ONLYONCE) ;
//		}
		// (this means it is a loop link; they have become an acceptable data structure within matsim.  kai, sep'19)
		this.inlinks.put(linkid, inlink);
		return true; // yy should return true only if collection changed as result of call
	}

	private static int cnt = 0 ;
	@Override
	public final boolean addOutLink(Link outlink) {
		Id<Link> linkid = outlink.getId();
		if (this.outlinks.containsKey(linkid)) {
			throw new IllegalArgumentException(this + ": outlink_id=" + outlink.getId() + " already exists");
		}
		if (this.inlinks.containsKey(linkid) && (cnt < 1)) {
			cnt++ ;
			log.warn(this.toString() + ": outlink_id=" + outlink.getId() + " is now in- and out-link");
			log.warn(Gbl.ONLYONCE) ;
		}
		this.outlinks.put(linkid, outlink);
		return true ; // yy should return true only if collection changed as result of call
	}
	@Override
	public void setCoord(final Coord coord){
		testForLocked();
		this.coord = coord;
	}

	/*package*/ void setOrigId(final String origId){
		this.origid = origId ;
	}

	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final Link removeInLink( final Id<Link> linkId ) {
		return this.inlinks.remove(linkId) ;
	}

	@Override
	public Link removeOutLink(final Id<Link> outLinkId) {
		return this.outlinks.remove(outLinkId);
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	/*package*/ String getOrigId() {
		return this.origid ;
	}
	
	/*package*/ String getType() {
		return this.type ;
	}
	
	@Override
	public Map<Id<Link>, ? extends Link> getInLinks() {
		return Collections.unmodifiableMap(this.inlinks);
	}

	@Override
	public Map<Id<Link>, ? extends Link> getOutLinks() {
		return Collections.unmodifiableMap(this.outlinks);
	}

	@Override
	public Coord getCoord() {
		return this.coord;
	}

	@Override
	public Id<Node> getId() {
		return this.id;
	}


//	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
//		ois.defaultReadObject();
//
//		inlinks = new LinkedHashMap<>(4, 0.95f);
//		outlinks = new LinkedHashMap<>(4, 0.95f);
//
//	}

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

	@Override
	public void setLocked() {
		this.locked = true ;
	}
	private void testForLocked() {
		if ( locked ) {
			throw new RuntimeException( "Network is locked; too late to do this.  See comments in code.") ;
		}
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}
}
