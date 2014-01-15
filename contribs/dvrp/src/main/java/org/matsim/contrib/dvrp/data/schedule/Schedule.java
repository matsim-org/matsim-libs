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

import java.util.List;

import org.matsim.contrib.dvrp.data.model.Vehicle;


public interface Schedule<T extends Task>
{
    public enum ScheduleStatus
    {
        UNPLANNED(0), PLANNED(1), STARTED(2), COMPLETED(3);

        private final int stage;


        private ScheduleStatus(int stage)
        {
            this.stage = stage;
        }


        public boolean isUnplanned()
        {
            return this == UNPLANNED;
        }


        public boolean isPlanned()
        {
            return this == PLANNED;
        }


        public boolean isStarted()
        {
            return this == STARTED;
        }


        public boolean isCompleted()
        {
            return this == COMPLETED;
        }


        public boolean ge(ScheduleStatus other)
        {
            return this.stage >= other.stage;
        }


        public boolean gt(ScheduleStatus other)
        {
            return this.stage > other.stage;
        }


        public boolean le(ScheduleStatus other)
        {
            return this.stage <= other.stage;
        }


        public boolean lt(ScheduleStatus other)
        {
            return this.stage < other.stage;
        }


        public int compareStage(ScheduleStatus other)
        {
            return this.stage - other.stage;
        }
    };


    Vehicle getVehicle();


    List<T> getTasks();// unmodifiableList


    int getTaskCount();


    T getCurrentTask();


    ScheduleStatus getStatus();


    double getBeginTime();


    double getEndTime();


    // schedule modification functionality:

    void addTask(T task);


    void addTask(int taskIdx, T task);


    void removeLastTask();


    void removeTask(T task);


    T nextTask();// sets the next task as the current one, updates this schedule status
}
