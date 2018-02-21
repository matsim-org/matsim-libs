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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.util.distance.DistanceUtils;

import com.google.common.collect.Maps;

public class ZonalSystems {
	public interface ZonalDistanceCalculator {
		double calcDistance(Zone z1, Zone z2);
	}

	public static Map<Id<Zone>, List<Zone>> initZonesByDistance(Map<Id<Zone>, Zone> zones) {
		Map<Id<Zone>, List<Zone>> zonesByDistance = Maps.newHashMapWithExpectedSize(zones.size());
		for (final Zone currentZone : zones.values()) {
			List<Zone> sortedZones = zones.values().stream()//
					.sorted(Comparator.comparing(z -> DistanceUtils.calculateSquaredDistance(currentZone, z)))//
					.collect(Collectors.toList());
			zonesByDistance.put(currentZone.getId(), sortedZones);
		}
		return zonesByDistance;
	}
}
