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

package playground.michalm.taxi.optimizer.rules;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.data.Request;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.zone.util.*;


public class UnplannedRequestZonalRegistry
{
    private final ZonalSystem zonalSystem;

    private final Map<Id<Request>, TaxiRequest>[] requestsInZones;

    private int requestCount = 0;


    @SuppressWarnings("unchecked")
    public UnplannedRequestZonalRegistry(ZonalSystem zonalSystem)
    {
        this.zonalSystem = zonalSystem;

        this.requestsInZones = (Map<Id<Request>, TaxiRequest>[])new Map[zonalSystem.getZoneCount()];
        for (int i = 0; i < requestsInZones.length; i++) {
            requestsInZones[i] = new HashMap<>();
        }
    }


    //after submitted
    public void addRequest(TaxiRequest request)
    {
        int zoneIdx = getZone(request);
        requestsInZones[zoneIdx].put(request.getId(), request);
        requestCount++;
    }


    //after scheduled
    public void removeRequest(TaxiRequest request)
    {
        int zoneIdx = getZone(request);
        requestsInZones[zoneIdx].remove(request.getId());
        requestCount--;
    }
    
    
    public Iterable<TaxiRequest> findNearestRequests(Node node, int minCount)
    {
        Iterable<Integer> zonesIdxByDistance = ((SquareGridSystem)zonalSystem).getZonesIdxByDistance(node);
        List<TaxiRequest> nearestReqs = new ArrayList<>();
        
        for (int idx : zonesIdxByDistance) {
            nearestReqs.addAll(requestsInZones[idx].values());
            if (nearestReqs.size() >= minCount) {
                return nearestReqs;
            }
        }

        return nearestReqs;
    }
    

    private int getZone(TaxiRequest request)
    {
        return zonalSystem.getZoneIdx(request.getFromLink().getFromNode());
    }


    public Map<Id<Request>, TaxiRequest> getRequestsInZone(Node node)
    {
        return requestsInZones[zonalSystem.getZoneIdx(node)];//TODO return immutables?
    }


    public Map<Id<Request>, TaxiRequest> getRequestsInZone(int zoneIdx)
    {
        return requestsInZones[zoneIdx];//TODO return immutables?
    }


    public int getRequestCount()
    {
        return requestCount;
    }
}
