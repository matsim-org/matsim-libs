/* *********************************************************************** *
 * project: org.matsim.*
 * TransitStopFacility.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.transitSchedule;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.transitSchedule.api.TransitStopFacility;

/**
 * A facility (infrastructure) describing a public transport stop.
 *
 * @author mrieser
 */
public class TransitStopFacilityImpl implements TransitStopFacility {

	private static final long serialVersionUID = 1L;

	private final Id id;
	private Id stopPostAreaId;
	private final Coord coord;
	private Link link = null;
	private final boolean isBlockingLane;
	private String name = null;

	protected TransitStopFacilityImpl(final Id id, final Coord coord, final boolean isBlockingLane) {
		this.id = id;
		this.coord = coord;
		this.isBlockingLane = isBlockingLane;
		this.stopPostAreaId = id;
	}

	public Link getLink() {
		return this.link;
	}

	public void setLink(final Link link) {
		this.link = link;
	}

	public Id getLinkId() {
		if (this.link == null) {
			return null;
		}
		return this.link.getId();
	}

	public Coord getCoord() {
		return this.coord;
	}

	public Id getId() {
		return this.id;
	}

	public boolean getIsBlockingLane() {
		return this.isBlockingLane;
	}
	
	public void setName(final String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return "TransitStopFacilityImpl_" + this.id;
	}
	
	public Id getStopPostAreaId() {
		return stopPostAreaId;
	}
	
	public void setStopPostAreaId(Id stopPostAreaId) {
		this.stopPostAreaId = stopPostAreaId;
	}
	
}
