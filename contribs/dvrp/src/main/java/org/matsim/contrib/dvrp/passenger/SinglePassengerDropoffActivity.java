/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.passenger;

import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.vrpagent.VrpActivity;
import org.matsim.contrib.dynagent.DynAgent;


public class SinglePassengerDropoffActivity
    extends VrpActivity
{
    private final PassengerEngine passengerEngine;
    private final StayTask dropoffTask;
    private final PassengerRequest request;


    public SinglePassengerDropoffActivity(PassengerEngine passengerEngine, StayTask dropoffTask,
            PassengerRequest request, String activityType)
    {
        super(activityType, dropoffTask);

        this.passengerEngine = passengerEngine;
        this.dropoffTask = dropoffTask;
        this.request = request;
    }


    @Override
    public void finalizeAction(double now)
    {
        DynAgent driver = dropoffTask.getSchedule().getVehicle().getAgentLogic().getDynAgent();
        passengerEngine.dropOffPassenger(driver, request, now);
    }
}
