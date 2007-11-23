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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facilities;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.identifiers.IdI;

/**
 * Basic collection of same geographical objects in MATSim.
 * @see NetworkLayer
 * @see Facilities
 * @see ZoneLayer
 * @author Michael Balmer
 */
public abstract class Layer {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	protected final IdI type;
	protected String name;

	protected MappingRule up_rule = null; // to aggregate
	protected MappingRule down_rule = null; // to disaggregate

	protected final TreeMap<IdI,Location> locations = new TreeMap<IdI,Location>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	protected Layer(final IdI type, final String name) {
		this.type = type;
		this.name = name;
	}
	
	protected Layer(final String type, final String name) {
		this(new Id(type),name);
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	protected final void setUpRule(final MappingRule up_rule) {
		if (up_rule == null) {
			Gbl.errorMsg(this.toString() + "[up_rule=null not allowed.]");
		}
		this.up_rule = up_rule;
	}

	protected final void setDownRule(final MappingRule down_rule) {
		if (down_rule == null) {
			Gbl.errorMsg(this.toString() + "[down_rule=null not allowed.]");
		}
		this.down_rule = down_rule;
	}
	
	public final void setName(final String name) {
		this.name = name;
	}

	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////
	
	protected final boolean removeUpRule() {
		if (this.up_rule == null) { return true; }
		if (this.up_rule.getUpLayer().down_rule == null) { Gbl.errorMsg("This should never happen!"); }
		
		Iterator<Location> l_it = this.locations.values().iterator();
		while (l_it.hasNext()) { l_it.next().removeAllUpMappings(); }

		l_it = this.up_rule.getUpLayer().locations.values().iterator();
		while (l_it.hasNext()) { l_it.next().removeAllDownMappings(); }
		
		this.up_rule.getUpLayer().down_rule = null;
		this.up_rule = null;
		return true;
	}

	protected final boolean removeDownRule() {
		if (this.down_rule == null) { return true; }
		if (this.down_rule.getDownLayer().up_rule == null) { Gbl.errorMsg("This should never happen!"); }
		
		Iterator<Location> l_it = this.locations.values().iterator();
		while (l_it.hasNext()) { l_it.next().removeAllDownMappings(); }

		l_it = this.down_rule.getDownLayer().locations.values().iterator();
		while (l_it.hasNext()) { l_it.next().removeAllUpMappings(); }
		
		this.down_rule.getDownLayer().up_rule = null;
		this.up_rule = null;
		return true;
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final IdI getType() {
		return this.type;
	}

	public final String getName() {
		return this.name;
	}

	public final MappingRule getUpRule() {
		return this.up_rule;
	}

	public final MappingRule getDownRule() {
		return this.down_rule;
	}

	public final Location getLocation(final IdI location_id) {
		return this.locations.get(location_id);
	}

	public final Location getLocation(final String location_id) {
		return this.locations.get(new Id(location_id));
	}

	public final Location getLocation(final int location_id) {
		return this.getLocation(new Id(location_id));
	}

	public final ArrayList<Location> getLocations(final CoordI center) {
		ArrayList<Location> locs = new ArrayList<Location>();
		Iterator<Location> loc_it = this.locations.values().iterator();
		while (loc_it.hasNext()) {
			Location loc = loc_it.next();
			if (loc.getCenter().equals(center)) { locs.add(loc); }
		}
		return locs;
	}

	public final ArrayList<Location> getNearestLocations(final CoordI coord, final Location excludeLocation) {
		ArrayList<Location> locs = new ArrayList<Location>();
		double shortestDistance = Double.MAX_VALUE;
		Iterator<Location> loc_it = this.locations.values().iterator();
		while (loc_it.hasNext()) {
			Location loc = loc_it.next();
			if (loc != excludeLocation) {
				double distance = loc.calcDistance(coord);
				if (distance == shortestDistance) { locs.add(loc); }
				if (distance < shortestDistance) { shortestDistance = distance; locs.clear(); locs.add(loc); }
			}
		}
		return locs;
	}

	public final TreeMap<IdI,Location> getLocations() {
		return this.locations;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public String toString() {
		return "[type=" + this.type + "]" +
		       "[name=" + this.name + "]" +
		       "[up_rule=" + this.up_rule + "]" +
		       "[down_rule=" + this.down_rule + "]" +
		       "[nof_locations=" + this.locations.size() + "]";
	}
}
