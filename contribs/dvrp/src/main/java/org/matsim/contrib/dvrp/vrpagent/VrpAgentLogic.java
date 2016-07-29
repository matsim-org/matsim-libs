/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.vrpagent;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dynagent.*;


public class VrpAgentLogic
    implements DynAgentLogic
{
    public static final String BEFORE_SCHEDULE_ACTIVITY_TYPE = "BeforeVrpSchedule";
    public static final String AFTER_SCHEDULE_ACTIVITY_TYPE = "AfterVrpSchedule";


    public interface DynActionCreator
    {
        DynAction createAction(Task task, double now);
    }


    private final VrpOptimizer optimizer;
    private final DynActionCreator dynActionCreator;
    private final Vehicle vehicle;
    private DynAgent agent;


    public VrpAgentLogic(VrpOptimizer optimizer, DynActionCreator dynActionCreator, Vehicle vehicle)
    {
        this.optimizer = optimizer;
        this.dynActionCreator = dynActionCreator;

        this.vehicle = vehicle;
        this.vehicle.setAgentLogic(this);
    }


    @Override
    public DynActivity computeInitialActivity(DynAgent dynAgent)
    {
        this.agent = dynAgent;
        return createBeforeScheduleActivity();// INITIAL ACTIVITY (activate the agent in QSim)
    }


    @Override
    public DynAgent getDynAgent()
    {
        return agent;
    }


    public Vehicle getVehicle()
    {
        return vehicle;
    }


    @Override
    public DynAction computeNextAction(DynAction oldAction, double now)
    {
        Schedule<?> schedule = vehicle.getSchedule();

        if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
            return createAfterScheduleActivity();// FINAL ACTIVITY (deactivate the agent in QSim)
        }
        // else: PLANNED or STARTED

        optimizer.nextTask(schedule);
        // remember to REFRESH status (after nextTask -> now it can be COMPLETED)!!!

        if (schedule.getStatus() == ScheduleStatus.COMPLETED) {// no more tasks
            return createAfterScheduleActivity();// FINAL ACTIVITY (deactivate the agent in QSim)
        }

        Task task = schedule.getCurrentTask();
        DynAction action = dynActionCreator.createAction(task, now);

        return action;
    }


    private DynActivity createBeforeScheduleActivity()
    {
        return new AbstractDynActivity(BEFORE_SCHEDULE_ACTIVITY_TYPE) {
            public double getEndTime()
            {
                Schedule<?> s = vehicle.getSchedule();

                switch (s.getStatus()) {
                    case PLANNED:
                        return s.getBeginTime();
                    case UNPLANNED:
                        return vehicle.getT1();
                    default:
                        throw new IllegalStateException();
                }
            }
        };
    }


    private DynActivity createAfterScheduleActivity()
    {
        return new StaticDynActivity(AFTER_SCHEDULE_ACTIVITY_TYPE, Double.POSITIVE_INFINITY);
    }
}
