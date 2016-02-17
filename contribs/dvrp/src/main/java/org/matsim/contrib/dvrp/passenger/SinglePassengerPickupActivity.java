/* *********************************************************************** *
 *                                                                         *
 * project: org.matsim.*
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
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;


public class SinglePassengerPickupActivity
    implements PassengerPickupActivity
{
    private final PassengerEngine passengerEngine;
    private final StayTask pickupTask;
    private final PassengerRequest request;
    private final double pickupDuration;

    private boolean passengerAboard = false;
    private boolean passengerWaiting = false;
    private double endTime;


    public SinglePassengerPickupActivity(PassengerEngine passengerEngine, StayTask pickupTask,
            PassengerRequest request, double pickupDuration)
    {
        this.passengerEngine = passengerEngine;
        this.pickupTask = pickupTask;
        this.request = request;
        this.pickupDuration = pickupDuration;
        
        // Only happens for direct requests (not prescheduled)
		tryPickupPassenger(pickupTask.getBeginTime());
		
        if (!passengerAboard) {
            //try to predict the end time and stay alive
            endTime = Math.max(pickupTask.getBeginTime(), request.getT0()) + pickupDuration + 1.0;
        }
    }


    @Override
    public void finalizeAction(double now)
    {}


    @Override
    public double getEndTime()
    {
        return endTime;
    }


    @Override
    public String getActivityType()
    {
        return "PassengerPickup";
    }
    
    private void tryPickupPassenger(double now) {
		DynAgent driver = pickupTask.getSchedule().getVehicle().getAgentLogic().getDynAgent();
		passengerAboard = passengerEngine.pickUpPassenger(this, driver, request, now);
		
		if (passengerAboard) {
			endTime = now + pickupDuration;
		}
    }

    @Override
    public void doSimStep(double now)
    {
    	if (passengerWaiting && !passengerAboard) {
    		tryPickupPassenger(now);
    	}
    	
        if (!passengerAboard) {
            // try to predict the end time and stay alive
            endTime = Math.max(now, request.getT0()) + pickupDuration + 1.0;
        }
    }


    @Override
    public void notifyPassengerIsReadyForDeparture(MobsimPassengerAgent passenger, double now)
    {
        if (passenger.equals(request.getPassenger())) {
            throw new IllegalArgumentException("I am waiting for a different passenger!");
        }
        
        passengerWaiting = true;
    }
}
