
/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityWrapperActivity.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.router;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class FacilityWrapperActivity implements Activity {
	private final Facility wrapped;

	public FacilityWrapperActivity(final Facility toWrap) {
		this.wrapped = toWrap;
	}

	@Override
	public OptionalTime getEndTime() {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public void setEndTime(double seconds) {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public void setEndTimeUndefined() {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public String getType() {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public void setType(String type) {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public Coord getCoord() {
		return wrapped.getCoord();
	}

	@Override
	public OptionalTime getStartTime() {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public void setStartTime(double seconds) {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public void setStartTimeUndefined() {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public OptionalTime getMaximumDuration() {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public void setMaximumDuration(double seconds) {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public void setMaximumDurationUndefined() {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public Id<Link> getLinkId() {
		return wrapped.getLinkId();
	}

	@Override
	public Id<ActivityFacility> getFacilityId() {
		throw new UnsupportedOperationException( "only facility fields access are supported" );
	}

	@Override
	public String toString() {
		return "[FacilityWrapper: wrapped="+wrapped+"]";
	}

	@Override
	public void setLinkId(Id<Link> id) {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void setFacilityId(Id<ActivityFacility> id) {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void setCoord(Coord coord) {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Attributes getAttributes() {
		throw new UnsupportedOperationException();
	}
}