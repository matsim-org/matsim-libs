/* *********************************************************************** *
 * project: org.matsim.*
 * Act.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.core.population;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;

/**
 * Some comments:<ul>
 * <li> When we developed the API, we were afraid of potentially inconsistent Coord, LinkId and FacilityId.
 * As a result, one can set the Activity either from Coord or from LinkId, but not from both.  The FacilityId cannot be set at all
 * (in the API).
 * </ul>
 *
 */
public final class ActivityImpl implements Activity {
	// Assume this as input to iterations.  Cases:
	// Case (0): comes with coord and linkId.  No problem.
	// Case (1): comes with linkId but w/o coord.  Coord is (presumably) set in prepareForIterations.
	// Case (2): comes with coord but w/o linkId.  LinkId is (presumably) set in prepareForIterations.
	
	// Case (X): facilityId inconsistent with linkId, coord.  Idea: mobsim takes the facilityId and (a) checks the other
	// attribs or (b) ignores them.

	private double endTime = Time.UNDEFINED_TIME;

	/**
	 * Used for reporting outcomes in the scoring. Not interpreted for the demand.
	 */
	private double startTime = Time.UNDEFINED_TIME;

	private double dur = Time.UNDEFINED_TIME;

	private String type;
	private Coord coord = null;
	private Id<Link> linkId = null;
	private Id<ActivityFacility> facilityId = null;
	
	/*package*/ ActivityImpl(final String type) {
		this.type = type.intern();
	}

	public ActivityImpl(final String type, final Id<Link> linkId) {
		this(type);
		this.linkId = linkId ;
	}

	public ActivityImpl(final String type, final Coord coord) {
		this(type);
		this.coord = coord ;
	}

	public ActivityImpl(final String type, final Coord coord, final Id<Link> linkId) {
		this(type, linkId);
		this.coord = coord ;
	}

	public ActivityImpl(final Activity act) {
		this(act.getType());
		// Act coord could be null according to first c'tor!
		Coord c = act.getCoord() == null ? null : new Coord(act.getCoord().getX(), act.getCoord().getY());
		this.coord = c ;
		this.linkId = act.getLinkId();
		this.setStartTime(act.getStartTime());
		this.setEndTime(act.getEndTime());
		this.setMaximumDuration(act.getMaximumDuration());
		this.setFacilityId(act.getFacilityId());
	}

	@Override
	public final double getEndTime() {
		return this.endTime;
	}

	@Override
	public final void setEndTime(final double endTime) {
		this.endTime = endTime;
	}

	/**
	 * Used for reporting outcomes in the scoring. Not interpreted for the demand.
	 */
	@Override
	public final double getStartTime() {
		return this.startTime;
	}

	/**
	 * Used for reporting outcomes in the scoring. Not interpreted for the demand.
	 */
	@Override
	public final void setStartTime(final double startTime) {
		this.startTime = startTime;
	}

	@Override
	public final String getType() {
		return this.type;
	}

	@Override
	public final void setType(final String type) {
		this.type = type.intern();
	}

	@Override
	public final Coord getCoord() {
		return this.coord;
	}

	public void setCoord(final Coord coord) {
//		testForLocked();
		// I currently think that rather than enforcing data consistency we should just walk them from coordinate to link. kai, dec'15
		this.coord = coord;
	}
	@Override
	public final Id<Link> getLinkId() {
		return this.linkId;
	}

	@Override
	public final Id<ActivityFacility> getFacilityId() {
		return this.facilityId;
	}

	public final void setFacilityId(final Id<ActivityFacility> facilityId) {
//		testForLocked();
		this.facilityId = facilityId;
	}

	public final void setLinkId(final Id<Link> linkId) {
//		testForLocked();
		// I currently think that rather than enforcing data consistency we should just walk them from coordinate to link. kai, dec'15
		this.linkId = linkId;
	}

	@Override
	public final String toString() {
		return "[type=" + this.getType() + "]" +
				"[coord=" + this.getCoord() + "]" +
				"[linkId=" + this.linkId + "]" +
				"[startTime=" + Time.writeTime(this.getStartTime()) + "]" +
				"[endTime=" + Time.writeTime(this.getEndTime()) + "]" +
				"[duration=" + Time.writeTime(this.getMaximumDuration()) + "]" +
				"[facilityId=" + this.facilityId + "]" ;
	}

	@Override
	public double getMaximumDuration() {
		return this.dur;
	}

	@Override
	public void setMaximumDuration(final double dur) {
		this.dur = dur;
	}
	
//	private boolean locked = false ;
//	public final void setLocked() {
//		this.locked = true ;
//	}
//	private final void testForLocked() {
//		if ( this.locked ) {
//			throw new RuntimeException("too late to do this") ;
//		}
//	}
}
