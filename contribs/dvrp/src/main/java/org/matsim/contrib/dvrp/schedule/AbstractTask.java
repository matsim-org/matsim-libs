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



public abstract class AbstractTask
    implements Task
{
    // ==== BEGIN: fields managed by ScheduleImpl
    /*package*/Schedule<? extends AbstractTask> schedule;
    /*package*/int taskIdx;

    /*package*/TaskStatus status;
    // ==== END: fields managed by ScheduleImpl

    private double beginTime;
    private double endTime;


    public AbstractTask(double beginTime, double endTime)
    {
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
        if (status != TaskStatus.PLANNED) { // PERFORMED or STARTED
            throw new IllegalStateException("Allowed only for PLANNED tasks");
        }

        this.beginTime = beginTime;
    }


    @Override
    public void setEndTime(double endTime)
    {
        if (status != TaskStatus.PLANNED && status != TaskStatus.STARTED) { // PERFORMED
            throw new IllegalStateException("Allowed only for PLANNED and STARTED tasks");
        }

        this.endTime = endTime;
    }


    protected String commonToString()
    {
        return " [" + beginTime + " : " + endTime + "]";
    }
}