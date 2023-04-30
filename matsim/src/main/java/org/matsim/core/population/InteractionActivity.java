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
 * An optimized implementation for interaction activities where start- and endtime is undefined and duration is 0.
 * Since these values are final, we do not need to store them and therefore we can save quite some memory.
 */
/*package*/ final class InteractionActivity implements Activity {

	private static final Attributes EMPTY_ATTRIBUTES = new LazyAllocationAttributes(attributes -> {
		throw new RuntimeException("interaction activities cannot have attributes.");
	}, () -> null);
	
	private String type;
	private Coord coord = null;
	private Id<Link> linkId = null;
	private Id<ActivityFacility> facilityId = null;

	/*package*/ InteractionActivity(final String type) {
		this.type = type.intern();
	}

	@Override
	public OptionalTime getEndTime() {
		return OptionalTime.undefined();
	}

	@Override
	public void setEndTime(final double endTime) {
		throw new UnsupportedOperationException("Setting duration is not supported for InteractionActivity.");
	}

	@Override
	public void setEndTimeUndefined() {
		// It is already undefined, i.e. do nothing.
	}

	@Override
	public OptionalTime getStartTime() {
		return OptionalTime.undefined();
	}

	@Override
	public void setStartTime(final double startTime) {
		throw new UnsupportedOperationException("Setting start time is not supported for InteractionActivity.");
	}

	@Override
	public void setStartTimeUndefined() {
		// It is already undefined, i.e. do nothing.
	}

	@Override
	public OptionalTime getMaximumDuration() {
		return OptionalTime.zeroSeconds();
	}

	@Override
	public void setMaximumDuration(final double dur) {
		// For compatibility reasons: allow setting duration to 0 which is the default value anyway.
		if (dur != 0) throw new UnsupportedOperationException("Setting duration is not supported for InteractionActivity.");
	}

	@Override
	public void setMaximumDurationUndefined() {
		throw new UnsupportedOperationException("Setting duration to undefined is not supported for InteractionActivity.");
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
		this.facilityId = facilityId;
	}

	@Override
	public void setLinkId(final Id<Link> linkId) {
		this.linkId = linkId;
	}

	@Override
	public Attributes getAttributes() {
		return EMPTY_ATTRIBUTES;
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
				+ Time.writeTime(getEndTime())
				+ "]"
				+ "[duration="
				+ Time.writeTime(getMaximumDuration())
				+ "]"
				+ "[facilityId="
				+ this.facilityId + "]" ;
	}
}
