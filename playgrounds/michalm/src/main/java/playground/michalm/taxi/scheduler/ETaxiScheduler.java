/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.scheduler;

import java.util.*;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.schedule.*;
import org.matsim.contrib.taxi.scheduler.*;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.*;

import playground.michalm.ev.data.Charger;
import playground.michalm.taxi.data.EvrpVehicle;
import playground.michalm.taxi.data.EvrpVehicle.Ev;
import playground.michalm.taxi.ev.ETaxiChargingLogic;
import playground.michalm.taxi.schedule.ETaxiChargingTask;


public class ETaxiScheduler
    extends TaxiScheduler
{
    public ETaxiScheduler(Scenario scenario, TaxiData taxiData, MobsimTimer timer,
            TaxiSchedulerParams params, TravelTime travelTime, TravelDisutility travelDisutility)
    {
        super(scenario, taxiData, timer, params, travelTime, travelDisutility);
    }


    @Override
    protected double calcNewEndTime(TaxiTask task, double newBeginTime)
    {
        if (task instanceof ETaxiChargingTask) {
            //FIXME underestimated due to the ongoing AUX/drive consumption
            double duration = task.getEndTime() - task.getBeginTime();
            return newBeginTime + duration;
        }
        else {
            return super.calcNewEndTime(task, newBeginTime);
        }
    }

    //FIXME underestimated due to the ongoing AUX/drive consumption
    //not a big issue for e-rule-based dispatching (no look ahead)
    //more problematic for e-assignment dispatching
    public void scheduleCharging(EvrpVehicle vehicle, Charger charger,
            VrpPathWithTravelData vrpPath)
    {
        Schedule<TaxiTask> schedule = TaxiSchedules.asTaxiSchedule(vehicle.getSchedule());
        divertOrAppendDrive(schedule, vrpPath);

        ETaxiChargingLogic logic = (ETaxiChargingLogic)charger.getLogic();
        Ev ev = vehicle.getEv();
        double chargingEndTime = vrpPath.getArrivalTime() + logic.estimateMaxWaitTimeOnArrival()
                + logic.estimateChargeTime(ev);
        schedule.addTask(
                new ETaxiChargingTask(vrpPath.getArrivalTime(), chargingEndTime, charger, ev));

        appendStayTask(schedule);
    }


    //=========================================================================================

    private boolean chargingTasksRemovalMode = false;
    private List<Vehicle> vehiclesWithUnscheduledCharging;


    public void beginChargingTaskRemoval()
    {
        chargingTasksRemovalMode = true;
        vehiclesWithUnscheduledCharging = new ArrayList<>();
    }


    public List<Vehicle> endChargingTaskRemoval()
    {
        chargingTasksRemovalMode = false;
        return vehiclesWithUnscheduledCharging;
    }


    // Drives-to-chargers can be diverted if diversion is on.
    // Otherwise, we do not remove stays-at-chargers from schedules.
    @Override
    protected Integer countUnremovablePlannedTasks(Schedule<TaxiTask> schedule)
    {
        TaxiTask currentTask = schedule.getCurrentTask();
        switch (currentTask.getTaxiTaskType()) {
            case EMPTY_DRIVE:
                TaxiTask nextTask = TaxiSchedules.getNextTaxiTask(currentTask);
                if (! (nextTask instanceof ETaxiChargingTask)) {
                    return super.countUnremovablePlannedTasks(schedule);
                }

                if (params.vehicleDiversion) {
                    return 0;//though questionable
                }

                //if no diversion and driving to a charger then keep 'charge'
                return 1;

            case STAY:
                if (! (currentTask instanceof ETaxiChargingTask)) {
                    return super.countUnremovablePlannedTasks(schedule);
                }

                return 0;

            default:
                return super.countUnremovablePlannedTasks(schedule);
        }
    }


    // A vehicle doing 'charge' is not considered idle.
    // This is ensured by having at least one task (e.g. 'wait') following the 'charge'.
    //
    // Maybe for a more complex algos we would like to interrupt charging tasks as well.
    @Override
    protected void removePlannedTasks(Schedule<TaxiTask> schedule, int newLastTaskIdx)
    {
        List<TaxiTask> tasks = schedule.getTasks();

        for (int i = schedule.getTaskCount() - 1; i > newLastTaskIdx; i--) {
            TaxiTask task = tasks.get(i);

            if (!chargingTasksRemovalMode && task instanceof ETaxiChargingTask) {
                break;//cannot remove -> stop removing
            }

            schedule.removeTask(task);
            taskRemovedFromSchedule(schedule, task);
        }

        if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
            return;
        }

        TaxiTask lastTask = Schedules.getLastTask(schedule);
        if (lastTask instanceof ETaxiChargingTask) {
            //we must append 'wait' because both 'charge' and 'wait' are 'STAY' tasks,
            //so the standard TaxiScheduler cannot distinguish them and would treat 'charge' as 'wait'

            //we can use chargeEndTime for both begin and end times,
            //the right endTime is set in TaxiScheduler.removeAwaitingRequestsImpl()
            double chargeEndTime = lastTask.getEndTime();
            Link chargeLink = ((ETaxiChargingTask)lastTask).getLink();
            schedule.addTask(new TaxiStayTask(chargeEndTime, chargeEndTime, chargeLink));
        }
    }


    @Override
    protected void taskRemovedFromSchedule(Schedule<TaxiTask> schedule, TaxiTask task)
    {
        if (task instanceof ETaxiChargingTask) {
            ((ETaxiChargingTask)task).removeFromChargerLogic();
            vehiclesWithUnscheduledCharging.add(schedule.getVehicle());
        }
        else {
            super.taskRemovedFromSchedule(schedule, task);
        }
    }
}
