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

import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dynagent.DynAction;

import playground.michalm.taxi.schedule.*;


public class TaxiActionCreator
    implements VrpAgentLogic.DynActionCreator
{
    private final PassengerEngine passengerEngine;
    private final VrpDynLegs.LegCreator legCreator;
    private final double pickupDuration;


    public TaxiActionCreator(PassengerEngine passengerEngine, VrpDynLegs.LegCreator legCreator,
            double pickupDuration)
    {
        this.passengerEngine = passengerEngine;
        this.legCreator = legCreator;
        this.pickupDuration = pickupDuration;
    }


    @Override
    public DynAction createAction(final Task task, double now)
    {
        TaxiTask tt = (TaxiTask)task;

        switch (tt.getTaxiTaskType()) {
            case PICKUP_DRIVE:
            case DROPOFF_DRIVE:
            case CRUISE_DRIVE:
                return legCreator.createLeg((DriveTask)task);

            case PICKUP_STAY:
                final TaxiPickupStayTask pst = (TaxiPickupStayTask)task;
                return new SinglePassengerPickupActivity(passengerEngine, pst, pst.getRequest(),
                        pickupDuration);

            case DROPOFF_STAY:
                final TaxiDropoffStayTask dst = (TaxiDropoffStayTask)task;
                return new SinglePassengerDropoffActivity(passengerEngine, dst, dst.getRequest());

            case WAIT_STAY:
                return new VrpActivity("Waiting", (TaxiWaitStayTask)task);

            default:
                throw new IllegalStateException();
        }
    }
}
