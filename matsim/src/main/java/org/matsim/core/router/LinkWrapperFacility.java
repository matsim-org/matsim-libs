
/* *********************************************************************** *
 * project: org.matsim.*
 * LinkWrapperFacility.java
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
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

import java.util.Map;

/*
 * Wraps a Link into a Facility.
 */
public final class LinkWrapperFacility implements Facility, Identifiable<ActivityFacility> {
	
	private final Link wrapped;

	public LinkWrapperFacility(final Link toWrap) {
		wrapped = toWrap;
	}

	@Override
	public Coord getCoord() {
		return wrapped.getCoord();
	}

	@Override
	public Id<ActivityFacility> getId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Id<Link> getLinkId() {
		return wrapped.getId();
	}

	@Override
	public String toString() {
		return "[LinkWrapperFacility: wrapped="+wrapped+"]";
	}
}
