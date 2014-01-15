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

import org.matsim.contrib.dvrp.VrpSimEngine;
import org.matsim.contrib.dvrp.data.schedule.*;
import org.matsim.contrib.dvrp.data.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dynagent.*;


public class VrpAgentLogic
    implements DynAgentLogic
{
    public interface DynActionCreator
    {
        DynAction createAction(Task task, double now);
    }


    private final VrpSimEngine vrpSimEngine;
    private final DynActionCreator dynActionCreator;
    private final VrpAgentVehicle vrpVehicle;
    private DynAgent agent;


    public VrpAgentLogic(VrpSimEngine vrpSimEngine, DynActionCreator dynActionCreator,
            VrpAgentVehicle vrpVehicle)
    {
        this.vrpSimEngine = vrpSimEngine;
        this.dynActionCreator = dynActionCreator;

        this.vrpVehicle = vrpVehicle;
        this.vrpVehicle.setAgentLogic(this);
    }


    @Override
    public DynActivity init(DynAgent dynAgent)
    {
        this.agent = dynAgent;
        return createBeforeScheduleActivity();// INITIAL ACTIVITY (activate the agent in QSim)
    }


    @Override
    public DynAgent getDynAgent()
    {
        return agent;
    }


    @Override
    public DynAction computeNextAction(DynAction oldAction, double now)
    {
        Schedule<?> schedule = vrpVehicle.getSchedule();

        if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
            return createAfterScheduleActivity();// FINAL ACTIVITY (deactivate the agent in QSim)
        }
        // else: PLANNED or STARTED

        vrpSimEngine.nextTask(vrpVehicle.getSchedule(), now);
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
        return new AbstractDynActivity("Before schedule: " + vrpVehicle.getId()) {
            public double getEndTime()
            {
                Schedule<?> s = vrpVehicle.getSchedule();

                switch (s.getStatus()) {
                    case PLANNED:
                        return s.getBeginTime();
                    case UNPLANNED:
                        return vrpVehicle.getT1();
                    default:
                        throw new IllegalStateException();
                }
            }
        };
    }


    private DynActivity createAfterScheduleActivity()
    {
        return new StaticDynActivity("After schedule: " + vrpVehicle.getId(),
                Double.POSITIVE_INFINITY);
    }


    @Override
    public void actionPossiblyChanged()
    {
        agent.update();
    }
}
