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

package org.matsim.contrib.dvrp.examples.dapp;

import org.matsim.contrib.dvrp.VrpSimEngine;
import org.matsim.contrib.dvrp.passenger.PassengerCustomer;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.core.mobsim.framework.MobsimAgent;

import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.extensions.vrppd.schedule.DeliveryTask;


public class DAPPActionCreator
    implements VrpAgentLogic.DynActionCreator
{
    private final VrpSimEngine vrpSimEngine;


    public DAPPActionCreator(VrpSimEngine vrpSimEngine)
    {
        this.vrpSimEngine = vrpSimEngine;
    }


    @Override
    public DynAction createAction(final Task task, double now)
    {
        switch (task.getType()) {
            case DRIVE:
                return new VrpDynLeg((DriveTask)task);

            case STAY://Delivery or waiting
                if (task instanceof DeliveryTask) {
                    return new VrpActivity("StayTask", (StayTask)task) {
                        
                        //after leg="dial_a_pizza" ends (== the pizza arrives) we need to get back home and start eating it
                        @Override
                        public void endAction(double now)
                        {
                            DeliveryTask dt = (DeliveryTask)task;
                            MobsimAgent passenger = PassengerCustomer.getPassenger(dt.getRequest());
                            passenger.notifyArrivalOnLinkByNonNetworkMode(passenger
                                    .getDestinationLinkId());
                            passenger.endLegAndComputeNextState(now);
                            vrpSimEngine.getInternalInterface().arrangeNextAgentState(passenger);

                            System.out.println("Pizza delivered to customer: " + passenger.getId() + " at time: " + (int)now);
                        }
                    };
                }
                else {
                    return new VrpActivity("StayTask", (StayTask)task);
                }

        }

        throw new RuntimeException();
    }
}
