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

package playground.jbischoff.drt.scheduler;

import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.SinglePassengerDropoffActivity;
import org.matsim.contrib.dvrp.passenger.SinglePassengerPickupActivity;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.VrpActivity;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.contrib.dynagent.DynAction;

import playground.jbischoff.drt.scheduler.tasks.DrtDropoffTask;
import playground.jbischoff.drt.scheduler.tasks.DrtPickupTask;
import playground.jbischoff.drt.scheduler.tasks.DrtStayTask;
import playground.jbischoff.drt.scheduler.tasks.DrtTask;



public class DrtActionCreator
    implements VrpAgentLogic.DynActionCreator
{
    public static final String TAXIBUS_STAY_NAME = "DrtStay";
	public static final String TAXIBUS_DROPOFF_NAME = "DrtDropoff";
	public final static String TAXIBUS_PICKUP_NAME = "DrtPickup";
	private final PassengerEngine passengerEngine;
    private final VrpLegs.LegCreator legCreator;
    private final double pickupDuration;


    public DrtActionCreator(PassengerEngine passengerEngine, VrpLegs.LegCreator legCreator,
            double pickupDuration)
    {
        this.passengerEngine = passengerEngine;
        this.legCreator = legCreator;
        this.pickupDuration = pickupDuration;
    }


    @Override
    public DynAction createAction(final Task task, double now)
    {
        DrtTask tt = (DrtTask)task;

        switch (tt.getDrtTaskType()) {
            case DRIVE_EMPTY:
            case DRIVE_WITH_PASSENGER:
                return legCreator.createLeg((DriveTask)task);

            case PICKUP:
                final DrtPickupTask pst = (DrtPickupTask)task;
                return new SinglePassengerPickupActivity(passengerEngine, pst, pst.getRequest(),
                        pickupDuration, TAXIBUS_PICKUP_NAME);

            case DROPOFF:
                final DrtDropoffTask dst = (DrtDropoffTask)task;
                return new SinglePassengerDropoffActivity(passengerEngine, dst, dst.getRequest(), TAXIBUS_DROPOFF_NAME);

            case STAY:
                return new VrpActivity(TAXIBUS_STAY_NAME, (DrtStayTask)task);

            default:
                throw new IllegalStateException();
        }
    }
}
