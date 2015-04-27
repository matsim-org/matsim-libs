/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.optimizer;

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.michalm.taxi.scheduler.TaxiScheduler;


public class VehicleData
{
    public static class Entry
    {
        public final Vehicle vehicle;
        public final Link link;
        public final double time;
        public final boolean idle;


        public Entry(Vehicle vehicle, LinkTimePair linkTimePair, boolean idle)
        {
            this.vehicle = vehicle;
            this.link = linkTimePair.link;
            this.time = linkTimePair.time;
            this.idle = idle;
        }
    }


    public final List<Entry> entries = new ArrayList<>();

    public final int idleCount;
    public final int dimension;


    public VehicleData(TaxiOptimizerConfiguration optimConfig)
    {
        TaxiScheduler scheduler = optimConfig.scheduler;
        int idleCounter = 0;
        for (Vehicle v : optimConfig.context.getVrpData().getVehicles()) {
            LinkTimePair departure = scheduler.getImmediateDiversionOrEarliestIdleness(v);

            if (departure != null) {
                boolean idle = scheduler.isIdle(v);
                entries.add(new Entry(v, departure, idle));

                if (idle) {
                    idleCounter++;
                }
            }
        }

        idleCount = idleCounter;
        dimension = entries.size();
    }
}
