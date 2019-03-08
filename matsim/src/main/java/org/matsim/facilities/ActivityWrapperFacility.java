/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityWrapperFacility.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;

/**
 * When ActivityFacilities are not used, use this class
 * to wrap activity geographical information (coord and linkid)
 * in a facility interface (for example to pass to the router)
 * @author thibautd
 */
 class ActivityWrapperFacility implements Facility, Identifiable<ActivityFacility> {
	private final Activity wrapped;

	ActivityWrapperFacility( final Activity toWrap ) {
		this.wrapped = toWrap;
	}

	@Override
	public Coord getCoord() {
		return this.wrapped.getCoord();
	}

	@Override
	public Id<ActivityFacility> getId() {
		return this.wrapped.getFacilityId();
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Id<Link> getLinkId() {
		return this.wrapped.getLinkId();
	}

	@Override
	public String toString() {
		return "[ActivityWrapperFacility: wrapped="+this.wrapped+"]";
	}
}

