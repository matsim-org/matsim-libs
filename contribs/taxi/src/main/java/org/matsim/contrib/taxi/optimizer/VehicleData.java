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

package org.matsim.contrib.taxi.optimizer;

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.taxi.scheduler.TaxiScheduleInquiry;


public class VehicleData
{
    public static class Entry
    {
        public final int idx;
        public final Vehicle vehicle;
        public final Link link;
        public final double time;
        public final boolean idle;


        public Entry(int idx, Vehicle vehicle, LinkTimePair linkTimePair, boolean idle)
        {
            this.idx = idx;
            this.vehicle = vehicle;
            this.link = linkTimePair.link;
            this.time = linkTimePair.time;
            this.idle = idle;
        }
    }


    public final List<Entry> entries = new ArrayList<>();
    public final int idleCount;
    public final int dimension;


    public VehicleData(TaxiOptimizerContext optimContext)
    {
        this(optimContext, 2 * 24 * 3600);//max 48 hours of departure delay (== not a real constraint)
    }


    //skipping vehicles with departure.time > curr_time + maxDepartureDelay
    public VehicleData(TaxiOptimizerContext optimContext, double planningHorizon)
    {
        double currTime = optimContext.timer.getTimeOfDay();
        double maxDepartureTime = currTime + planningHorizon;
        TaxiScheduleInquiry scheduleInquiry = optimContext.scheduler;

        int idx = 0;
        int idleCounter = 0;
        for (Vehicle v : optimContext.taxiData.getVehicles().values()) {
            LinkTimePair departure = scheduleInquiry.getImmediateDiversionOrEarliestIdleness(v);

            if (departure != null && departure.time <= maxDepartureTime) {
                boolean idle = departure.time == currTime //(small optimization to avoid unnecessary calls to Scheduler.isIdle())
                        && scheduleInquiry.isIdle(v);

                entries.add(new Entry(idx++, v, departure, idle));

                if (idle) {
                    idleCounter++;
                }
            }
        }

        idleCount = idleCounter;
        dimension = entries.size();
    }
}
