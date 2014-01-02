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

package playground.michalm.taxi;

import org.matsim.contrib.dvrp.VrpSimEngine;
import org.matsim.contrib.dvrp.passenger.PassengerHandlingUtils;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dynagent.DynAction;

import pl.poznan.put.vrp.dynamic.data.schedule.*;
import playground.michalm.taxi.schedule.*;


public class TaxiActionCreator
    implements VrpAgentLogic.DynActionCreator
{
    private final VrpSimEngine vrpSimEngine;
    private final boolean onlineVehicleTracker;


    public TaxiActionCreator(VrpSimEngine vrpSimEngine, boolean onlineVehicleTracker)
    {
        this.vrpSimEngine = vrpSimEngine;
        this.onlineVehicleTracker = onlineVehicleTracker;
    }


    @Override
    public DynAction createAction(final Task task, double now)
    {
        TaxiTask tt = (TaxiTask)task;

        switch (tt.getTaxiTaskType()) {
            case PICKUP_DRIVE:
            case DROPOFF_DRIVE:
            case CRUISE_DRIVE:
                if (onlineVehicleTracker) {
                    return VrpDynLeg.createLegWithOnlineVehicleTracker((DriveTask)task,
                            vrpSimEngine);
                }
                else {
                    return VrpDynLeg.createLegWithOfflineVehicleTracker((DriveTask)task);
                }

            case PICKUP_STAY:
                final TaxiPickupStayTask pst = (TaxiPickupStayTask)task;

                return new VrpActivity("ServeTask" + pst.getRequest().getId(), pst) {
                    public void endAction(double now)
                    {
                        PassengerHandlingUtils.pickUpPassenger(vrpSimEngine, task,
                                pst.getRequest(), now);
                    }
                };

            case DROPOFF_STAY:
                final TaxiDropoffStayTask dst = (TaxiDropoffStayTask)task;

                return new VrpActivity("ServeTask" + dst.getRequest().getId(), dst) {
                    public void endAction(double now)
                    {
                        PassengerHandlingUtils.dropOffPassenger(vrpSimEngine, dst,
                                dst.getRequest(), now);
                    }
                };

            case WAIT_STAY:
                return new VrpActivity("WaitTask", (TaxiWaitStayTask)task);

            default:
                throw new IllegalStateException();
        }
    }
}
