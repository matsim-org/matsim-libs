/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingFacilityImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package eu.eunoiaproject.bikesharing.framework.scenario;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default implementation of a bike sharing facility.
 * @author thibautd
 */
public class BikeSharingFacilityImpl implements BikeSharingFacility {
	private final Coord coord;
	private final Id id;
	private final Id linkId;
	private final int capacity;
	private final int initialNumberOfBikes;

	private final Map<String, Object> customAttributes = new LinkedHashMap<String, Object>();

	public BikeSharingFacilityImpl(
			final Id id,
			final Coord coord,
			final Id linkId,
			final int capacity,
			final int initialNumberOfBikes) {
		this.id = id;
		this.coord = coord;
		this.linkId = linkId;
		this.capacity = capacity;
		this.initialNumberOfBikes = initialNumberOfBikes;
	}

	// /////////////////////////////////////////////////////////////////////////
	// facility interface
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public Coord getCoord() {
		return coord;
	}

	@Override
	public Id getId() {
		return id;
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		return customAttributes;
	}

	@Override
	public Id getLinkId() {
		return linkId;
	}

	// /////////////////////////////////////////////////////////////////////////
	// specific methods
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public int getCapacity() {
		return capacity;
	}

	@Override
	public int getInitialNumberOfBikes() {
		return initialNumberOfBikes;
	}
}

