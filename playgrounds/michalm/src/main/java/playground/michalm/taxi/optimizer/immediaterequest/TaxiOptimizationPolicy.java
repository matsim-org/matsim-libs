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

import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Task.TaskType;
import playground.michalm.taxi.optimizer.schedule.*;
import playground.michalm.taxi.optimizer.schedule.TaxiDriveTask.TaxiDriveType;


public interface TaxiOptimizationPolicy
{
    // it is called directly before switching to the next task
    boolean shouldOptimize(Task completedTask);


    static final TaxiOptimizationPolicy ALWAYS = new TaxiOptimizationPolicy() {
        public boolean shouldOptimize(Task completedTask)
        {
            return true;
        }
    };

    static final TaxiOptimizationPolicy AFTER_DRIVE_TASKS = new TaxiOptimizationPolicy() {
        public boolean shouldOptimize(Task completedTask)
        {
            return (completedTask.getType() == TaskType.DRIVE);
        }
    };

    static final TaxiOptimizationPolicy AFTER_REQUEST = new TaxiOptimizationPolicy() {
        public boolean shouldOptimize(Task completedTask)
        {
            switch (completedTask.getType()) {
                case DRIVE:
                    return ((TaxiDriveTask)completedTask).getDriveType() == TaxiDriveType.DELIVERY;

                case SERVE:
                case WAIT:
                    return false;

                default:
                    throw new IllegalStateException();
            }
        }
    };
}
