/* *********************************************************************** *
 * project: org.matsim.*
 * Facility.java
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

package org.matsim.facilities;

import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.LocationType;
import org.matsim.network.Link;
import org.matsim.utils.geometry.Coord;
import org.matsim.world.AbstractLocation;

public class Facility extends AbstractLocation {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	private final TreeMap<String, Activity> activities = new TreeMap<String, Activity>();
	private String desc = null;
	
	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	protected Facility(final Facilities layer, final Id id, final Coord center) {
		super(layer,id,center);
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public double calcDistance(Coord coord) {
		return this.center.calcDistance(coord);
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final Activity createActivity(final String type) {
		if (this.activities.containsKey(type)) {
			Gbl.errorMsg(this + "[type=" + type + " already exists]");
		}
		String type2 = type.intern();
		Activity a = new Activity(type2, this);
		this.activities.put(type2, a);
		return a;
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public void setDesc(String desc) {
		if (desc == null) { this.desc = null; }
		else { this.desc = desc.intern(); }
	}
	
	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final String getDesc() {
		return this.desc;
	}
	
	public final TreeMap<String,Activity> getActivities() {
		return this.activities;
	}

	public final Activity getActivity(final String type) {
		return this.activities.get(type);
	}

	public final Link getLink() {
		if (this.down_mapping.isEmpty()) { return null; }
		if (this.down_mapping.size() > 1) { Gbl.errorMsg("Something is wrong!!! A facility contains at most one Link (as specified for the moment)!"); }
		return (Link)this.getDownMapping().get(this.down_mapping.firstKey());
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return super.toString() +
		       "[nof_activities=" + this.activities.size() + "]";
	}

	public LocationType getLocationType() {
		return LocationType.FACILITY;
	}
}
