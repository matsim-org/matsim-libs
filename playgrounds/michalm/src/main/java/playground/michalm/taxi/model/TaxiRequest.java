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

package playground.michalm.taxi.model;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.model.Customer;
import org.matsim.contrib.dvrp.data.model.impl.RequestImpl;
import org.matsim.contrib.dvrp.data.schedule.Task.TaskStatus;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.core.mobsim.framework.MobsimAgent;

import playground.michalm.taxi.schedule.*;


public class TaxiRequest
    extends RequestImpl
    implements PassengerRequest
{
    public enum TaxiRequestStatus
    {
        //INACTIVE("I"), // invisible to the dispatcher (ARTIFICIAL STATE!)
        UNPLANNED("U"), // submitted by the CUSTOMER and received by the DISPATCHER
        PLANNED("P"), // planned - included into one of the routes
        STARTED("S"), // vehicle starts serving
        PERFORMED("PE"), //
        //REJECTED("R"), // rejected by the DISPATCHER
        //CANCELLED("C"), // canceled by the CUSTOMER
        ;

        public final String shortName;


        private TaxiRequestStatus(String shortName)
        {
            this.shortName = shortName;
        }
    };


    private final Link fromLink;
    private final Link toLink;

    private TaxiPickupDriveTask pickupDriveTask;
    private TaxiPickupStayTask pickupStayTask;
    private TaxiDropoffDriveTask dropoffDriveTask;
    private TaxiDropoffStayTask dropoffStayTask;


    public TaxiRequest(Id id, Customer customer, Link fromLink, Link toLink, int t0,
            int submissionTime)
    {
        super(id, customer, 1, t0, t0, submissionTime);
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
    public MobsimAgent getPassengerAgent()
    {
        return ((PassengerCustomer)getCustomer()).getPassengerAgent();
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

        if (pickupDriveTask.getStatus() == TaskStatus.PLANNED) {
            return TaxiRequestStatus.PLANNED;
        }

        if (dropoffStayTask != null && dropoffStayTask.getStatus() == TaskStatus.PERFORMED) {
            return TaxiRequestStatus.PERFORMED;
        }

        return TaxiRequestStatus.STARTED;
    }
}
