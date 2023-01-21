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
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.LazyAllocationAttributes;

/**
 * Some comments:<ul>
 * <li> When we developed the API, we were afraid of potentially inconsistent Coord, LinkId and FacilityId.
 * As a result, one can set the Activity either from Coord or from LinkId, but not from both.  The FacilityId cannot be set at all
 * (in the API).
 * </ul>
 *
 */
/* package */ final class ActivityImpl implements Activity {
	// Assume this as input to iterations.  Cases:
	// Case (0): comes with coord and linkId.  No problem.
	// Case (1): comes with linkId but w/o coord.  Coord is (presumably) set in prepareForIterations.
	// Case (2): comes with coord but w/o linkId.  LinkId is (presumably) set in prepareForIterations.

	// Case (X): facilityId inconsistent with linkId, coord.  Idea: mobsim takes the facilityId and (a) checks the other
	// attribs or (b) ignores them.

	private static final double UNDEFINED_TIME = Double.NEGATIVE_INFINITY;
	private double endTime = UNDEFINED_TIME;

	/**
	 * Used for reporting outcomes in the scoring. Not interpreted for the demand.
	 */
	private double startTime = UNDEFINED_TIME;

	private double dur = UNDEFINED_TIME;

	private String type;
	private Coord coord = null;
	private Id<Link> linkId = null;
	private Id<ActivityFacility> facilityId = null;

	private Attributes attributes = null;
	
	/*package*/ ActivityImpl(final String type) {
		this.type = type.intern();
	}

	private static OptionalTime asOptionalTime(double seconds) {
		return seconds == UNDEFINED_TIME ? OptionalTime.undefined() : OptionalTime.defined(seconds);
	}

	@Override
	public OptionalTime getEndTime() {
		return asOptionalTime(this.endTime);
	}

	@Override
	public void setEndTime(final double endTime) {
		OptionalTime.assertDefined(endTime);
		this.endTime = endTime;
	}

	@Override
	public void setEndTimeUndefined() {
		this.endTime = UNDEFINED_TIME;
	}

	/**
	 * Used for reporting outcomes in the scoring. Not interpreted for the demand.
	 */
	@Override
	public OptionalTime getStartTime() {
		return asOptionalTime(this.startTime);
	}

	/**
	 * Used for reporting outcomes in the scoring. Not interpreted for the demand.
	 */
	@Override
	public void setStartTime(final double startTime) {
		OptionalTime.assertDefined(startTime);
		this.startTime = startTime;
	}

	public void setStartTimeUndefined() {
		this.startTime = UNDEFINED_TIME;
	}

	@Override
	public String getType() {
		return this.type;
	}

	@Override
	public void setType(final String type) {
		this.type = type.intern();
	}

	@Override
	public Coord getCoord() {
		return this.coord;
	}
	@Override
	public void setCoord(final Coord coord) {
//		testForLocked();
		// I currently think that rather than enforcing data consistency we should just walk them from coordinate to link. kai, dec'15
		this.coord = coord;
	}
	@Override
	public Id<Link> getLinkId() {
		return this.linkId;
	}

	@Override
	public Id<ActivityFacility> getFacilityId() {
		return this.facilityId;
	}

	@Override
	public void setFacilityId(final Id<ActivityFacility> facilityId) {
//		testForLocked();
		this.facilityId = facilityId;
	}

	@Override
	public void setLinkId(final Id<Link> linkId) {
//		testForLocked();
		// I currently think that rather than enforcing data consistency we should just walk them from coordinate to link. kai, dec'15
		this.linkId = linkId;
	}

	@Override
	public String toString() {
		return "act [type="
				+ this.getType()
				+ "]"
				+ "[coord="
				+ this.getCoord()
				+ "]"
				+ "[linkId="
				+ this.linkId
				+ "]"
				+ "[startTime="
				+ Time.writeTime(getStartTime())
				+ "]"
				+ "[endTime="
				+ Time.writeTime(this.endTime)
				+ "]"
				+ "[duration="
				+ Time.writeTime(getMaximumDuration())
				+ "]"
				+ "[facilityId="
				+ this.facilityId + "]" ;
	}

	@Override
	public OptionalTime getMaximumDuration() {
		return asOptionalTime(this.dur);
	}

	@Override
	public void setMaximumDuration(final double dur) {
		OptionalTime.assertDefined(dur);
		this.dur = dur;
	}

	@Override
	public void setMaximumDurationUndefined() {
		this.dur = UNDEFINED_TIME;
	}

	@Override
	public Attributes getAttributes() {
		if (this.attributes != null) {
			return this.attributes;
		}
		return new LazyAllocationAttributes(attributes -> this.attributes = attributes, () -> this.attributes);
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
