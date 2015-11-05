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


public abstract class AbstractTask
    implements Task
{
    // ==== BEGIN: fields managed by ScheduleImpl
    Schedule<? extends AbstractTask> schedule;
    int taskIdx;

    TaskStatus status;
    // ==== END: fields managed by ScheduleImpl

    private double beginTime;
    private double endTime;

    private TaskTracker taskTracker;


    public AbstractTask(double beginTime, double endTime)
    {
        if (beginTime > endTime) {
            throw new IllegalArgumentException();
        }

        this.beginTime = beginTime;
        this.endTime = endTime;
    }


    @Override
    public final TaskStatus getStatus()
    {
        return status;
    }


    @Override
    public final int getTaskIdx()
    {
        return taskIdx;
    }


    @Override
    public final Schedule<? extends AbstractTask> getSchedule()
    {
        return schedule;
    }


    @Override
    public final double getBeginTime()
    {
        return beginTime;
    }


    @Override
    public final double getEndTime()
    {
        return endTime;
    }


    @Override
    public void setBeginTime(double beginTime)
    {
        if (status == TaskStatus.STARTED || status == TaskStatus.PERFORMED) {
            throw new IllegalStateException("It is too late to change the beginTime");
        }

        this.beginTime = beginTime;
    }


    @Override
    public void setEndTime(double endTime)
    {
        if (status == TaskStatus.PERFORMED) {
            throw new IllegalStateException("It is too late to change the endTime");
        }

        this.endTime = endTime;
    }


    @Override
    public TaskTracker getTaskTracker()
    {
        if (status != TaskStatus.STARTED) {
            throw new IllegalStateException("Allowed only for STARTED tasks");
        }

        return taskTracker;
    }


    @Override
    public void initTaskTracker(TaskTracker taskTracker)
    {
        if (this.taskTracker != null) {
            throw new IllegalStateException("Tracking already initialized");
        }

        if (status != TaskStatus.STARTED) {
            throw new IllegalStateException("Allowed only for STARTED tasks");
        }

        this.taskTracker = taskTracker;
    }


    protected String commonToString()
    {
        return " [" + beginTime + " : " + endTime + "]";
    }
}