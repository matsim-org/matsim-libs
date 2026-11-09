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

package org.matsim.contrib.drt.routing;

import java.net.URL;
import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.run.DrtPickupDropoffRestrictionParams;
import org.matsim.contrib.drt.run.DrtPickupDropoffRestrictionParams.RequiredInArea;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileReader;

import com.google.common.base.Verify;

/**
 * Decides whether a DRT trip is allowed given its pickup and dropoff link, see {@link DrtPickupDropoffRestrictionParams}.
 * The location of a pickup or dropoff is the toNode of its link. It is used during routing (see
 * {@link PickupDropoffRestrictionAccessEgressFacilityFinder}) and as a safety net in the QSim (see
 * {@link org.matsim.contrib.drt.passenger.DrtPickupDropoffRestrictionRequestValidator}), so both decide in the same way.
 */
public final class DrtPickupDropoffRestriction {

	private final List<PreparedGeometry> area;
	private final RequiredInArea requiredInArea;

	public DrtPickupDropoffRestriction(List<PreparedGeometry> area, RequiredInArea requiredInArea) {
		Verify.verify(!area.isEmpty(), "The pickup/dropoff restriction area does not contain any geometry.");
		this.area = area;
		this.requiredInArea = requiredInArea;
	}

	public static DrtPickupDropoffRestriction create(Config config, DrtPickupDropoffRestrictionParams params) {
		URL url = ConfigGroup.getInputFileURL(config.getContext(), params.getAreaFile());
		PreparedGeometryFactory factory = new PreparedGeometryFactory();
		List<PreparedGeometry> area = GeoFileReader.getAllFeatures(url)
				.stream()
				.map(feature -> factory.create((Geometry)feature.getDefaultGeometry()))
				.toList();
		return new DrtPickupDropoffRestriction(area, params.getRequiredInArea());
	}

	public boolean isAllowed(Link pickupLink, Link dropoffLink) {
		return switch (requiredInArea) {
			case pickup -> isInArea(pickupLink);
			case dropoff -> isInArea(dropoffLink);
			case pickupOrDropoff -> isInArea(pickupLink) || isInArea(dropoffLink);
			case pickupAndDropoff -> isInArea(pickupLink) && isInArea(dropoffLink);
		};
	}

	public boolean isInArea(Link link) {
		var point = MGC.coord2Point(link.getToNode().getCoord());
		return area.stream().anyMatch(geometry -> geometry.contains(point));
	}

	public RequiredInArea getRequiredInArea() {
		return requiredInArea;
	}
}
