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
    public static final Predicate<Task> IS_PICKUP = new Predicate<Task>() {
        public boolean apply(Task t)
        {
            return ((TaxiTask)t).getTaxiTaskType() == TaxiTaskType.PICKUP;
        };
    };

    public static final Function<Task, TaxiRequest> TAXI_TASK_TO_REQUEST = new Function<Task, TaxiRequest>() {
        public TaxiRequest apply(Task t)
        {
            if (t instanceof TaxiTaskWithRequest) {
                return ((TaxiTaskWithRequest)t).getRequest();
            }
            else {
                return null;
            }
        }
    };


    public static Iterable<TaxiRequest> getTaxiRequests(Schedule schedule)
    {
        Iterable<Task> pickupTasks = Iterables.filter(schedule.getTasks(), IS_PICKUP);
        return Iterables.transform(pickupTasks, TAXI_TASK_TO_REQUEST);
    }
}
