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

package org.matsim.contrib.common.zones;

import static java.util.stream.Collectors.*;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.util.DistanceUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

//TODO add zone indexing?
public class ZoneSystems {
	public static Set<Zone> filterZonesWithNodes(Collection<? extends Node> nodes, ZoneSystem zoneSystem) {
		return nodes.stream().map(zoneSystem::getZoneForNodeId).filter(Optional::isPresent).map(Optional::get).collect(toSet());
	}

	public static List<Node> selectNodesWithinArea(Collection<? extends Node> nodes, List<PreparedGeometry> areaGeoms) {
		return nodes.stream().filter(node -> {
			Point point = MGC.coord2Point(node.getCoord());
			return areaGeoms.stream().anyMatch(serviceArea -> serviceArea.intersects(point));
		}).collect(toList());
	}

	public static Map<Zone, Node> computeMostCentralNodes(Collection<? extends Node> nodes, ZoneSystem zoneSystem) {
		BinaryOperator<Node> chooseMoreCentralNode = (n1, n2) -> {
			Zone zone = zoneSystem.getZoneForNodeId(n1).orElseThrow();
			return DistanceUtils.calculateSquaredDistance(n1, zone) <= DistanceUtils.calculateSquaredDistance(n2,
					zone) ? n1 : n2;
		};
		return nodes.stream()
				.map(n -> Pair.of(n, zoneSystem.getZoneForNodeId(n).orElseThrow()))
				.collect(toMap(Pair::getValue, Pair::getKey, chooseMoreCentralNode));
	}

	public static IdMap<Zone, List<Zone>> initZonesByDistance(Map<Id<Zone>, Zone> zones) {
		IdMap<Zone, List<Zone>> zonesByDistance = new IdMap<>(Zone.class);
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
