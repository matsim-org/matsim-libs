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

package org.matsim.contrib.taxi.util.stats;

import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.taxi.data.*;
import org.matsim.contrib.taxi.data.TaxiRequest.TaxiRequestStatus;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.contrib.taxi.schedule.*;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.taxi.scheduler.TaxiSchedulerUtils;
import org.matsim.contrib.taxi.util.stats.TimeProfileCollector.ProfileCalculator;
import org.matsim.contrib.util.*;

import com.google.common.collect.Iterables;


public class TimeProfiles
{
    public static ProfileCalculator<String> combineProfileCalculators(
            final ProfileCalculator<?>... calculators)
    {
        return new ProfileCalculator<String>() {
            @Override
            public String calcCurrentPoint()
            {
                String s = "";
                for (ProfileCalculator<?> pc : calculators) {
                    s += pc.calcCurrentPoint() + "\t";
                }
                return s;
            }
        };
    }


    public static ProfileCalculator<Integer> createIdleVehicleCounter(
            final TaxiOptimizerContext optimContext)
    {
        return new ProfileCalculator<Integer>() {
            @Override
            public Integer calcCurrentPoint()
            {
                return Iterables.size(Iterables.filter(optimContext.taxiData.getVehicles().values(),
                        TaxiSchedulerUtils.createIsIdle(optimContext.scheduler)));
            }
        };
    }


    public static final String TAXI_TASK_TYPES_HEADER = combineValues(TaxiTaskType.values());


    public static ProfileCalculator<String> createCurrentTaxiTaskOfTypeCounter(
            final VrpData taxiData)
    {
        return new ProfileCalculator<String>() {
            @Override
            public String calcCurrentPoint()
            {
                LongEnumAdder<TaxiTaskType> counter = new LongEnumAdder<>(TaxiTaskType.class);

                for (Vehicle veh : taxiData.getVehicles().values()) {
                    if (veh.getSchedule().getStatus() == ScheduleStatus.STARTED) {
                        Schedule<TaxiTask> schedule = TaxiSchedules
                                .asTaxiSchedule(veh.getSchedule());
                        counter.increment(schedule.getCurrentTask().getTaxiTaskType());
                    }
                }

                String s = "";
                for (TaxiTaskType e : TaxiTaskType.values()) {
                    s += counter.getLong(e) + "\t";
                }
                return s;
            }
        };
    }


    public static String combineValues(Object[] values)
    {
        String s = "";
        for (Object v : values) {
            s += v.toString() + "\t";
        }
        return s;
    }


    public static ProfileCalculator<Integer> createRequestsWithStatusCounter(
            final TaxiData taxiData, final TaxiRequestStatus requestStatus)
    {
        return new ProfileCalculator<Integer>() {
            @Override
            public Integer calcCurrentPoint()
            {
                return TaxiRequests.countRequestsWithStatus(taxiData.getTaxiRequests().values(),
                        requestStatus);
            }
        };
    }
}
