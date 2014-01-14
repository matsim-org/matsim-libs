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

package org.matsim.contrib.dvrp.examples.onetaxi;

import org.matsim.contrib.dvrp.VrpSimEngine;
import org.matsim.contrib.dvrp.data.schedule.*;
import org.matsim.contrib.dvrp.passenger.PassengerHandlingUtils;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dynagent.DynAction;


public class OneTaxiActionCreator
    implements VrpAgentLogic.DynActionCreator
{
    private final VrpSimEngine vrpSimEngine;


    public OneTaxiActionCreator(VrpSimEngine vrpSimEngine)
    {
        this.vrpSimEngine = vrpSimEngine;
    }


    @Override
    public DynAction createAction(final Task task, double now)
    {
        switch (task.getType()) {
            case DRIVE:
                return VrpDynLegs.createLegWithOfflineVehicleTracker((DriveTask)task);

            case STAY:
                if (task instanceof OneTaxiServeTask) { //PICKUP or DROPOFF
                    final OneTaxiServeTask serveTask = (OneTaxiServeTask)task;
                    final OneTaxiRequest request = serveTask.getRequest();

                    if (serveTask.isPickup()) {
                        return new VrpActivity("ServeTask" + request.getId(), serveTask) {
                            public void endAction(double now)
                            {
                                PassengerHandlingUtils.pickUpPassenger(vrpSimEngine, serveTask,
                                        request, now);
                            }
                        };

                    }
                    else {
                        return new VrpActivity("ServeTask" + request.getId(), serveTask) {
                            public void endAction(double now)
                            {
                                PassengerHandlingUtils.dropOffPassenger(vrpSimEngine, serveTask,
                                        request, now);
                            }
                        };
                    }
                }
                else { //WAIT
                    return new VrpActivity("StayTask", (StayTask)task);
                }
        }

        throw new RuntimeException();
    }
}
