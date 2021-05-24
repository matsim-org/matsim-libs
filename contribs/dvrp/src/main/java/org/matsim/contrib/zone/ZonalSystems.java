/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.zone;

import static java.util.stream.Collectors.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.google.common.collect.Maps;

//TODO add zone indexing?
public class ZonalSystems {
	public static Set<Zone> filterZonesWithNodes(Collection<? extends Node> nodes, ZonalSystem zonalSystem) {
		return nodes.stream().map(zonalSystem::getZone).collect(toSet());
	}

	public static List<Node> selectNodesWithinArea(Collection<? extends Node> nodes, List<PreparedGeometry> areaGeoms) {
		return nodes.stream().filter(node -> {
			Point point = MGC.coord2Point(node.getCoord());
			return areaGeoms.stream().anyMatch(serviceArea -> serviceArea.intersects(point));
		}).collect(toList());
	}

	public static Map<Zone, Node> computeMostCentralNodes(Collection<? extends Node> nodes, ZonalSystem zonalSystem) {
		BinaryOperator<Node> chooseMoreCentralNode = (n1, n2) -> {
			Zone zone = zonalSystem.getZone(n1);
			return DistanceUtils.calculateSquaredDistance(n1, zone) <= DistanceUtils.calculateSquaredDistance(n2,
					zone) ? n1 : n2;
		};
		return nodes.stream()
				.map(n -> Pair.of(n, zonalSystem.getZone(n)))
				.collect(toMap(Pair::getValue, Pair::getKey, chooseMoreCentralNode));
	}

	public static Map<Id<Zone>, List<Zone>> initZonesByDistance(Map<Id<Zone>, Zone> zones) {
		Map<Id<Zone>, List<Zone>> zonesByDistance = Maps.newHashMapWithExpectedSize(zones.size());
		for (final Zone currentZone : zones.values()) {
			List<Zone> sortedZones = zones.values()
					.stream()
					.sorted(Comparator.comparing(z -> DistanceUtils.calculateSquaredDistance(currentZone, z)))
					.collect(Collectors.toList());
			zonesByDistance.put(currentZone.getId(), sortedZones);
		}
		return zonesByDistance;
	}
}
