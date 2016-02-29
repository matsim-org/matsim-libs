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

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.zone.Zone;


public class ZoneData
{
    public static class Entry
    {
        public final Zone zone;
        public final double potential;


        public Entry(Zone zone, double potential)
        {
            this.zone = zone;
            this.potential = potential;
        }
    }


    public final List<Entry> entries = new ArrayList<>();
    public final double totalPotential;
    public final double potentialToEnergy;


    public ZoneData(Map<Id<Zone>, Zone> zones, Map<Id<Zone>, Double> zonePotentials,
            double potentialToEnergy)
    {
        this.potentialToEnergy = potentialToEnergy;

        double potentialSum = 0;
        for (Zone z : zones.values()) {
            double p = zonePotentials.get(z.getId());
            entries.add(new Entry(z, p));
            potentialSum += p;
        }
        totalPotential = potentialSum;
    }
}
