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

import java.util.List;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtZone {
	private final String id;
	private final PreparedGeometry preparedGeometry;
	private final List<Link> links;

	public DrtZone(String id, PreparedGeometry preparedGeometry, List<Link> links) {
		this.id = id;
		this.preparedGeometry = preparedGeometry;
		this.links = links;
	}

	public String getId() {
		return id;
	}

	public PreparedGeometry getPreparedGeometry() {
		return preparedGeometry;
	}

	public Coord getCentroid() { return MGC.point2Coord(preparedGeometry.getGeometry().getCentroid());	}

	public List<Link> getLinks() { return links; }
}
