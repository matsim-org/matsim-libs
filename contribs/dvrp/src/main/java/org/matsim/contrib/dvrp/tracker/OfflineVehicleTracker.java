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

package org.matsim.contrib.dvrp.tracker;

public interface OfflineVehicleTracker
{
    double predictEndTime(double currentTime);


    double getPlannedEndTime();


    /**
     * Delay relative to the initial driveTask.getEndTime(), i.e. getInitialEndTime(), since the end
     * time my be updated periodically, thus driveTask.getEndTime() may return different results
     * over time)
     */
    double calculateCurrentDelay(double currentTime);
}
