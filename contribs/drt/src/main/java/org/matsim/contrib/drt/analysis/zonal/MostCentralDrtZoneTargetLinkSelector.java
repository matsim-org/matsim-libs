/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.analysis.zonal;

import static java.util.stream.Collectors.toMap;
import static org.matsim.contrib.util.distance.DistanceUtils.calculateSquaredDistance;

import java.util.Map;

import org.matsim.api.core.v01.network.Link;

import one.util.streamex.StreamEx;

/**
 * @author tschlenther
 */
public class MostCentralDrtZoneTargetLinkSelector implements DrtZoneTargetLinkSelector {
	private final Map<DrtZone, Link> targetLinks;

	public MostCentralDrtZoneTargetLinkSelector(DrtZonalSystem drtZonalSystem) {
		targetLinks = drtZonalSystem.getZones()
				.values()
				.stream()
				.collect(toMap(zone -> zone, zone -> StreamEx.of(zone.getLinks())
						.minByDouble(link -> calculateSquaredDistance(zone.getCentroid(), link.getToNode().getCoord()))
						.orElseThrow()));
	}

	@Override
	public Link selectTargetLink(DrtZone zone) {
		return this.targetLinks.get(zone);
	}
}
