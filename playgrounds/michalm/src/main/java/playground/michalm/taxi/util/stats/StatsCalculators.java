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

package playground.michalm.taxi.util.stats;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.util.EnumCounter;

import com.google.common.collect.Iterables;

import playground.michalm.ev.UnitConversionRatios;
import playground.michalm.taxi.data.*;
import playground.michalm.taxi.data.TaxiRequest.TaxiRequestStatus;
import playground.michalm.taxi.optimizer.TaxiOptimizerContext;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;
import playground.michalm.taxi.scheduler.TaxiSchedulerUtils;
import playground.michalm.taxi.util.stats.StatsCollector.StatsCalculator;


public class StatsCalculators
{
    public static StatsCalculator<String> combineStatsCalculators(
            final StatsCalculator<?>... calculators)
    {
        return new StatsCalculator<String>() {
            @Override
            public String calculateStat()
            {
                String s = "";
                for (StatsCalculator<?> sc : calculators) {
                    s += sc.calculateStat() + "\t";
                }
                return s;
            }
        };
    }


    public static StatsCalculator<Integer> createIdleVehicleCounter(
            final TaxiOptimizerContext optimContext)
    {
        return new StatsCalculator<Integer>() {
            @Override
            public Integer calculateStat()
            {
                return Iterables.size(
                        Iterables.filter(optimContext.context.getVrpData().getVehicles().values(),
                                TaxiSchedulerUtils.createIsIdle(optimContext.scheduler)));
            }
        };
    }

    
    public static final String TAXI_TASK_TYPES_HEADER = combineValues(TaxiTaskType.values());
    

    public static StatsCalculator<String> createCurrentTaxiTaskOfTypeCounter(
            final VrpData taxiData)
    {
        return new StatsCalculator<String>() {
            @Override
            public String calculateStat()
            {
                EnumCounter<TaxiTaskType> counter = new EnumCounter<>(TaxiTaskType.class);

                for (Vehicle veh : taxiData.getVehicles().values()) {
                    if (veh.getSchedule().getStatus() == ScheduleStatus.STARTED) {
                        Schedule<TaxiTask> schedule = TaxiSchedules
                                .asTaxiSchedule(veh.getSchedule());
                        counter.increment(schedule.getCurrentTask().getTaxiTaskType());
                    }
                }

                String s = "";
                for (TaxiTaskType e : TaxiTaskType.values()) {
                    s += counter.getCount(e) + "\t";
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


    public static StatsCalculator<Integer> createRequestsWithStatusCounter(final ETaxiData taxiData,
            final TaxiRequestStatus requestStatus)
    {
        return new StatsCalculator<Integer>() {
            @Override
            public Integer calculateStat()
            {
                return TaxiRequests.countRequestsWithStatus(taxiData.getTaxiRequests().values(),
                        requestStatus);
            }
        };
    }


    public static StatsCalculator<Integer> createDischargedVehiclesCounter(final ETaxiData taxiData)
    {
        return new StatsCalculator<Integer>() {
            @Override
            public Integer calculateStat()
            {
                int count = 0;
                for (ETaxi t : taxiData.getETaxis().values()) {
                    if (t.getBattery().getSoc() < 0) {
                        count++;
                    }
                }
                return count;
            }
        };
    }


    public static StatsCalculator<Double> createMeanSocCalculator(final ETaxiData taxiData)
    {
        return new StatsCalculator<Double>() {
            @Override
            public Double calculateStat()
            {
                Mean mean = new Mean();
                for (ETaxi t : taxiData.getETaxis().values()) {
                    mean.increment(t.getBattery().getSoc());
                }
                return mean.getResult() / UnitConversionRatios.J_PER_kWh;//print out in [kWh]
            }
        };
    }
}
