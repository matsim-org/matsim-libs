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

package playground.michalm.taxi.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.RequestImpl;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

import playground.michalm.taxi.schedule.TaxiDropoffDriveTask;
import playground.michalm.taxi.schedule.TaxiDropoffStayTask;
import playground.michalm.taxi.schedule.TaxiPickupDriveTask;
import playground.michalm.taxi.schedule.TaxiPickupStayTask;


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
        PICKUP_DRIVE,

        //we have to carry out the request
        PICKUP_STAY, DROPOFF_DRIVE, DROPOFF_STAY,

        PERFORMED, //
        //REJECTED, // rejected by the DISPATCHER
        //CANCELLED, // canceled by the CUSTOMER
        ;
    };


    private final MobsimPassengerAgent passenger;
    private final Link fromLink;
    private final Link toLink;

    private TaxiPickupDriveTask pickupDriveTask;
    private TaxiPickupStayTask pickupStayTask;
    private TaxiDropoffDriveTask dropoffDriveTask;
    private TaxiDropoffStayTask dropoffStayTask;


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


    @Override
    public MobsimPassengerAgent getPassenger()
    {
        return passenger;
    }


    public TaxiPickupDriveTask getPickupDriveTask()
    {
        return pickupDriveTask;
    }


    public void setPickupDriveTask(TaxiPickupDriveTask pickupDriveTask)
    {
        this.pickupDriveTask = pickupDriveTask;
    }


    public TaxiPickupStayTask getPickupStayTask()
    {
        return pickupStayTask;
    }


    public void setPickupStayTask(TaxiPickupStayTask pickupStayTask)
    {
        this.pickupStayTask = pickupStayTask;
    }


    public TaxiDropoffDriveTask getDropoffDriveTask()
    {
        return dropoffDriveTask;
    }


    public void setDropoffDriveTask(TaxiDropoffDriveTask dropoffDriveTask)
    {
        this.dropoffDriveTask = dropoffDriveTask;
    }


    public TaxiDropoffStayTask getDropoffStayTask()
    {
        return dropoffStayTask;
    }


    public void setDropoffStayTask(TaxiDropoffStayTask dropoffStayTask)
    {
        this.dropoffStayTask = dropoffStayTask;
    }


    public TaxiRequestStatus getStatus()
    {
        if (pickupDriveTask == null) {
            return TaxiRequestStatus.UNPLANNED;
        }

        switch (pickupDriveTask.getStatus()) {
            case PLANNED:
                return TaxiRequestStatus.PLANNED;

            case STARTED:
                return TaxiRequestStatus.PICKUP_DRIVE;

            case CANCELLED:
                //may happen after diverting vehicles or cancellation by the customer
                throw new IllegalStateException(
                        "Request.pickupDriveTask should not point to a cancelled task");

            case PERFORMED:
                //at some later stage...
        }

        switch (pickupStayTask.getStatus()) {
            case PLANNED:
                throw new IllegalStateException("Unreachable code");

            case STARTED:
                return TaxiRequestStatus.PICKUP_STAY;

            case CANCELLED:
                //may happen only after cancellation by the customer
                throw new IllegalStateException(
                        "Request.pickupStayTask should not point to a cancelled task");

            case PERFORMED:
                break;//at some later stage...
        }

        switch (dropoffDriveTask.getStatus()) {
            case PLANNED:
                throw new IllegalStateException("Unreachable code");

            case STARTED:
                return TaxiRequestStatus.DROPOFF_DRIVE;

            case CANCELLED:
                throw new IllegalStateException("Cannot cancel at this stage");

            case PERFORMED:
                break;//at some later stage...
        }

        switch (dropoffStayTask.getStatus()) {
            case PLANNED:
                throw new IllegalStateException("Unreachable code");

            case STARTED:
                return TaxiRequestStatus.DROPOFF_STAY;

            case CANCELLED:
                throw new IllegalStateException("Cannot cancel at this stage");

            case PERFORMED:
                return TaxiRequestStatus.PERFORMED;
        }

        throw new IllegalStateException("Unreachable code");
    }
}
