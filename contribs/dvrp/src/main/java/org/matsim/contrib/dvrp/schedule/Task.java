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

package org.matsim.contrib.dvrp.schedule;

import org.matsim.contrib.dvrp.tracker.TaskTracker;


public interface Task
{
    public enum TaskStatus
    {
        PLANNED, STARTED, PERFORMED;
    }


    TaskStatus getStatus();


    // inclusive
    double getBeginTime();


    // exclusive
    double getEndTime();


    Schedule getSchedule();


    int getTaskIdx();


    void setBeginTime(double beginTime);


    void setEndTime(double endTime);


    TaskTracker getTaskTracker();


    void initTaskTracker(TaskTracker taskTracker);
}
