/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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
package org.matsim.contrib.dvrp.schedule;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

public interface ScheduleTimingUpdater {
    double REMOVE_STAY_TASK = Double.NEGATIVE_INFINITY;

    void updateBeforeNextTask(DvrpVehicle vehicle);

    void updateTimings(DvrpVehicle vehicle);

    void updateTimingsStartingFromTaskIdx(DvrpVehicle vehicle, int startIdx, double newBeginTime);

    interface StayTaskEndTimeCalculator {
        double calcNewEndTime(DvrpVehicle vehicle, StayTask task, double newBeginTime);
    }
}
