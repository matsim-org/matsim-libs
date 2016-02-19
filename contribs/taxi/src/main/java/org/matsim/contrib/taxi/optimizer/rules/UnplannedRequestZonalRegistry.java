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

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.zone.*;


public class UnplannedRequestZonalRegistry
{
    private final ZonalSystem zonalSystem;
    private final Map<Id<Zone>, List<Zone>> zonesSortedByDistance;
    private final Map<Id<Zone>, Map<Id<Request>, TaxiRequest>> requestsInZones;

    private int requestCount = 0;


    public UnplannedRequestZonalRegistry(ZonalSystem zonalSystem)
    {
        this.zonalSystem = zonalSystem;
        zonesSortedByDistance = ZonalSystems.initZonesByDistance(zonalSystem.getZones());

        requestsInZones = new HashMap<>(zonalSystem.getZones().size());
        for (Id<Zone> id : zonalSystem.getZones().keySet()) {
            requestsInZones.put(id, new HashMap<Id<Request>, TaxiRequest>());
        }
    }


    //after submitted
    public void addRequest(TaxiRequest request)
    {
        Id<Zone> zoneId = getZoneId(request);

        if (requestsInZones.get(zoneId).put(request.getId(), request) != null) {
            throw new IllegalStateException(request + " is already in the registry");
        }

        requestCount++;
    }


    //after scheduled
    public void removeRequest(TaxiRequest request)
    {
        Id<Zone> zoneId = getZoneId(request);

        if (requestsInZones.get(zoneId).remove(request.getId()) == null) {
            throw new IllegalStateException(request + " is not in the registry");
        }

        requestCount--;
    }


    public Iterable<TaxiRequest> findNearestRequests(Node node, int minCount)
    {
        Zone zone = zonalSystem.getZone(node);
        Iterable<? extends Zone> zonesByDistance = zonesSortedByDistance.get(zone.getId());
        List<TaxiRequest> nearestReqs = new ArrayList<>();

        for (Zone z : zonesByDistance) {
            nearestReqs.addAll(requestsInZones.get(z.getId()).values());

            if (nearestReqs.size() >= minCount) {
                return nearestReqs;
            }
        }

        return nearestReqs;
    }


    private Id<Zone> getZoneId(TaxiRequest request)
    {
        return zonalSystem.getZone(request.getFromLink().getFromNode()).getId();
    }


    public int getRequestCount()
    {
        return requestCount;
    }
}
