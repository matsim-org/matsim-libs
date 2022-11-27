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

/**
 * An optimized implementation for interaction activities where start- and endtime is undefined and duration is 0.
 * Since these values are final, we do not need to store them and therefore we can save quite some memory.
 */
/*package*/ final class InteractionActivity implements Activity {

	private String type;
	private Coord coord = null;
	private Id<Link> linkId = null;
	private Id<ActivityFacility> facilityId = null;

	/*package*/ InteractionActivity(final String type) {
		this.type = type.intern();
	}

	@Override
	public final OptionalTime getEndTime() {
		return OptionalTime.undefined();
	}

	@Override
	public final void setEndTime(final double endTime) {
		throw new UnsupportedOperationException("Setting duration is not supported for InteractionActivity.");
	}

	@Override
	public final void setEndTimeUndefined() {
		// It is already undefined, i.e. do nothing.
	}

	@Override
	public final OptionalTime getStartTime() {
		return OptionalTime.undefined();
	}

	@Override
	public final void setStartTime(final double startTime) {
		throw new UnsupportedOperationException("Setting start time is not supported for InteractionActivity.");
	}

	@Override
	public final void setStartTimeUndefined() {
		// It is already undefined, i.e. do nothing.
	}

	@Override
	public OptionalTime getMaximumDuration() {
		return OptionalTime.zeroSeconds();
	}

	@Override
	public void setMaximumDuration(final double dur) {
		throw new UnsupportedOperationException("Setting duration is not supported for InteractionActivity.");
	}

	@Override
	public void setMaximumDurationUndefined() {
		// It is already undefined, i.e. do nothing.
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
	@Override
	public void setCoord(final Coord coord) {
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

	@Override
	public final void setFacilityId(final Id<ActivityFacility> facilityId) {
		this.facilityId = facilityId;
	}

	@Override
	public final void setLinkId(final Id<Link> linkId) {
		this.linkId = linkId;
	}

	@Override
	public Attributes getAttributes() {
		throw new UnsupportedOperationException("Using attributes is not supported for InteractionActivity.");
	}
	
	@Override
	public final String toString() {
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
