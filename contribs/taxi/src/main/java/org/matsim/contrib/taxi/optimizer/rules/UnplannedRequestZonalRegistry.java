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

package org.matsim.contrib.taxi.optimizer.rules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.taxi.passenger.TaxiRequest;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.contrib.zone.ZonalSystems;
import org.matsim.contrib.zone.Zone;

public class UnplannedRequestZonalRegistry {
	private final ZonalSystem zonalSystem;
	private final Map<Id<Zone>, List<Zone>> zonesSortedByDistance;
	private final Map<Id<Zone>, Map<Id<Request>, TaxiRequest>> requestsInZones;

	private int requestCount = 0;

	public UnplannedRequestZonalRegistry(ZonalSystem zonalSystem) {
		this.zonalSystem = zonalSystem;
		zonesSortedByDistance = ZonalSystems.initZonesByDistance(zonalSystem.getZones());

		requestsInZones = new HashMap<>(zonalSystem.getZones().size());
		for (Id<Zone> id : zonalSystem.getZones().keySet()) {
			requestsInZones.put(id, new HashMap<>());
		}
	}

	// after submitted
	public void addRequest(TaxiRequest request) {
		Id<Zone> zoneId = getZoneId(request);

		if (requestsInZones.get(zoneId).put(request.getId(), request) != null) {
			throw new IllegalStateException(request + " is already in the registry");
		}

		requestCount++;
	}

	// after scheduled
	public void removeRequest(TaxiRequest request) {
		Id<Zone> zoneId = getZoneId(request);

		if (requestsInZones.get(zoneId).remove(request.getId()) == null) {
			throw new IllegalStateException(request + " is not in the registry");
		}

		requestCount--;
	}

	public Stream<TaxiRequest> findNearestRequests(Node node, int minCount) {
		return zonesSortedByDistance.get(zonalSystem.getZone(node).getId()).stream()//
				.flatMap(z -> requestsInZones.get(z.getId()).values().stream())//
				.limit(minCount);
	}

	private Id<Zone> getZoneId(TaxiRequest request) {
		return zonalSystem.getZone(request.getFromLink().getFromNode()).getId();
	}

	public int getRequestCount() {
		return requestCount;
	}
}
