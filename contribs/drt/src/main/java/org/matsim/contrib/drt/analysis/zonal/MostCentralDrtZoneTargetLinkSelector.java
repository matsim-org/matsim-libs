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

import java.util.Comparator;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

/**
 * @author tschlenther
 */
public class MostCentralDrtZoneTargetLinkSelector implements DrtZoneTargetLinkSelector {
	private final Map<DrtZone, Link> targetLinks;

	public MostCentralDrtZoneTargetLinkSelector(DrtZonalSystem drtZonalSystem) {
		targetLinks = drtZonalSystem.getZones()
				.values()
				.stream()
				.collect(toMap(zone -> zone, zone -> zone.getLinks().stream().min(
						//1. choose the most central toNode of all links (could be more than one node!)
						//2. if more than one link (quite normal even if only one node found),
						//   choose the one (potentially) easiest to access (most central fromNode)
						Comparator.<Link>comparingDouble(link -> squaredDistance(zone, link.getToNode()))//
								.thenComparing(link -> squaredDistance(zone, link.getFromNode()))).orElseThrow()));
	}

	@Override
	public Link selectTargetLink(DrtZone zone) {
		return this.targetLinks.get(zone);
	}

	private double squaredDistance(DrtZone zone, Node node) {
		return calculateSquaredDistance(zone.getCentroid(), node.getCoord());
	}
}
