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
import org.matsim.core.gbl.Gbl;
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

	MappingRule upRule = null; // to aggregate
	MappingRule downRule = null; // to disaggregate

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

	@Deprecated // use of mapping rules is discouraged
	public final void setUpRule(final MappingRule up_rule) {
		if (up_rule == null) {
			Gbl.errorMsg(this.toString() + "[up_rule=null not allowed.]");
		}
		this.upRule = up_rule;
	}

	@Deprecated // use of mapping rules is discouraged
	public final void setDownRule(final MappingRule down_rule) {
		if (down_rule == null) {
			Gbl.errorMsg(this.toString() + "[down_rule=null not allowed.]");
		}
		this.downRule = down_rule;
	}

	public final void setName(final String name) {
		this.name = name;
	}

	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////

	@Deprecated // use of mapping rules is discouraged
	public final boolean removeUpRule() {
		if (this.upRule == null) { return true; }

//		if (this.upRule.getUpLayer().downRule == null) { Gbl.errorMsg("This should never happen!"); }
		if (this.upRule.getUpLayer().getDownRule() == null) { Gbl.errorMsg("This should never happen!"); }

		Iterator<MappedLocation> l_it = this.locations.values().iterator();
		while (l_it.hasNext()) { l_it.next().removeAllUpMappings(); }

//		l_it = this.upRule.getUpLayer().locations.values().iterator(); // manually replaced by following two lines.  kai, jul09
		Map<Id,MappedLocation> lll = (Map<Id, MappedLocation>) this.upRule.getUpLayer().getLocations();
		l_it = lll.values().iterator();

		while (l_it.hasNext()) { l_it.next().removeAllDownMappings(); }

//		this.upRule.getUpLayer().downRule = null;
		this.upRule.getUpLayer().forceDownRuleToNull();

		this.upRule = null;
		return true;
		
	}
	
	@Deprecated // do not use; this is here only for re-factoring purposes.  kai, jul09
	public void forceDownRuleToNull() {
		this.downRule = null ;
	}

	@Deprecated // do not use; this is here only for re-factoring purposes.  kai, jul09
	public void forceUpRuleToNull() {
		this.upRule = null ;
	}

	@Deprecated // use of mapping rules is discouraged
	public final boolean removeDownRule() {
		if (this.downRule == null) { return true; }
		if (this.downRule.getDownLayer().getUpRule() == null) { Gbl.errorMsg("This should never happen!"); }

		Iterator<MappedLocation> l_it = this.locations.values().iterator();
		while (l_it.hasNext()) { l_it.next().removeAllDownMappings(); }

//		l_it = this.downRule.getDownLayer().locations.values().iterator(); // manually replaced by following two lines.  kai, jul09
		Map<Id,MappedLocation> lll = (Map<Id, MappedLocation>) this.downRule.getDownLayer().getLocations();
		l_it = lll.values().iterator();

		while (l_it.hasNext()) { l_it.next().removeAllUpMappings(); }

//		this.downRule.getDownLayer().upRule = null; // manually replaced by following line.  kai, jul09
		this.downRule.getDownLayer().forceUpRuleToNull() ;

		this.upRule = null;
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

	@Deprecated // use of mapping rules is discouraged
	public final MappingRule getUpRule() {
		return this.upRule;
	}

	@Deprecated // use of mapping rules is discouraged
	public final MappingRule getDownRule() {
		return this.downRule;
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
		       "[up_rule=" + this.upRule + "]" +
		       "[down_rule=" + this.downRule + "]" +
		       "[nof_locations=" + this.locations.size() + "]";
	}
}
