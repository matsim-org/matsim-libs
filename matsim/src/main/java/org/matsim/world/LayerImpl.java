/* *********************************************************************** *
 * project: org.matsim.*
 * Layer.java
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

package org.matsim.world;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkLayer;

/**
 * Basic collection of same geographical objects in MATSim.
 * @see NetworkLayer
 * @see ActivityFacilitiesImpl
 * @see ZoneLayer
 * @author Michael Balmer
 */
public class LayerImpl implements Serializable, Layer {
	private final Id type;
	private String name;

	transient Layer upLayer = null;
	transient Layer downLayer = null;

	final TreeMap<Id, MappedLocation> locations = new TreeMap<Id,MappedLocation>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public LayerImpl(final Id type, final String name) {
		this.type = type;
		this.name = name;
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	@Override
	@Deprecated // use of mapping rules is discouraged
	public final void setUpLayer(final Layer up_layer) {
		this.upLayer = up_layer;
	}

	@Override
	@Deprecated // use of mapping rules is discouraged
	public final void setDownLayer(final Layer layer) {
		this.downLayer = layer;
	}

	public final void setName(final String name) {
		this.name = name;
	}

	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////

	@Override
	@Deprecated // use of mapping rules is discouraged
	public final boolean removeUpLayer() {
		if (this.upLayer == null) { return true; }

		Iterator<MappedLocation> l_it = this.locations.values().iterator();
		while (l_it.hasNext()) { l_it.next().removeAllUpMappings(); }

		Map<Id,MappedLocation> lll = (Map<Id, MappedLocation>) this.upLayer.getLocations();
		l_it = lll.values().iterator();

		while (l_it.hasNext()) { l_it.next().removeAllDownMappings(); }

		this.upLayer.setDownLayer(null);

		this.upLayer = null;
		return true;

	}

	@Override
	@Deprecated // use of mapping rules is discouraged
	public final boolean removeDownLayer() {
		if (this.downLayer == null) { return true; }

		Iterator<MappedLocation> l_it = this.locations.values().iterator();
		while (l_it.hasNext()) { l_it.next().removeAllDownMappings(); }

		Map<Id,MappedLocation> lll = (Map<Id, MappedLocation>) this.downLayer.getLocations();
		l_it = lll.values().iterator();

		while (l_it.hasNext()) { l_it.next().removeAllUpMappings(); }

		this.downLayer.setUpLayer(null);
		this.upLayer = null;
		throw new UnsupportedOperationException(" the previous line is what I found but I think it should be this.downRule=null.  kai, jul09" ) ;
//		return true;

	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	@Deprecated // a "type" that returns an "Id" ???
	public final Id getType() {
		return this.type;
	}

	public final String getName() {
		return this.name;
	}

	@Override
	@Deprecated // use of mapping rules is discouraged
	public final Layer getUpLayer() {
		return this.upLayer;
	}

	@Override
	@Deprecated // use of mapping rules is discouraged
	public final Layer getDownLayer() {
		return this.downLayer;
	}

	public final MappedLocation getLocation(final Id location_id) {
		return this.locations.get(location_id);
	}

	/**
	 * Note: this is method is, I think, <em> not </em> quad-tree based, and therefore is rather slow in
	 * most cases.
     *
	 * @param coord A coordinate to which the nearest location should be returned.
	 *
	 * @return the Location with the smallest distance to the given coordinate. If multiple locations have
	 * the same minimal distance, all of them are returned.
	 */
	public final ArrayList<MappedLocation> getNearestLocations(final Coord coord) {
		return getNearestLocations(coord, null);
	}

	/**
	 * Note: this is method is, I think, <em> not </em> quad-tree based, and therefore is rather slow in
	 * most cases.
	 *
	 * @param coord A coordinate to which the nearest location should be returned.
	 * @param excludeLocation A location that should be ignored when finding the nearest location. Useful to
	 * find the nearest neighbor of the excluded location.
	 *
	 * @return the Location with the smallest distance to the given coordinate. If multiple locations have
	 * the same minimal distance, all of them are returned.
	 *
	 */
	public final ArrayList<MappedLocation> getNearestLocations(final Coord coord, final Location excludeLocation) {
		ArrayList<MappedLocation> locs = new ArrayList<MappedLocation>();
		double shortestDistance = Double.MAX_VALUE;
		Iterator<MappedLocation> loc_it = this.locations.values().iterator();
		while (loc_it.hasNext()) {
			MappedLocation loc = loc_it.next();
			if (loc != excludeLocation) {
				double distance = loc.calcDistance(coord);
				if (distance == shortestDistance) { locs.add(loc); }
				if (distance < shortestDistance) { shortestDistance = distance; locs.clear(); locs.add(loc); }
			}
		}
		return locs;
	}

	public final TreeMap<Id, ? extends MappedLocation> getLocations() {
		return this.locations;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public String toString() {
		return "[type=" + this.type + "]" +
		       "[name=" + this.name + "]" +
		       "[nof_locations=" + this.locations.size() + "]";
	}
}
