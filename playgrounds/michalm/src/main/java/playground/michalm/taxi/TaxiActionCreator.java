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
    implements VrpAgentLogic.ActionCreator
{
    private final VrpSimEngine vrpSimEngine;


    public TaxiActionCreator(VrpSimEngine vrpSimEngine)
    {
        this.vrpSimEngine = vrpSimEngine;
    }


    @Override
    public DynAction createAction(Task task, double now)
    {
        TaxiTask tt = (TaxiTask)task;

        switch (tt.getTaxiTaskType()) {
            case PICKUP_DRIVE:
            case DROPOFF_DRIVE:
            case CRUISE_DRIVE:
                return new VrpDynLeg((DriveTask)task);

            case PICKUP_STAY:
                TaxiPickupStayTask pst = (TaxiPickupStayTask)task;
                PassengerHandlingUtils.pickUpPassenger(vrpSimEngine, task, pst.getRequest(), now);
                return new VrpActivity("ServeTask" + pst.getRequest().getId(), pst);

            case DROPOFF_STAY:
                TaxiDropoffStayTask dst = (TaxiDropoffStayTask)task;
                PassengerHandlingUtils.dropOffPassenger(vrpSimEngine, dst, dst.getRequest(), now);
                return new VrpActivity("ServeTask" + dst.getRequest().getId(), dst);

            case WAIT_STAY:
                return new VrpActivity("WaitTask", (TaxiWaitStayTask)task);

            default:
                throw new IllegalStateException();
        }
    }
}
