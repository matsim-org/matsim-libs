/* *********************************************************************** *
 * project: org.matsim.*
 * BasicNode.java
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

package org.matsim.core.basic.v01.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicLink;
import org.matsim.api.basic.v01.network.BasicNode;

public class BasicNodeImpl implements BasicNode {
	private final static Logger log = Logger.getLogger(BasicNodeImpl.class);

	protected transient  Map<Id, BasicLink> inlinks  = new LinkedHashMap<Id, BasicLink>(4, 0.95f);
	protected transient  Map<Id, BasicLink> outlinks = new LinkedHashMap<Id, BasicLink>(4, 0.95f);

	protected Coord coord;
	protected final Id id;


	protected BasicNodeImpl(Id id){
		// this is deliberately "protected"
		this.id = id;
	}
	
	public BasicNodeImpl(Id id, Coord coord) {
		// yyyyyy should be protected and then go through builder
		this(id);
		this.coord = coord;
	}

	public final boolean addInLink(BasicLink inlink) {
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

	public final boolean addOutLink(BasicLink outlink) {
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

	public Map<Id, ? extends BasicLink> getInLinks() {
		return this.inlinks;
	}

	public Map<Id, ? extends BasicLink> getOutLinks() {
		return this.outlinks;
	}
	
	public void setCoord(final Coord coord){
		this.coord = coord;
	}

	public Coord getCoord() {
		return this.coord;
	}

	public Id getId() {
		return this.id;
	}
	

	private void readObject(ObjectInputStream ois)
    	throws ClassNotFoundException, IOException {
  		ois.defaultReadObject();

  		inlinks = new LinkedHashMap<Id, BasicLink>(4, 0.95f);
  		outlinks = new LinkedHashMap<Id, BasicLink>(4, 0.95f);
  
	}

}
