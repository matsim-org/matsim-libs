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

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Customizable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.scenario.CustomizableUtils;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * maintainer: mrieser / Senozon AG
 */
public class ActivityFacilityImpl implements ActivityFacility {
	
	private Customizable customizableDelegate;

	private final Map<String, ActivityOption> activities = new TreeMap<String, ActivityOption>();

	private String desc = null;

	private Coord coord;

	private Id<ActivityFacility> id;

	private Id<Link> linkId;

	protected ActivityFacilityImpl(final Id<ActivityFacility> id, final Coord center) {
		this.id = id;
		this.coord = center;
	}

	public double calcDistance(Coord otherCoord) {
		return CoordUtils.calcDistance(this.coord, otherCoord);
	}

	public final ActivityOptionImpl createAndAddActivityOption(final String type) {
		String type2 = type.intern();
		ActivityOptionImpl a = new ActivityOptionImpl(type2);
		addActivityOption(a);
		return a;
	}

	@Override
	public void addActivityOption(ActivityOption option) {
		String type = option.getType() ;
		if (this.activities.containsKey(type)) {
			throw new RuntimeException(this + "[type=" + type + " already exists]");
		}
		if (option.getFacility() != null && option.getFacility() != this) {
			throw new RuntimeException("This activity option already belongs to a different ActivityFacility!");
		}
		option.setFacility(this);
		this.activities.put(type, option);
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
	public Id<Link> getLinkId() {
		return this.linkId;
	}

	public void setLinkId(Id<Link> linkId) {
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
	public Id<ActivityFacility> getId() {
		return this.id;
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		if (this.customizableDelegate == null) {
			this.customizableDelegate = CustomizableUtils.createCustomizable();
		}
		return this.customizableDelegate.getCustomAttributes();
	}

}
