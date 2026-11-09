/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.run;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Restricts a DRT service to trips whose pickup and/or dropoff lies within an area, independently of the
 * operational scheme. If absent, every trip is allowed (default behaviour).
 *
 * @see org.matsim.contrib.drt.routing.DrtPickupDropoffRestriction
 */
public final class DrtPickupDropoffRestrictionParams extends ReflectiveConfigGroup {
	public static final String SET_NAME = "pickupDropoffRestriction";

	public static final String AREA_FILE = "areaFile";
	public static final String REQUIRED_IN_AREA = "requiredInArea";

	public enum RequiredInArea {
		/** The pickup has to lie within the area, the dropoff may lie anywhere. */
		pickup,
		/** The dropoff has to lie within the area, the pickup may lie anywhere. */
		dropoff,
		/** At least one of pickup and dropoff has to lie within the area. */
		pickupOrDropoff,
		/** Both pickup and dropoff have to lie within the area. */
		pickupAndDropoff
	}

	@Parameter(AREA_FILE)
	@Comment("Path to a GIS file (shp or gpkg) holding the polygons of the area. The location of a pickup or dropoff is"
			+ " the toNode of its link, as for the stops of operationalScheme=serviceAreaBased.")
	@NotBlank
	private String areaFile;

	@Parameter(REQUIRED_IN_AREA)
	@Comment("Which end of a DRT trip has to lie within the area: pickup (pickup in the area, dropoff anywhere),"
			+ " dropoff (dropoff in the area, pickup anywhere), pickupOrDropoff (at least one of them in the area) or"
			+ " pickupAndDropoff (both in the area). Trips violating the restriction are not routed with DRT.")
	@NotNull
	private RequiredInArea requiredInArea = RequiredInArea.pickupOrDropoff;

	public DrtPickupDropoffRestrictionParams() {
		super(SET_NAME);
	}

	public String getAreaFile() {
		return areaFile;
	}

	public void setAreaFile(String areaFile) {
		this.areaFile = areaFile;
	}

	public RequiredInArea getRequiredInArea() {
		return requiredInArea;
	}

	public void setRequiredInArea(RequiredInArea requiredInArea) {
		this.requiredInArea = requiredInArea;
	}
}
