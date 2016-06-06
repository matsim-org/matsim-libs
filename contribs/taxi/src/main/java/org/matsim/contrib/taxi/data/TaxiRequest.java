/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.taxi.schedule.*;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;


public class TaxiRequest
    extends RequestImpl
    implements PassengerRequest
{
    public enum TaxiRequestStatus
    {
        //INACTIVE, // invisible to the dispatcher (ARTIFICIAL STATE!)
        UNPLANNED, // submitted by the CUSTOMER and received by the DISPATCHER
        PLANNED, // planned - included into one of the routes

        //we have started serving the request but we may still divert the cab
        TAXI_DISPATCHED,

        //we have to carry out the request
        PICKUP, RIDE, DROPOFF,

        PERFORMED, //
        //REJECTED, // rejected by the DISPATCHER
        //CANCELLED, // canceled by the CUSTOMER
        ;
    };


    private final MobsimPassengerAgent passenger;
    private final Link fromLink;
    private Link toLink;//toLink may be provided during the pickup

    private TaxiPickupTask pickupTask;
    private TaxiOccupiedDriveTask occupiedDriveTask;
    private TaxiDropoffTask dropoffTask;


    public TaxiRequest(Id<Request> id, MobsimPassengerAgent passenger, Link fromLink,
            double t0, double submissionTime)
    {
        this(id, passenger, fromLink, null, t0, submissionTime);
    }

    public TaxiRequest(Id<Request> id, MobsimPassengerAgent passenger, Link fromLink, Link toLink,
            double t0, double submissionTime)
    {
        super(id, 1, t0, t0, submissionTime);
        this.passenger = passenger;
        this.fromLink = fromLink;
        this.toLink = toLink;
    }


    @Override
    public Link getFromLink()
    {
        return fromLink;
    }


    @Override
    public Link getToLink()
    {
        return toLink;
    }


    public void setToLink(Link toLink)
    {
        this.toLink = toLink;
    }


    @Override
    public MobsimPassengerAgent getPassenger()
    {
        return passenger;
    }


    public TaxiPickupTask getPickupTask()
    {
        return pickupTask;
    }


    public void setPickupTask(TaxiPickupTask pickupTask)
    {
        this.pickupTask = pickupTask;
    }


    public TaxiOccupiedDriveTask getDriveWithPassengerTask()
    {
        return occupiedDriveTask;
    }


    public void setOccupiedDriveTask(TaxiOccupiedDriveTask occupiedDriveTask)
    {
        this.occupiedDriveTask = occupiedDriveTask;
    }


    public TaxiDropoffTask getDropoffTask()
    {
        return dropoffTask;
    }


    public void setDropoffTask(TaxiDropoffTask dropoffTask)
    {
        this.dropoffTask = dropoffTask;
    }


    public TaxiRequestStatus getStatus()
    {
        if (pickupTask == null) {
            return TaxiRequestStatus.UNPLANNED;
        }

        switch (pickupTask.getStatus()) {
            case PLANNED:
                if (pickupTask.getSchedule().getStatus() == ScheduleStatus.PLANNED) {
                    return TaxiRequestStatus.PLANNED;
                }
                
                TaxiTask currentTask = (TaxiTask)pickupTask.getSchedule().getCurrentTask();
                if (currentTask.getTaxiTaskType() == TaxiTaskType.EMPTY_DRIVE && //
                        pickupTask.getTaskIdx() == currentTask.getTaskIdx() + 1) {
                    return TaxiRequestStatus.TAXI_DISPATCHED;
                }

                return TaxiRequestStatus.PLANNED;

            case STARTED:
                return TaxiRequestStatus.PICKUP;

            case PERFORMED://continue
        }

        if (occupiedDriveTask.getStatus() == TaskStatus.STARTED) {
            return TaxiRequestStatus.RIDE;
        }

        switch (dropoffTask.getStatus()) {
            case STARTED:
                return TaxiRequestStatus.DROPOFF;

            case PERFORMED:
                return TaxiRequestStatus.PERFORMED;

            case PLANNED://illegal
        }

        throw new IllegalStateException("Unreachable code");
    }
}
