/*
 * *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.routing;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.Facility;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtStopFacilityImpl implements DrtStopFacility {
	public static <F extends Identifiable<?> & Facility> DrtStopFacility createFromIdentifiableFacility(F facility) {
		return new DrtStopFacilityImpl(Id.create(facility.getId(), DrtStopFacility.class), facility.getLinkId(),
				facility.getCoord());
	}

	public static DrtStopFacility createFromLink(Link link) {
		return new DrtStopFacilityImpl(Id.create(link.getId(), DrtStopFacility.class), link.getId(), link.getToNode().getCoord());
	}

	private final Id<DrtStopFacility> id;
	private final Id<Link> linkId;
	private final Coord coord;

	public DrtStopFacilityImpl(Id<DrtStopFacility> id, Id<Link> linkId, Coord coord) {
		this.id = id;
		this.linkId = linkId;
		this.coord = coord;
	}

	@Override
	public Id<DrtStopFacility> getId() {
		return id;
	}

	@Override
	public Id<Link> getLinkId() {
		return linkId;
	}

	@Override
	public Coord getCoord() {
		return coord;
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		throw new UnsupportedOperationException();
	}
}
