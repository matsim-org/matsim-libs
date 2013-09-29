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
import playground.michalm.taxi.optimizer.schedule.TaxiDriveTask;


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
        switch (task.getType()) {
            case DRIVE: // driving both with and without a passenger
                final TaxiDriveTask tdt = (TaxiDriveTask)task;

                switch (tdt.getDriveType()) {
                    case PICKUP:
                    case CRUISE:
                        return new VrpDynLeg(tdt);

                    case DELIVERY:
                        return new VrpDynLeg(tdt) {
                            public void endAction(double now)
                            {
                                PassengerHandlingUtils.dropOffPassenger(vrpSimEngine, tdt,
                                        tdt.getRequest(), now);
                            }
                        };

                    default:
                        throw new IllegalStateException("Not supported enum type");
                }

            case SERVE: // pick up a passenger
                ServeTask st = (ServeTask)task;
                PassengerHandlingUtils.pickUpPassenger(vrpSimEngine, task, st.getRequest(), now);
                return new VrpActivity("ServeTask" + st.getRequest().getId(), st);

            case WAIT:
                return new VrpActivity("WaitTask", (WaitTask)task);

            default:
                throw new IllegalStateException();
        }
    }
}
