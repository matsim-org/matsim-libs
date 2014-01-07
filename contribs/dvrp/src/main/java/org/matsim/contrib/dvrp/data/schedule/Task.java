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

package org.matsim.contrib.dvrp.data.schedule;

public interface Task
{
    public enum TaskType
    {
        STAY, DRIVE;
    }


    public enum TaskStatus
    {
        PLANNED, STARTED, PERFORMED;
    }


    TaskType getType();


    TaskStatus getStatus();


    // inclusive
    int getBeginTime();


    // exclusive
    int getEndTime();


    Schedule<? extends Task> getSchedule();


    int getTaskIdx();


    // SETTERS:
    void setBeginTime(int beginTime);


    void setEndTime(int endTime);
}
