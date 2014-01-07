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

package playground.michalm.taxi.optimizer.immediaterequest;

import org.matsim.contrib.dvrp.data.schedule.Task.TaskType;

import playground.michalm.taxi.schedule.TaxiTask;


public interface TaxiOptimizationPolicy
{
    // it is called directly before switching to the next task
    boolean shouldOptimize(TaxiTask completedTask);


    static final TaxiOptimizationPolicy ALWAYS = new TaxiOptimizationPolicy() {
        public boolean shouldOptimize(TaxiTask completedTask)
        {
            return true;
        }
    };

    static final TaxiOptimizationPolicy AFTER_DRIVE_TASKS = new TaxiOptimizationPolicy() {
        public boolean shouldOptimize(TaxiTask completedTask)
        {
            return (completedTask.getType() == TaskType.DRIVE);
        }
    };

    static final TaxiOptimizationPolicy AFTER_REQUEST = new TaxiOptimizationPolicy() {
        public boolean shouldOptimize(TaxiTask completedTask)
        {
            switch (completedTask.getTaxiTaskType()) {
                case DROPOFF_STAY:
                    return true;

                default:
                    return false;
            }
        }
    };
}
