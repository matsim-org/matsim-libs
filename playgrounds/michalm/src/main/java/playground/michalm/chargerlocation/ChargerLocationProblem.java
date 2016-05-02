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

package playground.michalm.chargerlocation;

import org.matsim.contrib.util.distance.DistanceCalculator;
import org.matsim.contrib.zone.Zone;


public class ChargerLocationProblem
{
    static class ChargerLocationSolution
    {
        final int[] x;
        final double[][] f;


        ChargerLocationSolution(int[] x, double[][] f)
        {
            this.x = x;
            this.f = f;
        }
    }


    final ZoneData zoneData;
    final ChargerData chargerData;
    final DistanceCalculator distanceCalculator;
    final double maxDistance;
    final int maxChargersInZone;
    final int maxChargers;

    final int I;
    final int J;

    final double[][] distances;


    public ChargerLocationProblem(ZoneData zoneData, ChargerData chargerData,
            DistanceCalculator distanceCalculator, double maxDistance, int maxChargersInZone,
            int maxChargers)
    {
        this.zoneData = zoneData;
        this.chargerData = chargerData;
        this.distanceCalculator = distanceCalculator;
        this.maxChargersInZone = maxChargersInZone;
        this.maxDistance = maxDistance;
        this.maxChargers = maxChargers;

        I = zoneData.entries.size();
        J = chargerData.locations.size();

        distances = calcDistanceMatrix();
    }


    private double[][] calcDistanceMatrix()
    {
        double[][] d = new double[I][J];
        for (int i = 0; i < I; i++) {
            Zone zone_i = zoneData.entries.get(i).zone;

            for (int j = 0; j < J; j++) {
                ChargerLocation location_j = chargerData.locations.get(j);
                d[i][j] = distanceCalculator.calcDistance(zone_i.getCoord(), location_j.getCoord());
            }
        }
        return d;
    }
}
