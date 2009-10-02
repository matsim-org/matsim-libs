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

package org.matsim.core.network;

import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.world.Layer;
import org.matsim.world.Location;
import org.matsim.world.MappedLocation;
import org.matsim.world.MappingRule;

public class NetworkLayer extends NetworkImpl implements Layer {
	// yyyy I would say that this should be moved to org.matsim.world.  Right now, however, it needs to be used quite a lot
	// and so I leave it here for the time being.  kai, jul09
	private final static Logger log = Logger.getLogger(NetworkLayer.class);
	
	// ////////////////////////////////////////////////////////////////////
	// creational methods
	// ////////////////////////////////////////////////////////////////////

	public NetworkLayer() {
		this.factory = new NetworkFactoryImpl(this);
	}

	public NetworkLayer(final NetworkFactoryImpl factory) {
		this.factory = factory;
		this.factory.setNetwork(this);
	}

	public final NodeImpl createAndAddNode(final Id id, final Coord coord) {
		return createAndAddNode(id, coord, null);
	}

	public final NodeImpl createAndAddNode(final Id id, final Coord coord, final String nodeType) {
		if (this.nodes.containsKey(id)) {
			throw new IllegalArgumentException(this + "[id=" + id + " already exists]");
		}
		NodeImpl n = this.factory.createNode(id, coord, nodeType);
//		this.nodes.put(id, n);
//		if (this.nodeQuadTree != null) {
//			// we changed the nodes, invalidate the quadTree
//			this.nodeQuadTree.clear();
//			this.nodeQuadTree = null;
//		}
		this.addNode( n ) ;
		return n;
	}

	public final LinkImpl createAndAddLink(final Id id, final NodeImpl fromNode, final NodeImpl toNode, final double length, final double freespeed, final double capacity, final double numLanes) {
		return createAndAddLink(id, fromNode, toNode, length, freespeed, capacity, numLanes, null, null);
	}

	public final LinkImpl createAndAddLink(final Id id, final NodeImpl fromNode, final NodeImpl toNode, final double length, final double freespeed, final double capacity, final double numLanes, final String origId, final String type) {

		if (this.nodes.get(fromNode.getId()) == null) {
			throw new IllegalArgumentException(this+"[from="+fromNode+" does not exist]");
		}

		if (this.nodes.get(toNode.getId()) == null) {
			throw new IllegalArgumentException(this+"[to="+toNode+" does not exist]");
		}

		if (this.getLocations().containsKey(id)) {
			throw new IllegalArgumentException("Link id=" + id + " already exists in 'locations'!");
		}

		LinkImpl link = this.factory.createLink(id, fromNode, toNode, this, length, freespeed, capacity, numLanes);
		link.setType(type);
		link.setOrigId(origId);
		
		this.addLink( link ) ;

		return link;
	}
	
	// things below here are originally automatically generated.  kai, jul09
	
	public MappingRule getDownRule() {
		return layerDelegate.getDownRule();
	}

	public MappedLocation getLocation(Id location_id) {
		return layerDelegate.getLocation(location_id);
	}

	public Location getLocation(String location_id) {
		return layerDelegate.getLocation(location_id);
	}

	public TreeMap<Id, ? extends MappedLocation> getLocations() {
		return layerDelegate.getLocations();
	}

	public String getName() {
		return layerDelegate.getName();
	}

	public ArrayList<MappedLocation> getNearestLocations(Coord coord, Location excludeLocation) {
		return layerDelegate.getNearestLocations(coord, excludeLocation);
	}

	public ArrayList<MappedLocation> getNearestLocations(Coord coord) {
		return layerDelegate.getNearestLocations(coord);
	}

	public Id getType() {
		return layerDelegate.getType();
	}

	public MappingRule getUpRule() {
		return layerDelegate.getUpRule();
	}

	public void setName(String name) {
		layerDelegate.setName(name);
	}

	public final boolean removeUpRule() {
		return layerDelegate.removeUpRule();
	}

	public final boolean removeDownRule() {
		return layerDelegate.removeDownRule();
	}

	public final void setUpRule(final MappingRule up_rule) {
		layerDelegate.setUpRule( up_rule ) ;
	}

	public final void setDownRule(final MappingRule down_rule) {
		layerDelegate.setDownRule( down_rule ) ;
	}

	public void forceDownRuleToNull() {
		layerDelegate.forceDownRuleToNull();
	}

	public void forceUpRuleToNull() {
		layerDelegate.forceUpRuleToNull() ;
	}

}
