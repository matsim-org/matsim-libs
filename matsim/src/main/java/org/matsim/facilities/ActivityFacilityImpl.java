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
import org.matsim.core.api.internal.MatsimDataClassImplMarkerInterface;
import org.matsim.core.scenario.CustomizableUtils;
import org.matsim.core.scenario.Lockable;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

/**
 * maintainer: mrieser / Senozon AG
 */
public class ActivityFacilityImpl implements ActivityFacility, MatsimDataClassImplMarkerInterface, Lockable {
	// After some thinking, we think that this design is ok:
	// * all methods are final (reduce maintenance for upstream maintainers)
	// * the class itself is not final
	// * the constructor is protected
	// * derived classes can thus extend to the attributes

	// yyyyyy I have to say that I am at this point not so happy.  Better make it final, make it package-protected, make 
	// all functionality accessible by interface or from static methods, and then let external users use delegation.  
	// People need to get un-used to casting things into the impl to get hold of "special" functionality. kai, jul'16
	
	private Customizable customizableDelegate;

	private final Map<String, ActivityOption> activities = new TreeMap<>();

	private String desc = null;

	private Coord coord;

	private Id<ActivityFacility> id;

	private Id<Link> linkId;

	private boolean locked = false ;

	private final Attributes attributes = new AttributesImpl();

	/**
	 * Deliberately protected, see {@link MatsimDataClassImplMarkerInterface}
	 * 
	 * @param id
	 * @param center
	 */
	protected ActivityFacilityImpl(final Id<ActivityFacility> id, final Coord center, final Id<Link> linkId) {
		this.id = id;
		this.coord = center;
		this.linkId = linkId;
	}

	public final double calcDistance(Coord otherCoord) {
		return CoordUtils.calcEuclideanDistance(this.coord, otherCoord);
	}

	public final ActivityOptionImpl createAndAddActivityOption(final String type) {
		String type2 = type.intern();
		ActivityOptionImpl a = new ActivityOptionImpl(type2);
		addActivityOption(a);
		return a;
	}

	@Override
	public final void addActivityOption(ActivityOption option) {
		String type = option.getType() ;
		if (this.activities.containsKey(type)) {
			throw new RuntimeException(this + "[type=" + type + " already exists]");
		}
		this.activities.put(type, option);
	}
	@Override
	public final void setCoord(Coord newCoord) {
		testForLocked() ;
		this.coord = newCoord;
	}

	public final void setDesc(String desc) {
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
	public final Id<Link> getLinkId() {
		return this.linkId;
	}

	public final void setLinkId(Id<Link> linkId) {
		this.linkId = linkId;
	}

	@Override
	public final String toString() {
		return "[" + super.toString() +
				   " ID=" + this.id +
				   "| linkID=" + this.linkId +
				   "| nof_activities=" + this.activities.size() +
				   "]";
	}

	@Override
	public final Coord getCoord() {
		return this.coord;
	}

	@Override
	public final Id<ActivityFacility> getId() {
		return this.id;
	}

	@Override
	public final Map<String, Object> getCustomAttributes() {
		if (this.customizableDelegate == null) {
			this.customizableDelegate = CustomizableUtils.createCustomizable();
		}
		return this.customizableDelegate.getCustomAttributes();
	}

	@Override
	public void setLocked() {
		this.locked = true ;
	}
	
	private void testForLocked() {
		if ( this.locked ) {
			throw new RuntimeException("too late to do this") ;
		}
	}

	@Override
	public Attributes getAttributes() {
		return this.attributes;
	}
}
