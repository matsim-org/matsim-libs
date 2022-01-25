
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

import java.util.Map;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

/*
 * Wraps a Link into a Facility with a specific coordinate.
 * Useful for, e.g., Access and egress leg distance calculations
 */
public final class LinkWrapperFacilityWithSpecificCoord implements Facility, Identifiable<ActivityFacility> {

	private final Link wrappedLink;
	private final Coord wrappedCoord;

	public LinkWrapperFacilityWithSpecificCoord(final Link linkToWrap, final Coord coordToWrap) {
		wrappedLink = linkToWrap;
		wrappedCoord = coordToWrap;
	}

	@Override
	public Coord getCoord() {
		return wrappedCoord;
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
		return wrappedLink.getId();
	}

	@Override
	public String toString() {
		return "[LinkWrapperFacilityWithSpecificCoord: wrappedLink="+ wrappedLink +", wrapped Coord: "+wrappedCoord+"]";
	}
}
