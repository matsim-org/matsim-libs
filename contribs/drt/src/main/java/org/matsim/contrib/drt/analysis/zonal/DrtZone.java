/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.analysis.zonal;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtZone {
	private final String id;
	private final Geometry geometry;
	private final Coord centroid;

	public DrtZone(String id, Geometry geometry) {
		this(id, geometry, MGC.point2Coord(geometry.getCentroid()));
	}

	public DrtZone(String id, Geometry geometry, Coord centroid) {
		this.id = id;
		this.geometry = geometry;
		this.centroid = centroid;
	}

	public String getId() {
		return id;
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public Coord getCentroid() {
		return centroid;
	}

	//TODO add Link closest to geometry.centroid
}
