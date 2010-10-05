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

package org.matsim.core.facilities;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordUtils;

public class ActivityFacilityImpl implements ActivityFacility {

	private final Map<String, ActivityOption> activities = new TreeMap<String, ActivityOption>();

	private String desc = null;

	private Coord coord;

	private Id id;

	private Id linkId;

	protected ActivityFacilityImpl(final Id id, final Coord center) {
		this.id = id;
		this.coord = center;
	}

	public double calcDistance(Coord coord) {
		return CoordUtils.calcDistance(this.coord, coord);
	}

	public final ActivityOptionImpl createActivityOption(final String type) {
		if (this.activities.containsKey(type)) {
			Gbl.errorMsg(this + "[type=" + type + " already exists]");
		}
		String type2 = type.intern();
		ActivityOptionImpl a = new ActivityOptionImpl(type2, this);
		this.activities.put(type2, a);
		return a;
	}

	public final void setCoord(Coord newCoord) {
		this.coord = newCoord;
	}

	public void setDesc(String desc) {
		if (desc == null) { this.desc = null; }
		else { this.desc = desc.intern(); }
	}

	public final String getDesc() {
		return this.desc;
	}

	@Override
	public final Map<String, ActivityOption> getActivityOptions() {
		return this.activities;
	}

	@Override
	public Id getLinkId() {
		return this.linkId;
	}

	public void setLinkId(Id linkId) {
		this.linkId = linkId;
	}

	@Override
	public final String toString() {
		return super.toString() +
		       "[nof_activities=" + this.activities.size() + "]";
	}

	@Override
	public Coord getCoord() {
		return this.coord;
	}

	@Override
	public Id getId() {
		return this.id;
	}

}
