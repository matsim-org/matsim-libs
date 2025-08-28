/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.scheduler;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.schedule.*;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.DRIVE;
import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.getBaseTypeOrElseThrow;

/**
 * @author michalm
 */
public class EmptyVehicleRelocator {
    public static final DrtTaskType RELOCATE_VEHICLE_TASK_TYPE = new DrtTaskType("RELOCATE", DRIVE);
    public static final DrtTaskType RELOCATE_VEHICLE_TO_DEPOT_TASK_TYPE = new DrtTaskType("RELOCATE_TO_DEPOT", DRIVE);

    private final TravelTime travelTime;
    private final MobsimTimer timer;
    private final DrtTaskFactory taskFactory;
    private final LeastCostPathCalculator router;

    public EmptyVehicleRelocator(Network network, TravelTime travelTime, TravelDisutility travelDisutility,
                                 MobsimTimer timer, DrtTaskFactory taskFactory) {
        this.travelTime = travelTime;
        this.timer = timer;
        this.taskFactory = taskFactory;
        router = new SpeedyALTFactory().createPathCalculator(network, travelDisutility, travelTime);
    }

    public void relocateVehicle(DvrpVehicle vehicle, Link link, DrtTaskType relocationTaskType) {
        DrtStayTask currentTask = (DrtStayTask) vehicle.getSchedule().getCurrentTask();
        Link currentLink = currentTask.getLink();

        if (currentLink != link) {
            VrpPathWithTravelData relocationPath = VrpPaths.calcAndCreatePath(
                    currentLink, link, timer.getTimeOfDay(), router, travelTime);

            if (relocationPath.getArrivalTime() < vehicle.getServiceEndTime()) {

                Schedule schedule = vehicle.getSchedule();
                int nextIdx = currentTask.getTaskIdx() + 1;

                // follow up tasks
                if (schedule.getTaskCount() > nextIdx) {
                    Task nextTask = schedule.getTasks().get(nextIdx);

                    Link nextLink;
                    double nextFixedTime;

                    switch (getBaseTypeOrElseThrow(nextTask)) {
                        case DRIVE -> {
                            nextLink = ((DriveTask) nextTask).getPath().getToLink();
                            nextFixedTime = nextTask.getEndTime();
                        }
                        case STOP -> {
                            nextLink = ((DrtStopTask) nextTask).getLink();
                            Gbl.assertIf(nextLink == currentLink);
                            nextFixedTime = nextTask.getBeginTime();
                        }
                        case STAY -> throw new IllegalStateException("Did not expect two consecutive STAY tasks");
                        default -> throw new IllegalStateException("Unexpected base type");
                    }

                    if(relocationPath.getArrivalTime() > nextFixedTime) {
                        return;
                    }

                    double followUpArrivalTime = relocationPath.getArrivalTime();

                    if (nextLink != relocationPath.getToLink()) {
                        VrpPathWithTravelData followUpPath = VrpPaths.calcAndCreatePath(
                                relocationPath.getToLink(), nextLink, relocationPath.getArrivalTime(), router, travelTime);
                        followUpArrivalTime += followUpPath.getTravelTime();

                        if (followUpArrivalTime > nextFixedTime) {
                            // relocation would make us late for next fixed task
                            return;
                        }

                        if(nextTask instanceof DriveTask) {
                            // remove replaced drive
                            schedule.removeTask(nextTask);
                        }

                        double slack = nextFixedTime - followUpArrivalTime;
                        relocateVehicleSubtourImpl(nextIdx, vehicle, relocationPath, slack, relocationTaskType, followUpPath);
                    } else {
                        // relocation destination already matches next target
                        if(nextTask instanceof DriveTask) {
                            // remove replaced drive
                            schedule.removeTask(nextTask);
                        }
                        double slack = nextFixedTime - followUpArrivalTime;
                        relocateVehicleSubtourImpl(nextIdx, vehicle, relocationPath, slack, relocationTaskType, null);
                    }

                } else {
                    // last task → simple relocation to target
                    relocateVehicleSimpleImpl(vehicle, relocationPath, relocationTaskType);
                }
            }
        }
    }

    private void relocateVehicleSubtourImpl(int index, DvrpVehicle vehicle,
                                            VrpPathWithTravelData relocatePath, double slack,
                                            DrtTaskType relocationTaskType,
                                            VrpPathWithTravelData followUpPathOrNull) {
        Schedule schedule = vehicle.getSchedule();
        DrtStayTask stayTask = (DrtStayTask) schedule.getCurrentTask();

        // finish current STAY
        stayTask.setEndTime(relocatePath.getDepartureTime());

        // insert RELOCATE drive
        schedule.addTask(index, taskFactory.createDriveTask(vehicle, relocatePath, relocationTaskType));

        // insert STAY during slack at relocation destination
        double followUpDepartureTime = relocatePath.getArrivalTime() + slack;
        schedule.addTask(index + 1, taskFactory.createStayTask(
                vehicle, relocatePath.getArrivalTime(), followUpDepartureTime, relocatePath.getToLink()));

        // optional drive back to original plan target, AFTER the slack STAY
        if (followUpPathOrNull != null) {
            VrpPathWithTravelData followUpPath = followUpPathOrNull.withDepartureTime(followUpDepartureTime);
            schedule.addTask(index + 2, taskFactory.createDriveTask(vehicle, followUpPath, DrtDriveTask.TYPE));
        }
    }

    private void relocateVehicleSimpleImpl(DvrpVehicle vehicle, VrpPathWithTravelData vrpPath, DrtTaskType relocationTaskType) {
        Schedule schedule = vehicle.getSchedule();
        DrtStayTask stayTask = (DrtStayTask)schedule.getCurrentTask();
        if (stayTask.getTaskIdx() != schedule.getTaskCount() - 1) {
            throw new IllegalStateException("The current STAY task is not last. Not possible without prebooking");
        }

        stayTask.setEndTime(vrpPath.getDepartureTime()); // finish STAY
        schedule.addTask(taskFactory.createDriveTask(vehicle, vrpPath, relocationTaskType)); // add RELOCATE
        // append STAY
        schedule.addTask(taskFactory.createStayTask(vehicle, vrpPath.getArrivalTime(), vehicle.getServiceEndTime(),
                vrpPath.getToLink()));
    }
}
