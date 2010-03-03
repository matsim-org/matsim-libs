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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.world.Layer;
import org.matsim.world.Location;
import org.matsim.world.MappedLocation;

public class NetworkLayer extends NetworkImpl implements Layer {
	// yyyy I would say that this should be moved to org.matsim.world.  Right now, however, it needs to be used quite a lot
	// and so I leave it here for the time being.  kai, jul09

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

	public final Node createAndAddNode(final Id id, final Coord coord) {
		if (this.nodes.containsKey(id)) {
			throw new IllegalArgumentException(this + "[id=" + id + " already exists]");
		}
		NodeImpl n = this.factory.createNode(id, coord);
		this.addNode(n) ;
		return n;
	}

	public final Node createAndAddNode(final Id id, final Coord coord, final String nodeType) {
		NodeImpl n = (NodeImpl) createAndAddNode(id, coord);
		n.setType(nodeType);
		return n;
	}

	public final Link createAndAddLink(final Id id, final Node fromNode, final Node toNode, final double length, final double freespeed, final double capacity, final double numLanes) {
		return createAndAddLink(id, fromNode, toNode, length, freespeed, capacity, numLanes, null, null);
	}

	public final LinkImpl createAndAddLink(final Id id, final Node fromNode, final Node toNode, final double length, final double freespeed, final double capacity, final double numLanes, final String origId, final String type) {

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

	@Deprecated
	@Override
	public Layer getDownLayer() {
		return layerDelegate.getDownLayer();
	}

	@Override
	public MappedLocation getLocation(Id location_id) {
		return layerDelegate.getLocation(location_id);
	}

	@Override
	public TreeMap<Id, ? extends MappedLocation> getLocations() {
		return layerDelegate.getLocations();
	}

	@Override
	public String getName() {
		return layerDelegate.getName();
	}

	@Override
	public ArrayList<MappedLocation> getNearestLocations(Coord coord, Location excludeLocation) {
		return layerDelegate.getNearestLocations(coord, excludeLocation);
	}

	@Override
	public ArrayList<MappedLocation> getNearestLocations(Coord coord) {
		return layerDelegate.getNearestLocations(coord);
	}

	@Deprecated
	@Override
	public Id getType() {
		return layerDelegate.getType();
	}

	@Deprecated
	@Override
	public Layer getUpLayer() {
		return layerDelegate.getUpLayer();
	}

	@Override
	public void setName(String name) {
		layerDelegate.setName(name);
	}

	@Deprecated
	@Override
	public final boolean removeUpLayer() {
		return layerDelegate.removeUpLayer();
	}

	@Deprecated
	@Override
	public final boolean removeDownLayer() {
		return layerDelegate.removeDownLayer();
	}

	@Deprecated
	@Override
	public final void setUpLayer(final Layer up_Layer) {
		layerDelegate.setUpLayer( up_Layer ) ;
	}

	@Deprecated
	@Override
	public final void setDownLayer(final Layer down_Layer) {
		layerDelegate.setDownLayer( down_Layer ) ;
	}

}
