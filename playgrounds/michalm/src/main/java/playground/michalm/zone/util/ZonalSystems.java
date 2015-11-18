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

package playground.michalm.zone.util;

import java.util.*;

import org.matsim.api.core.v01.network.*;

import com.google.common.primitives.Booleans;

import playground.michalm.zone.util.ZonalSystem.Zone;


public class ZonalSystems
{
    public interface DistanceCalculator<Z extends Zone>
    {
        double calcDistance(Z z1, Z z2);
    }


    public static <Z extends Zone> List<Z>[] initZonesByDistance(ZonalSystem<Z> zonalSystem,
            Network network, Z[] zones, final DistanceCalculator<? super Z> distCalc)
    {
        int count = zones.length;
        @SuppressWarnings("unchecked")
        List<Z>[] zonesByDistance = new List[count];

        //find zones with/without nodes
        boolean[] containsNodes = new boolean[count];
        for (Node n : network.getNodes().values()) {
            containsNodes[zonalSystem.getZone(n).getIdx()] = true;
        }

        int zonesWithNodesCount = Booleans.countTrue(containsNodes);
        List<Z> zonesWithNodes = new ArrayList<>(zonesWithNodesCount);
        for (int i = 0; i < count; i++) {
            if (containsNodes[i]) {
                zonesWithNodes.add(zones[i]);
            }
        }

        for (int i = 0; i < count; i++) {
            if (!containsNodes[i]) {
                continue;
            }

            final Z currentZone = zones[i];
            Collections.sort(zonesWithNodes, new Comparator<Z>() {
                public int compare(Z z1, Z z2)
                {
                    return Double.compare(distCalc.calcDistance(currentZone, z1),
                            distCalc.calcDistance(currentZone, z2));
                }
            });

            zonesByDistance[i] = new ArrayList<>(zonesWithNodes);
        }

        return zonesByDistance;
    }
}
