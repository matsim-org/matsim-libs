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

package playground.michalm.zone;

import java.util.*;

import org.matsim.api.core.v01.Id;

import com.google.common.collect.Maps;

import playground.michalm.util.distance.DistanceCalculators;


public class ZonalSystems
{
    public interface ZonalDistanceCalculator
    {
        double calcDistance(Zone z1, Zone z2);
    }


    public static final ZonalDistanceCalculator BEELINE_DISTANCE_CALCULATOR = new ZonalDistanceCalculator() {
        @Override
        public double calcDistance(Zone z1, Zone z2)
        {
            return DistanceCalculators.BEELINE_DISTANCE_CALCULATOR.calcDistance(z1.getCoord(),
                    z2.getCoord());
        }
    };


    public static Map<Id<Zone>, List<Zone>> initZonesByDistance(Map<Id<Zone>, Zone> zones)
    {
        return initZonesByDistance(zones, BEELINE_DISTANCE_CALCULATOR);
    }


    public static Map<Id<Zone>, List<Zone>> initZonesByDistance(Map<Id<Zone>, Zone> zones,
            final ZonalDistanceCalculator distCalc)
    {
        Map<Id<Zone>, List<Zone>> zonesByDistance = Maps.newHashMapWithExpectedSize(zones.size());
        List<Zone> sortedList = new ArrayList<>(zones.values());

        for (final Zone currentZone : zones.values()) {
            Collections.sort(sortedList, new Comparator<Zone>() {
                public int compare(Zone z1, Zone z2)
                {
                    return Double.compare(distCalc.calcDistance(currentZone, z1),
                            distCalc.calcDistance(currentZone, z2));
                }
            });

            zonesByDistance.put(currentZone.getId(), new ArrayList<>(sortedList));
        }

        return zonesByDistance;
    }
}
