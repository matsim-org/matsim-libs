/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.schedule;

import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;

import com.google.common.base.*;
import com.google.common.collect.Iterables;


public class TaxiSchedules
{
    public static final Predicate<TaxiTask> IS_PICKUP = new Predicate<TaxiTask>() {
        public boolean apply(TaxiTask t)
        {
            return t.getTaxiTaskType() == TaxiTaskType.PICKUP;
        };
    };

    public static final Function<TaxiTask, TaxiRequest> TAXI_TASK_TO_REQUEST = new Function<TaxiTask, TaxiRequest>() {
        public TaxiRequest apply(TaxiTask t)
        {
            if (t instanceof TaxiTaskWithRequest) {
                return ((TaxiTaskWithRequest)t).getRequest();
            }
            else {
                return null;
            }
        }
    };


    @SuppressWarnings("unchecked")
    public static Schedule<TaxiTask> asTaxiSchedule(Schedule<? extends Task> schedule)
    {
        return (Schedule<TaxiTask>)schedule;
    }


    public static Iterable<TaxiRequest> getTaxiRequests(Schedule<TaxiTask> schedule)
    {
        Iterable<TaxiTask> pickupTasks = Iterables.filter(schedule.getTasks(), IS_PICKUP);
        return Iterables.transform(pickupTasks, TAXI_TASK_TO_REQUEST);
    }


    public static TaxiTask getNextTaxiTask(TaxiTask task)
    {
        return asTaxiSchedule(task.getSchedule()).getTasks().get(task.getTaskIdx() + 1);
    }


    public static TaxiTask getPreviousTaxiTask(TaxiTask task)
    {
        return asTaxiSchedule(task.getSchedule()).getTasks().get(task.getTaskIdx() - 1);
    }
}
