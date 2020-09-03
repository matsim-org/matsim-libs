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

import java.util.Map;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtZone {
	private final String id;
	private final PreparedGeometry preparedGeometry;
	private final Link targetLink;
	private final Map<Id<Link>, Link> links;

	public DrtZone(String id, PreparedGeometry preparedGeometry, Link targetLink, Map<Id<Link>, Link> links) {
		this.id = id;
		this.preparedGeometry = preparedGeometry;
		this.targetLink = targetLink;
		this.links = links;
	}

	public String getId() {
		return id;
	}

	public PreparedGeometry getPreparedGeometry() {
		return preparedGeometry;
	}

	public Link getTargetLink() {
		return targetLink;
	}

	public Coord getCentroid() {
		return targetLink.getCoord();
	}

	public Map<Id<Link>, Link> getLinks() {
		return links;
	}
}
